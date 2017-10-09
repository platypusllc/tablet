package com.platypus.android.tablet;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.platypus.crw.FunctionObserver;
import com.platypus.crw.PoseListener;
import com.platypus.crw.SensorListener;
import com.platypus.crw.VehicleServer;
import com.platypus.crw.WaypointListener;
import com.platypus.crw.data.SensorData;
import com.platypus.crw.data.Twist;
import com.platypus.crw.data.UtmPose;
import com.platypus.crw.udp.UdpVehicleServer;

import org.jscience.geography.coordinates.UTM;
import org.jscience.geography.coordinates.crs.ReferenceEllipsoid;

import javax.measure.unit.SI;


public class Boat
{
		private UdpVehicleServer server = null;
		private String ipAddressString;
		private String name;
		private PoseListener pl;
		private SensorListener sl;
		private WaypointListener wl;
		private AtomicLong time_of_last_connection = new AtomicLong(0);
		private AtomicLong time_of_last_joystick = new AtomicLong(0);
		private AtomicBoolean connected = new AtomicBoolean(false);
		private AtomicBoolean autonomous = new AtomicBoolean(false);
		private AtomicBoolean sensors_ready = new AtomicBoolean(false);
		private AtomicInteger current_waypoint_index = new AtomicInteger(-1);
		private String logTag = "Boat"; //Boat.class.getName();
		private LatLng currentLocation = null;
		private Object location_lock = new Object();
		private double currentYaw = 0.0; // [-pi, pi]
		private Object yaw_lock = new Object();
		private double[][] PID_gains = {{0., 0., 0.}, {0., 0., 0.}}; // thrust, heading
		private Object PID_lock = new Object();
		private boolean[] sampler_running = {false, false, false, false};
		final int THRUST_GAIN_AXIS = 0;
		final int RUDDER_GAIN_AXIS = 5;
		final int SAMPLER_GAIN_AXIS = 7;
		private final int CONNECTION_POLL_S = 3;
		private final int WAYPOINTS_INDEX_POLL_S = 1;
		private ScheduledThreadPoolExecutor polling_thread_pool;
		private Handler uiHandler = new Handler(Looper.getMainLooper());
		private SensorData lastSensorDataReceived;
		private Object sensor_lock = new Object();
		private String waypointState;
		private Object waypoint_state_lock = new Object();
		int boat_color;
		int line_color;

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

		public Boat(String boat_name)
		{
				name = boat_name;
				server = new UdpVehicleServer();
		}

		public void createListeners(
						final Runnable poseListenerCallback,
						final Runnable sensorListenerCallback,
						final Runnable waypointListenerCallback)
		{
				pl = new PoseListener()
				{
						@Override
						public void receivedPose(UtmPose utmPose)
						{
								setConnected(true);
								setYaw(Math.PI / 2 - utmPose.pose.getRotation().toYaw());
								setLocation(
												jscienceLatLng_to_mapboxLatLng(
																UTM.utmToLatLong(
																				UTM.valueOf(
																								utmPose.origin.zone,
																								utmPose.origin.isNorth ? 'T' : 'L',
																								utmPose.pose.getX(),
																								utmPose.pose.getY(),
																								SI.METER
																				),
																				ReferenceEllipsoid.WGS84
																)
												)
								);
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

		public double getYaw()
		{
				synchronized (yaw_lock)
				{
						return currentYaw;
				}
		}
		public void setYaw(double yaw)
		{
				while (Math.abs(yaw) > Math.PI)
				{
						yaw -= 2*Math.PI*Math.signum(yaw);
				}
				synchronized (yaw_lock)
				{
						currentYaw = yaw;
				}
		}
		public SensorData getLastSensorDataReceived()
		{
				synchronized (sensor_lock)
				{
						return lastSensorDataReceived;
				}
		}
		public String getWaypointState()
		{
				synchronized (waypoint_state_lock)
				{
						return waypointState;
				}
		}

		public void setBoatColor(int _color) { boat_color = _color; }
		public int getBoatColor() { return boat_color; }
		public void setLineColor(int _color) { line_color = _color; }
		public int getLineColor() { return line_color; }

		public void setAddress(InetSocketAddress a)
		{
				Log.i(logTag, String.format("connection to ip address %s", a.toString()));
				server.setVehicleService(a);
		}

		public void setIpAddressString(String addr)
		{
				ipAddressString = addr;
		}

		public InetSocketAddress getIpAddress()
		{
				return (InetSocketAddress) server.getVehicleService();
		}

		public String getIpAddressString()
		{
				return ipAddressString;
		}

		public UdpVehicleServer returnServer()
		{
				return server;
		}

		public String getName() { return name; }

		public void setConnected(boolean b)
		{
				connected.set(b);
				time_of_last_connection.set(System.currentTimeMillis());
		}
		public boolean isConnected()
		{
				return connected.get();
		}

		public void addWaypoint(UtmPose waypoint, final String controller_name, final Runnable failureCallback)
		{
				if (server == null) return;
				UtmPose[] wpPose = {waypoint};
				startWaypoints(wpPose, controller_name, failureCallback);
		}

		public LatLng getLocation()
		{
				synchronized (location_lock)
				{
						return currentLocation;
				}
		}
		public void setLocation(LatLng loc)
		{
				synchronized (location_lock)
				{
						currentLocation = loc;
				}
		}

		public void startWaypoints(final UtmPose[] waypoints, final String controller_name, final Runnable failureCallback)
		{
				class StartWaypointsAsyncTask extends AsyncTask<Void, Void, Void>
				{
						@Override
						protected Void doInBackground(Void... params)
						{
								server.startWaypoints(waypoints, controller_name, new FunctionObserver<Void>()
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

		public int getWaypointsIndex()
		{
				return current_waypoint_index.get();
		}

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

		public double[][] getPID()
		{
				synchronized (PID_lock)
				{
						return PID_gains.clone();
				}
		}

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


		public com.mapbox.mapboxsdk.geometry.LatLng jscienceLatLng_to_mapboxLatLng(org.jscience.geography.coordinates.LatLong jlatlng)
		{
				LatLng result = new LatLng(
								jlatlng.latitudeValue(SI.RADIAN)*180./Math.PI,
								jlatlng.longitudeValue(SI.RADIAN)*180./Math.PI);
				return result;
		}
}
