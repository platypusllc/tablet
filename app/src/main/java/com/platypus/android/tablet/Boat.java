package com.platypus.android.tablet;

import android.os.Handler;
import android.os.Looper;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.platypus.crw.data.Pose3D;
import com.platypus.crw.data.Quaternion;
import com.platypus.crw.data.SensorData;
import com.platypus.crw.data.Utm;
import com.platypus.crw.data.UtmPose;

import org.jscience.geography.coordinates.LatLong;
import org.jscience.geography.coordinates.UTM;
import org.jscience.geography.coordinates.crs.ReferenceEllipsoid;

import java.net.InetSocketAddress;
import java.util.HashMap;
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
		final Object location_lock = new Object();
		LatLng new_crumb_LatLng = null;
		UTM new_crumb_UTM = null;
		HashMap<Long, double[]> crumb_map= new HashMap<>();
		final Object crumb_lock = new Object();
		double currentYaw = 0.0; // [-pi, pi]
		final Object yaw_lock = new Object();
		double[][] PID_gains = {{0., 0., 0.}, {0., 0., 0.}}; // thrust, heading
		final Object PID_lock = new Object();
		final int THRUST_GAIN_AXIS = 0;
		final int RUDDER_GAIN_AXIS = 5;
		final int SAMPLER_GAIN_AXIS = 7;
		ScheduledThreadPoolExecutor polling_thread_pool;
		Handler uiHandler = new Handler(Looper.getMainLooper());
		SensorData lastSensorDataReceived;
		Object sensor_lock = new Object();
		String waypointState;
		Object waypoint_state_lock = new Object();
		boolean[] sampler_running = {false, false, false, false};
		int boat_color;
		int line_color;

		abstract public void createListeners(
						final Runnable poseListenerCallback,
						final Runnable sensorListenerCallback,
						final Runnable waypointListenerCallback,
						final Runnable crumbListenerCallback);
		abstract public void startWaypoints(final double[][] waypoints, final Runnable failureCallback);
		abstract public void stopWaypoints(final Runnable failureCallback);
		abstract public void updateControlSignals(final double thrust, final double heading, final Runnable failureCallback);
		abstract public void setAutonomous(final boolean b, final Runnable failureCallback);
		abstract public void setPID(final double[] thrustPID, final double[] headingPID, final Runnable failureCallback);
		abstract public void addWaypoint(double[] waypoint, final Runnable failureCallback);
		abstract public void sendAutonomousPredicateMessage(String apm, final Runnable failureCallback);
		abstract public void setAddress(InetSocketAddress a);
		abstract public InetSocketAddress getIpAddress();
		abstract public void startSample(final int jar_number, final Runnable TimerStartRunnable, final Runnable failureCallback);
		abstract public void stopSample(final int jar_number, final Runnable successCallback, final Runnable failureCallback);
		abstract public void stopSampleAll(final Runnable successCallback, final Runnable failureCallback);
		abstract public void resetSampler(final Runnable successCallback, final Runnable failureCallback);

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

		public LatLng getNewCrumb()
		{
				synchronized (crumb_lock)
				{
						return new_crumb_LatLng;
				}
		}

		public static com.mapbox.mapboxsdk.geometry.LatLng jscienceLatLng_to_mapboxLatLng(org.jscience.geography.coordinates.LatLong jlatlng)
		{
				LatLng result = new LatLng(
								jlatlng.latitudeValue(SI.RADIAN)*180./Math.PI,
								jlatlng.longitudeValue(SI.RADIAN)*180./Math.PI);
				return result;
		}

		public static UTM UtmPose_to_UTM(UtmPose utmPose)
		{
				return UTM.valueOf (
								utmPose.origin.zone,
								utmPose.origin.isNorth ? 'T' : 'L',
								utmPose.pose.getX(),
								utmPose.pose.getY(),
								SI.METER
				);
		}
		public static UtmPose UTM_to_UtmPose(UTM utm)
		{
				if (utm == null) return null;
				Pose3D pose = new Pose3D(utm.eastingValue(SI.METER),
								utm.northingValue(SI.METER),
								0.0,
								Quaternion.fromEulerAngles(0, 0, 0));
				Utm origin = new Utm(utm.longitudeZone(),
								utm.latitudeZone() > 'O');
				return new UtmPose(pose, origin);
		}
		public static com.mapbox.mapboxsdk.geometry.LatLng UtmPose_to_LatLng(UtmPose utmPose)
		{
				return jscienceLatLng_to_mapboxLatLng(UTM.utmToLatLong(UtmPose_to_UTM(utmPose), ReferenceEllipsoid.WGS84));
		}

		public static double planarDistanceSq(Pose3D a, Pose3D b) {
				double dx = a.getX() - b.getX();
				double dy = a.getY() - b.getY();
				return dx * dx + dy * dy;
		}

		public static double angleBetween(Pose3D src, Pose3D dest) {
				return Math.atan2((dest.getY() - src.getY()),
								(dest.getX() - src.getX()));
		}

		public static double normalizeAngle(double angle) {
				while (angle > Math.PI)
						angle -= 2 * Math.PI;
				while (angle < -Math.PI)
						angle += 2 * Math.PI;
				return angle;
		}
}