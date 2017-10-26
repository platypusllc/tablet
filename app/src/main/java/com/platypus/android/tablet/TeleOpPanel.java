package com.platypus.android.tablet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.jscience.geography.coordinates.LatLong;
import org.jscience.geography.coordinates.UTM;
import org.jscience.geography.coordinates.crs.ReferenceEllipsoid;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.MarkerView;
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions;
import com.mapbox.mapboxsdk.annotations.Polyline;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.ILatLng;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;

import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.view.MenuItem;
import android.view.View;

import com.platypus.android.tablet.Path.AreaType;
import com.platypus.android.tablet.Path.Path;
import com.platypus.android.tablet.Path.Region;
import com.platypus.crw.CrwNetworkUtils;
import com.platypus.crw.VehicleServer;
import com.platypus.crw.data.SensorData;
import com.platypus.crw.data.Pose3D;

import android.app.Activity;
import android.content.Context;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.platypus.crw.data.Utm;
import com.platypus.crw.data.UtmPose;

import android.app.Dialog;

import android.view.View.OnClickListener;

import com.platypus.android.tablet.Joystick.*;

public class TeleOpPanel extends Activity implements SensorEventListener
{
		HashMap<String, Boat> boats_map = new HashMap<>();
		HashMap<String, MarkerViewOptions> boat_markers_map = new HashMap<>();
		HashMap<String, Path> path_map = new HashMap<>();
		HashMap<String, ArrayList<Polyline>> waypath_outline_map = new HashMap<>();
		HashMap<String, ArrayList<Polyline>> waypath_top_map = new HashMap<>();
		//HashMap<String, Polyline> boat_to_wp_line_map = new HashMap<>();
		HashMap<String, Integer> current_wp_index_map = new HashMap<>();
		HashMap<String, Integer> old_wp_index_map = new HashMap<>();
		HashMap<String, ArrayList<Marker>> crumb_markers_map = new HashMap<>();

		// TODO: key: boat name, value: {key: sensor's (channel, type) hash, value: Mapbox marker objects}
		HashMap<String, HashMap<Integer, ArrayList<Marker>>> sensordata_markers_map = new HashMap<>();

		HashMap<String, PlatypusMarkerTypes> marker_types_map = new HashMap<>();

		// TODO: if I'm using a recycler view for the APM GUI, that has a List of views
		// TODO: Doesn't this key: id, value: APM hashmap have to align with that list?
		HashMap<Integer, AutonomousPredicateMessage> ap_messages_map = new HashMap<>();

		void newAutonomousPredicateMessage(String name, String action,
		                                   String trigger, long interval, boolean ends)
		{
				AutonomousPredicateMessage apm = new AutonomousPredicateMessage(name, action, trigger, interval, ends);
				if (apm.generateStringifiedJSON() != null)
				{
						ap_messages_map.put(ap_messages_map.size(), apm);
				}
		}

		void sendAllAPMessages()
		{
				for (AutonomousPredicateMessage apm : ap_messages_map.values())
				{
						if (!apm.getAcknowledged())
						{
								Boat boat = currentBoat();
								// TODO: core lib call with void listener, if completed, set acknowledged to true
								boat.sendAutonomousPredicateMessage(apm.generateStringifiedJSON(), null);
						}
				}
		}

		final Context context = this;
		TextView ipAddressBox = null;
		RelativeLayout linlay = null;

		Button connect_button = null;
		RadioButton real_boat_button = null;
		RadioButton sim_boat_button = null;
		boolean use_real_boat = true;
		Button advanced_options_button = null;
		Button center_view_button = null;
		Button start_wp_button = null;
		Button pause_wp_button = null;
		boolean paused = false;
		Button stop_wp_button = null;
		Button undo_last_wp_button = null;
		Button remove_all_wp_button = null;
		Button drop_wp_button = null;
		Button normal_path_button = null;
		Button spiral_button = null;
		Button lawnmower_button = null;
		Button jar1_button = null;
		Button jar2_button = null;
		Button jar3_button = null;
		Button jar4_button = null;
		Button sampler_reset_button = null;
		Button sampler_stop_all_button = null;

		TextView jar1_text = null;
		TextView jar2_text = null;
		TextView jar3_text = null;
		TextView jar4_text = null;

		/* TODO
		TextView sensorData1 = null;
		TextView sensorData2 = null;
		TextView sensorData3 = null;
		TextView sensorType1 = null;
		TextView sensorType2 = null;
		TextView sensorType3 = null;
		*/
		SensorStuff sensor_stuff = null;

		TextView battery_value = null;
		TextView waypointInfo = null;
		TextView path_length_value = null;

		JoystickView joystick;
		private boolean speed_spinner_erroneous_call = true;
		Spinner speed_spinner = null;
		Spinner available_boats_spinner = null;
		ColorfulSpinnerAdapter available_boats_spinner_adapter = null;

		Handler uiHandler = new Handler(Looper.getMainLooper()); // anything post to this is run on the main GUI thread

		double currentTransectDist = 20;

		MapView mv;
		MapboxMap mMapboxMap;

		LatLng home_location = null;
		Marker home_marker;
		IconFactory mIconFactory;

		int current_wp_list_selected = -1; // which element selected
		LatLng pHollowStartingPoint = new LatLng((float) 40.436871, (float) -79.948825);
		LatLng initialPan = new LatLng(0, 0);
		boolean setInitialPan = true;

		SensorManager senSensorManager;
		Sensor senAccelerometer;

		public static double THRUST_MIN = -1.0;
		public static double THRUST_MAX = 0.3;
		public static double RUDDER_MIN = -1.0;
		public static double RUDDER_MAX = 1.0;

		public EditText ipAddressInput = null;
		public EditText transect_distance_input = null;

		public static String textIpAddress;
		public static String boatPort = "11411";

		double[] tPID = {.2, .0, .0};
		double[] rPID = {1, 0, .2};

		double battery_voltage = 0.0;

		String waypointFileName = "waypoints.txt";

		ArrayList<LatLng> waypoint_list = new ArrayList<LatLng>(); // waypoints selected by the user
		ArrayList<Marker> marker_list = new ArrayList<>(); // markers associated with those waypoints
		ArrayList<Polyline> outline_list = new ArrayList<>(); // lines generated by user input but not yet assigned to any boat
		ArrayList<Polyline> topline_list = new ArrayList<>(); // lines generated by user input but not yet assigned to any boat
		Path unowned_path = new Path(); // a path generated by user input but not yet assigned to any boat
    
		final Object _wpGraphicsLock = new Object();

		private static final String logTag = "TeleOpPanel"; //TeleOpPanel.class.getName();

		NotificationManager notificationManager;
		Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		long sleep_start_time = 0;
		boolean alarm_on = false;
		final Object _batteryVoltageLock = new Object();
		Ringtone alarm_ringtone;
		Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

		int boat_color_count = 0;
		Map<Integer, Map<String, Integer>> color_map = new HashMap<>();

		public Icon colorIconFromDrawable(Drawable drawable, Integer color)
		{
				if (color != null)
				{
						PorterDuff.Mode mMode = PorterDuff.Mode.MULTIPLY;
						drawable.setColorFilter(color, mMode);
				}
				// https://github.com/mapbox/mapbox-gl-native/issues/8185
				Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
				Canvas canvas = new Canvas(bitmap);
				drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
				drawable.draw(canvas);
				return mIconFactory.fromBitmap(bitmap);
		}

		class ColorfulSpinnerAdapter extends ArrayAdapter<String>
		{
				public ColorfulSpinnerAdapter(@NonNull Context context, @LayoutRes int resource)
				{
						super(context, resource);
				}
				@Override
				public View getDropDownView(int position, View convertView, ViewGroup parent)
				{
						View view = super.getDropDownView(position, convertView, parent);
						applyColor(view, position);
						return view;
				}

				private void applyColor(View view, int position)
				{
						while (position > 5) // need to be within the limited set of 6 colors
						{
								position -= 6;
						}
						view.setBackgroundColor(color_map.get(position).get("boat"));
				}
				// TODO: don't rely on a hardcoded color circuit
		}

		void startNewBoat(final String boat_name)
		{
				// generate Boat object and put it into the boat_map
				Boat newBoat;
				if (use_real_boat)
				{
						newBoat = new RealBoat(boat_name);
				}
				else
				{
						// set the initial UTM to be the center of the current map view
						LatLng center = mMapboxMap.getCameraPosition().target;
						UTM initial_simulated_utm = UTM.latLongToUtm(LatLong.valueOf(center.getLatitude(),
										center.getLongitude(), NonSI.DEGREE_ANGLE), ReferenceEllipsoid.WGS84);
						newBoat = new SimulatedBoat(boat_name, initial_simulated_utm);
				}


				available_boats_spinner_adapter.add(boat_name);
				available_boats_spinner_adapter.notifyDataSetChanged();

				// marker view - automatically generate colored arrow
				Drawable arrow = getResources().getDrawable(R.drawable.arrow_white, null);
				PorterDuff.Mode mMode = PorterDuff.Mode.MULTIPLY;
				int boat_color = color_map.get(boat_color_count).get("boat");
				int line_color = color_map.get(boat_color_count).get("line");
				//arrow.setColorFilter(boat_color, mMode);
				newBoat.setBoatColor(boat_color);
				newBoat.setLineColor(line_color);
				boat_color_count++; // use the next set of colors
				if (boat_color_count > 5) boat_color_count = 0; // have a finite set of defined colors

				boat_markers_map.put(boat_name, new MarkerViewOptions()
								.position(pHollowStartingPoint)
								.title(boat_name)
								.icon(colorIconFromDrawable(arrow, boat_color)).rotation(0));

				// TODO: get the arrow centered on the boat's actual location using setAnchor
				//boat_markers_map.get(boat_name).getMarker().setAnchor(0.5f, 0.5f);

				// try to add the marker until mMapboxMap exists and it is added
				uiHandler.post(new Runnable()
				{
						@Override
						public void run()
						{
								if (mMapboxMap != null)
								{
										Log.i(logTag, String.format("Adding boat marker for %s", boat_name));
										mMapboxMap.addMarker(boat_markers_map.get(boat_name));
										marker_types_map.put(boat_markers_map.get(boat_name).getTitle(),
														PlatypusMarkerTypes.VEHICLE);
								}
								else
								{
										uiHandler.postDelayed(this, 1000);
								}
						}
				});

				// Path
				path_map.put(boat_name, new Path());
				waypath_outline_map.put(boat_name, new ArrayList<Polyline>());
				waypath_top_map.put(boat_name, new ArrayList<Polyline>());

				// waypoint indices
				current_wp_index_map.put(boat_name, -1);
				old_wp_index_map.put(boat_name, -2);

				newBoat.createListeners(
								new BoatMarkerUpdateRunnable(newBoat),
								new SensorDataReceivedRunnable(newBoat),
								new WaypointStateReceivedRunnable(newBoat),
								new CrumbReceivedRunnable(newBoat));
				boats_map.put(boat_name, newBoat);
		}
		class BoatMarkerUpdateRunnable implements Runnable
		{
				Boat boat;
				String name;
				MarkerView marker_view;
				long last_redraw = 0;
				public BoatMarkerUpdateRunnable(Boat _boat)
				{
						boat = _boat;
						name = boat.getName();
						marker_view = boat_markers_map.get(name).getMarker();
				}
				public void run()
				{
						if (marker_view == null) return;
						marker_view.setPosition(boat.getLocation());
						float degree = (float) (boat.getYaw() * 180 / Math.PI);  // degree is -90 to 270
						degree = (degree < 0 ? 360 + degree : degree); // degree is 0 to 360
						marker_view.setVisible(true);
						//Log.d(logTag, "BoatMarkerUpdateRunnable: \n" +
						//				String.format("%s, yaw = %f, isVisible = %s", name, degree, Boolean.toString(marker_view.isVisible())));
						marker_view.setRotation(degree);

						// boat to current waypoint line
						//Log.d(logTag, String.format("boat-to-wp last redraw is %d ms ago", System.currentTimeMillis() - last_redraw));
						/*
						if (System.currentTimeMillis() - last_redraw < 500) return; // don't update the line too often
						Polyline line = boat_to_wp_line_map.get(name);
						Path path = path_map.get(name);
						if (line != null)
						{
								mMapboxMap.removeAnnotation(line);
								line.remove();
						}
						if (path == null) return;
						ArrayList<ArrayList<LatLng>> point_pairs = path.getPointPairs();
						//Log.d(logTag, String.format("current index = %d,  point_pairs.size() = %d", current_wp_index_map.get(name), point_pairs.size()));
						if (current_wp_index_map.get(name) < 0 || point_pairs.size() < 1) return;
						//Log.d(logTag, String.format("redrawing boat-to-wp line for %s", name));
						ArrayList<LatLng> pair = new ArrayList<>();
						pair.add(boat.getLocation());
						pair.add(point_pairs.get(current_wp_index_map.get(name)).get(0));
						boat_to_wp_line_map.put(name, mMapboxMap.addPolyline(new PolylineOptions().addAll(pair).color(boat.getBoatColor()).width(1)));
						last_redraw = System.currentTimeMillis();
						*/
				}
		}

		class SensorDataReceivedRunnable implements Runnable
		{
				Boat boat;
				String name;
				SensorData lastReceived;
				public SensorDataReceivedRunnable(Boat _boat)
				{
						boat = _boat;
						name = boat.getName();
				}
				public void run()
				{
						lastReceived = boat.getLastSensorDataReceived();
						Log.v("SensorStuff", String.format("New SensorData received for %s: %s", name, lastReceived.toString()));
						// is the boat with this listener the selected boat?
						String selected_boat_name = available_boats_spinner.getSelectedItem().toString();
						if (name.equals(selected_boat_name))
						{
								// battery gets special treatment
								if (lastReceived.type == VehicleServer.DataType.BATTERY)
								{
										battery_value.setText(Double.toString(lastReceived.value) + " V");
								}
								else
								{
										sensor_stuff.newSensorData(lastReceived);
								}
						}
						/*
						// update the sensor text
						lastReceived = boat.getLastSensorDataReceived();
						String label = String.format("%s [%s]",lastReceived.type.getType(), lastReceived.type.getUnits());
						String data = Double.valueOf(lastReceived.value).toString();

						// is the boat with this listener the selected boat?
						String selected_boat_name = available_boats_spinner.getSelectedItem().toString();
						if (name.equals(selected_boat_name))
						{
								// TODO: use a recycler view instead of fixed "channels"
								// TODO: could use SensorData.key() to identify unique sensor tuple (channel, type)
								// TODO: each unique sensor get its own label to appear
								// TODO: the layout that appears has on-long-click listener to toggle the data overlay
								switch (lastReceived.channel)
								{
										case 1:
												sensorType1.setText(label);
												sensorData1.setText(data);
												break;
										case 2:
												sensorType2.setText(label);
												sensorData2.setText(data);
												break;
										case 3:
												sensorType3.setText(label);
												sensorData3.setText(data);
												break;
										case 4:
												String[] data_split = data.split(",");
												battery_value.setText(data_split[0].substring(1) + " V");
												synchronized (_batteryVoltageLock)
												{
														battery_voltage = Double.parseDouble(data_split[0].substring(1));
												}
												break;
								}
						}

						// TODO: generate a quasi-heatmap using GeoJSON collections and blurred circles
						// TODO: https://github.com/mapbox/mapbox-android-demo/blob/master/MapboxAndroidDemo/src/main/java/com/mapbox/mapboxandroiddemo/examples/dds/CreateHeatmapPointsActivity.java
						*/
				}
		}

		class WaypointStateReceivedRunnable implements Runnable
		{
				Boat boat;
				String name;
				public WaypointStateReceivedRunnable(Boat _boat)
				{
						boat = _boat;
						name = boat.getName();
				}
				public void run()
				{
						// only display the currently selected boat's WP status
						String waypointState = boat.getWaypointState();
						Object result = available_boats_spinner.getSelectedItem();
						String current_boat_name = result.toString();
						if (name.equals(current_boat_name))
						{
								waypointInfo.setText(waypointState);
						}
				}
		}

		class CrumbReceivedRunnable implements Runnable
		{
				Boat boat;
				String name;
				LatLng crumb;
				Icon icon;
				public CrumbReceivedRunnable(Boat _boat)
				{
						Log.i("ODE", "CrumbReceivedRunnable constructor");
						boat = _boat;
						name = boat.getName();
						crumb_markers_map.put(name, new ArrayList<Marker>());
						icon = colorIconFromDrawable(
										getResources().getDrawable(R.drawable.breadcrumb_pin, null),
										_boat.getBoatColor());
				}
				public void run()
				{
						crumb = boat.getNewCrumb();
						int size = crumb_markers_map.get(name).size();
						String title = "crumb_" + Integer.toString(size);
						crumb_markers_map.get(name).add(mMapboxMap.addMarker(new MarkerOptions().position(crumb).icon(icon).title(title)));
						marker_types_map.put(title, PlatypusMarkerTypes.BREADCRUMB);
				}
		}

		Boat currentBoat()
		{
				Object result = available_boats_spinner.getSelectedItem();
				if (result == null) return null;
				String boat_name = result.toString();
				return boats_map.get(boat_name);
		}

		class ToastFailureCallback implements Runnable
		{
				private String toastString;
				public ToastFailureCallback(String _toastString) { toastString = _toastString; }
				@Override
				public void run() { Toast.makeText(context, toastString, Toast.LENGTH_SHORT).show(); }
		}


		protected void onCreate(final Bundle savedInstanceState)
		{
				super.onCreate(savedInstanceState);
				Mapbox.getInstance(getApplicationContext(), getString(R.string.mapbox_access_token));
				this.setContentView(R.layout.tabletlayoutswitch);
				sensor_stuff = new SensorStuff(this);

				// establish color_map
				color_map.put(0, new HashMap<String, Integer>());
				color_map.put(1, new HashMap<String, Integer>());
				color_map.put(2, new HashMap<String, Integer>());
				color_map.put(3, new HashMap<String, Integer>());
				color_map.put(4, new HashMap<String, Integer>());
				color_map.put(5, new HashMap<String, Integer>());
				color_map.get(0).put("boat", ContextCompat.getColor(context, R.color.magenta_light));
				color_map.get(0).put("line", ContextCompat.getColor(context, R.color.magenta_dark));
				color_map.get(1).put("boat", ContextCompat.getColor(context, R.color.cyan_light));
				color_map.get(1).put("line", ContextCompat.getColor(context, R.color.cyan_dark));
				color_map.get(2).put("boat", ContextCompat.getColor(context, R.color.orange_light));
				color_map.get(2).put("line", ContextCompat.getColor(context, R.color.orange_dark));
				color_map.get(3).put("boat", ContextCompat.getColor(context, R.color.green_light));
				color_map.get(3).put("line", ContextCompat.getColor(context, R.color.green_dark));
				color_map.get(4).put("boat", ContextCompat.getColor(context, R.color.red_light));
				color_map.get(4).put("line", ContextCompat.getColor(context, R.color.red_dark));
				color_map.get(5).put("boat", ContextCompat.getColor(context, R.color.yellow_light));
				color_map.get(5).put("line", ContextCompat.getColor(context, R.color.yellow_dark));

				linlay = (RelativeLayout) this.findViewById(R.id.linlay);
				ipAddressBox = (TextView) this.findViewById(R.id.printIpAddress);
				connect_button = (Button) this.findViewById(R.id.connectButton);
				start_wp_button = (Button) this.findViewById(R.id.start_button);
				pause_wp_button = (Button) this.findViewById(R.id.pause_button);
				stop_wp_button = (Button) this.findViewById(R.id.stop_button);
				/* TODO
				sensorData1 = (TextView) this.findViewById(R.id.SValue1);
				sensorData2 = (TextView) this.findViewById(R.id.SValue2);
				sensorData3 = (TextView) this.findViewById(R.id.SValue3);
				sensorType1 = (TextView) this.findViewById(R.id.sensortype1);
				sensorType2 = (TextView) this.findViewById(R.id.sensortype2);
				sensorType3 = (TextView) this.findViewById(R.id.sensortype3);
				*/
				battery_value = (TextView) this.findViewById(R.id.batteryVoltage);
				joystick = (JoystickView) findViewById(R.id.joystickView);
				transect_distance_input = (EditText) this.findViewById(R.id.transect_distance_input);
				waypointInfo = (TextView) this.findViewById(R.id.waypoint_status);
				path_length_value = (TextView) this.findViewById(R.id.path_length_value);

				advanced_options_button = (Button) this.findViewById(R.id.advopt);
				center_view_button = (Button) this.findViewById(R.id.centermap);
				undo_last_wp_button = (Button) this.findViewById(R.id.undo_last_wp_button);
				remove_all_wp_button = (Button) this.findViewById(R.id.remove_all_wp_button);
				drop_wp_button = (Button) this.findViewById(R.id.drop_wp_button);
				normal_path_button = (Button) this.findViewById(R.id.path_button);
				spiral_button = (Button) this.findViewById(R.id.spiral_button);
				lawnmower_button = (Button) this.findViewById(R.id.lawnmower_button);

				jar1_button = (Button) this.findViewById(R.id.jar1_button);
				jar2_button = (Button) this.findViewById(R.id.jar2_button);
				jar3_button = (Button) this.findViewById(R.id.jar3_button);
				jar4_button = (Button) this.findViewById(R.id.jar4_button);
				sampler_reset_button = (Button) this.findViewById(R.id.sampler_reset_button);
				sampler_stop_all_button = (Button) this.findViewById(R.id.sampler_stop_all_button);
				jar1_button.setClickable(true);
				jar2_button.setClickable(true);
				jar3_button.setClickable(true);
				jar4_button.setClickable(true);
				jar1_text = (TextView) this.findViewById(R.id.jar1_text);
				jar2_text = (TextView) this.findViewById(R.id.jar2_text);
				jar3_text = (TextView) this.findViewById(R.id.jar3_text);
				jar4_text = (TextView) this.findViewById(R.id.jar4_text);
				jar1_text.setTextIsSelectable(false);
				jar2_text.setTextIsSelectable(false);
				jar3_text.setTextIsSelectable(false);
				jar4_text.setTextIsSelectable(false);

				alarm_ringtone = RingtoneManager.getRingtone(getApplicationContext(), alarmUri);
				notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

				speed_spinner_erroneous_call = true; // reset the erroneous call boolean
				speed_spinner = (Spinner) this.findViewById(R.id.speed_spinner);
				set_speed_spinner_from_pref();

				available_boats_spinner = (Spinner) this.findViewById(R.id.boat_name_spinner);
				available_boats_spinner_adapter = new ColorfulSpinnerAdapter(this, R.layout.boat_name);
				available_boats_spinner_adapter.setDropDownViewResource(R.layout.boat_name);
				available_boats_spinner.setAdapter(available_boats_spinner_adapter);
				available_boats_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
				{
						@Override
						public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
						{
								Toast.makeText(getApplicationContext(),
												String.format(
																"Controlling: %s",
																available_boats_spinner.getSelectedItem().toString()),
												Toast.LENGTH_SHORT
								).show();
								Boat boat = currentBoat();
								if (boat != null)
								{
										ipAddressBox.setText(boat.getIpAddressString());
										// reset the other status elements, let the listeners update them
										/* TODO
										sensorType1.setText("");
										sensorData1.setText("");
										sensorType2.setText("");
										sensorData2.setText("");
										sensorType3.setText("");
										sensorData3.setText("");
										*/
										sensor_stuff.newSensorSet(); // clear the sensor text data
										waypointInfo.setText("");
										battery_value.setText("");
								}
						}

						@Override
						public void onNothingSelected(AdapterView<?> parent)
						{

						}
				});

				alarm_ringtone = RingtoneManager.getRingtone(getApplicationContext(), alarmUri);
				notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

				SettingsActivity.set_TeleOpPanel(this);
				loadPreferences();

				/* TODO
				sensorType1.setText("");
				sensorType2.setText("");
				sensorType3.setText("");
				sensorData1.setText("");
				sensorData2.setText("");
				sensorData3.setText("");
				*/
				battery_value.setText("");

				//Create folder for the first time if it does not exist
				final File waypointDir = new File(Environment.getExternalStorageDirectory() + "/waypoints");
				if (!waypointDir.exists())
				{
						waypointDir.mkdir();
				}

				mv = (MapView) findViewById(R.id.mapview);
				mv.onCreate(savedInstanceState);
				mv.getMapAsync(new OnMapReadyCallback()
				{
						@Override
						public void onMapReady(@NonNull final MapboxMap mapboxMap)
						{
								Log.i(logTag, "mapboxmap ready");
								mMapboxMap = mapboxMap;
								if (setInitialPan == true && initialPan.getLatitude() != 0 || initialPan.getLongitude() != 0)
								{
										mMapboxMap.moveCamera(CameraUpdateFactory.newCameraPosition(
														new CameraPosition.Builder()
																		.target(initialPan)
																		.zoom(16)
																		.build()
										));
								}

								mMapboxMap.setStyle(Style.MAPBOX_STREETS); //vector map
								mMapboxMap.getUiSettings().setRotateGesturesEnabled(false);
								mMapboxMap.setAllowConcurrentMultipleOpenInfoWindows(true);
								mMapboxMap.setInfoWindowAdapter(new MapboxMap.InfoWindowAdapter()
								{
										// ASDF
										@Override
										public View getInfoWindow(final Marker marker)
										{
												String title = marker.getTitle();
												// TODO: display different information based on marker type
												if (marker_types_map.get(title) == PlatypusMarkerTypes.WAYPOINT)
												{
														View view = getLayoutInflater().inflate(R.layout.waypoint_info_window, null);
														TextView waypoint_index_textview = (TextView) view.findViewById(R.id.waypoint_index_textview);

														// parse waypoint title to get its index
														String[] title_parts = title.split("_");
														final int waypoint_index = Integer.valueOf(title_parts[1]);
														final Button move_button = (Button) view.findViewById(R.id.waypoint_move_button);
														move_button.setOnClickListener(new OnClickListener()
														{
																@Override
																public void onClick(View v)
																{
																		// next map click sets the marker's location and resets the map click listener to null
																		mMapboxMap.setOnMapClickListener(new MapboxMap.OnMapClickListener()
																		{
																				@Override
																				public void onMapClick(@NonNull LatLng point)
																				{
																						marker.setPosition(point);
																						waypoint_list.set(waypoint_index, point);
																						mMapboxMap.setOnMapClickListener(new MapboxMap.OnMapClickListener()
																						{
																								@Override
																								public void onMapClick(@NonNull LatLng point) { }
																						});
																				}
																		});
																}
														});
														waypoint_index_textview.setText(marker.getTitle());
														return view;
												}
												else
												{
														return null;
												}
										}
								});
								mMapboxMap.setOnMarkerClickListener(new MapboxMap.OnMarkerClickListener()
								{
										@Override
										public boolean onMarkerClick(@NonNull Marker marker)
										{
												return false;
										}
								});
								mMapboxMap.setOnMapLongClickListener(new MapboxMap.OnMapLongClickListener()
								{
										@Override
										public void onMapLongClick(LatLng point)
										{
												waypoint_list.add(point);
												String title = "waypoint_" + Integer.toString(marker_list.size());
												marker_list.add(mMapboxMap.addMarker(new MarkerOptions().position(point).title(title)));
												marker_types_map.put(title, PlatypusMarkerTypes.WAYPOINT);
												Log.d(logTag, String.format("waypoint_list.size() = %d,   marker_list.size() = %d", waypoint_list.size(), marker_list.size()));
										}
								});
								mIconFactory = IconFactory.getInstance(context);
						}
				});

				// Every second, display boat connection status
				uiHandler.post(new Runnable()
				{
						@Override
						public void run()
						{
								Boat boat = currentBoat();
								if (boat != null)
								{
										boolean isConnected = boat.isConnected();
										if (isConnected)
										{
												ipAddressBox.setBackgroundColor(Color.GREEN);
										}
										else
										{
												ipAddressBox.setBackgroundColor(Color.RED);
										}
								}
								else
								{
										ipAddressBox.setBackgroundColor(Color.RED);
								}
								uiHandler.postDelayed(this, 1000);
						}
				});

				center_view_button.setOnClickListener(new OnClickListener()
				{
						@Override
						public void onClick(View view)
						{
								if (mMapboxMap == null)
								{
										Toast.makeText(getApplicationContext(), "Please wait for the map to load", Toast.LENGTH_LONG).show();
										return;
								}
								Boat boat = currentBoat();
								if (boat == null)
								{
										Toast.makeText(getApplicationContext(), "Please Connect to a boat first", Toast.LENGTH_LONG).show();
										return;
								}
								LatLng location = currentBoat().getLocation();
								if (location == null)
								{
										Toast.makeText(getApplicationContext(), "Boat still finding GPS location", Toast.LENGTH_LONG).show();
										return;
								}
								mMapboxMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(location).zoom(16).build()));
						}
				});

				//Options menu
				advanced_options_button.setOnClickListener(new OnClickListener()
				{
						@Override
						public void onClick(View v)
						{
								PopupMenu popup = new PopupMenu(TeleOpPanel.this, advanced_options_button);
								popup.getMenuInflater().inflate(R.menu.dropdownmenu, popup.getMenu());
								popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
								{
										public boolean onMenuItemClick(MenuItem item)
										{
												switch (item.toString())
												{
														case "Satellite Map":
														{
																if (mMapboxMap != null) mMapboxMap.setStyle(Style.SATELLITE);
																break;
														}
														case "Vector Map":
														{
																if (mMapboxMap != null) mMapboxMap.setStyle(Style.MAPBOX_STREETS);
																break;
														}
														case "Set Home":
														{
																setHome();
																break;
														}
														case "Go Home":
														{
																goHome();
																break;
														}
														case "Send PIDs":
														{
																sendPID();
																break;
														}
														case "Save Waypoints":
														{
																{
																		try
																		{
																				SaveWaypointsToFile();
																		}
																		catch (Exception e) { }
																		break;
																}
														}
														case "Load Waypoints":
														{
																try
																{
																		LoadWaypointsFromFile(waypointFileName);  // TODO: reimplement this
																}
																catch (Exception e)
																{
																		Log.e(logTag, "failed to load WP file");
																		Log.e(logTag, e.toString());
																}
																break;
														}
														case "Load Waypoint File":
														{
																try
																{
																		loadWayointFiles();
																}
																catch (Exception e) { }
																break;
														}
														case "Snooze Alarms":
														{
																synchronized (_batteryVoltageLock)
																{
																		sleep_start_time = System.currentTimeMillis();
																		alarm_on = false;
																		alarm_ringtone.stop();
																}
																break;
														}
														case "Preferences":
														{
																Intent intent = new Intent(context, SettingsActivity.class);
																context.startActivity(intent);
																break;
														}
														case "Autonomy":
														{
																Intent intent = new Intent(context, AutonomyActivity.class);
																context.startActivity(intent);
																break;
														}
												}
												return true;
										}
								});
								popup.show(); //showing popup menu
						}
				});

				speed_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
				{
						@Override
						public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
						{
								// this listener triggers when the view is initialized, no user touch event
								// That causes undesired behavior, so we need to make sure it is ignored once
								if (speed_spinner_erroneous_call)
								{
										speed_spinner_erroneous_call = false;
										return;
								}
								SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
								SharedPreferences.Editor editor = sharedPref.edit();
								String item = String.valueOf(speed_spinner.getSelectedItem());
								switch (item)
								{
										case "Slow":
												editor.putString(SettingsActivity.KEY_PREF_SPEED, "SLOW");
												break;
										case "Medium":
												editor.putString(SettingsActivity.KEY_PREF_SPEED, "MEDIUM");
												break;
										case "Fast":
												editor.putString(SettingsActivity.KEY_PREF_SPEED, "FAST");
												break;
										case "Custom":
												editor.putString(SettingsActivity.KEY_PREF_SPEED, "CUSTOM");
												break;
										default:
												break;
								}
								editor.apply();
								editor.commit();

								Toast.makeText(getApplicationContext(), String.valueOf(speed_spinner.getSelectedItem()), Toast.LENGTH_SHORT).show();
								sendPID();
						}

						@Override
						public void onNothingSelected(AdapterView<?> parent)
						{

						}
				});

				// Joystick
				joystick.setYAxisInverted(false);
				joystick.setOnJostickMovedListener(joystick_moved_listener);
				joystick.setOnJostickClickedListener(null);

				senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
				senAccelerometer = senSensorManager
								.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
				senSensorManager.registerListener(this, senAccelerometer,
								SensorManager.SENSOR_DELAY_NORMAL);

				connect_button.setOnClickListener(new OnClickListener()
				{
						@Override
						public void onClick(View v)
						{
								connectBox();
						}
				});
				connectBox(); // start the app with the connect dialog popped up

				start_wp_button.setOnClickListener(new OnClickListener()
				{
						@Override
						public void onClick(View v)
						{
								Log.i(logTag, "startWaypoints() called...");
								paused = false;
								pause_wp_button.setBackground(getDrawable(R.drawable.pause_button));
								Boat boat = currentBoat();
								if (boat == null)
								{
										Toast.makeText(context, "Connect to a boat first", Toast.LENGTH_SHORT).show();
										Log.w(logTag, "TeleOpPanel.startWaypoints(): currentBoat is null");
										return;
								}
								String boat_name = boat.getName();
								// if there are no waypoints, the user has to create waypoints, then a path
								if (waypoint_list.size() < 1)
								{
										Toast.makeText(context, "Create waypoints and a path first", Toast.LENGTH_SHORT).show();
										return;
								}
								// if there is exactly one waypoint, create a "path" for the user
								if (waypoint_list.size() == 1)
								{
										unowned_path = new Path((ArrayList<LatLng>) waypoint_list.clone());
								}
								// if there are no points in unowned_path, the user has to create a path
								if (unowned_path.getPoints().size() < 1)
								{
										Toast.makeText(context, "Create a path first", Toast.LENGTH_SHORT).show();
										return;
								}

								ArrayList<LatLng> points = (ArrayList<LatLng>)unowned_path.getPoints().clone();
								path_map.put(boat_name, new Path(points));
								double[][] waypoints = new double[points.size()][2];
								for (int i = 0; i < points.size(); i++)
								{
										LatLng latlng = points.get(i);
										waypoints[i] = new double[] {latlng.getLatitude(), latlng.getLongitude()};
								}

								boat.startWaypoints(waypoints, new ToastFailureCallback("Start Waypoints Msg Timed Out"));
								current_wp_index_map.put(boat_name, 0);

								// draw the boat's lines, independent from the ones used to generate paths
								remove_waypaths(boat_name);
								add_waypaths(boat_name);

								// Here is where you'd clear the waypoints_list, marker_list, and clear the unowned path
								// But you might want to give the exact same path to another boat, so I'll leave it to the user to clear
						}
				});

				pause_wp_button.setOnClickListener(new OnClickListener()
				{
						@Override
						public void onClick(View v)
						{
								Boat boat = currentBoat();
								if (boat != null)
								{
										if (!paused) // pause
										{
												boat.setAutonomous(false, new ToastFailureCallback("Pause Msg Timed Out"));
												pause_wp_button.setBackground(getDrawable(R.drawable.resume_button));
										}
										else // unpause
										{
												boat.setAutonomous(true, new ToastFailureCallback("Resume Msg Timed Out"));
												pause_wp_button.setBackground(getDrawable(R.drawable.pause_button));
										}
										paused = !paused;

								}
								// TODO: how can we implement resume with boat state rather than button state?
						}
				});

				stop_wp_button.setOnClickListener(new OnClickListener()
				{
						@Override
						public void onClick(View v)
						{
								paused = false;
								pause_wp_button.setBackground(getDrawable(R.drawable.pause_button));
								Boat boat = currentBoat();
								if (boat != null)
								{
										String boat_name = boat.getName();
										boat.stopWaypoints(new ToastFailureCallback("Stop Waypoints Msg Timed Out"));
										remove_waypaths(boat_name);
										path_map.get(boat_name).clearPoints();
										waypath_outline_map.get(boat_name).clear();
										waypath_top_map.get(boat_name).clear();
										mMapboxMap.removeAnnotations(marker_list);
										marker_list.clear();
										waypoint_list.clear();
										remove_waypaths("");
										unowned_path.clearPoints();
										calculatePathDistance();
								}
						}
				});

				undo_last_wp_button.setOnClickListener(new OnClickListener()
				{
						@Override
						public void onClick(View v)
						{
								Log.i(logTag, String.format("waypoint_list.size() = %d,   marker_list.size() = %d", waypoint_list.size(), marker_list.size()));
								if (marker_list.size() > 0)
								{
										mMapboxMap.removeAnnotation(marker_list.get(marker_list.size()-1));
										waypoint_list.remove(waypoint_list.size() - 1);
										marker_list.remove(marker_list.size() - 1);
								}
								else
								{
										Toast.makeText(context, "No more waypoints", Toast.LENGTH_SHORT).show();
								}
						}
				});

				remove_all_wp_button.setOnClickListener(new OnClickListener()
				{
						@Override
						public void onClick(View v)
						{
								Log.i(logTag, String.format("waypoint_list.size() = %d,   marker_list.size() = %d", waypoint_list.size(), marker_list.size()));
								if (marker_list.size() > 0)
								{
										mMapboxMap.removeAnnotations(marker_list);
										marker_list.clear();
										waypoint_list.clear();
										remove_waypaths("");
										unowned_path.clearPoints();
										calculatePathDistance();
								}
						}
				});

				drop_wp_button.setOnClickListener(new OnClickListener()
				{
						@Override
						public void onClick(View v)
						{
								final Boat boat = currentBoat();
								if (boat == null)
								{
										Toast.makeText(getApplicationContext(), "No Boat Connected", Toast.LENGTH_LONG).show();
										return;
								}
								if (boat.getLocation() == null)
								{
										Toast.makeText(getApplicationContext(), "Waiting on boat GPS", Toast.LENGTH_LONG).show();
										return;
								}
								LatLng point = boat.getLocation();
								if (mMapboxMap == null)
								{
										Toast.makeText(getApplicationContext(), "Map still loading", Toast.LENGTH_LONG).show();
										return;
								}
								waypoint_list.add(point);
								String title = "waypoint_" + Integer.toString(marker_list.size());
								marker_list.add(mMapboxMap.addMarker(new MarkerOptions().position(point).title(title)));
								marker_types_map.put(title, PlatypusMarkerTypes.WAYPOINT);
						}
				});

				normal_path_button.setOnClickListener(new OnClickListener()
				{
						@Override
						public void onClick(View v)
						{
								// create a Path object from the current waypoints
								// draw the lines between the current waypoints to show the path
								// calculate the path length and show it in the text
								unowned_path.clearPoints();
								remove_waypaths("");
								Log.d(logTag, String.format("waypoint_list.size() = %d,   marker_list.size() = %d", waypoint_list.size(), marker_list.size()));
								remove_waypaths("");
								if (waypoint_list.size() > 0)
								{
										unowned_path = new Path((ArrayList<LatLng>)waypoint_list.clone());
										add_waypaths("");
										calculatePathDistance();
								}
								else
								{
										Toast.makeText(context, "Need waypoints to generate path", Toast.LENGTH_SHORT).show();
								}
						}
				});

				spiral_button.setOnClickListener(new OnClickListener()
				{
						@Override
						public void onClick(View v)
						{
								try
								{
										currentTransectDist = Double.valueOf(transect_distance_input.getText().toString());
								}
								catch (Exception ex)
								{
										// user probably has a bad transect distance typed in
										Toast.makeText(context, "Strange transect distance. Using 10.", Toast.LENGTH_SHORT).show();
										transect_distance_input.setText("10");
										currentTransectDist = 10;
								}
								unowned_path.clearPoints();
								remove_waypaths("");
								if (waypoint_list.size() > 2)
								{
										unowned_path = new Region((ArrayList<LatLng>)waypoint_list.clone(), AreaType.SPIRAL, currentTransectDist);
										add_waypaths("");
										calculatePathDistance();
								}
								else
								{
										Toast.makeText(context, "Need 3 waypoints to generate spiral", Toast.LENGTH_SHORT).show();
								}
						}
				});

				lawnmower_button.setOnClickListener(new OnClickListener()
				{
						@Override
						public void onClick(View v)
						{
								try
								{
										currentTransectDist = Double.valueOf(transect_distance_input.getText().toString());
								}
								catch (Exception ex)
								{
										// user probably has a bad transect distance typed in
										Toast.makeText(context, "Strange transect distance. Using 10.", Toast.LENGTH_SHORT).show();
										transect_distance_input.setText("10");
										currentTransectDist = 10;
								}
								unowned_path.clearPoints();
								remove_waypaths("");
								if (waypoint_list.size() > 2)
								{
										unowned_path = new Region((ArrayList<LatLng>)waypoint_list.clone(), AreaType.LAWNMOWER, currentTransectDist);
										add_waypaths("");
										calculatePathDistance();
								}
								else
								{
										Toast.makeText(context, "Need 3 waypoints to generate lawnmower", Toast.LENGTH_SHORT).show();
								}
						}
				});

				class JarCountdownTimer extends CountDownTimer
				{
						TextView clock_text;
						int seconds;
						int minutes;
						int full_seconds;
						int left_seconds;

						public JarCountdownTimer(long durationMillis, long tickIntervalMillis, TextView jar_textview)
						{
								super(durationMillis, tickIntervalMillis);
								clock_text = jar_textview;
						}

						public void onTick(long millisUntilFinished)
						{
								seconds = (int)millisUntilFinished/1000;
								minutes = seconds/60;
								full_seconds = minutes*60;
								left_seconds = seconds - full_seconds;
								if (left_seconds < 10)
								{
										clock_text.setText(String.format("%d:0%d", minutes, left_seconds));
								}
								else
								{
										clock_text.setText(String.format("%d:%d", minutes, left_seconds));
								}

						}

						public void onFinish()
						{
								clock_text.setText("DONE");
						}
				}
				class JarCountdownRunnable implements Runnable
				{
						TextView clock_text;
						Button button;
						CountDownTimer timer;
						public JarCountdownRunnable(TextView jar_textview, Button jar_button)
						{
								clock_text = jar_textview;
								button = jar_button;
						}
						@Override
						public void run()
						{
								timer = new JarCountdownTimer(1000*60*4, 1000, clock_text).start();
								button.setClickable(false);
						}

						public void cancel() { if (timer != null) timer.cancel(); }
				}
				class JarOnClickListener implements OnClickListener
				{
						TextView clock_text;
						Button button;
						int number;
						JarCountdownRunnable runnable;
						public JarOnClickListener(int jar_number, TextView jar_textview, Button jar_button)
						{
								clock_text = jar_textview;
								button = jar_button;
								number = jar_number;
						}
						@Override
						public void onClick(View v)
						{
								if (button.isClickable())
								{
										Boat boat = currentBoat();
										if (boat == null)
										{
												Toast.makeText(context, "Connect to a boat first", Toast.LENGTH_SHORT).show();
												return;
										}
										runnable = new JarCountdownRunnable(clock_text, button);
										boat.startSample(number-1, runnable,
														new ToastFailureCallback("Sampler Start Msg timed out"));
								}
								else
								{
										// TODO: why does this not run?
										Toast.makeText(context, String.format("Jar %d is not available until reset", number), Toast.LENGTH_SHORT).show();
								}
						}
						public void cancel()
						{
								// cancel the timer inside the countdown runnable
								if (runnable != null) runnable.cancel();
						}
				}

				final JarOnClickListener jar1_listener = new JarOnClickListener(1, jar1_text, jar1_button);
				final JarOnClickListener jar2_listener = new JarOnClickListener(2, jar2_text, jar2_button);
				final JarOnClickListener jar3_listener = new JarOnClickListener(3, jar3_text, jar3_button);
				final JarOnClickListener jar4_listener = new JarOnClickListener(4, jar4_text, jar4_button);
				jar1_button.setOnClickListener(jar1_listener);
				jar2_button.setOnClickListener(jar2_listener);
				jar3_button.setOnClickListener(jar3_listener);
				jar4_button.setOnClickListener(jar4_listener);

				class ResetSamplerSuccessRunnable implements Runnable
				{
						@Override
						public void run()
						{
								jar1_button.setClickable(true);
								jar2_button.setClickable(true);
								jar3_button.setClickable(true);
								jar4_button.setClickable(true);
								jar1_listener.cancel();
								jar2_listener.cancel();
								jar3_listener.cancel();
								jar4_listener.cancel();
								jar1_text.setText("");
								jar2_text.setText("");
								jar3_text.setText("");
								jar4_text.setText("");

								Toast.makeText(context, "Sampler reset or stopped successfully", Toast.LENGTH_SHORT).show();
						}
				}
				sampler_reset_button.setOnLongClickListener(new View.OnLongClickListener()
				{
						@Override
						public boolean onLongClick(View v)
						{
								Boat boat = currentBoat();
								if (boat == null)
								{
										Toast.makeText(context, "Connect to a boat first", Toast.LENGTH_SHORT).show();
								}
								boat.resetSampler(new ResetSamplerSuccessRunnable(),
												new ToastFailureCallback("Sampler Reset Msg timed out"));
								return false;
						}
				});
				sampler_stop_all_button.setOnLongClickListener(new View.OnLongClickListener()
				{
						@Override
						public boolean onLongClick(View v)
						{
								Boat boat = currentBoat();
								if (boat == null)
								{
										Toast.makeText(context, "Connect to a boat first", Toast.LENGTH_SHORT).show();
								}
								boat.stopSampleAll(new ResetSamplerSuccessRunnable(),
												new ToastFailureCallback("Sampler Stop All Msg timed out"));
								return false;
						}
				});
		}

		@Override
		protected void onStart()
		{
				super.onStart();
				mv.onStart();
		}

		@Override
		public void onResume()
		{
				super.onResume();
				mv.onResume();
		}

		@Override
		public void onPause()
		{
				super.onPause();
				mv.onPause();
		}

		@Override
		protected void onStop()
		{
				super.onStop();
				//mv.onStop();
		}

		@Override
		protected void onDestroy()
		{
				super.onDestroy();
				mv.onDestroy();

				SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
				SharedPreferences.Editor editor = sharedPref.edit();
				Boat boat = currentBoat();
				if (boat != null)
				{
						LatLng location = boat.getLocation();
						if (location != null)
						{
								editor.putString(SettingsActivity.KEY_PREF_LAT, Double.toString(location.getLatitude()));
								editor.putString(SettingsActivity.KEY_PREF_LON, Double.toString(location.getLongitude()));
						}
				}
				editor.apply();
				editor.commit();
		}

		@Override
		public void onLowMemory()
		{
				super.onLowMemory();
				mv.onLowMemory();
		}

		@Override
		protected void onSaveInstanceState(Bundle outState)
		{
				super.onSaveInstanceState(outState);
				mv.onSaveInstanceState(outState);
		}

		// This method checks the wifi connection but not Internet access
		public static boolean isNetworkAvailable(final Context context)
		{
				final ConnectivityManager connectivityManager = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
				return connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnectedOrConnecting();
		}

		// This method need to run in another thread except UI thread(main thread)
		public static boolean hasActiveInternetConnection(Context context)
		{
				if (isNetworkAvailable(context))
				{
						try
						{
								HttpURLConnection urlc = (HttpURLConnection) (new URL("http://www.google.com").openConnection());
								urlc.setRequestProperty("User-Agent", "Test");
								urlc.setRequestProperty("Connection", "close");
								urlc.setConnectTimeout(1500);
								urlc.connect();
								return (urlc.getResponseCode() == 200);
						}
						catch (IOException e)
						{
								Log.e(logTag, "Error checking internet connection", e);
						}
				}
				else
				{
						Log.d(logTag, "No network available!");
				}
				return false;
		}

		// *******************************
		//  JoystickView listener
		// *******************************
		Runnable joystickTimeoutCallback = new Runnable()
		{
				@Override
				public void run()
				{
						Toast.makeText(context, "Joystick Msg Timed Out!!", Toast.LENGTH_SHORT).show();
				}
		};
		private JoystickMovedListener joystick_moved_listener = new JoystickMovedListener()
		{
				Boat boat;
				@Override
				public void OnMoved(int x, int y)
				{
						Log.d(logTag, String.format("joystick (x, y) = %d, %d", x, y));
						boat = currentBoat();
						if (boat != null)
						{
								boat.updateControlSignals(
												fromProgressToRange(y, THRUST_MIN, THRUST_MAX),
												fromProgressToRange(x, RUDDER_MIN, RUDDER_MAX),
												joystickTimeoutCallback
								);
						}
				}

				@Override
				public void OnReleased() { }

				@Override
				public void OnReturnedToCenter()
				{
						boat = currentBoat();
						if (boat != null)
						{
								boat.updateControlSignals(0.0, 0.0, joystickTimeoutCallback);
						}
				}
		};

		/*
		private String unit(VehicleServer.SensorType stype)
		{
				String unit = "";

				if (stype == VehicleServer.SensorType.ATLAS_PH)
				{
						unit = "pH";
				}
				else if (stype == VehicleServer.SensorType.ATLAS_DO)
				{
						unit = "DO (mg/L)";
				}
				else if (stype == VehicleServer.SensorType.ES2)
				{
						unit = "EC(µS/cm)\n" +
										"T(°C)";
				}
				else if (stype == VehicleServer.SensorType.HDS_DEPTH)
				{
						unit = "depth (m)";
				}

				return unit;
		}
		*/

		// Converts from progress bar value to linear scaling between min and
		// max
		private double fromProgressToRange(int progress, double min, double max)
		{
				// progress will be between -10 and 10, with 0 being the center
				// evaluate linear range above and below zero separately
				double value;
				if (progress < 0)
				{
						value = min * Math.abs(progress) / 10.0;
						return value;
				}
				else
				{
						value = max * progress / 10.0;
						return value;
				}
		}

		/* accelerometer controls */
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) { }

		@Override
		public void onSensorChanged(SensorEvent sensorEvent)
		{
				Sensor mySensor = sensorEvent.sensor;
				if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) { }
		}

		public UtmPose convertLatLngUtm(ILatLng point)
		{
				UTM utmLoc = UTM.latLongToUtm(LatLong.valueOf(point.getLatitude(),
								point.getLongitude(), NonSI.DEGREE_ANGLE), ReferenceEllipsoid.WGS84);

				// Convert to UTM data structure
				Pose3D pose = new Pose3D(utmLoc.eastingValue(SI.METER), utmLoc.northingValue(SI.METER), 0.0, 0, 0, 0);
				Utm origin = new Utm(utmLoc.longitudeZone(), utmLoc.latitudeZone() > 'O');
				UtmPose utm = new UtmPose(pose, origin);
				return utm;
		}

		public void connectBox()
		{
				// TODO: check if the IP address is already being used. If so, prompt to reconnect or just switch to the existing boat
				final Dialog dialog = new Dialog(context);
				dialog.setContentView(R.layout.connectdialog);
				dialog.setTitle("Connect To A Boat");
				ipAddressInput = (EditText) dialog.findViewById(R.id.ip_address_input);
				Button submitButton = (Button) dialog.findViewById(R.id.submit);
				real_boat_button = (RadioButton) dialog.findViewById(R.id.realBoatButton);
				sim_boat_button = (RadioButton) dialog.findViewById(R.id.simBoatButton);

				loadPreferences();
				ipAddressInput.setText(textIpAddress);

				real_boat_button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
				{
						@Override
						public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
						{
								use_real_boat = isChecked;
						}
				});

				submitButton.setOnClickListener(new OnClickListener()
				{
						@Override
						public void onClick(View v)
						{

								if (ipAddressInput.getText() == null || ipAddressInput.getText().equals("") || ipAddressInput.getText().length() == 0)
								{
										ipAddressBox.setText("localhost");
								}
								else
								{
										ipAddressBox.setText(ipAddressInput.getText());
								}
								textIpAddress = ipAddressInput.getText().toString();
								InetSocketAddress address;
								if (ipAddressInput.getText() == null || ipAddressInput.getText().equals(""))
								{
										address = CrwNetworkUtils.toInetSocketAddress("127.0.0.1:" + boatPort);
								}
								else
								{
										address = CrwNetworkUtils.toInetSocketAddress(textIpAddress + ":" + boatPort);
								}
								int boat_count = boats_map.size();
								String boat_name = String.format("boat_%d", boat_count);
								// TODO: let the user choose the boat name
								startNewBoat(boat_name); // initialize the Boat
								boats_map.get(boat_name).setAddress(address);
								boats_map.get(boat_name).setIpAddressString(textIpAddress);
								available_boats_spinner.setSelection(boat_count); // automatically watch the new boat
								try
								{
										saveSession(); //save ip address
								}
								catch (Exception e) { }
								dialog.dismiss();
								latestWaypointPoll(); // Launch waypoint polling
								alertsAndAlarms(); // Launch alerts and alarms thread
								SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
								SharedPreferences.Editor editor = sharedPref.edit();
								editor.putString(SettingsActivity.KEY_PREF_IP, boats_map.get(boat_name).getIpAddress().getAddress().toString());
								editor.apply();
								editor.commit();
						}
				});
				dialog.show();
		}

		//  Make return button same as home button
		@Override
		public void onBackPressed()
		{
				/*
				Intent setIntent = new Intent(Intent.ACTION_MAIN);
				setIntent.addCategory(Intent.CATEGORY_HOME);
				setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(setIntent);
				*/
				// TODO: this causes a crash. Disabling the back button for now.
		}

		public void SaveWaypointsToFile() throws IOException
		{
				// ASDF TODO: add this back in
				Toast.makeText(context, "Save Waypoints is under construction", Toast.LENGTH_SHORT).show();
		}

		public void setHome()
		{
				if (home_location == null)
				{
						new AlertDialog.Builder(context)
										.setTitle("Set Home")
										.setMessage("Which position do you want to use?")
										.setPositiveButton("Boat", new DialogInterface.OnClickListener()
										{
												public void onClick(DialogInterface dialog, int which)
												{
														Boat boat = currentBoat();
														if (boat == null)
														{
																Toast.makeText(context, "Connect to a boat first", Toast.LENGTH_SHORT).show();
																return;
														}
														home_location = boat.getLocation();

														// TODO: set the home location for the server with a core lib call

														if (home_location == null)
														{
																Toast.makeText(context, "Waiting for boat GPS", Toast.LENGTH_SHORT).show();
																return;
														}
														Drawable mhome = ContextCompat.getDrawable(getApplicationContext(), R.drawable.home1);
														Icon home_icon = colorIconFromDrawable(mhome, null);
														MarkerOptions home_marker_options = new MarkerOptions()
																		.position(home_location)
																		.title("Home")
																		.icon(home_icon);
														home_marker = mMapboxMap.addMarker(home_marker_options);
														marker_types_map.put(home_marker.getTitle(), PlatypusMarkerTypes.HOME);
														mMapboxMap.moveCamera(CameraUpdateFactory.newLatLng(home_location));
												}
										})
										.setNegativeButton("Tablet", new DialogInterface.OnClickListener()
										{
												public void onClick(DialogInterface dialog, int which)
												{
														Toast.makeText(getApplicationContext(), "Tablet-based home location not available", Toast.LENGTH_SHORT).show();
														/* TODO: tablet location as home
														Location tempLocation = LocationServices.FusedLocationApi.getLastLocation();
														if (tempLocation == null)
														{
																Toast.makeText(getApplicationContext(), "Tablet doesn't have a location", Toast.LENGTH_SHORT).show();
																return;
														}
														LatLng loc = new LatLng(tempLocation.getLatitude(), tempLocation.getLongitude());

														if (loc != null)
														{
																Drawable mhome = ContextCompat.getDrawable(getApplicationContext(), R.drawable.home1);
																Icon home_icon = colorIconFromDrawable(mhome, null);
																home_location = loc;
																MarkerOptions home_marker_options = new MarkerOptions()
																				.position(home_location)
																				.title("Home")
																				.icon(home_icon);
																home_marker = mMapboxMap.addMarker(home_marker_options);
																marker_types_map.put(home_marker.getTitle(), PlatypusMarkerTypes.HOME);
																mMapboxMap.moveCamera(CameraUpdateFactory.newLatLng(home_location));
														}
														else
														{
																Toast.makeText(getApplicationContext(), "Tablet doesn't have GPS Signal", Toast.LENGTH_SHORT).show();
														}
														*/
												}
										})
										.show();
				}
				else
				{
						new AlertDialog.Builder(context)
										.setTitle("Set Home")
										.setMessage("Do you want to remove the home?")
										.setPositiveButton("Yes", new DialogInterface.OnClickListener()
										{
												public void onClick(DialogInterface dialog, int which)
												{
														mMapboxMap.removeMarker(home_marker);
														home_location = null;
												}
										})
										.setNegativeButton("No", new DialogInterface.OnClickListener()
										{
												public void onClick(DialogInterface dialog, int which)
												{
												}
										})
										.show();
				}
		}

		public void goHome()
		{
				if (home_location == null)
				{
						Toast.makeText(getApplicationContext(), "Set home first!", Toast.LENGTH_SHORT).show();
						return;
				}
				new AlertDialog.Builder(context)
								.setTitle("Go Home")
								.setMessage("Let the boat go home ?")
								.setPositiveButton("Yes", new DialogInterface.OnClickListener()
								{
										public void onClick(DialogInterface dialog, int which)
										{
												double[] home = new double[]{home_location.getLatitude(), home_location.getLongitude()};
												Boat boat = currentBoat();
												boat.addWaypoint(home, new ToastFailureCallback("Go home msg timed out"));
												Log.i(logTag, "Go home");
										}
								})
								.setNegativeButton("No", new DialogInterface.OnClickListener()
								{
										public void onClick(DialogInterface dialog, int which) { }
								})
								.show();
		}

		public void sendPID()
		{
				loadPreferences();
				Boat boat = currentBoat();
				if (boat != null)
				{
						boat.setPID(tPID, rPID, new ToastFailureCallback("Set PID Msg timed out"));
				}
				else
				{
						Toast.makeText(context, "Connect to a boat first", Toast.LENGTH_SHORT).show();
				}
		}

		public void latestWaypointPoll()
		{
				final Handler handler = new Handler();
				handler.post(new Runnable()
				{
						@Override
						public void run()
						{
								for (Map.Entry<String, Boat> entry : boats_map.entrySet())
								{
										String boat_name = entry.getKey();
										Boat boat = entry.getValue();
										Path path = path_map.get(boat_name);
										int cpwi = boat.getWaypointsIndex();
										current_wp_index_map.put(boat_name, cpwi);
										if (cpwi != old_wp_index_map.get(boat_name))
										{
												// need to update the line colors
												if (path != null && path.getPoints().size() > 0)
												{
														remove_waypaths(boat_name);
														add_waypaths(boat_name);
												}
										}
										old_wp_index_map.put(boat_name, cpwi);
								}
								handler.postDelayed(this, 3000);
						}
				});
		}

		public void alertsAndAlarms()
		{
				final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
				final Handler handler = new Handler();
				handler.post(new Runnable()
				{
						@Override
						public void run()
						{
								double batt_volt;
								synchronized (_batteryVoltageLock)
								{
										batt_volt = battery_voltage;
								}
								Log.i(logTag, "checking battery voltage...");
								String sleep_str = sharedPref.getString(SettingsActivity.KEY_PREF_SNOOZE, "1");
								long sleep_ms = (int) Double.parseDouble(sleep_str) * 60 * 1000;
								long current_time = System.currentTimeMillis();
								if (current_time - sleep_start_time >= sleep_ms)
								{
										sleep_start_time = 0;
										double alert_voltage = Double.parseDouble(sharedPref.getString(SettingsActivity.KEY_PREF_VOLTAGE_ALERT, "15.0"));
										double alarm_voltage = Double.parseDouble(sharedPref.getString(SettingsActivity.KEY_PREF_VOLTAGE_ALARM, "14.0"));
										if (alarm_voltage > alert_voltage) alarm_voltage = (alert_voltage - 0.5);
										String message = String.format("Boat battery = %.2fV", batt_volt);
										Log.i(logTag, message);
										if (batt_volt == 0.0)
										{
												// initial value before connecting to boat
												handler.postDelayed(this, 10000);
												return;
										}
										if (batt_volt < alert_voltage)
										{
												NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext())
																.setSmallIcon(R.drawable.logo) //just some random icon placeholder
																.setContentTitle("Boat battery warning")
																.setContentText(message);
												if (batt_volt < alarm_voltage)
												{
														if (!alarm_on)
														{
																alarm_ringtone.play();
																alarm_on = true;
														}
														Toast.makeText(getApplicationContext(), String.format("%s, retrieve the boat ASAP!", message), Toast.LENGTH_LONG).show();
												}
												if (!alarm_on)
												{
														mBuilder.setSound(soundUri); // Sound only if there isn't an alarm going
												}
												notificationManager.notify(0, mBuilder.build());
										}
										else
										{
												if (alarm_ringtone.isPlaying()) alarm_ringtone.stop();
												alarm_on = false;
										}
								}
								else
								{
										Log.i(logTag, "battery alerts snoozing...");
								}

								// run at least once every 60 seconds
								long sleep_remaining = Math.max(100, Math.min(current_time - sleep_start_time, 60000));
								handler.postDelayed(this, sleep_remaining);
						}
				});
		}

		void set_speed_spinner_from_pref()
		{
				final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
				String vehicle_speed = sharedPref.getString(SettingsActivity.KEY_PREF_SPEED, "MEDIUM");
				if (speed_spinner != null)
				{
						switch (vehicle_speed)
						{
								case "SLOW":
										speed_spinner.setSelection(0);
										break;
								case "MEDIUM":
										speed_spinner.setSelection(1);
										break;
								case "FAST":
										speed_spinner.setSelection(2);
										break;
								case "CUSTOM":
										speed_spinner.setSelection(3);
										break;
								default:
										break;
						}
				}
		}

		private void remove_waypaths(String boat_name)
		{
				synchronized (_wpGraphicsLock)
				{
						ArrayList<Polyline> outline;
						ArrayList<Polyline> top;
						if (!boat_name.isEmpty())
						{
								Log.d(logTag, String.format("remove_waypaths() for %s", boat_name));
								outline = waypath_outline_map.get(boat_name);
								top = waypath_top_map.get(boat_name);
						}
						else
						{
								Log.d(logTag, "remove_waypaths() for unowned path");
								outline = outline_list;
								top = topline_list;
						}

						if (outline != null && outline.size() > 0)
						{
								Log.d(logTag, String.format("outline.size() = %d", outline.size()));
								for (Polyline p : outline)
								{
										mMapboxMap.removeAnnotation(p);
										p.remove();
								}
						}
						if (top != null && top.size() > 0)
						{
								for (Polyline p : top)
								{
										mMapboxMap.removeAnnotation(p);
										p.remove();
								}
						}
				}
		}

		private void add_waypaths(String boat_name)
		{
				synchronized (_wpGraphicsLock)
				{

						ArrayList<Polyline> outline;
						ArrayList<Polyline> top;
						int color;
						Path path;
						int wp_index;
						if (!boat_name.isEmpty())
						{
								Log.d(logTag, String.format("add_waypaths() for %s", boat_name));
								outline = waypath_outline_map.get(boat_name);
								top = waypath_top_map.get(boat_name);
								path = path_map.get(boat_name);
								wp_index = current_wp_index_map.get(boat_name);
								color = boats_map.get(boat_name).getLineColor();
						}
						else
						{
								Log.d(logTag, "add_waypaths() for unowned path");
								outline = outline_list;
								top = topline_list;
								path = unowned_path;
								wp_index = -1;
								color = Color.WHITE;
						}

						ArrayList<ArrayList<LatLng>> point_pairs;
						ArrayList<LatLng> pair;
						if (path == null) return;
						point_pairs = path.getPointPairs();
						for (int i = 0; i < point_pairs.size(); i++)
						{
								pair = point_pairs.get(i);
								outline.add(mMapboxMap.addPolyline(new PolylineOptions().addAll(pair).color(Color.BLACK).width(8)));
								// i = 0 is waypoints (0, 1) --> should be white until current wp index = 2
								// i = 1 is waypoints (1, 2) --> should be white until current wp index = 3
								// ...
								if (wp_index > i + 1)
								{
										top.add(mMapboxMap.addPolyline(new PolylineOptions().addAll(pair).color(Color.GRAY).width(4)));
										Log.d(logTag, String.format("line i = %d, current_waypoint = %d, GRAY", i, wp_index));
								}
								else
								{
										top.add(mMapboxMap.addPolyline(new PolylineOptions().addAll(pair).color(color).width(4)));
										Log.d(logTag, String.format("line i = %d, current_waypoint = %d, COLORED", i, wp_index));
								}
						}
				}
		}

		public void saveSession() throws IOException
		{
				// TODO: what is this even for?
				/* ASDF
				final File sessionFile = new File(getFilesDir() + "/session.txt");
				System.out.println(sessionFile.getAbsolutePath());
				BufferedWriter writer = new BufferedWriter(new FileWriter(sessionFile, false));
				String tempaddr = currentBoat.getIpAddress().toString();
				tempaddr = tempaddr.substring(1, tempaddr.indexOf(":"));
				writer.write(tempaddr);
				writer.write("\n");
				LatLng cameraPan = mMapboxMap.getCameraPosition().target;
				writer.write(cameraPan.getLatitude() + "\n" + cameraPan.getLongitude());
				System.out.println(mMapboxMap.getCameraPosition().target.toString());
				writer.close();
				*/
		}

		public void loadWayointFiles() throws IOException
		{
				File waypointDir = new File(Environment.getExternalStorageDirectory() + "/waypoints");
				if (waypointDir.exists() == false)
				{
						waypointDir.mkdir();
						Toast.makeText(getApplicationContext(), "Folder Does not Exist Creating Folder", Toast.LENGTH_LONG).show();
						return;
				}
				final File[] listOfFiles = waypointDir.listFiles();
				if (listOfFiles.length == 0)
				{
						Toast.makeText(getApplicationContext(), "Waypoint Directory is empty", Toast.LENGTH_LONG).show();
						return;
				}
				final Dialog dialog = new Dialog(context);
				dialog.setContentView(R.layout.waypointsavelistview);
				dialog.setTitle("List of Waypoint Files");

				final ListView fileList = (ListView) dialog.findViewById(R.id.waypointlistview);
				Button submitButton = (Button) dialog.findViewById(R.id.submitsave);
				System.out.println("length s" + listOfFiles.length);

				final ArrayAdapter<String> adapter = new ArrayAdapter<String>(
								TeleOpPanel.this,
								android.R.layout.select_dialog_singlechoice);
				fileList.setAdapter(adapter);
				for (File i : listOfFiles)
				{
						System.out.println(i.getName());
						adapter.add(i.getName());
						adapter.notifyDataSetChanged();
				}
				fileList.setOnItemClickListener(new AdapterView.OnItemClickListener()
				{
						@Override
						public void onItemClick(AdapterView<?> parent, View view, int position, long id)
						{
								current_wp_list_selected = position; //Use a different variable, this is used by the list adapter in loading waypoints ..
						}
				});
				submitButton.setOnClickListener(new OnClickListener()
				{
						@Override
						public void onClick(View v)
						{
								dialog.dismiss();
								try
								{
										waypointFileName = listOfFiles[current_wp_list_selected].getName();
								}
								catch (Exception e)
								{
										Log.e(logTag, "err loading file in waypoint file load: ");
										Log.e(logTag, e.toString());
								}
						}
				});
				dialog.show();
		}

		void LoadWaypointsFromFile(String waypoints_filename)
		{
				final File waypointFile = new File(Environment.getExternalStorageDirectory() + "/waypoints/" + waypoints_filename);
				Scanner fileScanner;
				try
				{
						fileScanner = new Scanner(waypointFile); //Scans each line of the file
				}
				catch (Exception ex)
				{
						Toast.makeText(context, "Could not open waypoints file", Toast.LENGTH_SHORT).show();
						return;
				}

				final ArrayList<ArrayList<ILatLng>> waypointsaves = new ArrayList<ArrayList<ILatLng>>();
				final ArrayList<String> saveName = new ArrayList<>();
				// scans each line of the file as a waypoint save then scans each line every two elements makes a latlng

				if (waypointFile.exists())
				{
						while (fileScanner.hasNext())
						{
								final ArrayList<ILatLng> currentSave = new ArrayList<ILatLng>();
								String s = fileScanner.nextLine();
								Log.i(logTag, "fileScanner.nextLine():  " + s);

								final Scanner stringScanner = new Scanner(s);

								//get save name (everything between quotes)
								if (stringScanner.hasNext())
								{
										if (stringScanner.next().equals("\""))
										{ //found first "
												String currentdata = stringScanner.next();
												String name = currentdata;
												while (!currentdata.equals("\""))
												{
														currentdata = stringScanner.next();
														if (!currentdata.equals("\""))
														{
																name = name + " " + currentdata;
														}
												}
												saveName.add(name);
										}
								}

								while (stringScanner.hasNext())
								{
										final double templat = Double.parseDouble(stringScanner.next());
										final double templon = Double.parseDouble(stringScanner.next());
										Log.d(logTag, "load waypoints from file iteration");
										Log.d(logTag, Double.toString(templat) + " " + Double.toString(templon));

										ILatLng temp = new ILatLng()
										{
												@Override
												public double getLatitude() { return templat; }

												@Override
												public double getLongitude() { return templon; }

												@Override
												public double getAltitude() { return 0; }
										};
										currentSave.add(temp);
								}
								if (currentSave.size() > 0)
								{ //make sure no empty arrays
										waypointsaves.add(currentSave);
								}
								stringScanner.close();
						}
						fileScanner.close();

						final Dialog dialog = new Dialog(context);
						dialog.setContentView(R.layout.waypointsavelistview);
						dialog.setTitle("List of Waypoint Saves");
						final ListView wpsaves = (ListView) dialog.findViewById(R.id.waypointlistview);
						Button submitButton = (Button) dialog.findViewById(R.id.submitsave);

						final ArrayAdapter<String> adapter = new ArrayAdapter<>(TeleOpPanel.this, android.R.layout.select_dialog_singlechoice);
						wpsaves.setAdapter(adapter);
						for (String s : saveName)
						{
								adapter.add(s);
								adapter.notifyDataSetChanged();
						}

						wpsaves.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
						{
								@Override
								public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id)
								{
										final Dialog confirmdialog = new Dialog(context);
										confirmdialog.setContentView(R.layout.confirmdeletewaypoints);
										confirmdialog.setTitle("Delete This Waypoint Path?");
										Button deletebutton = (Button) confirmdialog.findViewById(R.id.yesbutton);
										Button cancel = (Button) confirmdialog.findViewById(R.id.nobutton);
										deletebutton.setOnClickListener(new OnClickListener()
										{
												@Override
												public void onClick(View v)
												{
														//delete line from file

														//delete object from list since update wont occur until you press load wp again
														adapter.remove(adapter.getItem(position));
														try
														{
																File inputFile = new File(getFilesDir() + "/waypoints.txt");
																File tempFile = new File(getFilesDir() + "/tempwaypoints.txt");

																BufferedReader reader = new BufferedReader(new FileReader(inputFile));
																BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

																String lineToRemove = "\" " + saveName.get(position) + " \"";
																String currentLine;

																while ((currentLine = reader.readLine()) != null)
																{

																		String trimmedLine = currentLine.trim();
																		if (trimmedLine.contains(lineToRemove))
																		{
																				continue;
																		}
																		writer.write(currentLine + System.getProperty("line.separator"));
																}
																writer.close();
																reader.close();
																tempFile.renameTo(inputFile);
														}
														catch (Exception e)
														{
														}
														confirmdialog.dismiss();
												}
										});
										cancel.setOnClickListener(new OnClickListener()
										{
												@Override
												public void onClick(View v)
												{
														confirmdialog.dismiss();
												}
										});
										confirmdialog.show();

										return false;
								}
						});
						wpsaves.setOnItemClickListener(new AdapterView.OnItemClickListener()
						{
								@Override
								public void onItemClick(AdapterView<?> parent, View view, int position, long id)
								{
										current_wp_list_selected = position;
								}
						});
						submitButton.setOnClickListener(new OnClickListener()
						{
								@Override
								public void onClick(View v)
								{
										/*ASDF*/
										if (current_wp_list_selected == -1)
										{
												dialog.dismiss();
												//write no selected box
										}
										mMapboxMap.removeAnnotations(marker_list);
										marker_list.clear();
										waypoint_list.clear();
										remove_waypaths("");

										for (ILatLng i : waypointsaves.get(current_wp_list_selected)) //tbh not sure why there is a 1 offset but there is
										{
												LatLng point = new LatLng(i.getLatitude(), i.getLongitude());
												waypoint_list.add(point);
												String title = "waypoint_" + Integer.toString(marker_list.size());
												marker_list.add(mMapboxMap.addMarker(new MarkerOptions().position(point).title(title)));
												marker_types_map.put(title, PlatypusMarkerTypes.WAYPOINT);
										}

										// don't bother generating a path. Let the user do that.

										dialog.dismiss();
								}
						});
						dialog.show();
				}
		}

		public void loadPreferences()
		{
				final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

				String iP = sharedPref.getString(SettingsActivity.KEY_PREF_IP, "192.168.1.1");
				String port = sharedPref.getString(SettingsActivity.KEY_PREF_PORT, "11411");

				textIpAddress = iP;
				textIpAddress = textIpAddress.replace("/", ""); // network on main thread error if this doesn't happen
				boatPort = port;

				// get vehicle type and speed preferences
				String vehicle_type = sharedPref.getString(SettingsActivity.KEY_PREF_VEHICLE_TYPE, "PROP");
				String vehicle_speed = sharedPref.getString(SettingsActivity.KEY_PREF_SPEED, "MEDIUM");
				set_speed_spinner_from_pref();

				Log.i(logTag, String.format("Vehicle type = %s, Speed = %s", vehicle_type, vehicle_speed));
				switch (vehicle_type)
				{
						case "PROP":
								switch (vehicle_speed)
								{
										case "SLOW":
												tPID[0] = 0.07;
												tPID[1] = 0.0;
												tPID[2] = 0.0;
												rPID[0] = 0.45;
												rPID[1] = 0;
												rPID[2] = 0.45;
												break;

										case "MEDIUM":
												tPID[0] = 0.2;
												tPID[1] = 0.0;
												tPID[2] = 0.0;
												rPID[0] = 0.8;
												rPID[1] = 0.0;
												rPID[2] = 0.8;
												break;

										case "FAST":
												tPID[0] = 0.6;
												tPID[1] = 0.0;
												tPID[2] = 0.0;
												rPID[0] = 0.7;
												rPID[1] = 0;
												rPID[2] = 0.9;
												break;

										case "CUSTOM":
												tPID[0] = Double.parseDouble(sharedPref.getString(SettingsActivity.KEY_PREF_PID_THRUST_P, "0.2"));
												tPID[1] = Double.parseDouble(sharedPref.getString(SettingsActivity.KEY_PREF_PID_THRUST_I, "0"));
												tPID[2] = Double.parseDouble(sharedPref.getString(SettingsActivity.KEY_PREF_PID_THRUST_D, "0"));
												rPID[0] = Double.parseDouble(sharedPref.getString(SettingsActivity.KEY_PREF_PID_RUDDER_P, "1.0"));
												rPID[1] = Double.parseDouble(sharedPref.getString(SettingsActivity.KEY_PREF_PID_RUDDER_I, "0"));
												rPID[2] = Double.parseDouble(sharedPref.getString(SettingsActivity.KEY_PREF_PID_RUDDER_D, "0.2"));
												break;

										default:
												break;
								}
								break;

						case "AIR":
								switch (vehicle_speed)
								{
										case "SLOW":
												tPID[0] = 0.2;
												tPID[1] = 0.0;
												tPID[2] = 0.0;
												rPID[0] = 0.9;
												rPID[1] = 0.0;
												rPID[2] = 0.9;
												break;

										case "MEDIUM":
												tPID[0] = 0.5;
												tPID[1] = 0.0;
												tPID[2] = 0.0;
												rPID[0] = 0.75;
												rPID[1] = 0.0;
												rPID[2] = 0.9;
												break;

										case "FAST":
												tPID[0] = 0.8;
												tPID[1] = 0;
												tPID[2] = 0;
												rPID[0] = 0.7;
												rPID[1] = 0;
												rPID[2] = 0.9;
												break;

										case "CUSTOM":
												tPID[0] = Double.parseDouble(sharedPref.getString(SettingsActivity.KEY_PREF_PID_THRUST_P, "0.4"));
												tPID[1] = Double.parseDouble(sharedPref.getString(SettingsActivity.KEY_PREF_PID_THRUST_I, "0"));
												tPID[2] = Double.parseDouble(sharedPref.getString(SettingsActivity.KEY_PREF_PID_THRUST_D, "0"));
												rPID[0] = Double.parseDouble(sharedPref.getString(SettingsActivity.KEY_PREF_PID_RUDDER_P, "0.75"));
												rPID[1] = Double.parseDouble(sharedPref.getString(SettingsActivity.KEY_PREF_PID_RUDDER_I, "0"));
												rPID[2] = Double.parseDouble(sharedPref.getString(SettingsActivity.KEY_PREF_PID_RUDDER_D, "0.90"));
												break;

										default:
												break;
								}
								break;

						default:
								break;
				}

				THRUST_MIN = Double.parseDouble(sharedPref.getString(SettingsActivity.KEY_PREF_THRUST_MIN, "-1.0"));
				THRUST_MAX = Double.parseDouble(sharedPref.getString(SettingsActivity.KEY_PREF_THRUST_MAX, "0.3"));

				RUDDER_MIN = Double.parseDouble(sharedPref.getString(SettingsActivity.KEY_PREF_RUDDER_MIN, "-1.0"));
				RUDDER_MAX = Double.parseDouble(sharedPref.getString(SettingsActivity.KEY_PREF_RUDDER_MAX, "1.0"));

				Double initialPanLat = Double.parseDouble(sharedPref.getString(SettingsActivity.KEY_PREF_LAT, "0"));
				Double initialPanLon = Double.parseDouble(sharedPref.getString(SettingsActivity.KEY_PREF_LON, "0"));
				initialPan = new LatLng(initialPanLat, initialPanLon);
				setInitialPan = sharedPref.getBoolean(SettingsActivity.KEY_PREF_SAVE_MAP, true);
		}

		public void calculatePathDistance()
		{
				// calculate the total path length (including current boat position to first waypoint)
				// display it in the GUI
				ArrayList<LatLng> points = (ArrayList<LatLng>)unowned_path.getPoints().clone();
				if (points.size() < 1)
				{
						path_length_value.setText("0");
						return;
				}
				Boat boat = currentBoat();
				if (boat != null && boat.getLocation() != null) points.add(0, boat.getLocation());
				double total_distance = 0;
				for (int i = 1; i < points.size(); i++)
				{
						total_distance += points.get(i).distanceTo(points.get(i-1));
				}
				path_length_value.setText(Long.toString(Math.round(total_distance)));
		}
}
