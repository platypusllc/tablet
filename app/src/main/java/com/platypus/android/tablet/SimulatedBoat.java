package com.platypus.android.tablet;
import android.util.Log;

import com.platypus.crw.data.Pose3D;
import com.platypus.crw.data.UtmPose;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.MaxCountExceededException;
import org.apache.commons.math3.ode.FirstOrderIntegrator;
import org.apache.commons.math3.ode.FirstOrderDifferentialEquations;
import org.apache.commons.math3.ode.nonstiff.ClassicalRungeKuttaIntegrator;

import org.jscience.geography.coordinates.LatLong;
import org.jscience.geography.coordinates.UTM;
import org.jscience.geography.coordinates.crs.ReferenceEllipsoid;

import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

/**
 * Created by jason on 7/12/17.
 *
 * A rough dynamics boat simulation.
 */

public class SimulatedBoat extends Boat
{
		InetSocketAddress addr;
		Runnable _poseListenerCallback = null;
		Runnable _sensorListenerCallback = null;
		Runnable _waypointListenerCallback = null;
		Runnable _crumbListenerCallback = null;
		final double NEW_CRUMB_DISTANCE = 5; // meters
		boolean executing_failsafe = false;
		double[][] _waypoints = new double[0][0];
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
						cu = 0.5;
						cw = 1.0;
						cth = 0.5;
						qdot[0] = u*Math.cos(th) - w*Math.sin(th);
						qdot[1] = u*Math.sin(th) + w*Math.cos(th);
						qdot[2] = 1.0/m*(thrustSurge - 0.5*1000.0*au*cu*Math.abs(u)*u);
						if (u < 0.25)
						{
								qdot[2] -= 5.0*u/m - Math.signum(u)*0.001;
						}
						qdot[3] = 1.0/m*(thrustSway - 0.5*1000.0*aw*cw*Math.abs(w)*w);
						qdot[4] = thdot;
						qdot[5] = 1.0/I*(torque - 0.5*1000.0*ath*cth*Math.abs(thdot)*thdot);
				}
		};


		public SimulatedBoat(String boat_name, UTM initial_utm)
		{
				name = boat_name;
				connected.set(true);
				Log.i("ODE", String.format("Creating simulated boat %s", name));
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
				long tLastControl = 0;
				double easting;
				double northing;
				double[] heading_pid = {0.3, 0., 0.3};
				double[] thrust_pid = {1.0, 0., 0.};
				double SUFFICIENT_PROXIMITY = 3.0;
				double LOOKAHEAD_BASE = 5.0;
				double lookahead;
				double distanceSq;

				int last_wp_index = -2;
				Pose3D destination_pose;
				double x_dest, x_source, x_current, y_dest, y_source, y_current, th_full, th_current;
				double x_projected, y_projected, x_lookahead, y_lookahead;
				double dx_current, dx_full, dy_current, dy_full, L_current, L_full, dth;
				double L_projected, distance_from_ideal_line;
				double heading_desired, heading_current, heading_error, heading_error_old, heading_error_deriv;
				double heading_signal, base_thrust, thrust_coefficient;
				double angle_from_projected_to_boat, cross_product, thrust_signal;

				private void control()
				{
						double dt = (t - tOld)/1000.;
						if (_waypoints.length <= 0 || current_waypoint_index.get() < 0)
						{
								Log.d("ODE", "Control: no waypoints to perform");
								synchronized (control_signals_lock)
								{
										thrustSignal = 0.;
										headingSignal = 0.;
								}
								return;
						}
						Log.v("ODE", String.format("current wp index = %d   last wp index = %d", current_waypoint_index.get(), last_wp_index));
						if (current_waypoint_index.get() != last_wp_index)
						{
								Log.i("ODE", String.format("new waypoint, # = %d", current_waypoint_index.get()));
								last_wp_index = current_waypoint_index.get();
								if (current_waypoint_index.get() == 0)
								{
										x_source = q[0];
										y_source = q[1];
								}
								else
								{
										x_source = x_dest;
										y_source = y_dest;
								}
								synchronized (waypoints_lock)
								{
										double[] latlng = _waypoints[current_waypoint_index.get()];
										UtmPose utmpose = new UtmPose(latlng);
										destination_pose = utmpose.pose;
								}
								x_dest = destination_pose.getX() - original_easting;
								y_dest = destination_pose.getY() - original_northing;
								dx_full = x_dest - x_source;
								dy_full = y_dest - y_source;
								th_full = Math.atan2(dy_full, dx_full);
								L_full = Math.sqrt(Math.pow(dx_full, 2.) + Math.pow(dy_full, 2.));
						}
						x_current = q[0];
						y_current = q[1];
						heading_current = q[4];

						distanceSq = Math.pow(x_dest - x_current, 2.0) + Math.pow(y_dest - y_current, 2.0);
						Log.v("ODE", String.format("source: %.0f, %.0f\ncurrent: %.0f, %.0f\ndest: %.0f, %.0f",
										x_source, y_source, x_current, y_current, x_dest, y_dest));
						Log.d("ODE", String.format("DistanceSq = %.1f", distanceSq));
						if (distanceSq <= SUFFICIENT_PROXIMITY*SUFFICIENT_PROXIMITY)
						{
								Log.i("ODE", String.format("Control: finished waypoint # %d", current_waypoint_index.get()));
								current_waypoint_index.incrementAndGet();
								if (current_waypoint_index.get() == _waypoints.length)
								{
										current_waypoint_index.set(-1); // finished last waypoint, reset
										last_wp_index = -2;

										synchronized (waypoints_lock)
										{
												_waypoints = new double[0][0]; // empty
										}
										synchronized (waypoint_state_lock)
										{
												waypointState = "DONE";
										}
										return;
								}
						}

						// Line following geometry
						dx_current = x_current - x_source;
						dy_current = y_current - y_source;
						th_current = Math.atan2(dy_current, dx_current);
						L_current = Math.sqrt(Math.pow(dx_current, 2.) + Math.pow(dy_current, 2.));
						dth = normalizeAngle(th_full - th_current);
						L_projected = L_current*Math.cos(dth);
						distance_from_ideal_line = L_current*Math.sin(dth);
						x_projected = x_source + L_projected*Math.cos(th_full);
						y_projected = y_source + L_projected*Math.sin(th_full);
						lookahead = LOOKAHEAD_BASE*(1. - Math.tanh(0.2*Math.abs(distance_from_ideal_line)));
						x_lookahead = x_projected + lookahead*Math.cos(th_full);
						y_lookahead = y_projected + lookahead*Math.sin(th_full);
						if (L_projected + lookahead > L_full)
						{
								x_lookahead = x_dest;
								y_lookahead = y_dest;
						}
						Log.v("ODE", String.format("dth = %.2f", dth*180./Math.PI));
						Log.d("ODE", String.format("Distance from ideal line = %.1f", distance_from_ideal_line));
						Log.d("ODE", String.format("Lookahead = %.1f", lookahead));

						heading_desired = Math.atan2(y_lookahead - y_current, x_lookahead - x_current);
						heading_error = normalizeAngle(heading_current - heading_desired);

						// PID
						heading_error_deriv = (heading_error - heading_error_old)/dt;
						heading_error_old = heading_error;
						heading_signal = heading_pid[0]*heading_error + heading_pid[2]*heading_error_deriv;
						if (Math.abs(heading_signal) > 1.0)
						{
								heading_signal = Math.copySign(1.0, heading_signal);
						}
						Log.d("ODE", String.format("Heading signal = %.1f", heading_signal));

						base_thrust = thrust_pid[0];
						angle_from_projected_to_boat = Math.atan2(y_lookahead - y_current, x_lookahead - x_current);
						cross_product = Math.cos(th_full)*Math.sin(angle_from_projected_to_boat)
													  - Math.cos(angle_from_projected_to_boat)*Math.sin(th_full);
						thrust_coefficient = 1.0;

						if (Math.abs(heading_error)*180./Math.PI > 45.0)
						{
								Log.d("ODE", "Heading error > 45 deg, cutting thrust");
								thrust_coefficient = 0.0;
						}
						thrust_signal = base_thrust*thrust_coefficient;

						synchronized (control_signals_lock)
						{
								thrustSignal = thrust_signal;
								headingSignal = heading_signal;
						}
				}

				private double[] motorSignals()
				{
						// calculate motor signals from thrust and moment signals, assuming Lutra tank
						double[] signals = new double[2];
						synchronized (control_signals_lock)
						{
								signals[0] = thrustSignal + headingSignal;
								signals[1] = thrustSignal - headingSignal;
						}
						for (int i = 0; i < 2; i++)
						{
								if (Math.abs(signals[i]) > 1.0)
								{
										signals[i] -= Math.signum(signals[i]) * (Math.abs(signals[i]) - 1.0);
								}
						}
						return signals;
				}

				private void thrustAndTorque()
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

				private void updateCrumb()
				{
						// create new crumb
						// 1) assign to latest crumb variables so GUI can be updated
						// 2) add the crumb to the overall list
						new_crumb_UTM = UTM.valueOf(
										original_utm.longitudeZone(),
										original_utm.latitudeZone(),
										easting,
										northing,
										SI.METER
						);
						new_crumb_LatLng = jscienceLatLng_to_mapboxLatLng(
										UTM.utmToLatLong(new_crumb_UTM, ReferenceEllipsoid.WGS84));
						newCrumb(new_crumb_UTM);
						uiHandler.post(_crumbListenerCallback);
				}

				@Override
				public void run()
				{
						// run control to find thrust and heading signals
						// take current thrust and heading signals, calculate thrust and torque
						// update thrust and torque values
						// run ode integrator
						t = System.currentTimeMillis();
						Log.v("ODE", String.format("ODE: t = %.2f", (t - t0)/1000.));
						if (!autonomous.get())
						{
								// nothing
						}
						else if (_waypoints.length == 0)
						{
								current_waypoint_index.set(-1);
								synchronized (waypoint_state_lock)
								{
										waypointState = "DONE";
								}
								synchronized (control_signals_lock)
								{
										thrustSignal = 0.;
										headingSignal = 0.;
								}
						}
						else
						{
								synchronized (waypoint_state_lock)
								{
										waypointState = "GOING";
								}
								Log.v("ODE", "Boat is autonomous and has waypoints. Calling control()");
								if (t - tLastControl > 200) // 5 Hz control
								{
										control();
										tLastControl = t;
								}
						}
						uiHandler.post(_waypointListenerCallback); // update GUI with result
						thrustAndTorque();
						Log.v("ODE", String.format("thrust = %.2f  torque = %.2f", thrustSurge, torque));
						rk4.integrate(ode, (tOld - t0)/1000., qOld, (t - t0)/1000., q);
						easting = q[0] + original_easting;
						northing = q[1] + original_northing;
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
						qOld = q.clone();
						if (_poseListenerCallback != null) uiHandler.post(_poseListenerCallback); // update GUI with result
						if (new_crumb_UTM == null)
						{
								Log.i("aStar", "First crumb");
								updateCrumb();
						}
						else if (!executing_failsafe)
						{
								double dx_to_last_crumb = q[0] - (new_crumb_UTM.eastingValue(SI.METER) - original_easting);
								double dy_to_last_crumb = q[1] - (new_crumb_UTM.northingValue(SI.METER) - original_northing);
								if (Math.pow(dx_to_last_crumb, 2.) + Math.pow(dy_to_last_crumb, 2.) >= Math.pow(NEW_CRUMB_DISTANCE, 2.))
								{
										// TODO: test A* by forcing it to activate
										updateCrumb();
										if (crumbs_by_index.size() > 30)
										{
												executing_failsafe = true;
												new Thread()
												{
														@Override
														public void run()
														{
																startPathSequence(aStar(new_crumb_UTM, original_utm));
														}
												}.start();
										}
								}
						}
						else
						{
								//Log.d("aStar", "Executing failsafe...");
						}
				}
		};

		@Override
		public void createListeners(final Runnable poseListenerCallback,
		                            final Runnable sensorListenerCallback,
		                            final Runnable waypointListenerCallback,
		                            final Runnable crumbListenerCallback)
		{
				_poseListenerCallback = poseListenerCallback;
				_sensorListenerCallback = sensorListenerCallback;
				_waypointListenerCallback = waypointListenerCallback;
				_crumbListenerCallback = crumbListenerCallback;
				polling_thread_pool = new ScheduledThreadPoolExecutor(1);
				polling_thread_pool.scheduleAtFixedRate(
								kinematicSimulationLoop, 0, DYNAMICS_POLL_MS, TimeUnit.MILLISECONDS);
				Log.i("ODE", "Started simulation thread");
		}

		@Override
		public void startWaypoints(double[][] waypoints, Runnable failureCallback)
		{
				synchronized (waypoints_lock)
				{
						_waypoints = waypoints.clone();
				}
				current_waypoint_index.set(0);
				autonomous.set(true);
				Log.i("ODE", "Starting new waypoints");
		}

		@Override
		public void stopWaypoints(Runnable failureCallback)
		{
				current_waypoint_index.set(-1);
				autonomous.set(false);
				synchronized (waypoint_state_lock)
				{
						waypointState = "CANCELLED";
				}
				synchronized (control_signals_lock)
				{
						thrustSignal = 0.0;
						headingSignal = 0.0;
				}
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
						autonomous.set(false); // TODO: is this necessary?
						synchronized (control_signals_lock)
						{
								thrustSignal = thrust;
								headingSignal = heading;
						}
				}

		}

		@Override
		public void setAutonomous(boolean b, Runnable failureCallback)
		{
				autonomous.set(b);
				if (b)
				{
						if (_waypoints.length > 0)
						{
								synchronized (waypoint_state_lock)
								{
										waypointState = "GOING";
								}
						}
				}
				else
				{
						synchronized (waypoint_state_lock)
						{
								waypointState = "PAUSED";
						}
						synchronized (control_signals_lock)
						{
								thrustSignal = 0.;
								headingSignal = 0.;
						}
				}
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
		public void addWaypoint(double[] waypoint, Runnable failureCallback)
		{
				double[][] waypoints = {waypoint};
				startWaypoints(waypoints, failureCallback);
		}

		@Override
		public void setAddress(InetSocketAddress a) { addr = a; }

		@Override
		public InetSocketAddress getIpAddress()
		{
				return addr;
		}

		///////////////////////////////////////////////////////////////////////////
		// BREADCRUMBS STUFF
		///////////////////////////////////////////////////////////////////////////

		final double MAX_NEIGHBOR_DISTANCE = 10;
		Map<Long, Crumb> crumbs_by_index = new HashMap<>();
		Map<Long, Map<Long, Double>> pairwise_distances = new HashMap<>();
		Map<Long, List<Long>> neighbors = new HashMap<>();
		public double distanceBetweenUTM(UTM location_i, UTM location_j)
		{
				double dx = (location_i.eastingValue(SI.METER) - location_j.eastingValue(SI.METER));
				double dy = (location_i.northingValue(SI.METER) - location_j.northingValue(SI.METER));
				return Math.sqrt(dx*dx + dy*dy);
		}
		public double distanceBetweenCrumbs(long index_i, long index_j)
		{
				//Log.v("aStar", String.format("distanceBetweenCrumbs(%d, %d)", index_i, index_j));
				UTM location_i = crumbs_by_index.get(index_i).getLocation();
				UTM location_j = crumbs_by_index.get(index_j).getLocation();
				return distanceBetweenUTM(location_i, location_j);
		}
		public long newCrumb(UTM _location)
		{
				// initialize objects
				long new_index = crumbs_by_index.size();
				Crumb new_crumb = new Crumb(new_index, _location);
				crumbs_by_index.put(new_index, new_crumb);
				pairwise_distances.put(new_index, new HashMap<Long, Double>());
				neighbors.put(new_index, new ArrayList<Long>());

				// calculate pairwise distances and neighbors
				for (Map.Entry<Long, Crumb> entry_i : crumbs_by_index.entrySet())
				{
						long index_i = entry_i.getKey();
						for (Map.Entry<Long, Crumb> entry_j : crumbs_by_index.entrySet())
						{
								long index_j = entry_j.getKey();

								// if a Crumb is being compared to itself
								// OR
								// if a calculation was previously performed for pair (i,j)
								if (index_i == index_j || pairwise_distances.get(index_i).containsKey(index_j))
								{
										continue; // don't perform the calculations
								}

								double pairwise_distance = distanceBetweenCrumbs(index_i, index_j);
								// Log.v("aStar", String.format("%d -- %d pairwise distance = %.2f", index_i, index_j, pairwise_distance));
								pairwise_distances.get(index_i).put(index_j, pairwise_distance);
								pairwise_distances.get(index_j).put(index_i, pairwise_distance);
								if (pairwise_distance <= MAX_NEIGHBOR_DISTANCE)
								{
										neighbors.get(index_i).add(index_j);
										neighbors.get(index_j).add(index_i);
								}
						}
				}
				return new_index;
		}

		public List<Long> straightHome(UTM start, UTM goal)
		{
				// Simple: go straight home from the start
				List<Long> path_sequence = new ArrayList<>();
				long start_index = newCrumb(start);
				long goal_index = newCrumb(goal);
				path_sequence.add(start_index);
				path_sequence.add(goal_index);
				return path_sequence;
		}

		public List<Long> aStar(UTM start, UTM goal)
		{
				Log.i("aStar", "Starting A* calculation...");
				long start_index = newCrumb(start);
				long goal_index = newCrumb(goal);

				// fill in distance to goal values
				for (Map.Entry<Long, Crumb> entry : crumbs_by_index.entrySet())
				{
						entry.getValue().setH(distanceBetweenCrumbs(entry.getKey(), goal_index));
				}

				// make sure start is reachable (i.e. it has at least one neighbor)
				// TODO: force the start to be reachable

				// make sure goal is reachable (i.e. it has at least one neighbor)
				// TODO: force the goal to be reachable

				HashMap<Long, Void> open_crumbs = new HashMap<>();
				HashMap<Long, Double> open_costs = new HashMap<>();
				HashMap<Long, Void> closed_crumbs = new HashMap<>();
				List<Long> path_sequence = new ArrayList<>();
				long current_crumb = 0;
				long iterations = 0;
				open_crumbs.put(start_index, null);
				Log.d("aStar", String.format("open_crumbs.size() = %d", open_crumbs.size()));

				while (open_crumbs.size() > 0)
				{
						iterations += 1;
						Log.d("aStar", String.format("A* iter %d:  %d open, %d closed",
										iterations, open_crumbs.size(), closed_crumbs.size()));

						// recreate open costs map
						open_costs.clear();
						double lowest_cost = 99999999;
						for (Map.Entry<Long, Void> entry : open_crumbs.entrySet())
						{
								double cost = crumbs_by_index.get(entry.getKey()).getCost();
								open_costs.put(entry.getKey(), cost);
								if (cost < lowest_cost)
								{
										//Log.v("aStar", String.format("crumb # %d has lowest cost = %.2f", entry.getKey(), cost));
										lowest_cost = cost;
										current_crumb = entry.getKey();
								}
						}
						Log.d("aStar", String.format("Current crumb index = %d", current_crumb));
						if (current_crumb == goal_index)
						{
								Log.i("aStar", String.format("Reached goal crumb %d, exiting loop", goal_index));
								break; // reached goal node, exit loop
						}
						Log.d("aStar", String.format("Current crumb has %d neighbors: %s",
										neighbors.get(current_crumb).size(),
										neighbors.get(current_crumb).toString()));
						for (long s : neighbors.get(current_crumb))
						{
								double potential_g = crumbs_by_index.get(current_crumb).getG() + pairwise_distances.get(current_crumb).get(s);
								if (open_crumbs.containsKey(s))
								{
										if (crumbs_by_index.get(s).getG() <= potential_g) continue;
								}
								else if (closed_crumbs.containsKey(s))
								{
										if (crumbs_by_index.get(s).getG() > potential_g)
										{
												open_crumbs.put(s, null);
												closed_crumbs.remove(s);
										}
										else
										{
												continue;
										}
								}
								else
								{
										open_crumbs.put(s, null);
								}
								crumbs_by_index.get(s).setG(potential_g);
								crumbs_by_index.get(s).setParent(current_crumb);
						}
						open_crumbs.remove(current_crumb);
						closed_crumbs.put(current_crumb, null);
				}
				path_sequence.add(current_crumb);
				while (path_sequence.get(0) != start_index)
				{
						path_sequence.add(0, crumbs_by_index.get(path_sequence.get(0)).getParent());
				}
				Log.i("aStar", "Path sequence = " + path_sequence.toString());
				return path_sequence;
		}

		public void startPathSequence(List<Long> path_sequence)
		{
				double[][] waypoints = new double[path_sequence.size()][2];
				for (int i = 0; i < path_sequence.size(); i++)
				{
						//waypoints[i] = UTM_to_UtmPose(crumbs_by_index.get(path_sequence.get(i)).getLocation());
						UTM utm = crumbs_by_index.get(path_sequence.get(i)).getLocation();
						LatLong latlong = UTM.utmToLatLong(utm, ReferenceEllipsoid.WGS84);
						waypoints[i] = new double[]{latlong.latitudeValue(NonSI.DEGREE_ANGLE), latlong.longitudeValue(NonSI.DEGREE_ANGLE)};
				}
				startWaypoints(waypoints, null);
		}
}
