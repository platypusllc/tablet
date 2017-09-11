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
//import com.platypus.crw.CrumbListener;
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
import javax.measure.unit.SI;

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
						public void receivedSensor(SensorData sensorData)
						{
								setConnected(true);
								synchronized (sensor_lock)
								{
										lastSensorDataReceived = sensorData;
								}
								sensors_ready.set(true);
								uiHandler.post(sensorListenerCallback); // update GUI with result
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
								for (int channel = 0; channel < 5; channel++)
								{
										server.addSensorListener(channel, sl, new FunctionObserver<Void>()
										{
												@Override
												public void completed(Void aVoid) { Log.i(logTag, "add sensor listener"); }

												@Override
												public void failed(FunctionError functionError) { }
										});
								}
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
		public void sendAutonomousPredicateMessage(final String apm, final Runnable failureCallback)
		{
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
												Log.w(logTag, )
										}
								});
						}
				}
				new sendAPMAsyncTask().execute();
		}

		@Override
		public InetSocketAddress getIpAddress()
		{
				return (InetSocketAddress) server.getVehicleService();
		}
}
