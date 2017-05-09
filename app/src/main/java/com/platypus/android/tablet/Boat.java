package com.platypus.android.tablet;

import android.util.Log;

import java.net.InetSocketAddress;

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
		private boolean connected;
		private String logTag = Boat.class.getName();
		private LatLng currentLocation = null;

		public Boat()
		{
				server = new UdpVehicleServer();
		}

		public Boat(PoseListener _pl, SensorListener _sl)
		{

				pl = _pl;
				sl = _sl;
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
												Log.i("Boat", "addPoseListener");
										}

										@Override
										public void failed(FunctionError functionError)
										{

										}
								});
						}
						if (sl != null)
						{
								for (int channel = 0; channel < 5; channel++)
								{
										server.addSensorListener(channel, sl, new FunctionObserver<Void>()
										{
												@Override
												public void completed(Void aVoid)
												{

												}

												@Override
												public void failed(FunctionError functionError)
												{

												}
										});
								}
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

		public boolean isConnected()
		{
				server.isConnected(new FunctionObserver<Boolean>()
				{
						public void completed(Boolean v)
						{
								connected = true;
								try
								{
										Thread.sleep(300);
								}
								catch (InterruptedException e)
								{
										System.out.println(e.toString());
								}
								isConnected();
						}

						public void failed(FunctionError fe)
						{
								connected = false;
								try
								{
										Thread.sleep(300);
								}
								catch (InterruptedException e)
								{
										System.out.println(e.toString());
								}
								isConnected();
						}
				});
				return connected;
		}

		public boolean getConnected()
		{
				return connected;
		}

		public void addWaypoint(Pose3D _pose, Utm _origin)
		{
				if (server == null)
						return;

				UtmPose[] wpPose = new UtmPose[1];

				wpPose[0] = new UtmPose(_pose, _origin);
				server.startWaypoints(wpPose, "POINT_AND_SHOOT", new FunctionObserver<Void>()
				{
						public void completed(Void v)
						{
								Log.i(logTag, "Start waypoint");
						}

						public void failed(FunctionError fe)
						{
								Log.i(logTag, "Failed to start waypoint");
						}
				});
		}

		public LatLng getLocation()
		{
				return currentLocation;
		}

		public void setLocation(LatLng loc)
		{
				currentLocation = loc;
		}

}
