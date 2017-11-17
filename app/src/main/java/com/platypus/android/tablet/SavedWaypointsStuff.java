package com.platypus.android.tablet;

import android.app.Dialog;
import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.ThemedSpinnerAdapter;

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
		private Context mContext;
		private String directory = Environment.getExternalStorageDirectory() + "/waypoints/";
		SavedWaypointsStuff(Context context)
		{
				mContext = context;
		}

		private void choosePathNameSaveDialog(ArrayList<LatLng> points, String filename)
		{
				// TODO: convert list of LatLng into JSONObject with JSONArray inside, print to a string
				// TODO: append or overwrite the points to the file in the form of that string

				// TODO: show list with existing paths --> requires parsing the file for existing paths
				HashMap<String, ArrayList<LatLng>> available_paths_map = parseWaypointsFile(new File(directory + filename));
				String[] available_path_names;
				if (available_paths_map == null)
				{
						Log.i(logTag, "No paths found in file.");
						available_path_names = new String[]{};
				}
				else
				{
						available_path_names = available_paths_map.keySet().toArray(new String[]{});
				}
				final Dialog dialog = new Dialog(mContext);
				dialog.setContentView(R.layout.save_waypoints_choose_path_layout);
				dialog.setTitle(String.format("Saving to %s: Choose path name", filename));
				ListView available_paths_listview = (ListView) dialog.findViewById(R.id.saved_paths_listview);
				available_paths_listview.setAdapter(new ArrayAdapter<String>(mContext, R.layout.boat_name, available_path_names));
				dialog.show();
		}

		private void chooseSaveFileDialog(final ArrayList<LatLng> points, final String[] waypoint_filenames)
		{
				final Dialog dialog = new Dialog(mContext);
				dialog.setContentView(R.layout.save_waypoints_choose_file_layout);
				dialog.setTitle("Choose (or create) a file");
				final EditText filename_edittext = (EditText) dialog.findViewById(R.id.new_wp_filename_text);
				Button use_existing_button = (Button) dialog.findViewById(R.id.use_existing_wp_file_button);
				Button create_new_button = (Button) dialog.findViewById(R.id.create_new_wp_file_button);
				final Spinner existing_files_spinner = (Spinner) dialog.findViewById(R.id.existing_wp_file_spinner);
				SpinnerAdapter existing_files_spinner_adapter = new ArrayAdapter<>(mContext, R.layout.boat_name, waypoint_filenames);
				existing_files_spinner.setAdapter(existing_files_spinner_adapter);
				use_existing_button.setOnClickListener(new View.OnClickListener()
				{
						@Override
						public void onClick(View v)
						{
								Long iL = existing_files_spinner.getSelectedItemId();
								int i = iL.intValue();
								Log.i(logTag, String.format("Using existing file: %s", waypoint_filenames[i]));
								choosePathNameSaveDialog(points, waypoint_filenames[i]);
								dialog.dismiss();
						}
				});
				create_new_button.setOnClickListener(new View.OnClickListener()
				{
						@Override
						public void onClick(View v)
						{
								String new_filename = filename_edittext.getText().toString();
								Log.i(logTag, String.format("Creating new file: %s", new_filename));
								choosePathNameSaveDialog(points, new_filename);
								dialog.dismiss();
						}
				});
				dialog.show();
		}


		void saveWaypointsToFile(ArrayList<LatLng> points)
		{
				// if the default waypoints file (one big JSON of missions and paths) doesn't exist, create it
				File waypoint_file = new File(directory + "saved_waypoint_paths.txt");
				try
				{
						Log.i(logTag, "Checking for default waypoints file...");
						if (!waypoint_file.exists())
						{
								Log.i(logTag, "Default file not found. Creating...");
								waypoint_file.createNewFile();
						}
				}
				catch (Exception e)
				{
						Log.e(logTag, String.format("saveWaypointsToFile error: %s", e.getMessage()));
				}
				File[] waypoint_files = new File(directory).listFiles();
				final String[] waypoint_filenames = new String[waypoint_files.length];
				for (int i = 0; i < waypoint_files.length; i++)
				{
						waypoint_filenames[i] = waypoint_files[i].getName();
				}

				chooseSaveFileDialog(points, waypoint_filenames);
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
						Log.w(logTag, String.format("readWaypointsFile() file parsing error: %s", e.getMessage()));
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
