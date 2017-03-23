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
import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;

import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.graphics.Matrix;

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
import robotutils.Pose3D;
import android.app.Activity;
import android.content.Context;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.CheckBox;
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

import com.platypus.crw.FunctionObserver;
import com.platypus.crw.PoseListener;
import com.platypus.crw.VehicleServer.WaypointState;
import com.platypus.crw.WaypointListener;
import com.platypus.crw.data.Twist;
import com.platypus.crw.data.Utm;
import com.platypus.crw.data.UtmPose;

import android.app.Dialog;

import android.view.View.OnClickListener;
import com.platypus.android.tablet.Joystick.*;


/*
  TODO somewhere invalidate is not getting called.
  This is being caused by the points not being recalculated after adding a point
*/

//TODO somewhere an extra point is getting added to the list or doesn't remove a point when calculating quickhull
public class TeleOpPanel extends Activity implements SensorEventListener {
  final Context context = this;
  final double GPSDIST = 0.0000449;
  TextView ipAddressBox = null;
  TextView mapInfo = null;
  RelativeLayout linlay = null;
  AsyncTask networkThread;

  ToggleButton pauseWP = null;
  ToggleButton spirallawn;

  ImageButton drawPoly = null;
  ImageButton waypointButton = null;

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
  //Switch speed = null;
  private boolean speed_spinner_erroneous_call = true;
  Spinner speed_spinner = null;

  int updateRateMili = 50;
  boolean checktest;
  boolean waypointLayoutEnabled = true; //if false were on region layout
  boolean containsRegion = false;

  double currentTransectDist = 5;
  double xValue;
  double yValue;
  double zValue;
  LatLong latlongloc;

  MapView mv;
  MapboxMap mMapboxMap;
  String zone;
  String rotation;

  //Marker boat;
  Marker home_M;
  Location location;

  int currentselected = -1; //which element selected
  String saveName; //shouldnt be here?
  LatLng pHollowStartingPoint = new LatLng((float) 40.436871,
                                           (float) -79.948825);
  LatLng initialPan = new LatLng(0,0);
    boolean setInitialPan = true;
  long lastTime = -1;
  String waypointStatus = "";
  double rudderTemp = 0;
  double thrustTemp = 0;
  double old_rudder = 0;
  double old_thrust = 0;
  double rot;
  String boatwaypoint;
  Twist twist = new Twist();
  boolean networkConnection = true;
  ScheduledFuture future;
  ScheduledThreadPoolExecutor exec;
  Runnable networkRun;

  boolean isAutonomous;
  boolean isCurrentWaypointDone = true;
  boolean isWaypointsRunning = false;

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
  public CheckBox autoBox;
  private final Object _waypointLock = new Object();

  public double[] data;
  SensorData Data;
  public String sensorV = "Loading...";
  boolean sensorReady = false;
  public static TextView log;
  public boolean Auto = false;

  private PoseListener pl;
  private SensorListener sl;
  private WaypointListener wl;
  private boolean startDraw = false;
  private boolean startDrawWaypoints = false;

  double[] tPID = {.2, .0, .0};

  double[] rPID = {1, 0, .2};

  double battery_voltage = 0.0;

  private UtmPose _pose;
  private UtmPose[] wpPose = null, tempPose = null;
  private int N_waypoint = 0;
  private boolean waypointlistener = false;

  Icon Ihome;

  Path boatPath = null;
  ArrayList<LatLng> touchpointList = new ArrayList<LatLng>();
  ArrayList<LatLng> waypointList = new ArrayList<LatLng>();
  ArrayList<LatLng> savePointList = new ArrayList<LatLng>();
  ArrayList<Marker> markerList = new ArrayList();

  String waypointFileName = "waypoints.txt";

  ArrayList<UtmPose> allWaypointsSent = new ArrayList<UtmPose>();

    /*ASDF*/
  //private Polyline Waypath_outline_layer;
  //private Polyline Waypath_top_layer;
    ArrayList<Polyline> Waypath_outline = new ArrayList<>();
    ArrayList<Polyline> Waypath_top = new ArrayList<>();

  boolean isFirstWaypointCompleted = false;
  public static final String PREF_NAME = "DataFile";
  private TabletLogger mlogger;

  LatLng home = null;
  Date d = new Date();
  SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");

  private static final String logTag = TeleOpPanel.class.getName();
  String sensorLogTag = "Sensor";
  String waypointLogTag = "Sensor";

  NotificationManager notificationManager;
  Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
  long sleep_start_time = 0;
  boolean alarm_on = false;
  final Object _batteryVoltageLock = new Object();
  Ringtone alarm_ringtone;
  Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);


  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.setContentView(R.layout.tabletlayoutswitch);

    ipAddressBox = (TextView) this.findViewById(R.id.printIpAddress);
    linlay = (RelativeLayout) this.findViewById(R.id.linlay);

    connectButton = (Button) this.findViewById(R.id.connectButton);
    log = (TextView) this.findViewById(R.id.log);
    autoBox = (CheckBox) this.findViewById(R.id.autonomousBox);
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
//    File waypointDir = new File(getFilesDir() + "/waypoints"); //FOLDER CALLED WAYPOINTS
    if (!waypointDir.exists())
    {
        waypointDir.mkdir();
    }

      //load inital waypoint menu
    onLoadWaypointLayout();
    switchView.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View view1) {
          if (switchView.isChecked()) {
            waypointlayout.removeAllViews();
            onLoadRegionLayout();
            waypointLayoutEnabled = false;
            startDraw = startDrawWaypoints;
            startDrawWaypoints = false;

            if (!startDrawWaypoints) {
              waypointButton.setBackgroundResource(R.drawable.draw_icon2);
            }
            else {
              waypointButton.setBackgroundResource(R.drawable.draw_icon);
            }

            if (boatPath == null) {
              boatPath = new Path();
            }
            ArrayList<LatLng> temp = new ArrayList<LatLng>(touchpointList);
            if (spirallawn.isChecked()) {
              boatPath = new Region(temp, AreaType.LAWNMOWER, currentTransectDist);
              System.out.println("switched mode to lawnmower");
            } else {

              boatPath = new Region(temp, AreaType.SPIRAL, currentTransectDist);
              System.out.println("switched mode to spiral");
            }


          }
          else {
            regionlayout.removeAllViews();
            onLoadWaypointLayout();
            waypointLayoutEnabled = true;
            startDrawWaypoints = startDraw;
            startDraw = false;

            if (!startDraw) {
              drawPoly.setBackgroundResource(R.drawable.draw_icon2);
            } else {
              drawPoly.setBackgroundResource(R.drawable.draw_icon);
            }


            if (boatPath == null) {
              boatPath = new Path();
            }
            ArrayList<LatLng> temp = new ArrayList<LatLng>(touchpointList);
            boatPath = new Path(temp);

          }
          invalidate();
        }
      });

    if (mlogger != null) {
      mlogger.close();
    }
    mlogger = new TabletLogger();


    centerToBoat.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View view) {
          if (mMapboxMap == null) {
            Toast.makeText(getApplicationContext(), "Please wait for the map to load", Toast.LENGTH_LONG).show();
            return;
          }
          if (currentBoat == null) {
            Toast.makeText(getApplicationContext(), "Please Connect to a boat first", Toast.LENGTH_LONG).show();
            return;
          }
          if (currentBoat.getLocation() == null) {
            Toast.makeText(getApplicationContext(), "Boat still finding GPS location", Toast.LENGTH_LONG).show();
            return;
          }
            System.out.println("center to boat: " + currentBoat.getLocation());
          mMapboxMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                                                                      new CameraPosition.Builder()
                                                                      .target(currentBoat.getLocation())
                                                                      .zoom(16)
                                                                      .build()
                                                                      ));
        }
      });
    //Options menu
    advancedOptions.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
          PopupMenu popup = new PopupMenu(TeleOpPanel.this, advancedOptions);
          popup.getMenuInflater().inflate(R.menu.dropdownmenu, popup.getMenu());
          popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
              public boolean onMenuItemClick(MenuItem item) {
                switch (item.toString()) {
                    case "Save Map": {
                        Thread thread = new Thread() {
                            @Override
                            public void run() {
                                saveMap();
                            }
                        };
                        thread.start();
                        break;
                    }
                    case "Satellite Map": {
                        if (mMapboxMap != null) {
                            mMapboxMap.setStyle(Style.SATELLITE);
                        }
                        break;
                    }
                    case "Vector Map": {
                        if (mMapboxMap != null) {
                            mMapboxMap.setStyle(Style.MAPBOX_STREETS);
                        }
                        break;
                    }
                    case "Set Home": {
                        setHome();
                        break;
                    }
                    case "Go Home": {
                        goHome();
                        break;
                    }
                    case "Send PIDs": {
                        sendPID();
                        break;
                    }
                    case "Save Waypoints": {
                        {
                            try {
                                SaveWaypointsToFile();
                            } catch (Exception e) {
                                System.out.println("failed to save waypoints from file");
                            }
                            break;
                        }
                    }
                    case "Load Waypoints": {
                        try {
                            LoadWaypointsFromFile(waypointFileName);
                        } catch (Exception e) {
                            Log.e(logTag, "failed to load WP file");
                            Log.e(logTag, e.toString());
                        }
                        break;
                    }
                    case "Load Waypoint File":
                    {
                        try {
                            loadWayointFiles();
                        }
                        catch(Exception e)
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
      pl = new PoseListener() { //gets the location of the boat
        public void receivedPose(UtmPose upwcs) {

            _pose = upwcs.clone();
            {
                xValue = _pose.pose.getX();
                yValue = _pose.pose.getY();
            zValue = _pose.pose.getZ();
            rotation = String.valueOf(Math.PI / 2
                                      - _pose.pose.getRotation().toYaw());
            rot = Math.PI / 2 - _pose.pose.getRotation().toYaw();

            zone = String.valueOf(_pose.origin.zone);

            latlongloc = UTM.utmToLatLong(UTM.valueOf(
                                                      _pose.origin.zone, 'T', _pose.pose.getX(),
                                                      _pose.pose.getY(), SI.METER),
                                          ReferenceEllipsoid.WGS84);
            // Log.i(logTag, "Pose listener called");
            //Log.i(logTag, "rot:" + rot);
          }
        }
      };

    //*******************************************************************************
    //  Initialize Sensorlistener
    //*******************************************************************************
    sl = new SensorListener() {
        @Override
        public void receivedSensor(SensorData sensorData) {
          Data = sensorData;
          //Log.e(logTag, "sensorListener: " + sensorData.toString());
          sensorV = Arrays.toString(Data.data);
          sensorV = sensorV.substring(1, sensorV.length() - 1);
          sensorReady = true;
        }
      };

    //*******************************************************************************
    //  Initialize Waypointlistener
    //*******************************************************************************

    wl = new WaypointListener() {
        @Override
        public void waypointUpdate(WaypointState waypointState) {
          boatwaypoint = waypointState.toString();
        }
      };

    //****************************************************************************
    //  Initialize the Boat
    // ****************************************************************************
    currentBoat = new Boat(pl, sl);

    senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    senAccelerometer = senSensorManager
      .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    senSensorManager.registerListener(this, senAccelerometer,
                                      SensorManager.SENSOR_DELAY_NORMAL);

    final IconFactory mIconFactory = IconFactory.getInstance(this);
    Drawable mhome = ContextCompat.getDrawable(this, R.drawable.home1);
    Ihome = mIconFactory.fromDrawable(mhome);

    mv = (MapView) findViewById(R.id.mapview);
    //mv.setAccessToken(ApiAccess.getToken(this));

    MapboxAccountManager.start(this,getString(R.string.mapbox_access_token));
    mv.onCreate(savedInstanceState);
    mv.getMapAsync(new OnMapReadyCallback()
    {
        @Override
        public void onMapReady(@NonNull MapboxMap mapboxMap)
        {
          System.out.println("mapboxmap ready");
          mMapboxMap = mapboxMap;
          if (setInitialPan == true && initialPan.getLatitude()!=0 || initialPan.getLongitude() != 0)
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
                final int index = markerList.indexOf(marker);
                return false;
              }
          });
          mMapboxMap.setOnMapLongClickListener(new MapboxMap.OnMapLongClickListener()
          {
              @Override
              public void onMapLongClick(LatLng point)
              {

                System.out.println("start draw waypoints: " + startDrawWaypoints);
                System.out.println("start draw region: " + startDraw);
                if (startDrawWaypoints == true && startDraw == false) {
                  touchpointList.add(point);
                  System.out.println(touchpointList.size());
                  boatPath = new Path(touchpointList);
                } else if (startDraw && !startDrawWaypoints) {
                    //System.out.println("tp list before add point: " + touchpointList.size());
                  touchpointList.add(point);
                  //System.out.println("tp list after add point: " + touchpointList.size());
                  if (spirallawn.isChecked()) {
                    ArrayList<LatLng> temp = new ArrayList<LatLng>(touchpointList);
                    boatPath = new Region(temp, AreaType.LAWNMOWER, currentTransectDist);
                    touchpointList = boatPath.getQuickHullList();
                  } else {
                    ArrayList<LatLng> temp = new ArrayList<LatLng>(touchpointList);
                    boatPath = new Region(temp, AreaType.SPIRAL, currentTransectDist);
                    touchpointList = boatPath.getQuickHullList();
                  }
                }
                  //System.out.println("tp list after quickhull: " + touchpointList.size());
                invalidate();
              }
          });

          Drawable userDraw = ContextCompat.getDrawable(context, R.drawable.userloc);
          Icon userIcon  = mIconFactory.fromDrawable(userDraw);
          updateMarkers(); //Launch update markers thread
          alertsAndAlarms(); // Launch alerts and alarms thread

        }
    });

    connectButton.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View view) {

          new AlertDialog.Builder(context)
            .setTitle("Connect")
            .setMessage("You are already connected,\n do you want to reconnect?")
            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                  currentBoat = new Boat(pl, sl);
                  currentBoat.returnServer().addWaypointListener(wl, new FunctionObserver<Void>() {
                      @Override
                        public void completed(Void aVoid) {

                      }

                      @Override
                        public void failed(FunctionError functionError) {

                      }
                    });
                  connectBox();
                  Log.i(logTag, "Reconnect");
                }
              })
            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                  Log.i(logTag, "Nothing");
                }
              })
            .show();
        }
    });
    connectBox();
  }
  @Override
  protected void onStart() {
    super.onStart();
    //mv.onStart();
  }

  @Override
  public void onResume() {
    super.onResume();
    mv.onResume();
  }

  @Override
  public void onPause() {
    super.onPause();
    mv.onPause();
  }

  @Override
  protected void onStop() {
    super.onStop();
    //mv.onStop();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mv.onDestroy();

      SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
      SharedPreferences.Editor editor = sharedPref.edit();
      if (currentBoat == null || currentBoat.getIpAddress() == null) {
          editor.putString(SettingsActivity.KEY_PREF_IP, "192.168.1.1");
      }

      else
      {
          editor.putString(SettingsActivity.KEY_PREF_IP, currentBoat.getIpAddress().getAddress().toString());
      }
      if (currentBoat.getLocation() == null)
      {
          editor.putString(SettingsActivity.KEY_PREF_LAT,Double.toString(pHollowStartingPoint.getLatitude()));
          editor.putString(SettingsActivity.KEY_PREF_LON,Double.toString(pHollowStartingPoint.getLongitude()));
      }
      else
      {
          editor.putString(SettingsActivity.KEY_PREF_LAT,Double.toString(currentBoat.getLocation().getLatitude()));
          editor.putString(SettingsActivity.KEY_PREF_LON,Double.toString(currentBoat.getLocation().getLongitude()));
      }

      editor.apply();
      editor.commit();
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    mv.onLowMemory();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mv.onSaveInstanceState(outState);
  }


  // This method checks the wifi connection but not Internet access
  public static boolean isNetworkAvailable(final Context context) {
    final ConnectivityManager connectivityManager = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
    return connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnectedOrConnecting();
  }

  // This method need to run in another thread except UI thread(main thread)
  public static boolean hasActiveInternetConnection(Context context) {

    if (isNetworkAvailable(context)) {
      try {
        HttpURLConnection urlc = (HttpURLConnection) (new URL("http://www.google.com").openConnection());
        urlc.setRequestProperty("User-Agent", "Test");
        urlc.setRequestProperty("Connection", "close");
        urlc.setConnectTimeout(1500);
        urlc.connect();
        return (urlc.getResponseCode() == 200);
      } catch (IOException e) {
        Log.e(logTag, "Error checking internet connection", e);
      }
    } else {
      Log.d(logTag, "No network available!");
    }
    return false;
  }

  // *******************************
  //  JoystickView listener
  // *******************************

  private JoystickMovedListener _listener = new JoystickMovedListener() {
      @Override
      public void OnMoved(int x, int y) {
          Log.d(logTag, String.format("joystick (x, y) = %d, %d", x, y));
        thrustTemp = fromProgressToRange(y, THRUST_MIN, THRUST_MAX);
        rudderTemp = fromProgressToRange(x, RUDDER_MIN, RUDDER_MAX);
      }

      @Override
      public void OnReleased() {
        //System.out.println("released");
      }

      @Override
      public void OnReturnedToCenter() {
        //System.out.println("returned to center");
        thrustTemp = 0;
        rudderTemp = 0;
        if (currentBoat != null) {
          Thread thread = new Thread() {
              public void run() {
                updateVelocity(currentBoat, new FunctionObserver<Void>() {
                    @Override
                      public void completed(Void aVoid) {
                    }

                    @Override
                      public void failed(FunctionError functionError) {
                    }
                  });
              }
            };
          thread.start();
        }
      }
    };

  public void dialogClose()
  {
    //if (getBoatType() == true)
    {

      networkThread = new NetworkAsync().execute(); //launch networking asnyc task

    }
  }


  public static boolean validIP(String ip) {
    if (ip == null || ip == "")
      return false;
    ip = ip.trim();
    if ((ip.length() < 6) & (ip.length() > 15))
      return false;

    try {
      Pattern pattern = Pattern
        .compile("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
      Matcher matcher = pattern.matcher(ip);
      return matcher.matches();
    } catch (PatternSyntaxException ex) {
      return false;
    }
  }

  /* Function observer is passed as parameter since it is needed when
   * the joystick is being moved but not when it is released  */
  public void updateVelocity(Boat a, FunctionObserver<Void> fobs) { //taken right from desktop client for updating velocity
    if (a.returnServer() != null) {

      twist.dx(thrustTemp);
      twist.drz(-1.0*rudderTemp); // left-right is backwards

      a.returnServer().setVelocity(twist, fobs);
    }
  }

  /*
   * Rotate the bitmap
   */
  public static Bitmap RotateBitmap(Bitmap source, float angle) {
    Matrix matrix = new Matrix();
    matrix.postRotate(angle);
    return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
  }

  /*
   * this async task handles all of the networking on the boat since networking has to be done on
   * a different thread and since gui updates have to be updated on the main thread ....
   */

  private class NetworkAsync extends AsyncTask<String, Integer, String> {
    boolean connected = false;

    BitmapFactory.Options options = new BitmapFactory.Options();

    public void setOptions(BitmapFactory.Options options) {
      this.options = options;
      this.options.inDither = false;
      this.options.inTempStorage = new byte[18 * 23];
    }

    @Override
      protected void onPreExecute() {

      setOptions(options);

    }

    @Override
      protected String doInBackground(String... arg0) {

      currentBoat.isConnected();
      networkRun = new Runnable() {
          @Override
          public void run() {
            if (currentBoat != null) {
              connected = currentBoat.getConnected();

              //if (currentBoat.getConnected() == true)
              {
                if (old_thrust != thrustTemp || old_rudder!=rudderTemp) {
                    updateVelocity(currentBoat, new FunctionObserver<Void>() {
                        @Override
                          public void completed(Void aVoid) {
                          }
                        @Override
                          public void failed(FunctionError functionError) {
//													System.out.println("ending update velocity function observer: " + System.currentTimeMillis());
                        }
                      });
                }
              }
              if (stopWaypoints == true) {
                currentBoat.returnServer().stopWaypoints(new FunctionObserver<Void>() {
                    @Override
                      public void completed(Void aVoid) {
                      Log.i(waypointLogTag,"Waypoints stopped");
                    }

                    @Override
                      public void failed(FunctionError functionError) {
                      Log.i(waypointLogTag,"Failed to stop waypoints");

                    }
                  });
                stopWaypoints = false;
              }
              old_thrust = thrustTemp;
              old_rudder = rudderTemp;

              //what is this?
              if (tempPose != null) {
                try {
                  Pose3D waypoint = tempPose[0].pose;
                  double distanceSq = planarDistanceSq(_pose.pose, waypoint);

                  if (distanceSq <= 5*5) {
                    //UtmPose[] queuedWaypoints = new UtmPose[tempPose.length - 1];

                    if (N_waypoint < waypointList.size()) {
                      N_waypoint += 1;
                      tempPose = Arrays.copyOfRange(tempPose, 1, tempPose.length);
                    }

                  }
                } catch (Exception e) {
                  Log.e(logTag, e.getMessage());
                }
              }
              publishProgress();
            }
          }
        };

      exec = new ScheduledThreadPoolExecutor(1);
      future = exec.scheduleAtFixedRate(networkRun, 0, updateRateMili, TimeUnit.MILLISECONDS);
      return null;
    }
    @Override
    protected void onProgressUpdate(Integer... result) {
      LatLng curLoc;
      if (latlongloc != null) {

        curLoc = new LatLng(latlongloc.latitudeValue(SI.RADIAN) * 180 / Math.PI, latlongloc.longitudeValue(SI.RADIAN) * 180 / Math.PI);
        float degree = (float) (rot * 180 / Math.PI);  // degree is -90 to 270
        degree = (degree < 0 ? 360 + degree : degree); // degree is 0 to 360

        float bias = mv.getRotation();  // bias is the map orientation

        try {
          currentBoat.setLocation(curLoc);
        }
        catch(Exception e)
        {

        }

      }

      if (connected == true) {
        ipAddressBox.setBackgroundColor(Color.GREEN);
      }
      if (connected == false) {
        ipAddressBox.setBackgroundColor(Color.RED);
      }

      if (sensorReady == true) {
        try {

          sensorvalueButton.setClickable(sensorReady);
          sensorvalueButton.setTextColor(Color.BLACK);
          sensorvalueButton.setText("Show SensorData");
          SharedPreferences settings = getSharedPreferences(PREF_NAME, 0);
          SharedPreferences.Editor editor = settings.edit();

          if (Data.channel == 4) {
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

          if (sensorvalueButton.isChecked()) {
            double value;
            switch (Data.channel) {
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
              value = (Double.parseDouble(sensorV) + getAverage(Data))/2;
              editor.putString(Data.type.toString(), Double.toString(value));
              editor.commit();
              break;
            case 9:
              break;
            default:
            }

          }
        }
        catch(Exception e)
        {
            Log.i(sensorLogTag, e.toString());
            System.out.println("Sensor error " + e.toString());
        }
        if (!sensorvalueButton.isChecked())
        {
          //sensorV = "";
          sensorData1.setText("----");
          sensorData2.setText("----");
          sensorData3.setText("----");
          //sensorValueBox.setBackgroundColor(Color.DKGRAY);
        }
      }
      else
      {
        sensorvalueButton.setText("Sensor Unavailable");
        sensorData1.setText("----");
        sensorData2.setText("----");
        sensorData3.setText("----");
      }
      //********************************//
      // Adding Joystick move listener//
      // ******************************//
      joystick.setOnJostickMovedListener(_listener);
      joystick.setOnJostickClickedListener(new JoystickClickedListener() {
          @Override
          public void OnClicked() {
            //System.out.println("joystick clicked");
          }

          @Override
          public void OnReleased() {
            //System.out.println("joystick released");
          }
        });

      if (waypointLayoutEnabled == true) {
        String status = boatwaypoint;
        if (status == null)
          {
            status = "\t\t-----";
          }
        waypointInfo.setText("Waypoint Status: \n" + status);

      }
      //uncomment this
      //waypointsCompleted();
    }
  }

  private String unit(VehicleServer.SensorType stype) {
    String unit = "";

    if (stype == VehicleServer.SensorType.ATLAS_PH) {
      unit = "pH";
    } else if (stype == VehicleServer.SensorType.ATLAS_DO) {
      unit = "DO (mg/L)";
    } else if (stype == VehicleServer.SensorType.ES2) {
      unit = "EC(µS/cm)\n" +
        "T(°C)";
    } else if (stype == VehicleServer.SensorType.HDS_DEPTH) {
      unit = "depth (m)";
    } else {
      unit = "";
    }

    return unit;
  }

  // Converts from progress bar value to linear scaling between min and
  // max
  private double fromProgressToRange(int progress, double min, double max) {
      // progress will be between -10 and 10, with 0 being the center
      //return ((max - min) * ((double) progress) / 20.0);
      // evaluate linear range above and below zero separately
      double value;
      if (progress < 0)
      {
          value = min*Math.abs(progress)/10.0;
          return value;
      }
      else
      {
          value = max*progress/10.0;
          return value;
      }
  }

  // Converts from progress bar value to linear scaling between min and
  // max
  private int fromRangeToProgress(double value, double min, double max) {
    return (int) (20.0 * (value) / (max - min));
  }

  /* accelerometer controls */
  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {

  }

  @Override
  public void onSensorChanged(SensorEvent sensorEvent) {
    Sensor mySensor = sensorEvent.sensor;
    if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
      float x = sensorEvent.values[0];
      float y = sensorEvent.values[1];
      float z = sensorEvent.values[2];

      long curTime = System.currentTimeMillis();

    }
  }

  public UtmPose convertLatLngUtm(ILatLng point) {

    UTM utmLoc = UTM.latLongToUtm(LatLong.valueOf(point.getLatitude(),
                                                  point.getLongitude(), NonSI.DEGREE_ANGLE), ReferenceEllipsoid.WGS84);

    // Convert to UTM data structure
    Pose3D pose = new Pose3D(utmLoc.eastingValue(SI.METER), utmLoc.northingValue(SI.METER), 0.0, 0, 0, 0);
    Utm origin = new Utm(utmLoc.longitudeZone(), utmLoc.latitudeZone() > 'O');
    UtmPose utm = new UtmPose(pose, origin);
    return utm;
  }

  public void connectBox() {
    final Dialog dialog = new Dialog(context);
    dialog.setContentView(R.layout.connectdialog);
    dialog.setTitle("Connect To A Boat");
    ipAddress = (EditText) dialog.findViewById(R.id.ipAddress1);

    Button submitButton = (Button) dialog.findViewById(R.id.submit);

    direct = (RadioButton) dialog.findViewById(R.id.wifi);
    reg = (RadioButton) dialog.findViewById(R.id.reg);
      System.out.println("ipaddr " + textIpAddress);
      //ipAddress.setText("127.0.0.1");
      loadPreferences();
      //textIpAddress = textIpAddress.replace("/",""); //that forward slash causes a network on main thread excep
      ipAddress.setText(textIpAddress);


    direct.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
          if (direct.isChecked()) {
            ipAddress.setText("192.168.1.20");
          }
        }
      });
    reg.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
          if (reg.isChecked()) {
            ipAddress.setText("tunnel.senseplatypus.com");
          } else {
            ipAddress.setText("192.168.1.20");
          }
        }
      });


    submitButton.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {

          if (ipAddress.getText() == null || ipAddress.getText().equals("") || ipAddress.getText().length() == 0) {
            ipAddressBox.setText("IP Address: 127.0.0.1 (localhost)");
          } else {
            ipAddressBox.setText("IP Address: " + ipAddress.getText());
          }
          markerList = new ArrayList<Marker>();
          //actual = actualBoat.isChecked();

          textIpAddress = ipAddress.getText().toString();
          //System.out.println("IP Address entered is: " + textIpAddress);
          if (direct.isChecked()) {
            if (ipAddress.getText() == null || ipAddress.getText().equals("")) {
              address = CrwNetworkUtils.toInetSocketAddress("127.0.0.1:" + boatPort);
            }
              else {
                address = CrwNetworkUtils.toInetSocketAddress(textIpAddress + ":" + boatPort);
            }
            // address = CrwNetworkUtils.toInetSocketAddress(textIpAddress + ":6077");
            //                    log.append("\n" + address.toString());
            //currentBoat = new Boat(address);
            currentBoat.setAddress(address);
          } else if (reg.isChecked()) {
            Log.i(logTag, "finding ip");
            FindIP();
          }
          try {
            saveSession(); //save ip address
          }
          catch(Exception e)
          {

          }
          dialog.dismiss();
          dialogClose();
          SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
          SharedPreferences.Editor editor = sharedPref.edit();
          editor.putString(SettingsActivity.KEY_PREF_IP,currentBoat.getIpAddress().getAddress().toString());
          editor.apply();
          editor.commit();
        }
      });
    dialog.show();

  }

  public static InetSocketAddress getAddress() {
    return address;
  }

  private void checkAndSleepForCmd() {
    if (lastTime >= 0) {
      long timeGap = 1000 - (System.currentTimeMillis() - lastTime);
      if (timeGap > 0) {
        try {
          Thread.sleep(timeGap);
        } catch (InterruptedException ex) {
        }
      }
    }
    lastTime = System.currentTimeMillis();
  }

  public void FindIP() {


    Thread thread = new Thread() {

        public void run() {
          address = CrwNetworkUtils.toInetSocketAddress(textIpAddress + ":6077");
          currentBoat.returnServer().setRegistryService(address);
          currentBoat.returnServer().getVehicleServices(new FunctionObserver<Map<SocketAddress, String>>() {
              @Override
                public void completed(Map<SocketAddress, String> socketAddressStringMap) {
                Log.i(logTag, "Completed");
                for (Map.Entry<SocketAddress, String> entry : socketAddressStringMap.entrySet()) {
                  Log.i(logTag, entry.toString());
                  currentBoat.returnServer().setVehicleService(entry.getKey());
                }
              }

              @Override
                public void failed(FunctionError functionError) {
                Log.i(logTag, "No Response");
              }
            });

        }
      };
    thread.start();

  }

  //  Make return button same as home button
  @Override
  public void onBackPressed() {
    //Log.d("CDA", "onBackPressed Called");
    Intent setIntent = new Intent(Intent.ACTION_MAIN);
    setIntent.addCategory(Intent.CATEGORY_HOME);
    setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(setIntent);
  }

  /*
   * format of waypoint file
   * x x x x x x (first save)
   * x x x x x x (second save) ..etc
   * */
  public void SaveWaypointsToFile() throws IOException {
    //nothing to
    // save if no waypoints
    if (boatPath.getOriginalPoints().isEmpty() == true) {
        System.out.println("path empty returning");
      return;
    }
    savePointList = new ArrayList<LatLng>(boatPath.getOriginalPoints());

    final BufferedWriter writer;
    try {
        File waypointFile = new File(Environment.getExternalStorageDirectory() + "/waypoints/" + waypointFileName);
        writer = new BufferedWriter(new FileWriter(waypointFile, true));
    } catch (Exception e) {
        System.out.println("error saving path to fle");
        System.out.println(e.toString());
      return;
    }

    final Dialog dialog = new Dialog(context);
    dialog.setContentView(R.layout.wpsavedialog);
    dialog.setTitle("Save Waypoint Set");
    final EditText input = (EditText) dialog.findViewById(R.id.newname);
    Button submit = (Button) dialog.findViewById(R.id.savebutton);
    submit.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {

          saveName = input.getText().toString();

          //if (!(saveName.contains(" ") || saveName.matches(".*\\d+.*"))) {
          if (!(saveName.contains("\""))) {

            //                if (!(saveName.contains(" ") || saveName.matches("^.*[^a-zA-Z0-9._-].*$"))) {

            try {
              writer.append("\n\" " + input.getText() + " \"");
              writer.flush();
              //writer.append(input.getText());
              for (ILatLng i : savePointList) {
                writer.append(" " + i.getLatitude() + " " + i.getLongitude());
                writer.flush();
              }
              //writer.write("\n");

              writer.close();
            } catch (Exception e) {
            }
            dialog.dismiss();
          } else {
            final AlertDialog alertDialog = new AlertDialog.Builder(context).create();
            alertDialog.setTitle("No Quotation Marks in Title");
            alertDialog.show();
            alertDialog.setCancelable(true);
            alertDialog.setCanceledOnTouchOutside(true);
          }

        }
      });
    dialog.show();
    File waypointFile = new File(getFilesDir() + "/waypoints.txt");

  }

  public void LoadWaypointsFromFile(String filename) throws IOException {
      //final File waypointFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + filename);//new File(getFilesDir() + "/waypoints.txt");
      final File waypointFile = new File(Environment.getExternalStorageDirectory() + "/waypoints/" + filename);//new File(getFilesDir() + "/waypoints.txt");
      try
      {
          touchpointList.clear();
          boatPath.clearPoints();
          invalidate();
      }
      catch(Exception e)
      {}
    //waypointFile.delete();
    Scanner fileScanner = new Scanner(waypointFile); //Scans each//line of the file
    final ArrayList<ArrayList<ILatLng>> waypointsaves = new ArrayList<ArrayList<ILatLng>>();
    final ArrayList<String> saveName = new ArrayList<String>();
    /* scans each line of the file as a waypoint save
     * then scans each line every two elements makes a latlng
     * adds all saves to arraylist
     * chose between arraylist later on
     */

    if (waypointFile.exists()) {
      while (fileScanner.hasNext()) {
        final ArrayList<ILatLng> currentSave = new ArrayList<ILatLng>();
        String s = fileScanner.nextLine();
        //System.out.println(s);
        final Scanner stringScanner = new Scanner(s);

        //get save name
        if (stringScanner.hasNext()) {
          if (stringScanner.next().equals("\"")) { //found first "
            String currentdata = stringScanner.next();
            String name = currentdata;
            while (!currentdata.equals("\"")) {
              currentdata = stringScanner.next();
              if (!currentdata.equals("\"")) {
                name = name + " " + currentdata;
              }
            }

            saveName.add(name);
          }
        }
        while (stringScanner.hasNext()) {
          // System.out.println(stringScanner.next());
          //                    System.out.println(Double.parseDouble(stringScanner.next()) + " " + Double.parseDouble(stringScanner.next()));

          final double templat = Double.parseDouble(stringScanner.next());
          final double templon = Double.parseDouble(stringScanner.next());
          ILatLng temp = new ILatLng() {
              @Override
              public double getLatitude() {
                return templat;

              }

              @Override
              public double getLongitude() {
                return templon;
              }

              @Override
              public double getAltitude() {
                return 0;
              }
            };

          currentSave.add(temp);

        }
        if (currentSave.size() > 0) { //make sure no empty arrays (throws offset of wpsaves also why this?!?!)
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


      final ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                                                                    TeleOpPanel.this,
                                                                    android.R.layout.select_dialog_singlechoice);
      wpsaves.setAdapter(adapter);
      for (String s : saveName) {
        adapter.add(s);
          adapter.notifyDataSetChanged();
      }
      final int chosensave;
      wpsaves.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
          @Override
          public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
            //System.out.println("on long click");
            final Dialog confirmdialog = new Dialog(context);
            confirmdialog.setContentView(R.layout.confirmdeletewaypoints);
            confirmdialog.setTitle("Delete This Waypoint Path?");
            Button deletebutton = (Button) confirmdialog.findViewById(R.id.yesbutton);
            Button cancel = (Button) confirmdialog.findViewById(R.id.nobutton);
            deletebutton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                  //delete line from file

                  //delete object from list since update wont occur until you press load wp again
                  adapter.remove(adapter.getItem(position));
                  try {
                    File inputFile = new File(getFilesDir() + "/waypoints.txt");
                    File tempFile = new File(getFilesDir() + "/tempwaypoints.txt");

                    BufferedReader reader = new BufferedReader(new FileReader(inputFile));
                    BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

                    String lineToRemove = "\" " + saveName.get(position) + " \"";
                    String currentLine;

                    while ((currentLine = reader.readLine()) != null) {

                      String trimmedLine = currentLine.trim();
                      if (trimmedLine.contains(lineToRemove)) {
                        continue;
                      }
                      writer.write(currentLine + System.getProperty("line.separator"));
                    }
                    writer.close();
                    reader.close();
                    tempFile.renameTo(inputFile);
                  } catch (Exception e) {
                  }
                  confirmdialog.dismiss();
                }
              });
            cancel.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                  confirmdialog.dismiss();
                }
              });
            confirmdialog.show();

            return false;
          }
        });
      wpsaves.setOnItemClickListener(new AdapterView.OnItemClickListener() {
          @Override
          public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            currentselected = position;
          }
        });
      submitButton.setOnClickListener(new OnClickListener() {
          @Override
          public void onClick(View v) {
            if (currentselected == -1) {
              dialog.dismiss();
              //write no selected box
            }
            waypointList.clear();
            markerList.clear();
            //System.out.println(currentselected);
            //for (ArrayList<ILatLng> i : waypointsaves)
            {
              //System.out.println(i.size());
            }

            int num = 1;
            for (ILatLng i : waypointsaves.get(currentselected)) //tbh not sure why there is a 1 offset but there is
              {
                //System.out.println(i.getLatitude() + " " + i.getLongitude());
                markerList.add(mMapboxMap.addMarker(new MarkerOptions().position(new LatLng(i.getLatitude(), i.getLongitude())).title(Integer.toString(num))));
                waypointList.add(new LatLng(i.getLatitude(), i.getLongitude()));
                num++;
              }
              /*ASDF*/
            //Waypath = mMapboxMap.addPolyline(new PolylineOptions().addAll(waypointList).color(Color.GREEN).width(5));
              //Waypath_outline_layer = mMapboxMap.addPolyline(new PolylineOptions().addAll(boatPath.getPoints()).color(Color.BLACK).width(8));
              //Waypath_top_layer = mMapboxMap.addPolyline(new PolylineOptions().addAll(boatPath.getPoints()).color(Color.WHITE).width(5));

            dialog.dismiss();
          }
        });
      dialog.show();
    }
  }

  public static double planarDistanceSq(Pose3D a, Pose3D b) {
    double dx = a.getX() - b.getX();
    double dy = a.getY() - b.getY();
    return dx * dx + dy * dy;
  }

  public void setHome() {
    final JSONObject Jhome = new JSONObject();
    final JSONObject JPhone = new JSONObject();
    final JSONObject JTablet = new JSONObject();
    if (home == null) {
      new AlertDialog.Builder(context)
        .setTitle("Set Home")
        .setMessage("Which position do you want to use?")
        .setPositiveButton("Phone", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              if (latlongloc != null) {
                home = new LatLng(latlongloc.latitudeValue(SI.RADIAN) * 180 / Math.PI, latlongloc.longitudeValue(SI.RADIAN) * 180 / Math.PI);
                try {
                  JPhone.put("Lat", home.getLatitude());
                  JPhone.put("Lng", home.getLongitude());
                  Jhome.put("Phone", JPhone);
                  mlogger.info(new JSONObject()
                               .put("Time", sdf.format(d))
                               .put("Home", Jhome));
                } catch (JSONException e) {

                }
                MarkerOptions home_MO = new MarkerOptions()
                  .position(home)
                  .title("Home")
                  .icon(Ihome);
                home_M = mMapboxMap.addMarker(home_MO);
                //Bitmap home_B = BitmapFactory.decodeResource(getResources(), R.drawable.home);
                //Drawable d = new BitmapDrawablegit (getResources(), home_B);


                mMapboxMap.moveCamera(CameraUpdateFactory.newLatLng(home));
              } else {

                Toast.makeText(getApplicationContext(), "Phone doesn't have GPS Signal", Toast.LENGTH_SHORT).show();
              }
            }
          })
        .setNegativeButton("Tablet", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

              Location tempLocation = LocationServices.FusedLocationApi.getLastLocation();
              LatLng loc = new LatLng(tempLocation.getLatitude(),tempLocation.getLongitude());
              //LatLng loc = new LatLng(mMapboxMap.getMyLocation());

              if (loc != null) {
                home = loc;
                MarkerOptions home_MO = new MarkerOptions()
                  .position(home)
                  .title("Home")
                  .icon(Ihome);
                home_M = mMapboxMap.addMarker(home_MO);
                mMapboxMap.moveCamera(CameraUpdateFactory.newLatLng(home));
                try {
                  JTablet.put("Lat", home.getLatitude());
                  JTablet.put("Lng", home.getLongitude());
                  Jhome.put("Tablet", JTablet);
                  mlogger.info(new JSONObject()
                               .put("Time", sdf.format(d))
                               .put("Home", Jhome));
                } catch (JSONException e) {

                }

              } else {
                Toast.makeText(getApplicationContext(), "Tablet doesn't have GPS Signal", Toast.LENGTH_SHORT).show();
              }


            }
          })
        .show();

    } else {
      new AlertDialog.Builder(context)
        .setTitle("Set Home")
        .setMessage("Do you want to remove the home?")
        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

              try {
                Jhome.put("Lat", home.getLatitude());
                Jhome.put("Lng", home.getLongitude());
                mlogger.info(new JSONObject()
                             .put("Time", sdf.format(d))
                             .put("Removed home", Jhome));
              } catch (JSONException e) {

              }
              mMapboxMap.removeMarker(home_M);
              home = null;
            }
          })
        .setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

            }
          })
        .show();
    }
  }

  public void goHome() {
    if (home == null) {
      Toast.makeText(getApplicationContext(), "Set home first!", Toast.LENGTH_LONG).show();
      //home = pHollowStartingPoint;
    } else {
      //stopWaypoints = true;

      //if(currentBoat.isConnected()){
      new AlertDialog.Builder(context)
        .setTitle("Go Home")
        .setMessage("Let the boat go home ?")
        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              Thread threadhome = new Thread() {
                  public void run() {
                    //if (currentBoat.isConnected()) {
                    if (currentBoat.isConnected()) {
                      //                                    currentBoat.returnServer().stopWaypoints(null);
                      //                                    checkAndSleepForCmd();
                      if (!isAutonomous) {
                        currentBoat.returnServer().setAutonomous(true, null);
                        isAutonomous = true;
                      }


                      UtmPose homeUTM = convertLatLngUtm(home);
                      currentBoat.addWaypoint(homeUTM.pose, homeUTM.origin);
                      Log.i(logTag, "Go home");
                    }
                  }
                };
              threadhome.start();
              Log.i(logTag, "Go home");
            }
          })
        .setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              Log.i(logTag, "Nothing");
            }
          })
        .show();

      //}

    }
  }

    public void sendPID()
    {
        loadPreferences(); //update values should be replaced automatically with
        final int THRUST_GAIN_AXIS = 0;
        final int RUDDER_GAIN_AXIS = 5;
        final Thread thread = new Thread() {
            public void run() {
                currentBoat.returnServer().setGains(THRUST_GAIN_AXIS, tPID, new FunctionObserver<Void>() {
                    @Override
                    public void completed(Void aVoid) {
                        Log.i(logTag, "Setting thrust PID completed.");
                    }

                    @Override
                    public void failed(FunctionError functionError) {
                        Log.i(logTag, "Setting thrust PID failed: " + functionError);
                    }
                });
                currentBoat.returnServer().setGains(RUDDER_GAIN_AXIS, rPID, new FunctionObserver<Void>() {
                    @Override
                    public void completed(Void aVoid) {
                        Log.i(logTag, "Setting rudder PID completed ");
                    }

                    @Override
                    public void failed(FunctionError functionError) {
                        Log.i(logTag, "Setting rudder PID failed: " + functionError);
                    }
                });
            }
        };
        thread.start();
        Thread PIDthread = new Thread()
        {
            @Override
            public void run()
            {
                currentBoat.returnServer().getGains(THRUST_GAIN_AXIS, new FunctionObserver<double[]>() {
                    @Override
                    public void completed(final double[] doubles) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.i(logTag, "pids are now: " + doubles[0] + " " + doubles[1] + " " + doubles[2]);
                            }
                        });
                    }

                    @Override
                    public void failed(FunctionError functionError) {
                        Log.e(logTag, "FAILED TO GET PIDS");
                    }
                });
                currentBoat.returnServer().getGains(RUDDER_GAIN_AXIS, new FunctionObserver<double[]>() {
                    @Override
                    public void completed(final double[] doubles) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.i(logTag, "pids are now: " + doubles[0] + " " + doubles[1] + " " + doubles[2]);
                            }
                        });
                    }

                    @Override
                    public void failed(FunctionError functionError) {
                        Log.e(logTag, "FAILED TO GET PIDS");
                    }
                });

            }
        };
        PIDthread.start();
    }

  public int isAverage(SensorData data, String value) {
    double v = Double.parseDouble(value);
    double average = getAverage(data);
    if ((average - v) > average * 0.001) {
      return Color.RED;
    } else if ((v - average) > average * 0.001) {
      return Color.GREEN;
    } else {
      return Color.GRAY;
    }
  }

  public double getAverage(SensorData data) {
    double average;
    SharedPreferences settings = getSharedPreferences(PREF_NAME, 0);
    String v = settings.getString(data.type.toString(), "0");
    average = Double.parseDouble(v);
    return average;
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
          public void run() {
            Toast.makeText(getApplicationContext(), "Please Connect to the Internet first", Toast.LENGTH_LONG).show();
          }
      });
      runOnUiThread(new Runnable() {
          @Override
          public void run() {
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
    //System.out.println("bounds: " + latLngBounds.toString());

    //        LatLngBounds latLngBounds = new LatLngBounds.Builder()
    //                .include(new LatLng(37.7897, -119.5073)) // Northeast
    //                .include(new LatLng(37.6744, -119.6815)) // Southwest
    //                .build();


    final String JSON_CHARSET = "UTF-8";
    final String JSON_FIELD_REGION_NAME = "FIELD_REGION_NAME";
    //System.out.println("zoom min" + mMapboxMap.getMinZoom());
    //System.out.println("zoom max" + mMapboxMap.getMaxZoom());
    //System.out.println("zoom current " + mMapboxMap.getCameraPosition().zoom);
    // Define the offline region
    //change zoom to 15 if need be

    OfflineTilePyramidRegionDefinition definition = new OfflineTilePyramidRegionDefinition(
                                                                                           mv.getStyleUrl(),latLngBounds,15,16, this.getResources().getDisplayMetrics().density); //try 19

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
          //list offline regions

        }
        @Override
        public void onError(String error) {
          Log.e(logTag, "Error: " + error);
        }
    });
  }

  public void alertsAndAlarms()
  {
      final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
      final Handler handler = new Handler();
      handler.post(new Runnable() {
          @Override
          public void run() {
              synchronized (_batteryVoltageLock)
              {
                  Log.i(logTag, "checking battery voltage...");
                  String sleep_str = sharedPref.getString(SettingsActivity.KEY_PREF_SNOOZE, "1");
                  long sleep_ms = (int)Double.parseDouble(sleep_str) * 60 * 1000;
                  long current_time = System.currentTimeMillis();
                  if (current_time - sleep_start_time >= sleep_ms) {
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
                          if (battery_voltage < alarm_voltage) {
                              if (!alarm_on) {
                                  alarm_ringtone.play();
                                  alarm_on = true;
                              }
                              Toast.makeText(getApplicationContext(), String.format("%s, retrieve the boat ASAP!", message), Toast.LENGTH_LONG).show();
                          }
                          if (!alarm_on) {
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


    public void updateMarkers()
    {
        final Handler handler = new Handler();

        final IconFactory mIconFactory = IconFactory.getInstance(context);
        Drawable userDraw = ContextCompat.getDrawable(context, R.drawable.userloc);
        Icon userIcon  = mIconFactory.fromDrawable(userDraw);
        final Marker userloc = mMapboxMap.addMarker(new MarkerOptions().position(pHollowStartingPoint).title("Your Location").icon(userIcon));
        final MarkerView boat2 = mMapboxMap.addMarker(new MarkerViewOptions().position(pHollowStartingPoint).title("Boat")
                             .icon(mIconFactory.fromResource(R.drawable.pointarrow)).rotation(0));

        Runnable markerRun = new Runnable()
        {
            @Override
            public void run()
            {
                int icon_Index;

                if (currentBoat != null && currentBoat.getLocation() != null && mMapboxMap != null)
                {
                    boat2.setPosition(currentBoat.getLocation());
                }

                float degree = (float) (rot * 180 / Math.PI);  // degree is -90 to 270
                degree = (degree < 0 ? 360 + degree : degree); // degree is 0 to 360
                if (mMapboxMap != null)
                {
                    // Log.e(logTag, String.format("Angle = %f", degree));
                    boat2.setRotation(degree);
                }

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

  //Change this to take a waypointlist in not sure global variable
  public void startWaypoints()
  {
    Thread thread = new Thread() {
        public void run() {
          //if (currentBoat.isConnected() == true) {
          if (currentBoat.getConnected() == true) {
            checktest = true;
            Log.i(logTag, "before if wp is: " + waypointList.size());
            try
            {
                Thread.currentThread().sleep(100);
            }
            catch(Exception e)
            {

            }
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
                //wpPose[0] = new UtmPose(tempUtm.pose, tempUtm.origin);
                for (int i = 0; i < waypointList.size(); i++) {
                  wpPose[i] = convertLatLngUtm(waypointList.get(i));
                  allWaypointsSent.add(wpPose[i]);
                }
                tempPose = wpPose;
              }

              currentBoat.returnServer().setAutonomous(true, new FunctionObserver<Void>() {
                  @Override
                    public void completed(Void aVoid) {
                    Log.i(logTag, "Autonomy set to true");
                  }

                  @Override
                    public void failed(FunctionError functionError) {
                    Log.i(logTag, "Failed to set autonomy");
                  }
                });
              checkAndSleepForCmd();
              currentBoat.returnServer().isAutonomous(new FunctionObserver<Boolean>() {
                  @Override
                    public void completed(Boolean aBoolean) {
                    isAutonomous = aBoolean;
                    Log.i(logTag, "isAutonomous: " + isAutonomous);
                  }

                  @Override
                    public void failed(FunctionError functionError) {

                  }
                });
              currentBoat.returnServer().startWaypoints(wpPose, "POINT_AND_SHOOT", new FunctionObserver<Void>() {
                  @Override
                    public void completed(Void aVoid) {

                    isWaypointsRunning = true;
                    System.out.println("startwaypoints - completed" + "size: " + waypointList.size());
                  }

                  @Override
                    public void failed(FunctionError functionError) {
                    isCurrentWaypointDone = false;
                    System.out.println("startwaypoints - failed");
                    // = waypointStatus + "\n" + functionError.toString();
                    // System.out.println(waypointStatus);
                  }
                });
              currentBoat.returnServer().getWaypoints(new FunctionObserver<UtmPose[]>() {
                  @Override
                    public void completed(UtmPose[] wps) {
                    for (UtmPose i : wps) {
                      //                                    System.out.println("wp");
                      //                                    System.out.println(i.toString());
                    }
                  }

                  @Override
                    public void failed(FunctionError functionError) {

                  }
                });
              Log.i(logTag, "startWaypoints():  before if wp is: " + waypointList.size());
            }
            else {
              runOnUiThread(new Runnable() {
                  @Override
                  public void run() {
                    System.out.println("wp list size: " + waypointList.size());
                    Toast.makeText(getApplicationContext(), "Please Select Waypoints", Toast.LENGTH_LONG).show();
                  }
                });
            }
            try {
              if (wpPose != null) {
                mlogger.info(new JSONObject()
                             .put("Time", sdf.format(d))
                             .put("startWP", new JSONObject()
                                  .put("WP_num", wpPose.length)
                                  .put("AddWaypoint", Auto)));
              }
            }catch(JSONException e){
              Log.w(logTag, "Failed to log startwaypoint");
            }

          }
        }
      };
    thread.start();
  }

  //this is used for checking if the boat reached the first waypoint for drawing a line
  public boolean reachedWaypoint(LatLng boatLocation,LatLng point)
  {
    //Marker i = markerList.get(0);
    if (isWaypointWithinDistance(boatLocation,point,GPSDIST))
      {
        return true;
      }
    return false;
  }
  public boolean isWaypointWithinDistance(LatLng a, LatLng b, double dist)
  {
    //        double x = Math.pow((a.getLatitude() - b.getLatitude()),2);
    //        double y = Math.pow((a.getLongitude() - b.getLongitude()),2);
    //        double distanceBetweenPoints = Math.sqrt(x+y);
    //        //if (distanceBetweenPoints < dist)//0.0000449)
    if (a.distanceTo(b) <= dist)
      {
        return true;
      }
    return false;
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
    waypointButton = (ImageButton) waypointregion.findViewById(R.id.waypointButton);
    waypointButton.setBackgroundResource(R.drawable.draw_icon);
    deleteWaypoint = (Button) waypointregion.findViewById(R.id.waypointDeleteButton);
    pauseWP = (ToggleButton) waypointregion.findViewById(R.id.pause);
    startWaypoints = (Button) waypointregion.findViewById(R.id.waypointStartButton);

    speed_spinner_erroneous_call = true; // reset the erroneous call boolean
    speed_spinner = (Spinner) waypointregion.findViewById(R.id.speed_spinner);
    set_speed_spinner_from_pref();

    waypointInfo = (TextView) waypointregion.findViewById(R.id.waypoints_waypointstatus);

    Button dropWP = (Button) waypointregion.findViewById(R.id.waypointDropWaypointButton);

    dropWP.setOnClickListener(new OnClickListener() {
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

    waypointButton.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View view) {
          System.out.println("called");
          if (!startDrawWaypoints) {
            waypointButton.setBackgroundResource(R.drawable.draw_icon2);
          }
          else {
            waypointButton.setBackgroundResource(R.drawable.draw_icon);
          }
          startDrawWaypoints = !startDrawWaypoints;
          System.out.println("startdraw wp is: " + startDrawWaypoints);

          Thread thread = new Thread() {
              public void run() {
                if (waypointButton.isActivated()) {
                  runOnUiThread(new Runnable() {
                      @Override
                      public void run() {
                        Toast.makeText(getApplicationContext(), "Use long press to add waypoints", Toast.LENGTH_LONG).show();
                      }
                    });
                  startDraw = false;
                  if (waypointLayoutEnabled == false)
                  {
                    drawPoly.setClickable(false);
                  }

                }
                else
                {
                  if (waypointLayoutEnabled == false)
                  {
                    drawPoly.setClickable(true);
                  }
                }
                if (!waypointlistener) {
                  currentBoat.returnServer().addWaypointListener(wl, new FunctionObserver<Void>() {
                      @Override
                        public void completed(Void aVoid) {
                        waypointlistener = true;
                      }

                      @Override
                        public void failed(FunctionError functionError) {
                        waypointlistener = false;
                      }
                    });
                }


                //System.out.println(currentBoat.returnServer().getGains(0);)
              }
            };
          thread.start();
        }
      });

    pauseWP.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
          Thread thread = new Thread() {
              public void run() {

                /* If pauseWp.isChecked is false that means boat is currently paused
                 * if pauseWP.isChecked is true that means boat is running */
                if (pauseWP.isChecked()) {
                  Auto = false;
                  isWaypointsRunning = false;
                } else {
                  isWaypointsRunning = true;
                  Auto = true;
                }
                if (Auto) {
                  currentBoat.returnServer().setAutonomous(true, null);
                } else {
                  currentBoat.returnServer().setAutonomous(false, null);
                }
              }
            };
          thread.start();
        }
      });
    deleteWaypoint.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
          if (containsRegion == true)
            {
              Toast.makeText(getApplicationContext(), "Please Delete Region in Region Menu", Toast.LENGTH_LONG).show();
              return;
            }

          isWaypointsRunning = false;
          pauseWP.setChecked(false);
          stopWaypoints = true;
          isCurrentWaypointDone = true;
          Thread thread = new Thread() {
              public void run() {
                currentBoat.returnServer().setAutonomous(false, null);
                currentBoat.returnServer().isAutonomous(new FunctionObserver<Boolean>() {
                    @Override
                      public void completed(Boolean aBoolean) {
                      isAutonomous = aBoolean;
                      Log.i(logTag, "isAutonomous: " + isAutonomous);
                    }
                    @Override
                      public void failed(FunctionError functionError) {

                    }
                  });
                currentBoat.returnServer().stopWaypoints(new FunctionObserver<Void>() {
                    @Override
                      public void completed(Void aVoid) {
                      Log.i(waypointLogTag,"Waypoints stopped");
                    }
                    @Override
                      public void failed(FunctionError functionError) {
                      Log.i(waypointLogTag,"Failed to stop waypoints");
                    }
                  });
              }
            };
          thread.start();
          boatPath = new Path();
          touchpointList.clear();
          invalidate();
          //mMapboxMap.removeAnnotations();
        }
      });

    startWaypoints.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View view) {
          //if (!pauseWP.isChecked() && isWaypointsRunning)
          isFirstWaypointCompleted = false;
          if (pauseWP.isChecked()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage("Currently waypoints are paused, by pressing start waypoints the boat will restart the path. \n If you want to resume waypoints press cancel then toggle resume ")
              .setCancelable(false)
              .setPositiveButton("Restart Path", new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int id) {
                    pauseWP.setChecked(false);
                    startWaypoints();
                  }
                })
              .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int id) {
                    return;
                  }
                });
            AlertDialog alert = builder.create();
            alert.show();
          }
          if (pauseWP.isChecked() == false) {
            startWaypoints();
          }

        }
      });

      speed_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
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
          public void onNothingSelected(AdapterView<?> parent) {

          }
      });
  }
  public void onLoadRegionLayout() {
    LayoutInflater inflater = (LayoutInflater) getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    regionlayout = (LinearLayout) findViewById(R.id.relativeLayout_sensor);
    waypointregion = inflater.inflate(R.layout.region_layout, regionlayout);
    startRegion = (Button) regionlayout.findViewById(R.id.region_start); //start button
    drawPoly = (ImageButton) regionlayout.findViewById(R.id.region_draw); //toggle adding points to region
    perimeter = (Button) regionlayout.findViewById(R.id.region_perimeter); //perimeter* start perimeter? didnt write this
    clearRegion = (Button) regionlayout.findViewById(R.id.region_clear); //region, not implemented yet
    Button stopButton = (Button) regionlayout.findViewById(R.id.stopButton);
    transectDistance = (EditText) regionlayout.findViewById(R.id.region_transect);
    drawPoly.setBackgroundResource(R.drawable.draw_icon);
    spirallawn = (ToggleButton) regionlayout.findViewById(R.id.region_spiralorlawn);
    updateTransect = (Button) regionlayout.findViewById(R.id.region_transectButton);

    spirallawn.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
          if (!spirallawn.isChecked()) {
            ArrayList<LatLng> temp = new ArrayList<LatLng>(touchpointList);
            boatPath = new Region(temp, AreaType.SPIRAL, currentTransectDist);
          } else {
            ArrayList<LatLng> temp = new ArrayList<LatLng>(touchpointList);
            boatPath = new Region(temp, AreaType.LAWNMOWER,currentTransectDist);
          }
          invalidate();
        }
      });

    updateTransect.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View view) {
          if (currentBoat == null) {
            return;
          }

          currentTransectDist = Double.parseDouble(transectDistance.getText().toString());
          boatPath.updateTransect(currentTransectDist);

          //boatPath = new Region(touchpointList,Are);
          invalidate();
        }
      });

    perimeter.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
          //addPointToRegion(currentBoat.getLocation());
        }
      });


    stopButton.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View view) {
          Thread thread = new Thread() {
              public void run() {
                currentBoat.returnServer().setAutonomous(false, null);
                currentBoat.returnServer().isAutonomous(new FunctionObserver<Boolean>() {
                    @Override
                      public void completed(Boolean aBoolean) {
                      isAutonomous = aBoolean;
                      Log.i(logTag, "isAutonomous: " + isAutonomous);
                    }

                    @Override
                      public void failed(FunctionError functionError) {

                    }
                  });
                currentBoat.returnServer().stopWaypoints(new FunctionObserver<Void>() {
                    @Override
                      public void completed(Void aVoid) {
                      Log.i(waypointLogTag, "Waypoints stopped");
                    }

                    @Override
                      public void failed(FunctionError functionError) {
                      Log.i(waypointLogTag, "Failed to stop waypoints");
                    }
                  });
              }
            };
          thread.start();
        }
      });
    clearRegion.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View view) {
          try { //in case they try to remove before something gets set
            if (touchpointList.size() == 0) {
              return;
            }
            touchpointList.clear();
            if (boatPath.getAreaType() == AreaType.LAWNMOWER) {
              boatPath = new Region(touchpointList, AreaType.LAWNMOWER);
            }
            else
              {
                boatPath = new Region(touchpointList, AreaType.SPIRAL);
              }
            invalidate();
          } catch (Exception e) {
            System.out.println("clear region");
            System.out.println(e.toString());
          }
        }
      });
    startRegion.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View view) {
          startWaypoints();
        }
      });
    drawPoly.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
          if (startDraw == false) {
            drawPoly.setBackgroundResource(R.drawable.draw_icon2);
          } else {
            drawPoly.setBackgroundResource(R.drawable.draw_icon);
          }
          startDraw = !startDraw;
        }
      });
  }

  /*
   * Not calling update transect
   *
   * */
  private void remove_waypaths()
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
  }
  private void add_waypaths()
  {
      ArrayList<ArrayList<LatLng>> point_pairs = boatPath.getPointPairs();
      for (ArrayList<LatLng> pair : point_pairs)
      {
          Waypath_outline.add(mMapboxMap.addPolyline(new PolylineOptions().addAll(pair).color(Color.BLACK).width(8)));
          Waypath_outline.add(mMapboxMap.addPolyline(new PolylineOptions().addAll(pair).color(Color.WHITE).width(5)));
      }
  }
  public void invalidate() {
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
    //touchpointList = boatPath.getQuickHullList();
    boatPath.updateRegionPoints();
      System.out.println("nullhaha");
      System.out.println("null" + (boatPath==null));
      System.out.println("null" + (boatPath.getPoints()==null));
      System.out.println("null" + (mMapboxMap==null));

      /*ASDF*/

    if (boatPath != null && boatPath.getPoints().size() > 0) {
        add_waypaths();
    }
    if (touchpointList.size() == 0 && Waypath_outline.size() > 0) {
        remove_waypaths();
    }

    if (boatPath instanceof Region) {
      for (LatLng i : boatPath.getQuickHullList()) {
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
    System.out.println("spiral full " + boatPath.getPoints().size());

  }

  public void saveSession() throws IOException
  {
    final File sessionFile = new File(getFilesDir() + "/session.txt");
      System.out.println(sessionFile.getAbsolutePath());
    BufferedWriter writer = new BufferedWriter(new FileWriter(sessionFile, false));
    String tempaddr = currentBoat.getIpAddress().toString();
    tempaddr = tempaddr.substring(1,tempaddr.indexOf(":"));
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
    //File waypointDir = new File(getFilesDir() + "/waypoints"); //FOLDER CALLED WAYPOINTS
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
        //adapter.add(i.getCanonicalPath());
        System.out.println(i.getName());
        adapter.add(i.getName());
        adapter.notifyDataSetChanged();
    }
      fileList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        currentselected = position; //Use a different variable, this is used by the list adapter in loading waypoints ..
      }
    });
    submitButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        dialog.dismiss();
        try {
            waypointFileName = listOfFiles[currentselected].getName();
        }
        catch(Exception e)
        {
          System.out.println("err loading file in waypoint file load");
          System.out.println(e.toString());
        }
      }
    });
      dialog.show();
  }

    public void loadPreferences()
    {
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        String iP = sharedPref.getString(SettingsActivity.KEY_PREF_IP, "192.168.1.1");
        String port = sharedPref.getString(SettingsActivity.KEY_PREF_PORT,"11411");

        textIpAddress = iP;
        textIpAddress = textIpAddress.replace("/",""); //network on main thread error if this doesnt happen
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
                        tPID[0] = 0.06;
                        tPID[1] = 0.0;
                        tPID[2] = 0.0;
                        rPID[0] = 0.35;
                        rPID[1] = 0;
                        rPID[2] = 0.15;
                        break;

                    case "MEDIUM":
                        tPID[0] = 0.2;
                        tPID[1] = 0.0;
                        tPID[2] = 0.0;
                        rPID[0] = 1.0;
                        rPID[1] = 0.0;
                        rPID[2] = 0.2;
                        break;

                    case "FAST":
                        tPID[0] = 0.5;
                        tPID[1] = 0.0;
                        tPID[2] = 0.0;
                        rPID[0] = 1.0;
                        rPID[1] = 0;
                        rPID[2] = 0.4;
                        break;

                    case "CUSTOM":
                        tPID[0] = Double.parseDouble(sharedPref.getString(SettingsActivity.KEY_PREF_PID_THRUST_P,"0.2"));
                        tPID[1] = Double.parseDouble(sharedPref.getString(SettingsActivity.KEY_PREF_PID_THRUST_I,"0"));
                        tPID[2] = Double.parseDouble(sharedPref.getString(SettingsActivity.KEY_PREF_PID_THRUST_D,"0"));
                        rPID[0] = Double.parseDouble(sharedPref.getString(SettingsActivity.KEY_PREF_PID_RUDDER_P,"1.0"));
                        rPID[1] = Double.parseDouble(sharedPref.getString(SettingsActivity.KEY_PREF_PID_RUDDER_I,"0"));
                        rPID[2] = Double.parseDouble(sharedPref.getString(SettingsActivity.KEY_PREF_PID_RUDDER_D,"0.2"));
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
                        tPID[0] = Double.parseDouble(sharedPref.getString(SettingsActivity.KEY_PREF_PID_THRUST_P,"0.4"));
                        tPID[1] = Double.parseDouble(sharedPref.getString(SettingsActivity.KEY_PREF_PID_THRUST_I,"0"));
                        tPID[2] = Double.parseDouble(sharedPref.getString(SettingsActivity.KEY_PREF_PID_THRUST_D,"0"));
                        rPID[0] = Double.parseDouble(sharedPref.getString(SettingsActivity.KEY_PREF_PID_RUDDER_P,"0.75"));
                        rPID[1] = Double.parseDouble(sharedPref.getString(SettingsActivity.KEY_PREF_PID_RUDDER_I,"0"));
                        rPID[2] = Double.parseDouble(sharedPref.getString(SettingsActivity.KEY_PREF_PID_RUDDER_D,"0.90"));
                        break;

                    default:
                        break;
                }
                break;

            default:
                break;
        }

        THRUST_MIN = Double.parseDouble(sharedPref.getString(SettingsActivity.KEY_PREF_THRUST_MIN,"-1.0"));
        THRUST_MAX = Double.parseDouble(sharedPref.getString(SettingsActivity.KEY_PREF_THRUST_MAX,"0.3"));

        RUDDER_MIN = Double.parseDouble(sharedPref.getString(SettingsActivity.KEY_PREF_RUDDER_MIN,"-1.0"));
        RUDDER_MAX = Double.parseDouble(sharedPref.getString(SettingsActivity.KEY_PREF_RUDDER_MAX,"1.0"));

        updateRateMili = Integer.parseInt(sharedPref.getString(SettingsActivity.KEY_PREF_COMMAND_RATE,"500"));
        Double initialPanLat = Double.parseDouble(sharedPref.getString(SettingsActivity.KEY_PREF_LAT,"0"));
        Double initialPanLon = Double.parseDouble(sharedPref.getString(SettingsActivity.KEY_PREF_LON,"0"));
        initialPan = new LatLng(initialPanLat,initialPanLon);
        setInitialPan = sharedPref.getBoolean(SettingsActivity.KEY_PREF_SAVE_MAP,true);
    }
}
