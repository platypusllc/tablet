package com.platypus.android.tablet;
import com.platypus.crw.data.UtmPose;
import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.MaxCountExceededException;
import org.apache.commons.math3.ode.FirstOrderIntegrator;
import org.apache.commons.math3.ode.FirstOrderDifferentialEquations;
import org.apache.commons.math3.ode.nonstiff.ClassicalRungeKuttaIntegrator;

import org.jscience.geography.coordinates.UTM;
import org.jscience.geography.coordinates.crs.ReferenceEllipsoid;

import javax.measure.unit.SI;

/**
 * Created by jason on 7/12/17.
 *
 * A rough dynamics boat simulation.
 */

public class SimulatedBoat extends Boat
{
		Runnable _poseListenerCallback = null;
		Runnable _sensorListenerCallback = null;
		Runnable _waypointListenerCallback = null;
		UtmPose[] _waypoints = new UtmPose[0];
		Object waypoints_lock = new Object();
		double thrustSignal, headingSignal;
		double thrustSurge, thrustSway, torque;
		Object control_signals_lock = new Object();
		final int DYNAMICS_POLL_MS = 200;
		UTM original_utm;
		double original_easting, original_northing;
		final double max_forward_thrust_per_motor = 25; // N
		final double max_backward_thrust_per_motor = 10; // N
		final double moment_arm = 0.3556; // distance between the motors [m]
		FirstOrderIntegrator rk4 = new ClassicalRungeKuttaIntegrator(0.05);
		FirstOrderDifferentialEquations ode = new FirstOrderDifferentialEquations()
		{
				double m, I, u, w, th, thdot, au, aw, ath, cu, cw, cth;

				@Override
				public int getDimension()
				{
						return 6;
				}

				@Override
				public void computeDerivatives(double t, double[] q, double[] qdot) throws MaxCountExceededException, DimensionMismatchException
				{
						m = 6; // kg
						I = 0.6; // kg/m^2
						u = q[2];
						w = q[3];
						th = q[4];
						thdot = q[5];
						au = 0.0108589939; // m^2
						aw = 0.0424551192;
						ath = 0.0424551192;
						cu = 0.258717640651218;
						cw = 1.088145891415693;
						cth = 0.048292066650533;
						qdot[0] = u*Math.cos(th) - w*Math.sin(th);
						qdot[1] = u*Math.sin(th) + w*Math.cos(th);
						qdot[2] = 1.0/m*(thrustSurge - 0.5*1000.0*au*cu*Math.abs(u)*u);
						qdot[3] = 1.0/m*(thrustSway - 0.5*1000.0*aw*cw*Math.abs(w)*w);
						qdot[4] = thdot;
						qdot[5] = 1.0/I*(torque - 0.5*1000.0*ath*cth*Math.abs(thdot)*thdot);
				}
		};


		public SimulatedBoat(String boat_name, UTM initial_utm)
		{
				name = boat_name;
				connected.set(true);
				original_utm = initial_utm.copy();
				original_easting = initial_utm.eastingValue(SI.METER);
				original_northing = initial_utm.northingValue(SI.METER);
		}

		private Runnable kinematicSimulationLoop = new Runnable()
		{
				double[] qOld = new double[6];
				double[] q = new double[6];
				long t0 = System.currentTimeMillis();
				long tOld = System.currentTimeMillis();
				long t = System.currentTimeMillis();
				double easting;
				double northing;

				public double[] control()
				{
						// TODO: perform some kind of autonomous control. Return thrust and moment signals
						thrustSignal = 0.;
						headingSignal = 0.;
						return null;
				}

				public double[] motorSignals()
				{
						// calculate motor signals from thrust and moment signals, assuming Lutra tank
						double[] signals = new double[2];
						signals[0] = thrustSignal + headingSignal;
						signals[1] = thrustSignal - headingSignal;
						for (int i = 0; i < 2; i++)
						{
								if (Math.abs(signals[i]) > 1.0)
								{
										signals[i] -= Math.signum(signals[i]) * (Math.abs(signals[i]) - 1.0);
								}
						}
						return signals;
				}

				public void thrustAndTorque()
				{
						// calculate thrust and torque from motor signals, assuming Lutra tank
						double[] motor_signals = motorSignals();
						double[] motor_thrusts = new double[2];
						for (int i = 0; i < 2; i++)
						{
								if (motor_signals[i] > 0)
								{
										motor_thrusts[i] = max_forward_thrust_per_motor * motor_signals[i];
								}
								else
								{
										motor_thrusts[i] = max_backward_thrust_per_motor * motor_signals[i];
								}
						}
						thrustSurge = motor_thrusts[0] + motor_thrusts[1];
						thrustSway = 0.0;
						torque = (motor_thrusts[1] - motor_thrusts[0])/2.0*moment_arm;
				}

				@Override
				public void run()
				{
						// run control to find thrust and heading signals
						// take current thrust and heading signals, calculate thrust and torque
						// update thrust and torque values
						// run ode integrator
						t = System.currentTimeMillis();
						// TODO: calculate the thrust and torque created from the motor signals
						if (autonomous.get())
						{
								control();
						}
						thrustAndTorque();
						rk4.integrate(ode, 1000.*(tOld - t0), qOld, 1000.*(t - t0), q);
						easting = q[0] + original_easting;
						northing = q[1] + original_northing;
						// TODO: take the updated easting and northing and update the boat location
						setYaw(Math.PI / 2 - q[4]);
						setLocation(
										jscienceLatLng_to_mapboxLatLng(
														UTM.utmToLatLong(
																		UTM.valueOf(
																						original_utm.longitudeZone(),
																						original_utm.latitudeZone(),
																						easting,
																						northing,
																						SI.METER
																		),
																		ReferenceEllipsoid.WGS84
														)
										)
						);
						tOld = t;
						if (_poseListenerCallback != null) uiHandler.post(_poseListenerCallback); // update GUI with result
				}
		};

		@Override
		public void createListeners(final Runnable poseListenerCallback,
		                            final Runnable sensorListenerCallback,
		                            final Runnable waypointListenerCallback)
		{
				_poseListenerCallback = poseListenerCallback;
				_sensorListenerCallback = sensorListenerCallback;
				_waypointListenerCallback = waypointListenerCallback;
				polling_thread_pool = new ScheduledThreadPoolExecutor(2);
				polling_thread_pool.scheduleAtFixedRate(
								kinematicSimulationLoop, 0, DYNAMICS_POLL_MS, TimeUnit.MILLISECONDS);
		}

		@Override
		public void startWaypoints(UtmPose[] waypoints, String controller_name, Runnable failureCallback)
		{
				synchronized (waypoints_lock)
				{
						_waypoints = waypoints.clone();
				}
				current_waypoint_index.set(0);
				autonomous.set(true);
		}

		@Override
		public void stopWaypoints(Runnable failureCallback)
		{
				current_waypoint_index.set(-1);
				autonomous.set(false);
		}

		@Override
		public void updateControlSignals(double thrust, double heading, Runnable failureCallback)
		{
				// Joystick generates many events per second!
				// Only send them if a little time has passed or if a (0,0) "all stop" has occurred
				if (System.currentTimeMillis() - time_of_last_joystick.get() > 100 ||
								(thrust == 0.0 && heading == 0.0))
				{
						time_of_last_joystick.set(System.currentTimeMillis());
						synchronized (control_signals_lock)
						{
								autonomous.set(false);
								thrustSignal = thrust;
								headingSignal = heading;
						}
				}

		}

		@Override
		public void setAutonomous(boolean b, Runnable failureCallback)
		{
				autonomous.set(b);
		}

		@Override
		public void setPID(double[] thrustPID, double[] headingPID, Runnable failureCallback)
		{
				synchronized (PID_lock)
				{
						PID_gains[0] = thrustPID.clone();
						PID_gains[1] = headingPID.clone();
				}
		}

		@Override
		public void addWaypoint(UtmPose waypoint, String controller_name, Runnable failureCallback)
		{
				UtmPose[] wpPose = {waypoint};
				startWaypoints(wpPose, controller_name, failureCallback);
		}

		@Override
		public void setAddress(InetSocketAddress a) { }

		@Override
		public InetSocketAddress getIpAddress()
		{
				return null;
		}
}
