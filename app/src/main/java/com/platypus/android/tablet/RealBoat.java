package com.platypus.android.tablet;

import android.os.AsyncTask;
import android.util.Log;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.platypus.crw.CrumbListener;
import com.platypus.crw.FunctionObserver;
import com.platypus.crw.PoseListener;
import com.platypus.crw.SensorListener;
import com.platypus.crw.VehicleServer;
import com.platypus.crw.WaypointListener;
import com.platypus.crw.data.SensorData;
import com.platypus.crw.data.Twist;
import com.platypus.crw.data.UtmPose;
import com.platypus.crw.udp.UdpVehicleServer;

import org.jscience.geography.coordinates.LatLong;
import org.jscience.geography.coordinates.UTM;
import org.jscience.geography.coordinates.crs.ReferenceEllipsoid;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.measure.unit.NonSI;

/**
 * Created by jason on 7/12/17.
 */

public class RealBoat extends Boat
{
		private UdpVehicleServer server = null;
		private PoseListener pl;
		private SensorListener sl;
		private WaypointListener wl;
		private CrumbListener cl;
		private final int CONNECTION_POLL_S = 3;
		private final int WAYPOINTS_INDEX_POLL_S = 1;

		public RealBoat(String boat_name)
		{
				name = boat_name;
				server = new UdpVehicleServer();
		}

		private Runnable isConnectedPoll = new Runnable()
		{
				@Override
				public void run()
				{
						if (System.currentTimeMillis() - time_of_last_connection.get() > 1000)
						{
								server.isConnected(new FunctionObserver<Boolean>()
								{
										@Override
										public void completed(Boolean aBoolean)
										{
												Log.i(logTag, String.format("isConnected() returned %s", Boolean.toString(aBoolean)));
												setConnected(true);
										}

										@Override
										public void failed(FunctionError functionError)
										{
												Log.w(logTag, String.format("isConnected() did not return"));
												connected.set(false);
										}
								});
						}
						else
						{
								connected.set(true); // do not update time of last connection
								Log.i(logTag, "skipping isConnected() due to recent traffic");
						}
				}
		};
		private Runnable currentWaypointIndexPoll = new Runnable()
		{
				@Override
				public void run()
				{
						server.getWaypointsIndex(new FunctionObserver<Integer>()
						{
								@Override
								public void completed(Integer integer)
								{
										current_waypoint_index.set(integer);
								}

								@Override
								public void failed(FunctionError functionError)
								{
										Log.w(logTag, String.format("getWaypointsIndex() did not return"));
								}
						});
				}
		};



		@Override
		public void createListeners(final Runnable poseListenerCallback,
		                            final Runnable sensorListenerCallback,
		                            final Runnable waypointListenerCallback,
		                            final Runnable crumbListenerCallback)
		{
				pl = new PoseListener()
				{
						@Override
						public void receivedPose(UtmPose utmPose)
						{
								setConnected(true);

								// check if the boat does not have a GPS lock yet
								if (utmPose.equals(new UtmPose()))
								{
										Log.d(logTag, "Received default pose from boat. Ignoring.");
										return;
								}

								setYaw(Math.PI / 2 - utmPose.pose.getRotation().toYaw());
								double[] latlng = utmPose.getLatLong();
								Log.v(logTag, String.format("Received pose UtmPose = %s", utmPose.toString()));
								Log.v(logTag, String.format("Received pose lat = %f, lng = %f", latlng[0], latlng[1]));
								setLocation(new LatLng(latlng[0], latlng[1]));
								uiHandler.post(poseListenerCallback); // update GUI with result
						}
				};
				sl = new SensorListener()
				{
						@Override
						public void receivedSensor(SensorData sensorData, long index)
						{
								setConnected(true);
								synchronized (sensor_lock)
								{
										lastSensorDataReceived = sensorData;
								}
								sensors_ready.set(true);
								uiHandler.post(sensorListenerCallback); // update GUI with result

								// TODO: using memoryless sensordata transmission for now
								//server.acknowledgeSensorData(index, null); // don't need function observer at all
						}
				};
				wl = new WaypointListener()
				{
						@Override
						public void waypointUpdate(VehicleServer.WaypointState state)
						{
								setConnected(true);
								synchronized (waypoint_state_lock)
								{
										waypointState = state.toString();
								}
								uiHandler.post(waypointListenerCallback); // update GUI with result
						}
				};
				cl = new CrumbListener()
				{
						@Override
						public void receivedCrumb(double[] crumb, long index)
						{
								setConnected(true);

								// check if the index has been seen before
								if (!crumb_map.containsKey(index))
								{
										Log.i(logTag, String.format("Received new crumb, index %d", index));
										// add the index to the known ones
										crumb_map.put(index, crumb);
										// create a LatLng from it
										synchronized (crumb_lock)
										{
												new_crumb_LatLng = new LatLng(crumb[0], crumb[1]);
												new_crumb_UTM = UTM.latLongToUtm(
																LatLong.valueOf(crumb[0], crumb[1], NonSI.DEGREE_ANGLE),
																ReferenceEllipsoid.WGS84
												);
										}
										uiHandler.post(crumbListenerCallback); // update GUI with result
										server.acknowledgeCrumb(index, null); // don't need function observer at all
								}
								else
								{
										Log.d(logTag, "Received a previously known crumb, ignoring...");
								}
						}
				};
				try
				{
						if (pl != null)
						{
								server.addPoseListener(pl, new FunctionObserver<Void>()
								{
										@Override
										public void completed(Void aVoid)
										{
												Log.i(logTag, "addPoseListener");
										}

										@Override
										public void failed(FunctionError functionError) { }
								});
						}
						if (sl != null)
						{
								server.addSensorListener(sl, new FunctionObserver<Void>()
								{
										@Override
										public void completed(Void aVoid) { Log.i(logTag, "add sensor listener"); }

										@Override
										public void failed(FunctionError functionError) { }
								});
						}
						if (wl != null)
						{
								server.addWaypointListener(wl, new FunctionObserver<Void>()
								{
										@Override
										public void completed(Void aVoid) { Log.i(logTag, "add waypoint listener"); }

										@Override
										public void failed(FunctionError functionError) { }
								});
						}
						if (cl != null)
						{
								server.addCrumbListener(cl, new FunctionObserver<Void>()
								{
										@Override
										public void completed(Void aVoid) { Log.i(logTag, "add crumb listener"); }

										@Override
										public void failed(FunctionError functionError) { }
								});
						}
				}
				catch (Exception e)
				{
						Log.i(logTag, "Failed to add listener");
				}
				polling_thread_pool = new ScheduledThreadPoolExecutor(2);
				polling_thread_pool.scheduleAtFixedRate(
								isConnectedPoll, 0, CONNECTION_POLL_S, TimeUnit.SECONDS);
				polling_thread_pool.scheduleAtFixedRate(
								currentWaypointIndexPoll, 0, WAYPOINTS_INDEX_POLL_S, TimeUnit.SECONDS);
		}

		@Override
		public void startWaypoints(final double[][] waypoints, final Runnable failureCallback)
		{
				class StartWaypointsAsyncTask extends AsyncTask<Void, Void, Void>
				{
						@Override
						protected Void doInBackground(Void... params)
						{
								server.startWaypoints(waypoints, new FunctionObserver<Void>()
								{
										@Override
										public void completed(Void aVoid)
										{
												Log.i(logTag, "startwaypoints completed with " + "wp count = " + waypoints.length);
										}

										@Override
										public void failed(FunctionError functionError)
										{
												uiHandler.post(failureCallback); // the same as the more complicated AsyncTask.publishProgress()
												Log.e(logTag, "startwaypoints failed");
										}
								});
								return null;
						}
				}
				// first calls setAutonomous, then calls startWaypoints
				setAutonomous(true, failureCallback);
				new StartWaypointsAsyncTask().execute();
		}

		@Override
		public void stopWaypoints(final Runnable failureCallback)
		{
				class StopWaypointsAsyncTask extends AsyncTask<Void, Void, Void>
				{
						@Override
						protected Void doInBackground(Void... params)
						{
								server.stopWaypoints(new FunctionObserver<Void>()
								{
										@Override
										public void completed(Void aVoid)
										{
												Log.i(logTag, "Waypoints stopped");
										}

										@Override
										public void failed(FunctionError functionError)
										{
												Log.i(logTag, "Failed to stop waypoints");
												uiHandler.post(failureCallback);
										}
								});
								return null;
						}
				}
				// first calls setAutonomous, then calls stopWaypoints
				setAutonomous(false, failureCallback);
				new StopWaypointsAsyncTask().execute();
		}

		@Override
		public void updateControlSignals(final double thrust, final double heading, final Runnable failureCallback)
		{
				class UpdateControlSignalsAsyncTask extends AsyncTask<Void, Void, Void>
				{
						@Override
						protected Void doInBackground(Void... params)
						{
								Twist twist = new Twist();
								twist.dx(thrust);
								twist.drz(-1.0*heading); // left-right is backwards
								server.setVelocity(twist, new FunctionObserver<Void>()
								{
										@Override
										public void completed(Void aVoid)
										{
												setConnected(true);
												Log.i(logTag, String.format("Boat received joystick commands = %.2f, %.2f", thrust, heading));
										}

										@Override
										public void failed(FunctionError functionError)
										{
												uiHandler.post(failureCallback);
												Log.e(logTag, "Joystick msg timed out");
										}
								});
								return null;
						}
				}
				// Joystick generates many events per second!
				// Only send them if a little time has passed or if a (0,0) "all stop" has occurred
				if (System.currentTimeMillis() - time_of_last_joystick.get() > 100 ||
								(thrust == 0.0 && heading == 0.0))
				{
						time_of_last_joystick.set(System.currentTimeMillis());
						new UpdateControlSignalsAsyncTask().execute();
				}
		}

		@Override
		public void setAutonomous(final boolean b, final Runnable failureCallback)
		{
				class SetAutonomousAsyncTask extends AsyncTask<Void, Void, Void>
				{
						@Override
						protected Void doInBackground(Void... params)
						{
								server.setAutonomous(b, new FunctionObserver<Void>()
								{
										@Override
										public void completed(Void aVoid)
										{
												Log.i(logTag, String.format("set autonomous to %s", Boolean.toString(b)));
												autonomous.set(b);
										}

										@Override
										public void failed(FunctionError functionError)
										{
												uiHandler.post(failureCallback);
										}
								});
								return null;
						}
				}
				new SetAutonomousAsyncTask().execute();
		}

		@Override
		public void setPID(final double[] thrustPID, final double[] headingPID, final Runnable failureCallback)
		{
				class SetPIDAsyncTask extends AsyncTask<Void, Void, Void>
				{
						@Override
						protected Void doInBackground(Void... params)
						{
								server.setGains(THRUST_GAIN_AXIS, thrustPID, new FunctionObserver<Void>()
								{
										@Override
										public void completed(Void aVoid)
										{
												synchronized (PID_lock)
												{
														PID_gains[0] = thrustPID.clone();
												}
												Log.i(logTag, "Setting thrust PID completed.");
										}

										@Override
										public void failed(FunctionError functionError)
										{
												uiHandler.post(failureCallback);
												Log.w(logTag, "Setting thrust PID failed: " + functionError);
										}
								});
								server.setGains(RUDDER_GAIN_AXIS, headingPID, new FunctionObserver<Void>()
								{
										@Override
										public void completed(Void aVoid)
										{
												synchronized (PID_lock)
												{
														PID_gains[1] = headingPID.clone();
												}
												Log.i(logTag, "Setting rudder PID completed.");
										}

										@Override
										public void failed(FunctionError functionError)
										{
												uiHandler.post(failureCallback);
												Log.w(logTag, "Setting rudder PID failed: " + functionError);
										}
								});
								return null;
						}
				}
				new SetPIDAsyncTask().execute();
		}

		@Override
		public void addWaypoint(final double[] waypoint, final Runnable failureCallback)
		{
				if (server == null) return;
				double[][] waypoints = {waypoint};
				startWaypoints(waypoints, failureCallback);
		}

		@Override
		public void setAddress(InetSocketAddress a)
		{
				Log.i(logTag, String.format("connection to ip address %s", a.toString()));
				server.setVehicleService(a);
		}

		@Override
		public void setHome(LatLng home, final Runnable successCallback, final Runnable failureCallback)
		{
				home_location = home;
				final double[] home_doubles = new double[]{home.getLatitude(), home.getLongitude()};
				class SetHomeAsyncTask extends AsyncTask<Void, Void, Void>
				{

						@Override
						protected Void doInBackground(Void... params)
						{
								server.setHome(home_doubles, new FunctionObserver<Void>()
								{
										@Override
										public void completed(Void aVoid)
										{
												Log.i(logTag, "setting boat home successfully completed");
												uiHandler.post(successCallback);
										}

										@Override
										public void failed(FunctionError functionError)
										{
												Log.w(logTag, "setting boat home failed:" + functionError);
												uiHandler.post(failureCallback);
										}
								});
								return null;
						}
				}
				new SetHomeAsyncTask().execute();
		}

		@Override
		public void goHome(final Runnable failureCallback)
		{
				class GoHomeAsyncTask extends AsyncTask<Void, Void, Void>
				{

						@Override
						protected Void doInBackground(Void... voids)
						{
								server.startGoHome(new FunctionObserver<Void>()
								{
										@Override
										public void completed(Void aVoid)
										{
												Log.i(logTag, "starting go home successful");
										}

										@Override
										public void failed(FunctionError functionError)
										{
												Log.w(logTag, "starting go home failed");
												uiHandler.post(failureCallback);
										}
								});
								return null;
						}
				}
				new GoHomeAsyncTask().execute();
		}

		@Override
		public void sendAutonomousPredicateMessage(final String apm, final Runnable failureCallback)
		{
				// TODO: need user interface to generate APMs
				// 1) Waypoint menu quick build
				//    a) automatic "near: here AND ..."
				//    b) available actions. Maybe that's it.
				//    c) "create" is not send. It only populates a partially defined APM in the list
				// 2) New layout that you access via advanced options
				//    RecyclerView: https://developer.android.com/training/material/lists-cards.html
				//    a) list of APMs (each with its own sub-layout) (corresponding HashMap(String: APM layout))
				//    b) APM layout
				//         i) key:value definition pull-down menus or whatever
				//             1) name (automatically generated, but can edit maybe?)
				//             2) action (limited pull-down menu of action strings)
				//             3) interval/Hz (type it in, default 1000 ms, enforced minimum 200 ms)
				//             4) priority #/level (pulldown menu with helpful labels)
				//             5) trigger...this needs its own section below. See #3.
				//             6) isPermanent or ends (just a checkbox)
				//        ii) send button
				//       iii) "sent" status checkbox
				//        iv) big red X delete button (only if unsent)
				//    c) a send all button at the top
				//         i) only send unsent
				//         ii) only send completely defined APMs (for example,
				//    d) every time a new APM is added, a new blank layout should appear with a plus button
				//         i) when the plus button is pressed, populate the other stuff and make a new blank one
				//    e) when an APM is successfully sent, it checks the checkbox and removes the invidual send button
				//    f) when APM is added, it pushes the list down and new stuff is added at the top.
				//    g) when APM is sent or added, list is sorted to move successfully sent APMs to the bottom
				// 3) Creating the trigger. Need to build a string.
				//    a) easiest option would be to just have them type it all manually, but that is error prone
				//    b) Remember, this is going to be dominated by starting the sampler, so don't make it crazy complicated
				//    c) main APM layout has a button to generate trigger, which pops up another layout
				//    d) popup has buttons at the top that insert variables
				//    e) buttons in that popup trigger generator layout
				//       i) add variable (companion pull-down chooses which variable)
				//      ii) add predicate (), step inside
				//     iii) add negated predicate ^(), step inside
				//      iv) add number (companion text field)
				//       v) add boolean comparator (<, ==, etc. pull-down or distinct buttons)
				//      vi) add boolean logical (&, | distinct buttons)
				//     vii) undo button
				//    viii) "step out" button to exit parentheses, or maybe just touch the text field to position cursor
				//      ix) done (closes popup)
				//       x) abandon, maybe big red x in the corner
				// OR ALTERNATIVELY, just have buttons for variable names too (i.e. there is a "battery_voltage" button)

				class sendAPMAsyncTask extends AsyncTask<Void, Void, Void>
				{
						@Override
						protected Void doInBackground(Void... params)
						{
								server.newAutonomousPredicateMessage(apm, new FunctionObserver<Void>()
								{
										@Override
										public void completed(Void aVoid)
										{

										}

										@Override
										public void failed(FunctionError functionError)
										{
												uiHandler.post(failureCallback);
												Log.w(logTag, "sendAutonomousPredicateMessage failure");
										}
								});
								return null;
						}
				}
				new sendAPMAsyncTask().execute();
		}

		@Override
		public InetSocketAddress getIpAddress()
		{
				return (InetSocketAddress) server.getVehicleService();
		}

		@Override
		public void startSample(final int jar_number, final Runnable TimerStartRunnable, final Runnable failureCallback)
		{
				class StartSampleAsyncTask extends AsyncTask<Void, Void, Void>
				{
						@Override
						protected Void doInBackground(Void... params)
						{
								double[] jar = {jar_number, 1};
								if (sampler_running[jar_number])
								{
										// don't resend a duplicate start
										return null;
								}
								server.setGains(SAMPLER_GAIN_AXIS, jar, new FunctionObserver<Void>()
								{
										@Override
										public void completed(Void aVoid)
										{
												Log.i(logTag, String.format("Started sampler jar %d", jar_number));
												sampler_running[jar_number] = true;
												uiHandler.post(TimerStartRunnable);
										}

										@Override
										public void failed(FunctionError functionError)
										{
												Log.e(logTag, String.format("Could not start jar %d", jar_number));
												uiHandler.post(failureCallback);
										}
								});
								return null;
						}
				}
				new StartSampleAsyncTask().execute();
		}

		@Override
		public void stopSample(final int jar_number, final Runnable successCallback, final Runnable failureCallback)
		{
				class StopSampleAsyncTask extends AsyncTask<Void, Void, Void>
				{
						@Override
						protected Void doInBackground(Void... params)
						{
								double[] jar = {jar_number, 0};
								server.setGains(SAMPLER_GAIN_AXIS, jar, new FunctionObserver<Void>()
								{
										@Override
										public void completed(Void aVoid)
										{
												uiHandler.post(successCallback);
										}
										@Override
										public void failed(FunctionError functionError)
										{
												Log.e(logTag, "Could not stop jar");
												uiHandler.post(failureCallback);
										}
								});

								return null;
						}
				}
				// stop a single jar
				new StopSampleAsyncTask().execute();
		}

		@Override
		public void stopSampleAll(final Runnable successCallback, final Runnable failureCallback)
		{
				class StopSampleAllAsyncTask extends AsyncTask<Void, Void, Void>
				{
						@Override
						protected Void doInBackground(Void... params)
						{
								double[] jar = {-1, 0};
								server.setGains(SAMPLER_GAIN_AXIS, jar, new FunctionObserver<Void>()
								{
										@Override
										public void completed(Void aVoid)
										{
												uiHandler.post(successCallback);
										}
										@Override
										public void failed(FunctionError functionError)
										{
												Log.e(logTag, "Could not stop all jars");
												uiHandler.post(failureCallback);
										}
								});

								return null;
						}
				}
				// stop all the jars
				new StopSampleAllAsyncTask().execute();
		}

		public void resetSampler(final Runnable successCallback, final Runnable failureCallback)
		{
				class ResetSamplerAsyncTask extends AsyncTask<Void, Void, Void>
				{

						@Override
						protected Void doInBackground(Void... params)
						{
								double[] jar = {-1, 1};
								server.setGains(SAMPLER_GAIN_AXIS, jar, new FunctionObserver<Void>()
								{
										@Override
										public void completed(Void aVoid)
										{
												Log.i(logTag, "Reset sampler");
												for (int i = 0; i < 4; i++)
												{
														sampler_running[i] = false;
												}
												uiHandler.post(successCallback);
										}

										@Override
										public void failed(FunctionError functionError)
										{
												Log.e(logTag, "Could not reset sample");
												uiHandler.post(failureCallback);
										}
								});
								return null;
						}
				}
				new ResetSamplerAsyncTask().execute();
		}
	public void holdPosition(boolean action)
	{
		if (server != null && isConnected() == true)
		{
			double value[]= new double[1];
			if (action == false) {
				value[0] = -1;
			}
			else
			{
				value[0] = 1;
			}
			server.setGains(9, value, new FunctionObserver<Void>() {
				@Override
				public void completed(Void aVoid) {

				}

				@Override
				public void failed(FunctionError functionError) {

				}
			});
		}
	}
	public void repeatWaypoints(boolean action)//9
	{
		if (server != null && isConnected() == true)
		{
			double value[] = new double[1];
			if (action == false)
			{
				value[0] = -1;
			}
			else
			{
				value[0] = 1;
			}
			server.setGains(8, value, new FunctionObserver<Void>() {
				@Override
				public void completed(Void aVoid) {

				}

				@Override
				public void failed(FunctionError functionError) {

				}
			});
		}
	}
}
