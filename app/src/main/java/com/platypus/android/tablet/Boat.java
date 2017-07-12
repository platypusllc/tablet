package com.platypus.android.tablet;

import android.os.Handler;
import android.os.Looper;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.platypus.crw.data.SensorData;
import com.platypus.crw.data.UtmPose;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.measure.unit.SI;

/**
 * Created by jason on 7/12/17.
 */

public abstract class Boat
{
		String ipAddressString;
		String name;
		AtomicLong time_of_last_connection = new AtomicLong(0);
		AtomicLong time_of_last_joystick = new AtomicLong(0);
		AtomicBoolean connected = new AtomicBoolean(false);
		AtomicBoolean autonomous = new AtomicBoolean(false);
		AtomicBoolean sensors_ready = new AtomicBoolean(false);
		AtomicInteger current_waypoint_index = new AtomicInteger(-1);
		String logTag = "Boat"; //Boat.class.getName();
		LatLng currentLocation = null;
		Object location_lock = new Object();
		double currentYaw = 0.0; // [-pi, pi]
		Object yaw_lock = new Object();
		double[][] PID_gains = {{0., 0., 0.}, {0., 0., 0.}}; // thrust, heading
		Object PID_lock = new Object();
		final int THRUST_GAIN_AXIS = 0;
		final int RUDDER_GAIN_AXIS = 5;
		ScheduledThreadPoolExecutor polling_thread_pool;
		Handler uiHandler = new Handler(Looper.getMainLooper());
		SensorData lastSensorDataReceived;
		Object sensor_lock = new Object();
		String waypointState;
		Object waypoint_state_lock = new Object();
		int boat_color;
		int line_color;

		abstract public void createListeners(
						final Runnable poseListenerCallback,
						final Runnable sensorListenerCallback,
						final Runnable waypointListenerCallback);
		abstract public void startWaypoints(final UtmPose[] waypoints, final String controller_name, final Runnable failureCallback);
		abstract public void stopWaypoints(final Runnable failureCallback);
		abstract public void updateControlSignals(final double thrust, final double heading, final Runnable failureCallback);
		abstract public void setAutonomous(final boolean b, final Runnable failureCallback);
		abstract public void setPID(final double[] thrustPID, final double[] headingPID, final Runnable failureCallback);
		abstract public void addWaypoint(UtmPose waypoint, final String controller_name, final Runnable failureCallback);
		abstract public void setAddress(InetSocketAddress a);
		abstract public InetSocketAddress getIpAddress();

		public String getName() { return name; }
		public void setBoatColor(int _color) { boat_color = _color; }
		public int getBoatColor() { return boat_color; }
		public void setLineColor(int _color) { line_color = _color; }
		public int getLineColor() { return line_color; }
		public void setIpAddressString(String addr)
		{
				ipAddressString = addr;
		}
		public String getIpAddressString()
		{
				return ipAddressString;
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

		public void setConnected(boolean b)
		{
				connected.set(b);
				time_of_last_connection.set(System.currentTimeMillis());
		}

		public boolean isConnected()
		{
				return connected.get();
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

		public int getWaypointsIndex()
		{
				return current_waypoint_index.get();
		}

		public double[][] getPID()
		{
				synchronized (PID_lock)
				{
						return PID_gains.clone();
				}
		}

		public com.mapbox.mapboxsdk.geometry.LatLng jscienceLatLng_to_mapboxLatLng(org.jscience.geography.coordinates.LatLong jlatlng)
		{
				LatLng result = new LatLng(
								jlatlng.latitudeValue(SI.RADIAN)*180./Math.PI,
								jlatlng.longitudeValue(SI.RADIAN)*180./Math.PI);
				return result;
		}
}
