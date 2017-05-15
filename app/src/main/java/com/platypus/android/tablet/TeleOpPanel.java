package com.platypus.android.tablet;

import java.io.BufferedReader;
import java.io.File;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Scanner;

import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.jscience.geography.coordinates.LatLong;
import org.jscience.geography.coordinates.UTM;
import org.jscience.geography.coordinates.crs.ReferenceEllipsoid;
import org.json.JSONException;
import org.json.JSONObject;

import com.mapbox.mapboxsdk.MapboxAccountManager;
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
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.offline.OfflineManager;
import com.mapbox.mapboxsdk.offline.OfflineRegion;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;

import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;

import com.mapbox.mapboxsdk.offline.OfflineRegionError;
import com.mapbox.mapboxsdk.offline.OfflineRegionStatus;
import com.mapbox.mapboxsdk.offline.OfflineTilePyramidRegionDefinition;
import com.mapzen.android.lost.api.LocationServices;

import com.platypus.android.tablet.Path.AreaType;
import com.platypus.android.tablet.Path.Path;
import com.platypus.android.tablet.Path.Region;
import com.platypus.crw.CrwNetworkUtils;
import com.platypus.crw.SensorListener;
import com.platypus.crw.VehicleServer;
import com.platypus.crw.data.SensorData;
import com.platypus.crw.data.Pose3D;

import android.app.Activity;
import android.content.Context;

import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.platypus.crw.FunctionObserver;
import com.platypus.crw.PoseListener;
import com.platypus.crw.VehicleServer.WaypointState;
import com.platypus.crw.WaypointListener;
import com.platypus.crw.data.Utm;
import com.platypus.crw.data.UtmPose;

import android.app.Dialog;

import android.view.View.OnClickListener;

import com.platypus.android.tablet.Joystick.*;

public class TeleOpPanel extends Activity implements SensorEventListener
{
		final Context context = this;
		TextView ipAddressBox = null;
		TextView mapInfo = null;
		RelativeLayout linlay = null;
		//AsyncTask networkThread;

		ToggleButton pauseWPButton = null;
		ToggleButton spirallawn;

		ImageButton createVertexStatusButton = null;
		ImageButton createWaypointStatusButton = null;

		Button deleteWaypoint = null;
		Button connectButton = null;
		Button advancedOptions = null;
		Button makeConvex = null; //for testing remove later
		Button perimeter = null;
		Button centerToBoat = null;
		Button startRegion = null;
		Button clearRegion = null;
		Button updateTransect = null;

		TextView sensorData1 = null;
		TextView sensorData2 = null;
		TextView sensorData3 = null;

		TextView sensorType1 = null;
		TextView sensorType2 = null;
		TextView sensorType3 = null;

		TextView battery = null;
		TextView Title = null;
		TextView waypointInfo = null;

		LinearLayout regionlayout = null;
		LinearLayout waypointlayout = null;
		View waypointregion = null;

		ToggleButton sensorvalueButton = null;
		JoystickView joystick;
		private boolean speed_spinner_erroneous_call = true;
		Spinner speed_spinner = null;

		ScheduledThreadPoolExecutor polling_thread_pool = new ScheduledThreadPoolExecutor(1);
		int updateRateMili = 200;
		boolean waypointLayoutEnabled = true; //if false were on region layout
		boolean containsRegion = false;

		double currentTransectDist = 10;

		MapView mv;
		MapboxMap mMapboxMap;

		Marker home_M;
		IconFactory mIconFactory;
		MarkerView boat_markerview;

		int currentselected = -1; //which element selected
		String saveName; //shouldnt be here?
		LatLng pHollowStartingPoint = new LatLng((float) 40.436871,
						(float) -79.948825);
		LatLng initialPan = new LatLng(0, 0);
		boolean setInitialPan = true;
		String waypointStatus = "";
		double rudderTemp = 0;
		double thrustTemp = 0;
		String boatwaypoint;
		boolean networkConnection = true;

		SensorManager senSensorManager;
		Sensor senAccelerometer;
		public boolean stopWaypoints = true;

		public static double THRUST_MIN = -1.0;
		public static double THRUST_MAX = 0.3;
		public static double RUDDER_MIN = -1.0;
		public static double RUDDER_MAX = 1.0;

		public EditText ipAddress = null;
		public EditText color = null;
		public EditText transectDistance;
		public Button startWaypoints = null;

		public RadioButton direct = null;
		public RadioButton reg = null;

		public static String textIpAddress;
		public static String boatPort = "11411";
		public static Boat currentBoat;
		public static InetSocketAddress address;
		private final Object _waypointLock = new Object();

		public double[] data;
		SensorData Data;
		public String sensorV = "Loading...";
		boolean sensorReady = false;

		private PoseListener pl;
		private SensorListener sl;
		private WaypointListener wl;
		private boolean startDrawRegions = false;
		private boolean startDrawWaypoints = false;

		double[] tPID = {.2, .0, .0};
		double[] rPID = {1, 0, .2};

		double battery_voltage = 0.0;

		//private UtmPose _pose;
		private UtmPose[] wpPose = null;

		Icon Ihome;

		Path boatPath = null;
		ArrayList<LatLng> touchpointList = new ArrayList<LatLng>();
		ArrayList<LatLng> waypointList = new ArrayList<LatLng>();
		ArrayList<LatLng> savePointList = new ArrayList<LatLng>();
		ArrayList<Marker> markerList = new ArrayList();

		String waypointFileName = "waypoints.txt";

		ArrayList<UtmPose> allWaypointsSent = new ArrayList<UtmPose>();

		ArrayList<Polyline> Waypath_outline = new ArrayList<>();
		ArrayList<Polyline> Waypath_top = new ArrayList<>();
		Polyline boat_to_waypoint_line; // TODO: implement line from boat to current WP
		int current_waypoint_index = -1;
		int last_waypoint_index = -2;
		final Object _wpGraphicsLock = new Object();

		private TabletLogger mlogger;

		LatLng home = null;
		Date d = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");

		private static final String logTag = "TeleOpPanel"; //TeleOpPanel.class.getName();

		NotificationManager notificationManager;
		Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		long sleep_start_time = 0;
		boolean alarm_on = false;
		final Object _batteryVoltageLock = new Object();
		Ringtone alarm_ringtone;
		Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

		/*ASDF*/
		public com.mapbox.mapboxsdk.geometry.LatLng jscienceLatLng_to_mapboxLatLng(org.jscience.geography.coordinates.LatLong jlatlng)
		{
				LatLng result = new LatLng(
								jlatlng.latitudeValue(SI.RADIAN)*180./Math.PI,
								jlatlng.longitudeValue(SI.RADIAN)*180./Math.PI);
				return result;
		}

		protected void onCreate(final Bundle savedInstanceState)
		{
				super.onCreate(savedInstanceState);
				this.setContentView(R.layout.tabletlayoutswitch);

				ipAddressBox = (TextView) this.findViewById(R.id.printIpAddress);
				linlay = (RelativeLayout) this.findViewById(R.id.linlay);

				connectButton = (Button) this.findViewById(R.id.connectButton);
				makeConvex = (Button) this.findViewById(R.id.makeconvex);
				sensorData1 = (TextView) this.findViewById(R.id.SValue1);
				sensorData2 = (TextView) this.findViewById(R.id.SValue2);
				sensorData3 = (TextView) this.findViewById(R.id.SValue3);
				sensorType1 = (TextView) this.findViewById(R.id.sensortype1);
				sensorType2 = (TextView) this.findViewById(R.id.sensortype2);
				sensorType3 = (TextView) this.findViewById(R.id.sensortype3);
				sensorvalueButton = (ToggleButton) this.findViewById(R.id.SensorStart);
				sensorvalueButton.setClickable(sensorReady);
				sensorvalueButton.setTextColor(Color.GRAY);
				battery = (TextView) this.findViewById(R.id.batteryVoltage);
				joystick = (JoystickView) findViewById(R.id.joystickView);
				Title = (TextView) this.findViewById(R.id.controlScreenEnter);
				advancedOptions = (Button) this.findViewById(R.id.advopt);
				centerToBoat = (Button) this.findViewById(R.id.centermap);
				mapInfo = (TextView) this.findViewById(R.id.mapinfo);
				final ToggleButton switchView = (ToggleButton) this.findViewById(R.id.switchviewbutton);
				alarm_ringtone = RingtoneManager.getRingtone(getApplicationContext(), alarmUri);
				notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

				mapInfo.setText("Map Information \n Nothing Pending");

				SettingsActivity.set_TeleOpPanel(this);
				loadPreferences();

				sensorData1.setText("Waiting");
				sensorData2.setText("Waiting");
				sensorData3.setText("Waiting");


				//Create folder for the first time if it does not exist
				File waypointDir = new File(Environment.getExternalStorageDirectory() + "/waypoints");
				if (!waypointDir.exists())
				{
						waypointDir.mkdir();
				}

				mv = (MapView) findViewById(R.id.mapview);
				//mv.setAccessToken(ApiAccess.getToken(this));

				MapboxAccountManager.start(this, getString(R.string.mapbox_access_token));
				mv.onCreate(savedInstanceState);
				mv.getMapAsync(new OnMapReadyCallback()
				{
						@Override
						public void onMapReady(@NonNull MapboxMap mapboxMap)
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
								mMapboxMap.getUiSettings().setRotateGesturesEnabled(false); //broken on mapbox side, currently fixing issue 4635 https://github.com/mapbox/mapbox-gl-native/issues/4635
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
												if (startDrawWaypoints == true && startDrawRegions == false)
												{
														touchpointList.add(point);
														Log.i(logTag, Integer.toString(touchpointList.size()));
														boatPath = new Path(touchpointList);
												}
												else if (startDrawRegions && !startDrawWaypoints)
												{
														touchpointList.add(point);
														if (spirallawn.isChecked())
														{
																ArrayList<LatLng> temp = new ArrayList<LatLng>(touchpointList);
																boatPath = new Region(temp, AreaType.LAWNMOWER, currentTransectDist);
																touchpointList = boatPath.getQuickHullList();
														}
														else
														{
																ArrayList<LatLng> temp = new ArrayList<LatLng>(touchpointList);
																boatPath = new Region(temp, AreaType.SPIRAL, currentTransectDist);
																touchpointList = boatPath.getQuickHullList();
														}
												}
												invalidate();
										}
								});
								mIconFactory = IconFactory.getInstance(context);
								boat_markerview = mMapboxMap.addMarker(new MarkerViewOptions().position(pHollowStartingPoint).title("Boat")
												.icon(mIconFactory.fromResource(R.drawable.pointarrow)).rotation(0));
						}
				});

				/*ASDF*/
				polling_thread_pool.scheduleAtFixedRate(new Runnable()
				{
						@Override
						public void run()
						{
								if (currentBoat != null)
								{
										final boolean isConnected = currentBoat.isConnected();
										runOnUiThread(new Runnable()
										{
												@Override
												public void run()
												{
														if (isConnected)
														{
																ipAddressBox.setBackgroundColor(Color.GREEN);
														}
														else
														{
																ipAddressBox.setBackgroundColor(Color.RED);
														}
												}
										});
								}
						}
				}, 0, 1, TimeUnit.SECONDS);


				//load inital waypoint menu
				onLoadWaypointLayout();
				switchView.setOnClickListener(new OnClickListener()
				{
						@Override
						public void onClick(View view1)
						{
								if (switchView.isChecked())
								{
										waypointlayout.removeAllViews();
										onLoadRegionLayout();
										waypointLayoutEnabled = false;
										startDrawRegions = startDrawWaypoints;
										startDrawWaypoints = false;

										if (!startDrawWaypoints)
										{
												createWaypointStatusButton.setBackgroundResource(R.drawable.draw_icon2);
										}
										else
										{
												createWaypointStatusButton.setBackgroundResource(R.drawable.draw_icon);
										}

										if (boatPath == null)
										{
												boatPath = new Path();
										}
										ArrayList<LatLng> temp = new ArrayList<LatLng>(touchpointList);
										if (spirallawn.isChecked())
										{
												boatPath = new Region(temp, AreaType.LAWNMOWER, currentTransectDist);
										}
										else
										{
												boatPath = new Region(temp, AreaType.SPIRAL, currentTransectDist);
										}
								}
								else
								{
										regionlayout.removeAllViews();
										onLoadWaypointLayout();
										waypointLayoutEnabled = true;
										startDrawWaypoints = startDrawRegions;
										startDrawRegions = false;

										if (!startDrawRegions)
										{
												createVertexStatusButton.setBackgroundResource(R.drawable.draw_icon2);
										}
										else
										{
												createVertexStatusButton.setBackgroundResource(R.drawable.draw_icon);
										}


										if (boatPath == null)
										{
												boatPath = new Path();
										}
										ArrayList<LatLng> temp = new ArrayList<LatLng>(touchpointList);
										boatPath = new Path(temp);

								}
								invalidate();
						}
				});

				if (mlogger != null)
				{
						mlogger.close();
				}
				mlogger = new TabletLogger();


				centerToBoat.setOnClickListener(new OnClickListener()
				{
						@Override
						public void onClick(View view)
						{
								if (mMapboxMap == null)
								{
										Toast.makeText(getApplicationContext(), "Please wait for the map to load", Toast.LENGTH_LONG).show();
										return;
								}
								if (currentBoat == null)
								{
										Toast.makeText(getApplicationContext(), "Please Connect to a boat first", Toast.LENGTH_LONG).show();
										return;
								}
								if (currentBoat.getLocation() == null)
								{
										Toast.makeText(getApplicationContext(), "Boat still finding GPS location", Toast.LENGTH_LONG).show();
										return;
								}
								mMapboxMap.moveCamera(CameraUpdateFactory.newCameraPosition(
												new CameraPosition.Builder()
																.target(currentBoat.getLocation())
																.zoom(16)
																.build()
								));
						}
				});
				//Options menu
				advancedOptions.setOnClickListener(new OnClickListener()
				{
						@Override
						public void onClick(View v)
						{
								PopupMenu popup = new PopupMenu(TeleOpPanel.this, advancedOptions);
								popup.getMenuInflater().inflate(R.menu.dropdownmenu, popup.getMenu());
								popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
								{
										public boolean onMenuItemClick(MenuItem item)
										{
												switch (item.toString())
												{
														case "Save Map":
														{
																Thread thread = new Thread()
																{
																		@Override
																		public void run()
																		{
																				saveMap();
																		}
																};
																thread.start();
																break;
														}
														case "Satellite Map":
														{
																if (mMapboxMap != null)
																{
																		mMapboxMap.setStyle(Style.SATELLITE);
																}
																break;
														}
														case "Vector Map":
														{
																if (mMapboxMap != null)
																{
																		mMapboxMap.setStyle(Style.MAPBOX_STREETS);
																}
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
																		catch (Exception e)
																		{
																				System.out.println("failed to save waypoints from file");
																		}
																		break;
																}
														}
														case "Load Waypoints":
														{
																try
																{
																		LoadWaypointsFromFile(waypointFileName);
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
																catch (Exception e)
																{

																}
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
												}
												return true;
										}
								});
								popup.show(); //showing popup menu
						}
				});


				// *****************//
				//      Joystick   //
				// ****************//

				joystick.setYAxisInverted(false);

				//*****************************************************************************
				//  Initialize Poselistener
				//*****************************************************************************
				pl = new PoseListener()
				{ //gets the location of the boat
						public void receivedPose(UtmPose upwcs)
						{
								if (currentBoat != null)
								{
										currentBoat.setConnected(true);
										currentBoat.setYaw(Math.PI / 2 - upwcs.pose.getRotation().toYaw());
										//currentBoat.setUtmZone(String.valueOf(upwcs.origin.zone));
										currentBoat.setLocation(
														jscienceLatLng_to_mapboxLatLng(
																		UTM.utmToLatLong(
																						UTM.valueOf(
																										upwcs.origin.zone,
																										upwcs.origin.isNorth ? 'T' : 'L',
																										upwcs.pose.getX(),
																										upwcs.pose.getY(),
																										SI.METER),
																						ReferenceEllipsoid.WGS84)));

										// update the boat marker
										/*ASDF*/
										runOnUiThread(new Runnable()
										{
												@Override
												public void run()
												{
														if (boat_markerview == null) return;
														boat_markerview.setPosition(currentBoat.getLocation());
														float degree = (float) (currentBoat.getYaw() * 180 / Math.PI);  // degree is -90 to 270
														degree = (degree < 0 ? 360 + degree : degree); // degree is 0 to 360
														boat_markerview.setRotation(degree);
												}
										});
								}
						}
				};


				//*******************************************************************************
				//  Initialize Sensorlistener
				//*******************************************************************************
				sl = new SensorListener()
				{
						@Override
						public void receivedSensor(SensorData sensorData)
						{
								Data = sensorData;
								sensorV = Arrays.toString(Data.data);
								sensorV = sensorV.substring(1, sensorV.length() - 1);
								sensorReady = true;
						}
				};

				//*******************************************************************************
				//  Initialize Waypointlistener
				//*******************************************************************************

				wl = new WaypointListener()
				{
						@Override
						public void waypointUpdate(WaypointState waypointState)
						{
								boatwaypoint = waypointState.toString();
						}
				};

				//****************************************************************************
				//  Initialize the Boat
				// ****************************************************************************
				currentBoat = new Boat(pl, sl, wl);

				senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
				senAccelerometer = senSensorManager
								.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
				senSensorManager.registerListener(this, senAccelerometer,
								SensorManager.SENSOR_DELAY_NORMAL);

				final IconFactory mIconFactory = IconFactory.getInstance(this);
				Drawable mhome = ContextCompat.getDrawable(this, R.drawable.home1);
				Ihome = mIconFactory.fromDrawable(mhome);

				connectButton.setOnClickListener(new OnClickListener()
				{
						@Override
						public void onClick(View view)
						{
								// ask the currentBoat server if it is connected
								if (currentBoat.isConnected())
								{
										new AlertDialog.Builder(context)
														.setTitle("Connect")
														.setMessage("You are already connected,\n do you want to reconnect?")
														.setPositiveButton("Yes", new DialogInterface.OnClickListener()
														{
																public void onClick(DialogInterface dialog, int which)
																{
																		currentBoat = new Boat(pl, sl, wl); // this is the line that forces a reconnection
																		connectBox();
																		Log.i(logTag, "Reconnect");
																}
														})
														.setNegativeButton("No", new DialogInterface.OnClickListener()
														{
																public void onClick(DialogInterface dialog, int which)
																{ /*nothing*/ }
														})
														.show();
								}
								else
								{
										connectBox();
										Log.i(logTag, "Initial connection");
								}
						}
				});
				connectBox(); // start the app with the connect dialog popped up
		}

		@Override
		protected void onStart()
		{
				super.onStart();
				//mv.onStart();
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
				if (currentBoat.getLocation() != null)
				{
						editor.putString(SettingsActivity.KEY_PREF_LAT, Double.toString(currentBoat.getLocation().getLatitude()));
						editor.putString(SettingsActivity.KEY_PREF_LON, Double.toString(currentBoat.getLocation().getLongitude()));
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

		private JoystickMovedListener joystick_moved_listener = new JoystickMovedListener()
		{
				@Override
				public void OnMoved(int x, int y)
				{
						Log.d(logTag, String.format("joystick (x, y) = %d, %d", x, y));
						thrustTemp = fromProgressToRange(y, THRUST_MIN, THRUST_MAX);
						rudderTemp = fromProgressToRange(x, RUDDER_MIN, RUDDER_MAX);
				}

				@Override
				public void OnReleased() { }

				@Override
				public void OnReturnedToCenter()
				{
						if (currentBoat != null)
						{
								/*ASDF*/
								currentBoat.updateControlSignals(0.0, 0.0);
						}
				}
		};


		/*ASDF*/

		// Shantanu: use AsyncTask to launch isConnected (which calls itself endlessly) and launches a runnable to poll the boat state, then call progressUpdate
		//           The AsyncTask then updates the UI with the onProgressUpdate and the results of the boat state poll
		//           This setup completely disconnects the main UI thread from waiting for the real result of the core comms call
		//               and therefore the UI may show incorrect state, but it doesn't look choppy at all
		// My stuff: use a Handler.postDelayed to periodically call isConnected, which creates a Future and waits for the result
		//           This guarantees a correct result, but causes the UI to look choppy, even if I use callbacks too
		//
		// How can I combine these two methods?
		//           Idea 1) Instead of a handler.postDelayed, why don't I use the asyncTask task to schedule the isConnected call
		//                   When isConnected returns, use the result as Progress and in onProgressUpdate, update the UI
		//           Idea 2) Put the AsyncTask inside the Boat method, like I was doing with Callable.
		//                   Set up callbacks in TeleOpPanel and feed them to the Boat method for the different parts of the AsyncTask
		//                   Essentially you are giving the Boat handles on the GUI
		//


  /*
   * this async task handles all of the networking on the boat since networking has to be done on
   * a different thread and since gui updates have to be updated on the main thread ....
   */
    /*
		private class NetworkAsync extends AsyncTask<String, Integer, String>
		{
				boolean connected = false;

				BitmapFactory.Options options = new BitmapFactory.Options();

				public void setOptions(BitmapFactory.Options options)
				{
						this.options = options;
						this.options.inDither = false;
						this.options.inTempStorage = new byte[18 * 23];
				}

				@Override
				protected void onPreExecute()
				{
						setOptions(options);
				}

				@Override
				protected String doInBackground(String... arg0)
				{
						networkRun = new Runnable()
						{
								@Override
								public void run()
								{

										Log.i(logTag, "TeleOpPanel.NetworkAsync.doInBackground() iteration...");
										if (currentBoat != null)
										{
												connected = currentBoat.isConnected();

												if (old_thrust != thrustTemp || old_rudder != rudderTemp)
												{
														currentBoat.updateControlSignals(thrustTemp, rudderTemp);
												}
												if (stopWaypoints)
												{
														currentBoat.stopWaypoints();
														stopWaypoints = false;
												}
												old_thrust = thrustTemp;
												old_rudder = rudderTemp;

												publishProgress();
										}

								}
						};

						exec = new ScheduledThreadPoolExecutor(1);
						future = exec.scheduleAtFixedRate(networkRun, 0, updateRateMili, TimeUnit.MILLISECONDS);
						return null;
				}

				@Override
				protected void onProgressUpdate(Integer... result)
				{
						LatLng curLoc;
						if (latlongloc != null)
						{
								curLoc = new LatLng(latlongloc.latitudeValue(SI.RADIAN) * 180 / Math.PI, latlongloc.longitudeValue(SI.RADIAN) * 180 / Math.PI);
								try
								{
										currentBoat.setLocation(curLoc);
								}
								catch (Exception e)
								{
								}
						}

						if (connected)
						{
								ipAddressBox.setBackgroundColor(Color.GREEN);
						}
						else
						{
								ipAddressBox.setBackgroundColor(Color.RED);
						}

						if (sensorReady)
						{
								try
								{
										sensorvalueButton.setClickable(sensorReady);
										sensorvalueButton.setTextColor(Color.BLACK);
										sensorvalueButton.setText("Show SensorData");
										SharedPreferences settings = getSharedPreferences(PREF_NAME, 0);
										SharedPreferences.Editor editor = settings.edit();

										if (Data.channel == 4)
										{
												String[] batteries = sensorV.split(",");
												battery.setText(batteries[0]);
												battery.setTextColor(isAverage(Data, batteries[0]));
												double value = (Double.parseDouble(batteries[0]) + getAverage(Data)) / 2;
												synchronized (_batteryVoltageLock)
												{
														battery_voltage = Double.parseDouble(batteries[0]);
												}
												editor.putString(Data.type.toString(), Double.toString(value));
												editor.commit();
										}

										if (sensorvalueButton.isChecked())
										{
												double value;
												switch (Data.channel)
												{
														case 4:
																break;
														case 1:
																sensorData1.setText(sensorV);
																sensorType1.setText(unit(Data.type));
																sensorData1.setTextColor(isAverage(Data, sensorV));
																value = (Double.parseDouble(sensorV) + getAverage(Data)) / 2;

																editor.putString(Data.type.toString(), Double.toString(value));
																editor.commit();

																break;
														case 2:
																sensorData2.setText(sensorV);
																sensorType2.setText(unit(Data.type));
																sensorData2.setTextColor(isAverage(Data, sensorV));
																value = (Double.parseDouble(sensorV) + getAverage(Data)) / 2;
																editor.putString(Data.type.toString(), Double.toString(value));
																editor.commit();

																break;
														case 3:
																sensorData3.setText(sensorV);
																sensorType3.setText(unit(Data.type));
																sensorData3.setTextColor(isAverage(Data, sensorV));
																value = (Double.parseDouble(sensorV) + getAverage(Data)) / 2;
																editor.putString(Data.type.toString(), Double.toString(value));
																editor.commit();
																break;
														case 9:
																break;
														default:
												}
										}
								}
								catch (Exception e)
								{
										Log.i(sensorLogTag, e.toString());
										System.out.println("Sensor error " + e.toString());
								}
								if (!sensorvalueButton.isChecked())
								{
										sensorData1.setText("----");
										sensorData2.setText("----");
										sensorData3.setText("----");
								}
						}
						else
						{
								sensorvalueButton.setText("Sensor Unavailable");
								sensorData1.setText("----");
								sensorData2.setText("----");
								sensorData3.setText("----");
						}

						// Adding Joystick move listener
						joystick.setOnJostickMovedListener(joystick_moved_listener);
						joystick.setOnJostickClickedListener(new JoystickClickedListener()
						{
								@Override
								public void OnClicked() { }

								@Override
								public void OnReleased() { }
						});

						if (waypointLayoutEnabled)
						{
								String status = boatwaypoint;
								if (status == null)
								{
										status = "\t\t-----";
								}
								waypointInfo.setText("Waypoint Status: \n" + status);

						}
				}
		}
*/

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
				else
				{
						unit = "";
				}

				return unit;
		}

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
				final Dialog dialog = new Dialog(context);
				dialog.setContentView(R.layout.connectdialog);
				dialog.setTitle("Connect To A Boat");
				ipAddress = (EditText) dialog.findViewById(R.id.ipAddress1);

				Button submitButton = (Button) dialog.findViewById(R.id.submit);

				direct = (RadioButton) dialog.findViewById(R.id.wifi);
				reg = (RadioButton) dialog.findViewById(R.id.reg);
				System.out.println("ipaddr " + textIpAddress);
				loadPreferences();
				ipAddress.setText(textIpAddress);

				direct.setOnClickListener(new OnClickListener()
				{
						@Override
						public void onClick(View v)
						{
								if (direct.isChecked())
								{
										ipAddress.setText("192.168.1.20");
								}
						}
				});
				reg.setOnClickListener(new OnClickListener()
				{
						@Override
						public void onClick(View v)
						{
								if (reg.isChecked())
								{
										ipAddress.setText("tunnel.senseplatypus.com");
								}
								else
								{
										ipAddress.setText("192.168.1.20");
								}
						}
				});

				submitButton.setOnClickListener(new OnClickListener()
				{
						@Override
						public void onClick(View v)
						{

								if (ipAddress.getText() == null || ipAddress.getText().equals("") || ipAddress.getText().length() == 0)
								{
										ipAddressBox.setText("IP Address: 127.0.0.1 (localhost)");
								}
								else
								{
										ipAddressBox.setText("IP Address: " + ipAddress.getText());
								}
								markerList = new ArrayList<Marker>();
								textIpAddress = ipAddress.getText().toString();
								if (direct.isChecked())
								{
										if (ipAddress.getText() == null || ipAddress.getText().equals(""))
										{
												address = CrwNetworkUtils.toInetSocketAddress("127.0.0.1:" + boatPort);
										}
										else
										{
												address = CrwNetworkUtils.toInetSocketAddress(textIpAddress + ":" + boatPort);
										}
										currentBoat.setAddress(address); // actual call that establishes a connection
								}
								else if (reg.isChecked())
								{
										Log.i(logTag, "finding ip");
										FindIP();
								}
								try
								{
										saveSession(); //save ip address
								}
								catch (Exception e)
								{

								}
								dialog.dismiss();
								/*ASDF*/
								//networkThread = new NetworkAsync().execute(); //launch networking asnyc task
								//boatConnectionPoll(); // Launch boat connection check thread
								//latestWaypointPoll(); // Launch waypoint polling thread
								//alertsAndAlarms(); // Launch alerts and alarms thread
								SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
								SharedPreferences.Editor editor = sharedPref.edit();
								editor.putString(SettingsActivity.KEY_PREF_IP, currentBoat.getIpAddress().getAddress().toString());
								editor.apply();
								editor.commit();
						}
				});
				dialog.show();
		}

		public void FindIP()
		{
				Thread thread = new Thread()
				{
						public void run()
						{
								address = CrwNetworkUtils.toInetSocketAddress(textIpAddress + ":6077");
								currentBoat.returnServer().setRegistryService(address);
								currentBoat.returnServer().getVehicleServices(new FunctionObserver<Map<SocketAddress, String>>()
								{
										@Override
										public void completed(Map<SocketAddress, String> socketAddressStringMap)
										{
												Log.i(logTag, "Completed");
												for (Map.Entry<SocketAddress, String> entry : socketAddressStringMap.entrySet())
												{
														Log.i(logTag, entry.toString());
														currentBoat.returnServer().setVehicleService(entry.getKey());
												}
										}

										@Override
										public void failed(FunctionError functionError)
										{
												Log.i(logTag, "No Response");
										}
								});

						}
				};
				thread.start();
		}

		//  Make return button same as home button
		@Override
		public void onBackPressed()
		{
				Intent setIntent = new Intent(Intent.ACTION_MAIN);
				setIntent.addCategory(Intent.CATEGORY_HOME);
				setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(setIntent);
		}

		public void SaveWaypointsToFile() throws IOException
		{
				//nothing to save if no waypoints
				if (boatPath.getOriginalPoints().isEmpty() == true)
				{
						return;
				}
				savePointList = new ArrayList<LatLng>(boatPath.getOriginalPoints());

				final BufferedWriter writer;
				try
				{
						File waypointFile = new File(Environment.getExternalStorageDirectory() + "/waypoints/" + waypointFileName);
						writer = new BufferedWriter(new FileWriter(waypointFile, true));
				}
				catch (Exception e)
				{
						Log.e(logTag, "error saving path to fle");
						Log.e(logTag, e.toString());
						return;
				}

				final Dialog dialog = new Dialog(context);
				dialog.setContentView(R.layout.wpsavedialog);
				dialog.setTitle("Save Waypoint Set");
				final EditText input = (EditText) dialog.findViewById(R.id.newname);
				Button submit = (Button) dialog.findViewById(R.id.savebutton);
				submit.setOnClickListener(new OnClickListener()
				{
						@Override
						public void onClick(View v)
						{

								saveName = input.getText().toString();

								if (!(saveName.contains("\"")))
								{
										try
										{
												writer.append("\n\" " + input.getText() + " \"");
												writer.flush();
												for (ILatLng i : savePointList)
												{
														writer.append(" " + i.getLatitude() + " " + i.getLongitude());
														writer.flush();
												}

												writer.close();
										}
										catch (Exception e)
										{
										}
										dialog.dismiss();
								}
								else
								{
										final AlertDialog alertDialog = new AlertDialog.Builder(context).create();
										alertDialog.setTitle("No Quotation Marks in Title");
										alertDialog.show();
										alertDialog.setCancelable(true);
										alertDialog.setCanceledOnTouchOutside(true);
								}
						}
				});
				dialog.show();
		}

		public void LoadWaypointsFromFile(String filename) throws IOException
		{
				final File waypointFile = new File(Environment.getExternalStorageDirectory() + "/waypoints/" + filename);
				try
				{
						touchpointList.clear();
						if (boatPath != null) boatPath.clearPoints();
						invalidate();
				}
				catch (Exception e)
				{
						Log.e(logTag, "LoadWaypointsFromFile error...");
						Log.e(logTag, e.toString());
				}

				Scanner fileScanner = new Scanner(waypointFile); //Scans each line of the file
				final ArrayList<ArrayList<ILatLng>> waypointsaves = new ArrayList<ArrayList<ILatLng>>();
				final ArrayList<String> saveName = new ArrayList<>();
				/* scans each line of the file as a waypoint save
				 * then scans each line every two elements makes a latlng
		     * adds all saves to arraylist
		     * chose between arraylist later on
		     */

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
												public double getLatitude()
												{
														return templat;
												}

												@Override
												public double getLongitude()
												{
														return templon;
												}

												@Override
												public double getAltitude()
												{
														return 0;
												}
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
										currentselected = position;
								}
						});
						submitButton.setOnClickListener(new OnClickListener()
						{
								@Override
								public void onClick(View v)
								{
										if (currentselected == -1)
										{
												dialog.dismiss();
												//write no selected box
										}
										waypointList.clear();
										markerList.clear();

										int num = 1;
										for (ILatLng i : waypointsaves.get(currentselected)) //tbh not sure why there is a 1 offset but there is
										{
												markerList.add(mMapboxMap.addMarker(new MarkerOptions().position(new LatLng(i.getLatitude(), i.getLongitude())).title(Integer.toString(num))));
												waypointList.add(new LatLng(i.getLatitude(), i.getLongitude()));
												num++;
										}

										boatPath = new Path(waypointList); // also need to put things into boatPath
										remove_waypaths();
										add_waypaths();

										dialog.dismiss();
								}
						});
						dialog.show();
				}
		}

		public static double planarDistanceSq(Pose3D a, Pose3D b)
		{
				double dx = a.getX() - b.getX();
				double dy = a.getY() - b.getY();
				return dx * dx + dy * dy;
		}

		public void setHome()
		{
				final JSONObject Jhome = new JSONObject();
				final JSONObject JPhone = new JSONObject();
				final JSONObject JTablet = new JSONObject();
				if (home == null)
				{
						new AlertDialog.Builder(context)
										.setTitle("Set Home")
										.setMessage("Which position do you want to use?")
										.setPositiveButton("Phone", new DialogInterface.OnClickListener()
										{
												public void onClick(DialogInterface dialog, int which)
												{
														/*ASDF*/
														/*
														if (latlongloc != null)
														{
																home = currentBoat.getLocation();
																try
																{
																		JPhone.put("Lat", home.getLatitude());
																		JPhone.put("Lng", home.getLongitude());
																		Jhome.put("Phone", JPhone);
																		mlogger.info(new JSONObject()
																						.put("Time", sdf.format(d))
																						.put("Home", Jhome));
																}
																catch (JSONException e)
																{

																}
																MarkerOptions home_MO = new MarkerOptions()
																				.position(home)
																				.title("Home")
																				.icon(Ihome);
																home_M = mMapboxMap.addMarker(home_MO);
																mMapboxMap.moveCamera(CameraUpdateFactory.newLatLng(home));
														}
														else
														{
																Toast.makeText(getApplicationContext(), "Phone doesn't have GPS Signal", Toast.LENGTH_SHORT).show();
														}
														*/
												}
										})
										.setNegativeButton("Tablet", new DialogInterface.OnClickListener()
										{
												public void onClick(DialogInterface dialog, int which)
												{

														Location tempLocation = LocationServices.FusedLocationApi.getLastLocation();
														if (tempLocation == null)
														{
																Log.w(logTag, "TeleOpPanel.setHome(): tablet does not have a location. Cannot be used as home.");
																Toast.makeText(getApplicationContext(), "Tablet doesn't have a location", Toast.LENGTH_SHORT).show();
																return;
														}
														LatLng loc = new LatLng(tempLocation.getLatitude(), tempLocation.getLongitude());

														if (loc != null)
														{
																home = loc;
																MarkerOptions home_MO = new MarkerOptions()
																				.position(home)
																				.title("Home")
																				.icon(Ihome);
																home_M = mMapboxMap.addMarker(home_MO);
																mMapboxMap.moveCamera(CameraUpdateFactory.newLatLng(home));
																try
																{
																		JTablet.put("Lat", home.getLatitude());
																		JTablet.put("Lng", home.getLongitude());
																		Jhome.put("Tablet", JTablet);
																		mlogger.info(new JSONObject()
																						.put("Time", sdf.format(d))
																						.put("Home", Jhome));
																}
																catch (JSONException e)
																{
																}
														}
														else
														{
																Toast.makeText(getApplicationContext(), "Tablet doesn't have GPS Signal", Toast.LENGTH_SHORT).show();
														}
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

														try
														{
																Jhome.put("Lat", home.getLatitude());
																Jhome.put("Lng", home.getLongitude());
																mlogger.info(new JSONObject()
																				.put("Time", sdf.format(d))
																				.put("Removed home", Jhome));
														}
														catch (JSONException e)
														{
														}
														mMapboxMap.removeMarker(home_M);
														home = null;
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
				if (home == null)
				{
						Toast.makeText(getApplicationContext(), "Set home first!", Toast.LENGTH_LONG).show();
				}
				else
				{
						new AlertDialog.Builder(context)
										.setTitle("Go Home")
										.setMessage("Let the boat go home ?")
										.setPositiveButton("Yes", new DialogInterface.OnClickListener()
										{
												public void onClick(DialogInterface dialog, int which)
												{
														if (currentBoat.isConnected())
														{
																if (!currentBoat.isAutonomous())
																{
																		currentBoat.setAutonomous(true);
																}
																UtmPose homeUTM = convertLatLngUtm(home);
																currentBoat.addWaypoint(homeUTM.pose, homeUTM.origin);
																Log.i(logTag, "Go home");
														}
												}
										})
										.setNegativeButton("No", new DialogInterface.OnClickListener()
										{
												public void onClick(DialogInterface dialog, int which)
												{
														Log.i(logTag, "Nothing");
												}
										})
										.show();
				}
		}

		public void sendPID()
		{
				loadPreferences(); //update values should be replaced automatically with
				currentBoat.setPID(tPID, rPID);
				currentBoat.getPID();
		}

		public void saveMap()
		{
				if (mMapboxMap == null)
				{
						runOnUiThread(new Runnable()
						{
								@Override
								public void run()
								{
										Toast.makeText(getApplicationContext(), "Map not saved (make sure youre connected to internet for downloading maps", Toast.LENGTH_LONG).show();
								}
						});
						return;
				}

				Thread thread = new Thread()
				{
						@Override
						public void run()
						{
								networkConnection = hasActiveInternetConnection(context);
						}
				};
				thread.start();
				if (networkConnection == false)
				{
						runOnUiThread(new Runnable()
						{
								@Override
								public void run()
								{
										Toast.makeText(getApplicationContext(), "Please Connect to the Internet first", Toast.LENGTH_LONG).show();
								}
						});
						runOnUiThread(new Runnable()
						{
								@Override
								public void run()
								{
										mapInfo.setText("Map Information \n Nothing Pending");
								}
						});

						return;
				}


				runOnUiThread(new Runnable()
				{
						@Override
						public void run()
						{
								Toast.makeText(getApplicationContext(), "Please leave app open and connected to the internet until the completion dialog shows", Toast.LENGTH_LONG).show();
						}
				});


				// Set up the OfflineManager
				OfflineManager offlineManager = OfflineManager.getInstance(this);
				offlineManager.setAccessToken(getString(R.string.mapbox_access_token));

				// Create a bounding box for the offline region
				LatLngBounds latLngBounds = mMapboxMap.getProjection().getVisibleRegion().latLngBounds;

				final String JSON_CHARSET = "UTF-8";
				final String JSON_FIELD_REGION_NAME = "FIELD_REGION_NAME";

				OfflineTilePyramidRegionDefinition definition = new OfflineTilePyramidRegionDefinition(
								mv.getStyleUrl(), latLngBounds, 15, 16, this.getResources().getDisplayMetrics().density); //try 19

				// Set the metadata not sure but changing "area" to something longer might be causing crash
				byte[] metadata;
				try
				{
						JSONObject jsonObject = new JSONObject();
						jsonObject.put(JSON_FIELD_REGION_NAME, "Area");
						String json = jsonObject.toString();
						metadata = json.getBytes(JSON_CHARSET);
				}
				catch (Exception e)
				{
						Log.e(logTag, "Failed to encode metadata: " + e.getMessage());
						metadata = null;
				}

				//create region
				offlineManager.createOfflineRegion(definition, metadata, new OfflineManager.CreateOfflineRegionCallback()
				{
						@Override
						public void onCreate(OfflineRegion offlineRegion)
						{
								offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE);

								offlineRegion.setObserver(new OfflineRegion.OfflineRegionObserver()
								{
										@Override
										public void onStatusChanged(OfflineRegionStatus status)
										{
												double percentage = status.getRequiredResourceCount() >= 0 ?
																(100.0 * status.getCompletedResourceCount() / status.getRequiredResourceCount()) : 0.0;
												percentage = Math.round(percentage);
												if (status.isComplete())
												{
														mapInfo.setText("Map Information \n Map Downloaded");
														System.out.println("download complete");
														runOnUiThread(new Runnable()
														{
																@Override
																public void run()
																{
																		final AlertDialog alertDialog = new AlertDialog.Builder(context).create();
																		alertDialog.setTitle("Download Completed");
																		alertDialog.show();
																		alertDialog.setCancelable(true);
																		alertDialog.setCanceledOnTouchOutside(true);
																		alertDialog.setButton("Dismiss", new DialogInterface.OnClickListener()
																		{
																				public void onClick(DialogInterface dialog, int which)
																				{
																						alertDialog.dismiss();
																				}
																		});

																}
														});

												}
												else if (status.isRequiredResourceCountPrecise())
												{
														mapInfo.setText("Map Information \n " + percentage + "% Downloaded");
														//setPercentage((int) Math.round(percentage));
												}
										}

										@Override
										public void onError(OfflineRegionError error)
										{
												// If an error occurs, print to logcat
												Log.e(logTag, "onError reason: " + error.getReason());
												Log.e(logTag, "onError message: " + error.getMessage());
										}

										@Override
										public void mapboxTileCountLimitExceeded(long limit)
										{
												// Notify if offline region exceeds maximum tile count
												Log.e(logTag, "Mapbox tile count limit exceeded: " + limit);
										}
								});
						}

						@Override
						public void onError(String error)
						{
								Log.e(logTag, "Error: " + error);
						}
				});
		}

		public void latestWaypointPoll()
		{
				final Handler handler = new Handler();
				handler.post(new Runnable()
				{
						@Override
						public void run()
						{
								current_waypoint_index = currentBoat.getWaypointsIndex();

								if (last_waypoint_index != current_waypoint_index)
								{
										// need to update the line colors
										if (boatPath != null && boatPath.getPoints().size() > 0)
										{
												remove_waypaths();
												add_waypaths();
										}
								}
								last_waypoint_index = current_waypoint_index;

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
								synchronized (_batteryVoltageLock)
								{
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
												String message = String.format("Boat battery = %.2fV", battery_voltage);
												Log.i(logTag, message);
												if (battery_voltage == 0.0)
												{
														// initial value before connecting to boat
														handler.postDelayed(this, 10000);
														return;
												}
												if (battery_voltage < alert_voltage)
												{
														NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext())
																		.setSmallIcon(R.drawable.logo) //just some random icon placeholder
																		.setContentTitle("Boat battery warning")
																		.setContentText(message);
														if (battery_voltage < alarm_voltage)
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
						}
				});
		}

		/*ASDF*/
		/*
		public void updateMarkers()
		{
				final Handler handler = new Handler();

				final IconFactory mIconFactory = IconFactory.getInstance(context);
				Drawable userDraw = ContextCompat.getDrawable(context, R.drawable.userloc);
				Icon userIcon = mIconFactory.fromDrawable(userDraw);
				final Marker userloc = mMapboxMap.addMarker(new MarkerOptions().position(pHollowStartingPoint).title("Your Location").icon(userIcon));
				final MarkerView boat_markerview = mMapboxMap.addMarker(new MarkerViewOptions().position(pHollowStartingPoint).title("Boat")
								.icon(mIconFactory.fromResource(R.drawable.pointarrow)).rotation(0));


				Runnable markerRun = new Runnable()
				{
						@Override
						public void run()
						{
								if (currentBoat == null || currentBoat.getLocation() == null || mMapboxMap == null) return;

								boat_markerview.setPosition(currentBoat.getLocation());

								float degree = (float) (currentBoat.getYaw() * 180 / Math.PI);  // degree is -90 to 270
								degree = (degree < 0 ? 360 + degree : degree); // degree is 0 to 360
								boat_markerview.setRotation(degree);

								location = LocationServices.FusedLocationApi.getLastLocation();
								if (location != null)
								{ //occurs when gps is off or no lock
										userloc.setPosition(new LatLng(location.getLatitude(), location.getLongitude()));
								}
								handler.postDelayed(this, 200);
						}
				};
				handler.post(markerRun);
		}
		*/

		public void startWaypoints()
		{
				Log.i(logTag, "startWaypoints() called...");
				if (boatPath == null)
				{
						Log.e(logTag, "TeleOpPanel.startWaypoints():  boatPath is null");
						return;
				}
				if (currentBoat == null)
				{
						Log.e(logTag, "TeleOpPanel.startWaypoints(): currentBoat is null");
						return;
				}
				if (currentBoat.isConnected())
				{
						if (boatPath.getPoints() == null)
						{
								Log.e(logTag, "TeleOpPanel.startWaypoints():  boatPath.getPoints() is null");
								return;
						}
						waypointList = boatPath.getPoints();
						if (waypointList.size() > 0)
						{
								//Convert all UTM to latlong
								UtmPose tempUtm = convertLatLngUtm(waypointList.get(waypointList.size() - 1));
								waypointStatus = tempUtm.toString();

								wpPose = new UtmPose[waypointList.size()];
								synchronized (_waypointLock)
								{
										for (int i = 0; i < waypointList.size(); i++)
										{
												wpPose[i] = convertLatLngUtm(waypointList.get(i));
												allWaypointsSent.add(wpPose[i]);
										}
								}
								/*ASDF*/
								currentBoat.setAutonomous(true);
								currentBoat.isAutonomous();
								currentBoat.startWaypoints(wpPose, "POINT_AND_SHOOT");
								current_waypoint_index = 0;
								invalidate();
						}
						else
						{
								runOnUiThread(new Runnable()
								{
										@Override
										public void run()
										{
												Log.i(logTag, "wp list size: " + waypointList.size());
												Toast.makeText(getApplicationContext(), "Please Select Waypoints", Toast.LENGTH_LONG).show();
										}
								});
						}
				}
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

		public void onLoadWaypointLayout()
		{
				LayoutInflater inflater = (LayoutInflater) getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				waypointlayout = (LinearLayout) findViewById(R.id.relativeLayout_sensor);
				waypointregion = inflater.inflate(R.layout.waypoint_layout, waypointlayout);
				createWaypointStatusButton = (ImageButton) waypointregion.findViewById(R.id.waypointButton);
				createWaypointStatusButton.setBackgroundResource(R.drawable.draw_icon);
				deleteWaypoint = (Button) waypointregion.findViewById(R.id.waypointDeleteButton);
				pauseWPButton = (ToggleButton) waypointregion.findViewById(R.id.pause);
				startWaypoints = (Button) waypointregion.findViewById(R.id.waypointStartButton);

				speed_spinner_erroneous_call = true; // reset the erroneous call boolean
				speed_spinner = (Spinner) waypointregion.findViewById(R.id.speed_spinner);
				set_speed_spinner_from_pref();

				waypointInfo = (TextView) waypointregion.findViewById(R.id.waypoints_waypointstatus);

				Button dropWP = (Button) waypointregion.findViewById(R.id.waypointDropWaypointButton);

				dropWP.setOnClickListener(new OnClickListener()
				{
						@Override
						public void onClick(View view)
						{
								runOnUiThread(new Runnable()
								{
										@Override
										public void run()
										{
												if (currentBoat == null)
												{
														Toast.makeText(getApplicationContext(), "No Boat Connected", Toast.LENGTH_LONG).show();
														return;
												}
												if (currentBoat.getLocation() == null)
												{
														Toast.makeText(getApplicationContext(), "Waiting on boat GPS", Toast.LENGTH_LONG).show();
														return;
												}
												if (mMapboxMap == null)
												{
														Toast.makeText(getApplicationContext(), "Map still loading", Toast.LENGTH_LONG).show();
														return;
												}
										}
								});
								if (currentBoat != null && currentBoat.getLocation() != null && mMapboxMap != null)
								{
										touchpointList.add(currentBoat.getLocation());
										System.out.println(touchpointList.size());
										boatPath = new Path(touchpointList);
										invalidate();
								}
						}
				});

				createWaypointStatusButton.setOnClickListener(new OnClickListener()
				{
						@Override
						public void onClick(View view)
						{
								if (!startDrawWaypoints)
								{
										createWaypointStatusButton.setBackgroundResource(R.drawable.draw_icon2);
								}
								else
								{
										createWaypointStatusButton.setBackgroundResource(R.drawable.draw_icon);
								}
								startDrawWaypoints = !startDrawWaypoints;

								Thread thread = new Thread()
								{
										public void run()
										{
												if (createWaypointStatusButton.isActivated())
												{
														runOnUiThread(new Runnable()
														{
																@Override
																public void run()
																{
																		Toast.makeText(getApplicationContext(), "Use long press to add waypoints", Toast.LENGTH_LONG).show();
																}
														});
														startDrawRegions = false;
														if (waypointLayoutEnabled == false)
														{
																createVertexStatusButton.setClickable(false);
														}

												}
												else
												{
														if (waypointLayoutEnabled == false)
														{
																createVertexStatusButton.setClickable(true);
														}
												}
										}
								};
								thread.start();
						}
				});

				pauseWPButton.setOnClickListener(new OnClickListener()
				{
						@Override
						public void onClick(View v)
						{
								// If pauseWPButton.isChecked is false that means boat is running
								// If pauseWPButton.isChecked is true that means boat is paused
								if (pauseWPButton.isChecked())
								{
										/*ASDF*/
										currentBoat.setAutonomous(false);
								}
								else
								{
										currentBoat.setAutonomous(true);
								}
						}
				});
				deleteWaypoint.setOnClickListener(new View.OnClickListener()
				{
						public void onClick(View v)
						{
								if (containsRegion == true)
								{
										Toast.makeText(getApplicationContext(), "Please Delete Region in Region Menu", Toast.LENGTH_LONG).show();
										return;
								}

								pauseWPButton.setChecked(false);
								stopWaypoints = true;

								/*ASDF
								Thread thread = new Thread()
								{
										public void run()
										{
												currentBoat.setAutonomous(false);
												isAutonomous = currentBoat.isAutonomous();
												currentBoat.stopWaypoints();
										}
								};
								thread.start();
								*/
								currentBoat.setAutonomous(false);
								currentBoat.isAutonomous();
								currentBoat.stopWaypoints();

								boatPath = new Path();
								touchpointList.clear();
								invalidate();
						}
				});

				startWaypoints.setOnClickListener(new OnClickListener()
				{
						@Override
						public void onClick(View view)
						{
								if (pauseWPButton.isChecked())
								{
										AlertDialog.Builder builder = new AlertDialog.Builder(context);
										builder.setMessage("Currently waypoints are paused, by pressing start waypoints the boat will restart the path. \n If you want to resume waypoints press cancel then toggle resume ")
														.setCancelable(false)
														.setPositiveButton("Restart Path", new DialogInterface.OnClickListener()
														{
																public void onClick(DialogInterface dialog, int id)
																{
																		pauseWPButton.setChecked(false);
																		startWaypoints();
																}
														})
														.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
														{
																public void onClick(DialogInterface dialog, int id)
																{
																		return;
																}
														});
										AlertDialog alert = builder.create();
										alert.show();
								}
								else
								{
										startWaypoints();
								}
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
		}

		public void onLoadRegionLayout()
		{
				LayoutInflater inflater = (LayoutInflater) getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				regionlayout = (LinearLayout) findViewById(R.id.relativeLayout_sensor);
				waypointregion = inflater.inflate(R.layout.region_layout, regionlayout);
				startRegion = (Button) regionlayout.findViewById(R.id.region_start); //start button
				createVertexStatusButton = (ImageButton) regionlayout.findViewById(R.id.region_draw); //toggle adding points to region
				createVertexStatusButton.setBackgroundResource(R.drawable.draw_icon);
				perimeter = (Button) regionlayout.findViewById(R.id.region_perimeter); //perimeter* start perimeter? didnt write this
				clearRegion = (Button) regionlayout.findViewById(R.id.region_clear); //region, not implemented yet
				Button stopButton = (Button) regionlayout.findViewById(R.id.stopButton);
				transectDistance = (EditText) regionlayout.findViewById(R.id.region_transect);
				spirallawn = (ToggleButton) regionlayout.findViewById(R.id.region_spiralorlawn);
				updateTransect = (Button) regionlayout.findViewById(R.id.region_transectButton);

				spirallawn.setOnClickListener(new OnClickListener()
				{
						@Override
						public void onClick(View v)
						{
								if (!spirallawn.isChecked())
								{
										ArrayList<LatLng> temp = new ArrayList<LatLng>(touchpointList);
										boatPath = new Region(temp, AreaType.SPIRAL, currentTransectDist);
								}
								else
								{
										ArrayList<LatLng> temp = new ArrayList<LatLng>(touchpointList);
										boatPath = new Region(temp, AreaType.LAWNMOWER, currentTransectDist);
								}
								invalidate();
						}
				});

				updateTransect.setOnClickListener(new OnClickListener()
				{
						@Override
						public void onClick(View view)
						{
								if (currentBoat == null)
								{
										return;
								}

								currentTransectDist = Double.parseDouble(transectDistance.getText().toString());
								boatPath.updateTransect(currentTransectDist);

								//boatPath = new Region(touchpointList,Are);
								invalidate();
						}
				});

				perimeter.setOnClickListener(new OnClickListener()
				{
						@Override
						public void onClick(View v)
						{
								//addPointToRegion(currentBoat.getLocation());
						}
				});


				stopButton.setOnClickListener(new OnClickListener()
				{
						@Override
						public void onClick(View view)
						{
								if (currentBoat != null)
								{
										currentBoat.setAutonomous(false);
										currentBoat.isAutonomous();
										currentBoat.stopWaypoints();
								}
						}
				});
				clearRegion.setOnClickListener(new OnClickListener()
				{
						@Override
						public void onClick(View view)
						{
								try
								{ //in case they try to remove before something gets set
										if (touchpointList.size() == 0)
										{
												return;
										}
										touchpointList.clear();
										if (boatPath.getAreaType() == AreaType.LAWNMOWER)
										{
												boatPath = new Region(touchpointList, AreaType.LAWNMOWER);
										}
										else
										{
												boatPath = new Region(touchpointList, AreaType.SPIRAL);
										}
										invalidate();
								}
								catch (Exception e)
								{
								}
						}
				});
				startRegion.setOnClickListener(new OnClickListener()
				{
						@Override
						public void onClick(View view)
						{
								startWaypoints();
						}
				});
				createVertexStatusButton.setOnClickListener(new OnClickListener()
				{
						@Override
						public void onClick(View v)
						{
								if (startDrawRegions == false)
								{
										createVertexStatusButton.setBackgroundResource(R.drawable.draw_icon2);
								}
								else
								{
										createVertexStatusButton.setBackgroundResource(R.drawable.draw_icon);
								}
								startDrawRegions = !startDrawRegions;
						}
				});
		}

		private void remove_waypaths()
		{
				synchronized (_wpGraphicsLock)
				{
						for (Polyline p : Waypath_outline)
						{
								mMapboxMap.removeAnnotation(p);
								p.remove();
						}
						for (Polyline p : Waypath_top)
						{
								mMapboxMap.removeAnnotation(p);
								p.remove();
						}
						// TODO: make a boat to waypoint line
						//mMapboxMap.removeAnnotation(boat_to_waypoint_line);
						//boat_to_waypoint_line.remove();
				}
		}

		private void add_waypaths()
		{
				synchronized (_wpGraphicsLock)
				{
						if (boatPath == null) return;
						ArrayList<ArrayList<LatLng>> point_pairs = boatPath.getPointPairs();
						for (int i = 0; i < point_pairs.size(); i++)
						{
								ArrayList<LatLng> pair = point_pairs.get(i);
								Waypath_outline.add(mMapboxMap.addPolyline(new PolylineOptions().addAll(pair).color(Color.BLACK).width(8)));
								// i = 0 is waypoints (0, 1) --> should be white until current wp index = 2
								// i = 1 is waypoints (1, 2) --> should be white until current wp index = 3
								// ...
								if (current_waypoint_index > i + 1)
								{
										Waypath_top.add(mMapboxMap.addPolyline(new PolylineOptions().addAll(pair).color(Color.GRAY).width(5)));
										Log.d(logTag, String.format("line i = %d, current_waypoint = %d, color = GRAY", i, current_waypoint_index));
								}
								else
								{
										Waypath_top.add(mMapboxMap.addPolyline(new PolylineOptions().addAll(pair).color(Color.WHITE).width(5)));
										Log.d(logTag, String.format("line i = %d, current_waypoint = %d, color = WHITE", i, current_waypoint_index));
								}
						}
				}
		}

		public void invalidate()
		{
				if (Waypath_outline.size() > 0)
				{
						remove_waypaths();
				}
				if (markerList != null)
				{
						mMapboxMap.removeAnnotations(markerList);
						markerList.clear();
				}
				IconFactory mIconFactory = IconFactory.getInstance(this);
				Drawable mboundry = ContextCompat.getDrawable(this, R.drawable.boundary);
				final Icon Iboundry = mIconFactory.fromDrawable(mboundry);

				if (boatPath != null)
				{
						boatPath.updateRegionPoints();
				}
				else
				{
						Log.w(logTag, "TeleOpPanel invalidate(): boatPath is null");
						return;
				}

				if (touchpointList.size() == 0 && Waypath_outline.size() > 0)
				{
						remove_waypaths();
				}
				if (boatPath != null && boatPath.getPoints().size() > 0)
				{
						add_waypaths();
				}

				if (boatPath instanceof Region)
				{
						for (LatLng i : boatPath.getQuickHullList())
						{
								markerList.add(mMapboxMap.addMarker(new MarkerOptions().position(i).title(Integer.toString(markerList.size())).icon(Iboundry)));
						}
				}
				else if (boatPath instanceof Path)
				{
						for (LatLng i : touchpointList)
						{
								markerList.add(mMapboxMap.addMarker(new MarkerOptions().position(i).title(Integer.toString(markerList.size()))));
						}

				}
		}

		public void saveSession() throws IOException
		{
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
								currentselected = position; //Use a different variable, this is used by the list adapter in loading waypoints ..
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
										waypointFileName = listOfFiles[currentselected].getName();
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

		public void loadPreferences()
		{
				final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

				String iP = sharedPref.getString(SettingsActivity.KEY_PREF_IP, "192.168.1.1");
				String port = sharedPref.getString(SettingsActivity.KEY_PREF_PORT, "11411");

				textIpAddress = iP;
				textIpAddress = textIpAddress.replace("/", ""); //network on main thread error if this doesnt happen
				boatPort = port;
				ipAddressBox.setText("IP Address: " + textIpAddress);

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

				updateRateMili = Integer.parseInt(sharedPref.getString(SettingsActivity.KEY_PREF_COMMAND_RATE, "500"));
				Double initialPanLat = Double.parseDouble(sharedPref.getString(SettingsActivity.KEY_PREF_LAT, "0"));
				Double initialPanLon = Double.parseDouble(sharedPref.getString(SettingsActivity.KEY_PREF_LON, "0"));
				initialPan = new LatLng(initialPanLat, initialPanLon);
				setInitialPan = sharedPref.getBoolean(SettingsActivity.KEY_PREF_SAVE_MAP, true);
		}
}
