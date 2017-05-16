package com.platypus.android.tablet;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.platypus.crw.FunctionObserver;
import com.platypus.crw.PoseListener;
import com.platypus.crw.SensorListener;
import com.platypus.crw.VehicleServer;
import com.platypus.crw.WaypointListener;
import com.platypus.crw.data.SensorData;
import com.platypus.crw.data.Utm;
import com.platypus.crw.data.UtmPose;
import com.platypus.crw.udp.UdpVehicleServer;
import com.platypus.crw.data.Pose3D;

import org.jscience.geography.coordinates.UTM;
import org.jscience.geography.coordinates.crs.ReferenceEllipsoid;

import javax.measure.unit.SI;


public class Boat
{
		UdpVehicleServer server = null;
		private InetSocketAddress ipAddress;
		private PoseListener pl;
		private SensorListener sl;
		private WaypointListener wl;
		private AtomicLong time_of_last_connection = new AtomicLong(0);
		private AtomicBoolean connected = new AtomicBoolean(false);
		private AtomicBoolean autonomous = new AtomicBoolean(false);
		private AtomicBoolean sensors_ready = new AtomicBoolean(false);
		private int current_waypoint_index;
		private String logTag = "Boat"; //Boat.class.getName();
		private LatLng currentLocation = null;
		private double currentYaw = 0.0; // [-pi, pi]
		private double[][] PID_gains = {{0., 0., 0.}, {0., 0., 0.}}; // thrust, heading
		final int THRUST_GAIN_AXIS = 0;
		final int RUDDER_GAIN_AXIS = 5;
		private final int CONNECTION_POLL_S = 3;
		private ScheduledThreadPoolExecutor polling_thread_pool;
		private ExecutorService oneshot_thread_pool;
		Handler uiHandler = new Handler(Looper.getMainLooper());
		SensorData lastSensorDataReceived;
		String waypointState;

		public Boat()
		{
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
								connected.set(true);
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
								lastSensorDataReceived = sensorData;
								sensors_ready.set(true);
								uiHandler.post(sensorListenerCallback); // update GUI with result
						}
				};
				wl = new WaypointListener()
				{
						@Override
						public void waypointUpdate(VehicleServer.WaypointState state)
						{
								waypointState = state.toString();
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
				polling_thread_pool = new ScheduledThreadPoolExecutor(1);
				polling_thread_pool.scheduleAtFixedRate(isConnectedPoll, 0, CONNECTION_POLL_S, TimeUnit.SECONDS);
				oneshot_thread_pool = Executors.newFixedThreadPool(5);
		}

		public Boat(InetSocketAddress _ipAddress)
		{
				ipAddress = _ipAddress;

				server = new UdpVehicleServer();

				server.setVehicleService(ipAddress);
		}

		public double getYaw() { return currentYaw; }
		public void setYaw(double yaw)
		{
				while (Math.abs(yaw) > Math.PI)
				{
						yaw -= 2*Math.PI*Math.signum(yaw);
				}
				currentYaw = yaw;
		}
		public SensorData getLastSensorDataReceived()
		{
				return lastSensorDataReceived;
		}
		public String getWaypointState()
		{
				return waypointState;
		}

		/*
		public long getTimeOfLastConnection() { return time_of_last_connection.get(); }
		public void setTimeOfLastConnection(long ms)
		{
				if (ms > time_of_last_connection.get()) time_of_last_connection.set(ms);
		}
		*/
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
												time_of_last_connection.set(System.currentTimeMillis());
												connected.set(true);
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
								connected.set(true);
								Log.i(logTag, "skipping isConnected() due to recent traffic");
						}
				}
		};


		public void setAddress(InetSocketAddress a)
		{
				Log.i(logTag, String.format("connection to ip address %s", a.toString()));
				server.setVehicleService(a);
		}

		public InetSocketAddress getIpAddress()
		{
				return (InetSocketAddress) server.getVehicleService();
		}

		public UdpVehicleServer returnServer()
		{
				return server;
		}

		public void setConnected(boolean b)
		{
				connected.set(b);
				time_of_last_connection.set(System.currentTimeMillis());
		}
		public boolean isConnected()
		{
				return connected.get();
		}

		public void addWaypoint(Pose3D _pose, Utm _origin)
		{
				if (server == null)
						return;

				UtmPose[] wpPose = new UtmPose[1];

				wpPose[0] = new UtmPose(_pose, _origin);
				startWaypoints(wpPose, "POINT_AND_SHOOT");
		}

		public LatLng getLocation()
		{
				return currentLocation;
		}

		public void setLocation(LatLng loc)
		{
				currentLocation = loc;
		}

		public void startWaypoints(final UtmPose[] waypoints, final String controller_name)
		{
				/*
				class startWaypointsCallable implements Callable<Void>
				{
						public Void call()
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
												Log.e(logTag, "startwaypoints failed");
										}
								});
								return null;
						}
				}
				try
				{
						Log.i(logTag, "new startWaypointsCallable...");
						comms_thread_pool.submit(new startWaypointsCallable()).get();
				}
				catch (Exception ex) { }
				*/
		}

		public void stopWaypoints()
		{
				/*
				class stopWaypointsCallable implements Callable<Void>
				{
						public Void call()
						{
								server.stopWaypoints(new FunctionObserver<Void>()
								{
										@Override
										public void completed(Void aVoid)
										{
												Log.i(logTag, "Waypoints stopped");
										}

										@Override
										public void failed(FunctionError functionError) { Log.i(logTag, "Failed to stop waypoints"); }
								});
								return null;
						}
				}
				try
				{
						comms_thread_pool.submit(new stopWaypointsCallable()).get();
				}
				catch (Exception ex) { }
				*/
		}

		public void updateControlSignals(final double thrust, final double heading)
		{
				/*
				class updateControlSignalsCallable implements Callable<Void>
				{
						public Void call()
						{
								Twist twist = new Twist();
								twist.dx(thrust);
								twist.drz(-1.0*heading); // left-right is backwards
								server.setVelocity(twist, new FunctionObserver<Void>()
								{
										@Override
										public void completed(Void aVoid) { }

										@Override
										public void failed(FunctionError functionError) { }
								});
								return null;
						}
				}
				try
				{
						comms_thread_pool.submit(new updateControlSignalsCallable()).get();
				}
				catch (Exception ex) { }
				*/
		}

		public void setAutonomous(final boolean b)
		{
				/*
				class setAutonomousCallable implements Callable<Void>
				{
						public Void call() throws Exception
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
										}
								});
								return null;
						}
				}
				try
				{
						comms_thread_pool.submit(new setAutonomousCallable()).get();
				}
				catch (Exception ex) { }
				*/
		}

		public boolean isAutonomous()
		{
				/*
				class isAutonomousCallable implements Callable<Boolean>
				{
						boolean response_received = false;
						Boolean result;
						public Boolean call() throws Exception
						{
								server.isAutonomous(new FunctionObserver<Boolean>()
								{
										@Override
										public void completed(Boolean aBoolean)
										{
												response_received = true;
												result = aBoolean;
												Log.i(logTag, "isAutonomous: " + result);
										}

										@Override
										public void failed(FunctionError functionError)
										{
												response_received = true;
										}
								});
								while (!response_received)
								{
										Thread.yield();
								}
								return result;
						}
				}
				try
				{
						autonomous.set(comms_thread_pool.submit(new isAutonomousCallable())
										.get(LONG_TIMEOUT_S, TimeUnit.SECONDS));
				}
				catch (Exception ex)
				{
						Log.e(logTag, "isAutonomous() call exception");
						Log.e(logTag, ex.getMessage());
				}
				return autonomous.get();
				*/
				return false;
		}

		public int getWaypointsIndex()
		{
				/*
				class getWaypointsIndexCallable implements Callable<Integer>
				{
						boolean response_received = false;
						Integer result = -2;
						public Integer call()
						{
								server.getWaypointsIndex(new FunctionObserver<Integer>()
								{
										@Override
										public void completed(Integer waypoint_index)
										{
												Log.i(logTag, String.format("Boat current waypoint i = %d", waypoint_index));
												result = waypoint_index;
												response_received = true;
										}

										@Override
										public void failed(FunctionError functionError)
										{
												response_received = true;
												Log.w(logTag, "Did not receive waypoint index");
										}
								});
								while (!response_received)
								{
										Thread.yield();
								}
								return result;
						}
				}
				try
				{
						current_waypoint_index = comms_thread_pool.submit(new getWaypointsIndexCallable())
										.get(LONG_TIMEOUT_S, TimeUnit.SECONDS);
				}
				catch (Exception ex)
				{
						Log.e(logTag, "getWaypointsIndex() call exception");
						Log.e(logTag, ex.getMessage());
				}
				return current_waypoint_index;
				*/
				return -2;
		}

		public void setPID(final double[] thrustPID, final double[] headingPID)
		{
				/*
				class setPIDCallable implements Callable<Void>
				{
						public Void call()
						{
								server.setGains(THRUST_GAIN_AXIS, thrustPID, new FunctionObserver<Void>()
								{
										@Override
										public void completed(Void aVoid)
										{
												Log.i(logTag, "Setting thrust PID completed.");
										}

										@Override
										public void failed(FunctionError functionError)
										{
												Log.i(logTag, "Setting thrust PID failed: " + functionError);
										}
								});
								server.setGains(RUDDER_GAIN_AXIS, headingPID, new FunctionObserver<Void>()
								{
										@Override
										public void completed(Void aVoid)
										{
												Log.i(logTag, "Setting rudder PID completed.");
										}

										@Override
										public void failed(FunctionError functionError)
										{
												Log.i(logTag, "Setting rudder PID failed: " + functionError);
										}
								});
								return null;
						}
				}
				try
				{
						comms_thread_pool.submit(new setPIDCallable()).get();
				}
				catch (Exception ex) { }
				*/
		}

		public double[][] getPID()
		{
				/*
				class getPIDCallable implements Callable<double[][]>
				{
						boolean thrust_response_received = false;
						boolean heading_response_received = false;
						double[][] result = {{0., 0., 0.}, {0., 0., 0.}};

						public double[][] call()
						{
								server.getGains(THRUST_GAIN_AXIS, new FunctionObserver<double[]>()
								{
										@Override
										public void completed(double[] doubles)
										{
												result[0] = doubles.clone();
												thrust_response_received = true;
												Log.i(logTag, "thrust pids are now: " + doubles[0] + " " + doubles[1] + " " + doubles[2]);
										}

										@Override
										public void failed(FunctionError functionError)
										{
												thrust_response_received = true;
										}
								});
								server.getGains(RUDDER_GAIN_AXIS, new FunctionObserver<double[]>()
								{
										@Override
										public void completed(double[] doubles)
										{
												result[1] = doubles.clone();
												heading_response_received = true;
												Log.i(logTag, "heading pids are now: " + doubles[0] + " " + doubles[1] + " " + doubles[2]);
										}

										@Override
										public void failed(FunctionError functionError)
										{
												heading_response_received = true;
										}
								});
								while (!thrust_response_received || !heading_response_received)
								{
										Thread.yield();
								}
								return result;
						}
				}
				try
				{
						PID_gains = comms_thread_pool.submit(new getPIDCallable())
										.get(LONG_TIMEOUT_S, TimeUnit.SECONDS).clone();
				}
				catch (Exception ex)
				{
						Log.e(logTag, "getPID() call exception");
						Log.e(logTag, ex.getMessage());
				}
				return PID_gains;
				*/
				return null;
		}


		public com.mapbox.mapboxsdk.geometry.LatLng jscienceLatLng_to_mapboxLatLng(org.jscience.geography.coordinates.LatLong jlatlng)
		{
				LatLng result = new LatLng(
								jlatlng.latitudeValue(SI.RADIAN)*180./Math.PI,
								jlatlng.longitudeValue(SI.RADIAN)*180./Math.PI);
				return result;
		}
}
