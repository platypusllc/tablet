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
import android.widget.TextView;
import android.widget.ThemedSpinnerAdapter;

import com.mapbox.mapboxsdk.geometry.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
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

		private JSONArray convertLatLngListToJSONArray(ArrayList<LatLng> points)
		{
				JSONArray result = new JSONArray();
				for (LatLng wp : points)
				{
						result.put(Arrays.toString(new Double[]{wp.getLatitude(), wp.getLongitude()}));
				}
				return result;
		}

		private void choosePathNameSaveDialog(final ArrayList<LatLng> points, final String filename)
		{
				JSONObject json_file = parseWaypointsFileIntoJSONObject(filename);
				ArrayList<String> available_path_names = new ArrayList<>();
				if (json_file == null)
				{
						Log.w(logTag, "parseWaypointsFileIntoJSONObject() could not find paths");
						json_file = new JSONObject();
				}
				else
				{
						available_path_names = getPathNames(json_file);
				}
				final Dialog dialog = new Dialog(mContext);
				dialog.setContentView(R.layout.save_waypoints_choose_path_layout);
				dialog.setTitle(String.format("Saving to %s: Choose path name", filename));
				ListView available_paths_listview = (ListView) dialog.findViewById(R.id.saved_paths_listview);
				available_paths_listview.setAdapter(new ArrayAdapter<>(mContext, R.layout.boat_name, available_path_names.toArray(new String[]{})));
				TextView available_paths_label = (TextView) dialog.findViewById(R.id.saved_paths_listview_label);
				final EditText path_name_edittext = (EditText) dialog.findViewById(R.id.path_name_edittext);
				if (available_path_names.size() < 1) available_paths_label.setText("File does not yet have any paths");
				Button save_path_button = (Button) dialog.findViewById(R.id.save_path_button);
				final JSONObject appended_json = json_file; // needs to be final, but original can't be declared final
				save_path_button.setOnClickListener(new View.OnClickListener()
				{
						@Override
						public void onClick(View v)
						{
								String path_name = path_name_edittext.getText().toString();
								Log.i(logTag, String.format("Saving %s to %s as path name %s", points.toString(), filename, path_name));
								try
								{
										appended_json.put(path_name, convertLatLngListToJSONArray(points));
										Log.i(logTag, String.format("New json: \n%s", appended_json.toString(4)));
								}
								catch (Exception e)
								{
										Log.e(logTag, String.format("Cannot append to json error: %s", e.getMessage()));
										return;
								}

								Writer writer;
								File file = new File(directory + filename);
								try
								{
										writer = new BufferedWriter(new FileWriter(file));
										writer.write(appended_json.toString(4));
										writer.close();
								}
								catch (Exception e)
								{
										Log.e(logTag, String.format("Cannot write json to file error: %s", e.getMessage()));
								}

								dialog.dismiss();
						}
				});
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

		private ArrayList<String> getPathNames(JSONObject json_file)
		{
				ArrayList<String> path_names = new ArrayList<>();
				Iterator<String> file_keys = json_file.keys();
				while (file_keys.hasNext())
				{
						path_names.add(file_keys.next());
				}
				return path_names;
		}

		private JSONObject parseWaypointsFileIntoJSONObject(String filename)
		{
				File file = new File(directory + filename);
				Scanner fileScanner;
				try
				{
						fileScanner = new Scanner(file);
				}
				catch (Exception e)
				{
						Log.e(logTag, String.format("parseWaypointsFileIntoJSONObject() file scanning error: %s", e.getMessage()));
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
						return file_json;
				}
				catch (Exception e)
				{
						Log.w(logTag, String.format("parseWaypointsFileIntoJSONObject() file parsing error: %s", e.getMessage()));
						return null;
				}
		}

		private HashMap<String, ArrayList<LatLng>> parseWaypointsFile(String filename)
		{
				JSONObject file_json = parseWaypointsFileIntoJSONObject(filename);
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

		void loadWaypointsFromFile(String filename, ArrayList<LatLng> result)
		{
				HashMap<String, ArrayList<LatLng>> waypoints_map = parseWaypointsFile(filename);
				// TODO: dialog to select which path

				result.add(new LatLng(45.40662232640679, 11.00061325283366));
		}


		ArrayList<LatLng> loadWaypointsFromFile()
		{
				File[] waypoint_files = new File(directory).listFiles();
				final String[] waypoint_filenames = new String[waypoint_files.length];
				for (int i = 0; i < waypoint_files.length; i++)
				{
						waypoint_filenames[i] = waypoint_files[i].getName();
				}
				// TODO: dialog to select a file
				final Dialog dialog = new Dialog(mContext);
				dialog.setContentView(R.layout.save_waypoints_choose_file_layout);
				dialog.setTitle("Choose a saved waypoints file");
				final EditText filename_edittext = (EditText) dialog.findViewById(R.id.new_wp_filename_text);
				Button use_existing_button = (Button) dialog.findViewById(R.id.use_existing_wp_file_button);
				use_existing_button.setText("Load from this file");
				Button create_new_button = (Button) dialog.findViewById(R.id.create_new_wp_file_button);
				final Spinner existing_files_spinner = (Spinner) dialog.findViewById(R.id.existing_wp_file_spinner);
				SpinnerAdapter existing_files_spinner_adapter = new ArrayAdapter<>(mContext, R.layout.boat_name, waypoint_filenames);
				existing_files_spinner.setAdapter(existing_files_spinner_adapter);
				create_new_button.setVisibility(View.GONE); // don't need this
				filename_edittext.setVisibility(View.GONE);

				final ArrayList<LatLng> result = new ArrayList<>();
				use_existing_button.setOnClickListener(new View.OnClickListener()
				{
						@Override
						public void onClick(View v)
						{
								// TODO: dialog to choose a path from the available ones (spinner with the above map's keys as options)
								Long iL = existing_files_spinner.getSelectedItemId();
								int i = iL.intValue();
								// TODO: have to pass the results list in order to make this process synchronous?
								loadWaypointsFromFile(waypoint_filenames[i], result);
								dialog.dismiss();
						}
				});
				dialog.show();

				StringBuilder sb = new StringBuilder();
				for (LatLng wp : result)
				{
						sb.append(wp.toString() + "\n");
				}
				Log.i(logTag, String.format("Loaded waypoints: %s", sb.toString()));

				return result;
		}
}
