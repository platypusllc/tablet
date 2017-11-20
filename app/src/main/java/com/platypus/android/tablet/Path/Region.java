package com.platypus.android.tablet.Path;

import com.mapbox.mapboxsdk.geometry.LatLng;

import org.jscience.geography.coordinates.UTM;

import java.util.ArrayList;

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
		private AreaType area_type;
		private double transect_distance = 10;

		public Region (ArrayList<LatLng> _original_points, AreaType _area_type, double _transect_distance)
		{
				original_points = (ArrayList<LatLng>)_original_points.clone();
				convexHull();
				area_type = _area_type;
				transect_distance = _transect_distance;
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


		double distance(Double[] a, Double[] b)
		{
				return Math.sqrt(Math.pow(b[0]-a[0], 2.) + Math.pow(b[1]-a[1], 2.));
		}

		double distance_to_centroid(Double[] a)
		{
				return distance(a, centroid);
		}

		double min_distance_to_centroid(ArrayList<Double[]> points)
		{
				double min_distance = -1;
				for (int i = 0; i < points.size(); i++)
				{
						double d = distance_to_centroid(points.get(i));
						if (min_distance < 0 || d < min_distance) min_distance = d;
				}
				return min_distance;
		}

		void calculateCentroid(ArrayList<Double[]> points)
		{
				Double[] result = new Double[]{0.0, 0.0};
				for (Double[] p : points)
				{
						result[0] += p[0]/points.size();
						result[1] += p[1]/points.size();
				}
				centroid = result.clone();
		}

		ArrayList<Double[]> lineSegmentDifferences(ArrayList<Double[]> points)
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
		ArrayList<Double> lineSegmentAngles(ArrayList<Double[]> points)
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




		Path convertToSimplePath()
		{
				// TODO: create a simple Path from path_points
				return null;
		}

		void inwardNextHull(ArrayList<Double[]> previous_hull)
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

				// find center points of the line segments that form the previous hull
				// find the normal angles of the line segments (make sure it points inward)
				// find the center and end points of the lines if they were moved inward along their normal by the transect distance
				//

		}

		void convexHull()
		{
				// https://github.com/bkiers/GrahamScan

				// TODO: set convex_xy based on original_points
				// TODO: find centroid of convex_xy
				// TODO: call inwardNextHull with convex_xy
		}

}
