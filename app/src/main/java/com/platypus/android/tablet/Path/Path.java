package com.platypus.android.tablet.Path;

import java.util.ArrayList;

import com.mapbox.mapboxsdk.geometry.LatLng;

public class Path
{
		int current_index = 0;

		protected ArrayList<LatLng> points = new ArrayList<LatLng>();
		protected ArrayList<LatLng> quickHulledPoints = new ArrayList<LatLng>();
		protected double transectDistance = .00000898 * 5; //10 meters, initial value
		protected final double ONE_METER = transectDistance / 10;

		public Path()
		{
				current_index = 0;
		}

		public Path(ArrayList<LatLng> list)
		{
				points = list;
				current_index = 0;
		}

		public ArrayList<LatLng> getPoints()
		{
				return points;
		}

		public ArrayList<ArrayList<LatLng>> getPointPairs()
		{
				ArrayList<ArrayList<LatLng>> point_pairs = new ArrayList<>();
				for (int i = 0; i < points.size() - 1; i++)
				{
						ArrayList<LatLng> pair = new ArrayList<>();
						pair.add(points.get(i));
						pair.add(points.get(i + 1));
						point_pairs.add(pair);
				}
				return point_pairs;
		}

		public ArrayList<LatLng> getOriginalPoints()
		{
				return points;
		}

		public void setPoints(ArrayList<LatLng> list)
		{
				points = list;
		}

		public void clearPoints()
		{
				points.clear();
		}

		public void updateTransect(double meters)
		{
				transectDistance = meters * ONE_METER;
		}

		public void updateRegionPoints()
		{
		}

		public ArrayList<LatLng> getQuickHullList()
		{
				if (points.size() < 3)
				{
						return points;
				}
				return quickHulledPoints;
		}

		public AreaType getAreaType()
		{
				return null;
		}
}
