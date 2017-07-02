package com.platypus.android.tablet;

import android.util.Log;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.platypus.crw.FunctionObserver;
import com.platypus.crw.PoseListener;
import com.platypus.crw.SensorListener;
import com.platypus.crw.WaypointListener;
import com.platypus.crw.data.Pose3D;
import com.platypus.crw.data.Twist;
import com.platypus.crw.data.Utm;
import com.platypus.crw.data.UtmPose;
import com.platypus.crw.udp.UdpVehicleServer;

import java.net.InetSocketAddress;


public class Boat
{
	UdpVehicleServer server = null;
	private String name;
	private InetSocketAddress ipAddress;
	private Twist tw = null;
	private PoseListener pl;
	private SensorListener sl;
	private UtmPose pose;
	private double xValue;
	private double yValue;
	private double zValue;
	private boolean connected;
	private WaypointListener waypointListen;
	private UtmPose _waypoint = new UtmPose();
	private final Object _waypointLock = new Object();
	private String boatLog = "";
	private String logTag = Boat.class.getName();
	private LatLng currentLocation = null;

	public Boat()
	{
		server = new UdpVehicleServer();
	}

	public Boat(PoseListener _pl, SensorListener _sl){

		pl = _pl;
		sl = _sl;
		server = new UdpVehicleServer();
		try {
			if(pl != null) {
				server.addPoseListener(pl, new FunctionObserver<Void>() {
					@Override
					public void completed(Void aVoid) {
						Log.i("Boat", "addPoseListener");
					}

					@Override
					public void failed(FunctionError functionError) {

					}
				});
			}
			if(sl != null) {
				for (int channel = 0; channel < 5; channel++) {
					server.addSensorListener(channel, sl, new FunctionObserver<Void>() {
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
		catch(Exception e){
			Log.i(logTag, "Failed to add listener");
		}
		tw = new Twist();
	}

	public Boat(InetSocketAddress _ipAddress)
	{
		ipAddress = _ipAddress;

		server = new UdpVehicleServer();

		server.setVehicleService(ipAddress);
		tw = new Twist();
	}

	public void setAddress(InetSocketAddress a)
	{
		server.setVehicleService(a);
	}

	public InetSocketAddress getIpAddress() {
		return (InetSocketAddress) server.getVehicleService();
	}

	public void getPose()
	{
		PoseListener pl = new PoseListener()
		{

			public void receivedPose(UtmPose upwcs)
			{
				UtmPose _pose = upwcs.clone();
				{
					// random = "" + _pose.pose.getX() +
					// "\n" + _pose.pose.getY() + "\n" +
					// _pose.pose.getZ();
					xValue = _pose.pose.getX();
					yValue = _pose.pose.getY();
					zValue = _pose.pose.getZ();
					// _pose.origin.

				}
			}
		};
		ConnectScreen.boat.returnServer().addPoseListener(pl, null);

	}

	// Removed null from here but this method needs fixing/implementation -ckt
	public double getRotation()
	{
		return 0.0;
	}
	public double getThrust()
	{
		return tw.dx() / .010;
	}

	public double getRudder()
	{
		return tw.drz() / .010;
	}

	public void setVelocity(int thrust, int rudder)
	{
		tw.dx(thrust * .010);
		tw.drz(rudder * .010);
		server.setVelocity(tw, null);
	}

	public UdpVehicleServer returnServer()
	{
		return server;
	}

	public double getPoseX()
	{
		return xValue;
	}

	public double getPoseY()
	{
		return yValue;
	}

	public double getPoseZ()
	{
		return zValue;
	}

    public boolean isConnected()
	{
		server.isConnected(new FunctionObserver<Boolean>()
		{
			public void completed(Boolean v)
			{
                connected = true;
                try {
                    Thread.sleep(300);
                }
                catch(InterruptedException e)
                {
                    System.out.println(e.toString());
                }
                isConnected();
			}

			public void failed(FunctionError fe)
			{
				connected = false;
                try {
                    Thread.sleep(300);
                }
                catch(InterruptedException e)
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

	public void initWaypointListener()
	{
		// waypointListen = new WaypointListener();
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
				Log.i(logTag,"Failed to start waypoint");
			}
		});
	}

	public void moveWaypoint()
	{
	}

	public void cancelWaypoint()
	{
		server.stopWaypoints(new FunctionObserver<Void>()
		{
			public void completed(Void v)
			{

			}
			public void failed(FunctionError fe)
			{

			}
		});
	}
	public void addToBoatLog(String s)
	{
		boatLog = boatLog + s  + "\n";
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
