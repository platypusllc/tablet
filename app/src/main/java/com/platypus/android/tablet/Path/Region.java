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

public class Region extends Path
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

		private static <T> ArrayList<T> shiftArrayList(ArrayList<T> aL, int shift)
		{
				// https://stackoverflow.com/questions/29548488/shifting-in-arraylist
				if (aL.size() == 0)
						return aL;

				T element = null;
				for(int i = 0; i < shift; i++)
				{
						// remove last element, add it to front of the ArrayList
						element = aL.remove( aL.size() - 1 );
						aL.add(0, element);
				}

				return aL;
		}

		private double distance(Double[] a, Double[] b)
		{
				return Math.sqrt(Math.pow(b[0]-a[0], 2.) + Math.pow(b[1]-a[1], 2.));
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
				ArrayList<Double[]> rolledPoints = shiftArrayList(points, 1);
				ArrayList<Double[]> result = new ArrayList<>();
				for (int i = 0; i < points.size(); i++)
				{
						Double[] a = points.get(i);
						Double[] b = rolledPoints.get(i);
						result.add(new Double[]{b[0]-a[0], b[1]-a[1]});
				}
				return result;
		}
		private ArrayList<Double> lineSegmentAngles(ArrayList<Double[]> points)
		{
				ArrayList<Double[]> rolledPoints = shiftArrayList(points, 1);
				ArrayList<Double> result = new ArrayList<>();
				for (int i = 0; i < points.size(); i++)
				{
						Double[] a = points.get(i);
						Double[] b = rolledPoints.get(i);
						result.add(Math.atan2(b[1]-a[1], b[0]-a[0]));
				}
				return result;
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

		private void inwardNextHull(ArrayList<Double[]> previous_hull)
		{
				// TODO: recursive. Given the previous set of points, calculate the next set of points. Append them to path_xy until the centroid is reached.
				// TODO: make sure the first points (i.e. path_xy is empty) are the first convex hull
				// TODO: make sure the final point (i.e. the final recursive call is completed) is the centroid

				// calculate minimum distance to centroid

				if (path_xy.size() < 1)
				{
						// first call: just add convex hull to path and recurse
						path_xy.addAll(previous_hull);
						inwardNextHull(previous_hull);
				}

				// TODO: find center points of the line segments that form the previous hull
				// TODO: find the normal angles of the line segments (make sure it points inward)
				// TODO: find the center and end points of the lines if they were moved inward along their normal by the transect distance
				//

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
				Double[] should_be_zero_centroid = calculateCentroid(convex_xy);
				/*
				if (!Arrays.equals(should_be_zero_centroid, new Double[]{0.0, 0.0}))
				{
						//Log.e(logTag, "Could not center convex hull around 0.0, 0.0");
						throw new Exception(String.format("Could not center convex hull around 0.0, 0.0: %f, %f",
										should_be_zero_centroid[0], should_be_zero_centroid[1]));
				}
				*/
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
