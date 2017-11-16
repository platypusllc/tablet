package com.platypus.android.tablet;

import android.app.Activity;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.mapbox.mapboxsdk.geometry.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;

/**
 * Created by jason on 11/16/17.
 *
 * Save and load paths (sequences of latitude, longitude values).
 * Uses JSON objects and JSONArray objects
 *
 */

class SavedWaypointsStuff
{
		private String logTag = "SavedWaypointsStuff";

		void saveWaypointsToFile(ArrayList<LatLng> points)
		{
				// if the default waypoints file (one big JSON of missions and paths) doesn't exist, create it
				File waypoint_file = new File(Environment.getExternalStorageDirectory() + "/waypoints/saved_waypoint_paths.txt");
				try
				{
						if (!waypoint_file.exists()) waypoint_file.createNewFile();
				}
				catch (Exception e)
				{
						Log.e(logTag, String.format("saveWaypointsToFile error: %s", e.getMessage()));
				}


				// TODO: dialog to select a file and to append or overwrite
				// append or overwrite the points to the file
		}

		ArrayList<LatLng> loadWaypointsFromFile()
		{
				// TODO: dialog to select a file
				String filename = "";
				File user_file = new File(filename);
				HashMap<String, ArrayList<LatLng>> available_paths_map = parseWaypointsFile(user_file);

				// make sure to return null if user selects options that lead to no real path
				if (available_paths_map == null)
				{
						// Toast saying there were no paths in the proper format found in the file
						return null;
				}

				// TODO: dialog to choose a path from the available ones (spinner with the above map's keys as options)
				String user_path = "";

				// return the path selected by the user
				return available_paths_map.get(user_path);
		}

		private HashMap<String, ArrayList<LatLng>> parseWaypointsFile(File file)
		{
				// TODO: read the JSON format saved waypoints file
				Scanner fileScanner;
				try
				{
						fileScanner = new Scanner(file);
				}
				catch (Exception e)
				{
						Log.e(logTag, String.format("readWaypointsFile() error: %s", e.getMessage()));
						return null;
				}
				StringBuffer buffer = new StringBuffer();
				if (file.exists())
				{
						while (fileScanner.hasNext())
						{
								buffer.append(fileScanner.nextLine());
						}
				}
				String human_string = buffer.toString();
				JSONTokener tokener = new JSONTokener(human_string);
				JSONObject file_json;
				try
				{
						file_json = (JSONObject)tokener.nextValue();
				}
				catch (Exception e)
				{
						Log.e(logTag, String.format("readWaypointsFile() file parsing error: %s", e.getMessage()));
						return null;
				}

				// iterate through JSONArray path and fill waypoint_list with new LatLng objects
				Iterator<String> file_keys = file_json.keys();
				String path_name;
				HashMap<String, ArrayList<LatLng>> available_paths_map = new HashMap<>();
				while (file_keys.hasNext())
				{
						path_name = file_keys.next();
						JSONArray path;
						try
						{
								path = file_json.getJSONArray(path_name);
								available_paths_map.put(path_name, new ArrayList<LatLng>());
								for (int i = 0; i < path.length(); i++)
								{
										double[] raw_latlng = (double[])path.get(i);
										available_paths_map.get(path_name).add(new LatLng(raw_latlng[0], raw_latlng[1]));
								}
						}
						catch (Exception e)
						{
								Log.e(logTag, String.format("readWaypointsFile() path parsing error: %s", e.getMessage()));
								continue;
						}
				}

				return available_paths_map;
		}
}
