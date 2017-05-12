package com.platypus.android.tablet;

import android.util.Log;

import java.net.InetSocketAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.platypus.crw.FunctionObserver;
import com.platypus.crw.PoseListener;
import com.platypus.crw.SensorListener;
import com.platypus.crw.WaypointListener;
import com.platypus.crw.data.Twist;
import com.platypus.crw.data.Utm;
import com.platypus.crw.data.UtmPose;
import com.platypus.crw.udp.UdpVehicleServer;
import com.platypus.crw.data.Pose3D;

public class Boat
{
		UdpVehicleServer server = null;
		private InetSocketAddress ipAddress;
		private PoseListener pl;
		private SensorListener sl;
		private WaypointListener wl;
		private boolean[] connected ={false, false}; // network_connected, eboard_connected
		private boolean autonomous;
		private int current_waypoint_index;
		private String logTag = "Boat"; //Boat.class.getName();
		private LatLng currentLocation = null;
		private ExecutorService comms_thread_pool = Executors.newFixedThreadPool(5);

		public Boat()
		{
				server = new UdpVehicleServer();
		}

		public Boat(PoseListener _pl, SensorListener _sl, WaypointListener _wl)
		{

				pl = _pl;
				sl = _sl;
				wl = _wl;
				server = new UdpVehicleServer();
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
		}

		public Boat(InetSocketAddress _ipAddress)
		{
				ipAddress = _ipAddress;

				server = new UdpVehicleServer();

				server.setVehicleService(ipAddress);
		}

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

		public boolean isNetworkConnected()
		{
				boolean[] isConnected = isConnected();
				return isConnected[0];
		}

		public boolean[] isConnected()
		{
				class isConnectedCallable implements Callable<boolean[]>
				{
						boolean completed = false;
						boolean[] result = {false, false};
						public boolean[] call()
						{
								server.isConnected(new FunctionObserver<Boolean>()
								{
										@Override
										public void completed(Boolean aBoolean)
										{
												Log.i(logTag, String.format("isConnected() returned %s", Boolean.toString(aBoolean)));
												result[0] = true;
												result[1] = aBoolean;
												completed = true;
										}

										@Override
										public void failed(FunctionError functionError)
										{
												Log.w(logTag, String.format("isConnected() did not return"));
												completed = true;
										}
								});
								while (!completed)
								{
										Thread.yield();
								}
								Log.d(logTag, String.format("-     isConnectedCallable output = [%s, %s]", Boolean.toString(result[0]), Boolean.toString(result[1])));
								return result;
						}
				}
				try
				{
						connected = comms_thread_pool.submit(new isConnectedCallable()).get(3000, TimeUnit.MILLISECONDS).clone(); // have to clone an array!
				}
				catch (Exception ex)
				{
						Log.e(logTag, "isConnected call timed out...");
						Log.e(logTag, ex.getMessage());
				}
				return connected;
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
		}

		public void stopWaypoints()
		{
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
		}

		public void updateControlSignals(final double thrust, final double heading)
		{
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
		}

		public void setAutonomous(final boolean b)
		{
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
												autonomous = b;
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
		}

		public boolean isAutonomous()
		{
				class isAutonomousCallable implements Callable<Boolean>
				{
						Boolean result;
						public Boolean call() throws Exception
						{
								server.isAutonomous(new FunctionObserver<Boolean>()
								{
										@Override
										public void completed(Boolean aBoolean)
										{
												result = aBoolean;
												Log.i(logTag, "isAutonomous: " + result);
										}

										@Override
										public void failed(FunctionError functionError) { }
								});
								return result;
						}
				}
				try
				{
						autonomous = comms_thread_pool.submit(new isAutonomousCallable()).get();
				}
				catch (Exception ex) { }
				return autonomous;
		}

		public int getWaypointsIndex()
		{
				class getWaypointsIndexCallable implements Callable<Integer>
				{
						Integer result;
						public Integer call()
						{
								server.getWaypointsIndex(new FunctionObserver<Integer>()
								{
										@Override
										public void completed(Integer waypoint_index)
										{
												Log.i(logTag, String.format("Boat current waypoint i = %d", waypoint_index));
												result = waypoint_index;
										}

										@Override
										public void failed(FunctionError functionError)
										{
												Log.w(logTag, "Did not receive waypoint index");
										}
								});
								return result;
						}
				}
				try
				{
						current_waypoint_index = comms_thread_pool.submit(new getWaypointsIndexCallable()).get();
				}
				catch (Exception ex) { }
				return current_waypoint_index;
		}

}
