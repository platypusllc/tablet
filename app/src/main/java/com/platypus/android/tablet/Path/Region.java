package com.platypus.android.tablet.Path;

import android.util.Log;

import com.mapbox.mapboxsdk.geometry.LatLng;

import org.jscience.geography.coordinates.LatLong;
import org.jscience.geography.coordinates.UTM;
import org.jscience.geography.coordinates.crs.ReferenceEllipsoid;

import java.util.ArrayList;
import java.util.Arrays;

import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

/**
 * Created by jason on 11/20/17.
 */

public class Region
{
		private ArrayList<LatLng> original_points = new ArrayList<>();
		private ArrayList<Double[]> convex_xy = new ArrayList<>();
		private ArrayList<Double[]> path_xy = new ArrayList<>();
		private ArrayList<LatLng> path_points = new ArrayList<>();
		private Double[] centroid = new Double[2];
		private UTM original_utm;
		private AreaType area_type;
		private double transect_distance = 10;
		private String logTag = "Region";

		public Region (ArrayList<LatLng> _original_points, AreaType _area_type, double _transect_distance) throws Exception
		{
				original_points = (ArrayList<LatLng>)_original_points.clone();
				convexHull();
				area_type = _area_type;
				transect_distance = _transect_distance;
				switch (area_type)
				{
						case SPIRAL:
								inwardNextHull(convex_xy);
								break;

						case LAWNMOWER:
								// TODO: fit rectangle over hull (rotated to have long axis sitting on hull diameter)
								// TODO: calculate equations for lines back and forth across this rectangle
								// TODO: calculate intersections between hull and the lawnmower lines
								// TODO: properly order the intersections to generate the lawnmower path
								break;

						default:
								Log.e(logTag, "Unknown region type");
								throw new Exception("Unknown region type");
				}
		}

		public Path convertToSimplePath()
		{
				// create a simple Path from path_points
				path_points.clear();
				for (Double[] p : path_xy)
				{
						// add back in the UTM offset
						p[0] += centroid[0];
						p[1] += centroid[1];
						LatLong latLong = UTM.utmToLatLong(UTM.valueOf(original_utm.longitudeZone(), original_utm.latitudeZone(), p[0], p[1], SI.METER), ReferenceEllipsoid.WGS84);
						path_points.add(new LatLng(latLong.latitudeValue(NonSI.DEGREE_ANGLE), latLong.longitudeValue(NonSI.DEGREE_ANGLE)));
				}
				return new Path(path_points);
		}

		private static double wrapToPi(double angle)
		{
				while (Math.abs(angle) > Math.PI)
				{
						angle -= 2*Math.PI*Math.signum(angle);
				}
				return angle;
		}

		private static <T> ArrayList<T> shiftArrayList(ArrayList<T> aL, int shift)
		{
				while (Math.abs(shift) > aL.size())
				{
						shift -= aL.size()*Math.signum(shift);
				}
				if (shift < 0)
				{
						shift = aL.size() + shift; // shifting backward is like shifting forward by more
				}
				Log.v("Region", String.format("Shifting forward by %d", shift));
				// https://stackoverflow.com/questions/29548488/shifting-in-arraylist
				ArrayList<T> aL_clone = (ArrayList<T>)aL.clone();
				if (aL.size() == 0)
						return aL_clone;

				T element = null;
				for(int i = 0; i < shift; i++)
				{
						// remove last element, add it to front of the ArrayList
						element = aL_clone.remove( aL_clone.size() - 1 );
						aL_clone.add(0, element);
				}

				return aL_clone;
		}

		private Double[] difference(Double[] a, Double[] b)
		{
				return new Double[]{b[0]-a[0], b[1] - a[1]};
		}

		private double distance(Double[] a, Double[] b)
		{
				Double[] diff = difference(a, b);
				return Math.sqrt(Math.pow(diff[0], 2.) + Math.pow(diff[1], 2.));
		}
		private double dot(Double[] a, Double[] b)
		{
				return a[0]*b[0] + a[1]*b[1];
		}

		private double distance_to_centroid(Double[] a)
		{
				return distance(a, centroid);
		}

		private double min_distance_to_centroid(ArrayList<Double[]> points)
		{
				double min_distance = -1;
				for (int i = 0; i < points.size(); i++)
				{
						double d = distance_to_centroid(points.get(i));
						if (min_distance < 0 || d < min_distance) min_distance = d;
				}
				return min_distance;
		}

		private Double[] calculateCentroid(ArrayList<Double[]> points)
		{
				Double[] result = new Double[]{0.0, 0.0};
				for (Double[] p : points)
				{
						result[0] += p[0]/points.size();
						result[1] += p[1]/points.size();
				}
				return result;
		}

		private ArrayList<Double[]> lineSegmentDifferences(ArrayList<Double[]> points)
		{
				ArrayList<Double[]> rolled_points = shiftArrayList(points, 1);
				ArrayList<Double[]> result = new ArrayList<>();
				for (int i = 0; i < points.size(); i++)
				{
						Double[] a = points.get(i);
						Double[] b = rolled_points.get(i);
						result.add(new Double[]{b[0]-a[0], b[1]-a[1]});
				}
				return result;
		}
		private ArrayList<Double> lineSegmentNormalAngles(ArrayList<Double[]> points)
		{
				ArrayList<Double[]> rolled_points = shiftArrayList(points, 1);

				StringBuilder sb0, sb1;
				sb0 = new StringBuilder();
				sb1 = new StringBuilder();
				for (int i = 0; i < points.size(); i++)
				{
						String a = String.format("[%.0f, %.0f]\n", points.get(i)[0], points.get(i)[1]);
						String b = String.format("[%.0f, %.0f]\n", rolled_points.get(i)[0], rolled_points.get(i)[1]);
						sb0.append(a);
						sb1.append(b);
				}
				Log.v(logTag, "points = \n" + sb0.toString());
				Log.v(logTag, "rolled_points = \n" + sb1.toString());

				ArrayList<Double> result = new ArrayList<>();
				for (int i = 0; i < points.size(); i++)
				{
						Double[] a = rolled_points.get(i);
						Double[] b = points.get(i);
						Log.d(logTag, String.format("a = [%.0f, %.0f], b = [%.0f, %.0f]", a[0], a[1], b[0], b[1]));
						double raw_angle = Math.atan2(b[1]-a[1], b[0]-a[0]) + Math.PI/2.0;
						result.add(wrapToPi(raw_angle));
				}
				return result;
		}

		private Double[] intersection(Double[] line1_point1, Double[] line1_point2, Double[] line2_point1, Double[] line2_point2)
		{
				Double[] d1 = difference(line1_point1, line1_point2);
				Double[] d2 = difference(line2_point1, line2_point2);
				Double[] dp = difference(line2_point1, line1_point1);
				Double[] d1p = new Double[]{-d1[1], d1[0]};
				double denominator = dot(d1p, d2);
				double numerator = dot(d1p, dp);
				double c = numerator/denominator;
				return new Double[]{c*d2[0] + line2_point1[0], c*d2[1] + line2_point1[1]};
		}

		private void inwardNextHull(ArrayList<Double[]> previous_hull)
		{
				// recursive. Given the previous set of points, calculate the next set of points. Append them to path_xy until the centroid is reached.
				// the first points (i.e. path_xy is empty) are the first convex hull
				// TODO: make sure the final point (i.e. the final recursive call is completed) is the centroid

				// calculate minimum distance to centroid

				if (path_xy.size() < 1)
				{
						// first call: just add convex hull to path and recurse
						path_xy.addAll(previous_hull);
						inwardNextHull(previous_hull);
						return; // recursion stack unravels here, so return, now with path_xy fully populated
				}

				// find the normal angles of the line segments (make sure it points inward)
				ArrayList<Double> normal_angles = lineSegmentNormalAngles(previous_hull);
				ArrayList<Double> rolled_angles = shiftArrayList(normal_angles, -1);

				// find the points if they were moved inward along their normal by the transect distance
				ArrayList<Double[]> new_segments_point_1 = new ArrayList<>();
				ArrayList<Double[]> new_segments_point_2 = new ArrayList<>();
				for (int i = 0; i < previous_hull.size(); i++)
				{
						Double[] p = previous_hull.get(i);
						double angle = normal_angles.get(i);
						double rolled_angle = rolled_angles.get(i);
						Log.v(logTag, String.format("Normal angle: %f", angle));
						Log.v(logTag, String.format("Rolled normal angle: %f", rolled_angle));
						new_segments_point_1.add(new Double[]{
										p[0] + transect_distance*Math.cos(angle),
										p[1] + transect_distance*Math.sin(angle)});
						new_segments_point_2.add(new Double[]{
										p[0] + transect_distance*Math.cos(rolled_angle),
										p[1] + transect_distance*Math.sin(rolled_angle)});
				}

				// shift new_segments_point_2 so that the proper new lines have their indices aligned
				new_segments_point_2 = shiftArrayList(new_segments_point_2, 1);

				// find intersections between the new lines
				ArrayList<Double[]> new_segments_point_1_rolled = shiftArrayList(new_segments_point_1, 1);
				ArrayList<Double[]> new_segments_point_2_rolled = shiftArrayList(new_segments_point_2, 1);
				ArrayList<Double[]> intersections = new ArrayList<>();
				for (int i = 0; i < new_segments_point_1.size(); i++)
				{
						intersections.add(intersection(
										new_segments_point_1.get(i),
										new_segments_point_2.get(i),
										new_segments_point_1_rolled.get(i),
										new_segments_point_2_rolled.get(i)));
				}
				path_xy.addAll(intersections);

				// TODO: determine if this is the final iteration, if not, recurse using intersections
		}

		private void convexHull() throws Exception
		{
				// set convex_xy based on original_points
				ArrayList<Double[]> utm_points = latLngToUTM(original_points);
				ArrayList<Integer> convex_indices = GrahamScan.getConvexHull(utm_points);
				ArrayList<Double[]> utm_hull = new ArrayList<>();
				for (Integer i : convex_indices)
				{
						utm_hull.add(utm_points.get(i));
				}
				convex_xy = zeroCentroid(utm_hull);
				// Double[] should_be_zero_centroid = calculateCentroid(convex_xy);
		}

		private ArrayList<Double[]> latLngToUTM(ArrayList<LatLng> points)
		{
				ArrayList<Double[]> result = new ArrayList<>();
				for (LatLng wp : points)
				{
						UTM utm = UTM.latLongToUtm(LatLong.valueOf(wp.getLatitude(), wp.getLongitude(), NonSI.DEGREE_ANGLE), ReferenceEllipsoid.WGS84);
						result.add(new Double[]{utm.eastingValue(SI.METER), utm.northingValue(SI.METER)});
				}
				original_utm = UTM.latLongToUtm(LatLong.valueOf(points.get(0).getLatitude(), points.get(0).getLongitude(), NonSI.DEGREE_ANGLE), ReferenceEllipsoid.WGS84);
				return result;
		}

		private ArrayList<Double[]> zeroCentroid(ArrayList<Double[]> points)
		{
				centroid = calculateCentroid(points);
				ArrayList<Double[]> result = new ArrayList<>();
				for (int i = 0; i < points.size(); i++)
				{
						Double[] p = points.get(i);
						result.add(new Double[]{p[0] - centroid[0], p[1] - centroid[1]});
				}
				return result;
		}

}
