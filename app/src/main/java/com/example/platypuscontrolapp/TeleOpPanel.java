package com.example.platypuscontrolapp;
//code load waypoitns from file
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.jscience.geography.coordinates.LatLong;
import org.jscience.geography.coordinates.UTM;
import org.jscience.geography.coordinates.crs.ReferenceEllipsoid;

import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.constants.MapboxConstants;
import com.mapbox.mapboxsdk.clustering.projection.Point;
import com.mapbox.mapboxsdk.constants.MapboxConstants;
import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.geometry.CoordinateRegion;
import com.mapbox.mapboxsdk.geometry.CoordinateSpan;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.offline.OfflineMapDatabase;
import com.mapbox.mapboxsdk.offline.OfflineMapDownloader;
import com.mapbox.mapboxsdk.offline.OfflineMapDownloaderListener;
import com.mapbox.mapboxsdk.overlay.Icon;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.overlay.OfflineMapTileProvider;
import com.mapbox.mapboxsdk.overlay.TilesOverlay;
import com.mapbox.mapboxsdk.overlay.UserLocationOverlay;
import com.mapbox.mapboxsdk.tileprovider.tilesource.*;
import com.mapbox.mapboxsdk.views.MapView;
import com.mapbox.mapboxsdk.views.util.Projection;
import com.mapbox.mapboxsdk.views.util.TilesLoadedListener;

import android.content.ComponentCallbacks;
import android.net.ConnectivityManager;
import android.nfc.Tag;
import android.view.MotionEvent;
import android.view.View;


import com.platypus.crw.CrwNetworkUtils;
import com.platypus.crw.CrwNetworkUtils.*;
import com.platypus.crw.SensorListener;
import com.platypus.crw.VehicleServer;
import com.platypus.crw.data.SensorData;
import robotutils.Pose3D;
import android.app.Activity;
import android.content.Context;

import android.content.DialogInterface;
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
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.ProgressBar;

import java.util.Map;
import com.platypus.crw.udp.UdpVehicleServer;

import com.platypus.crw.FunctionObserver;
import com.platypus.crw.ImageListener;
import com.platypus.crw.PoseListener;
import com.platypus.crw.SensorListener;
import com.platypus.crw.VehicleServer.WaypointState;
import com.platypus.crw.VelocityListener;
import com.platypus.crw.WaypointListener;
import com.platypus.crw.data.Twist;
import com.platypus.crw.data.Utm;
import com.platypus.crw.data.UtmPose;

import android.app.Dialog;
import android.app.AlertDialog;

import android.view.View.OnClickListener;
import com.example.platypuscontrolapp.Joystick.*;


public class TeleOpPanel extends Activity implements SensorEventListener {
    final Context context = this;
    SeekBar thrust = null;
    SeekBar rudder = null;
    TextView ipAddressBox = null;
    TextView thrustProgress = null;
    TextView rudderProgress = null;
    RelativeLayout linlay = null;
    CheckBox autonomous = null;
    Button mapButton = null;
    static TextView testIP = null;
    AsyncTask networkThread;
    TextView test = null;
    ToggleButton tiltButton = null;
    ToggleButton waypointButton = null;


    Button deleteWaypoint = null;
    Button connectButton = null;
    Button saveMap = null;
    Button loadMap = null;
    Button removeMap = null;
    Button refreshMap = null;
    //TextView log = null;
    Handler network = new Handler();
    ImageView cameraStream = null;
    Button loadWPFile = null;

    TextView sensorData1 = null;
    TextView sensorData2 = null;
    TextView sensorData3 = null;

    TextView sensorType1 = null;
    TextView sensorType2 = null;
    TextView sensorType3 = null;
    TextView battery = null;

    ToggleButton sensorvalueButton = null;
    JoystickView joystick;
    ProgressBar progressBar;



    boolean checktest;
    int a = 0;

    double xValue;
    double yValue;
    double zValue;
    LatLong latlongloc;
    LatLng boatLocation;


    MapView mv;
    String zone;
    String rotation;

    TextView loca = null;
    //Marker boat;
    Marker boat2;

    LatLng pHollowStartingPoint = new LatLng((float) 40.436871,
            (float) -79.948825);
    LatLng UCMerced = new LatLng((float)37.400732,(float) -120.487372);
    long lastTime = -1;
    double lat = 10;
    double lon = 10;
    String waypointStatus = "";
    Handler handlerRudder = new Handler();
    int thrustCurrent;
    int rudderCurrent;
    double heading = Math.PI / 2.;
    double rudderTemp = 0;
    double thrustTemp = 0;
    double old_rudder=0;
    double old_thrust=0;
    double temp;
    double rot;
    String boatwaypoint;
    double tempThrustValue = 0; //used for abs value of thrust
    Twist twist = new Twist();

    float tempX = 0;
    float tempY = 0;

    Bitmap currentImage = null;
    boolean isAutonomous;
    boolean isCurrentWaypointDone = true;

    SensorManager senSensorManager;
    Sensor senAccelerometer;
    public boolean stopWaypoints = true;
    private long lastUpdate = 0;
    private float last_x, last_y, last_z;
    private static final int SHAKE_THRESHOLD = 600;

    public static final double THRUST_MIN = -1.0;
    public static final double THRUST_MAX = 1.0;
    public static final double RUDDER_MIN = 1.0;
    public static final double RUDDER_MAX = -1.0;

    public EditText ipAddress = null;
    public EditText color = null;
    public RadioButton actualBoat = null;
    public RadioButton simulation = null;
    public Button startWaypoints = null;

    public RadioButton direct = null;
    public RadioButton reg = null;

    public Button submitButton = null;
    public static RadioGroup simvsact = null;
    public static String textIpAddress;
    public static boolean simul = false;
    public static boolean actual;
    public static Boat currentBoat;
    public static InetSocketAddress address;
    public CheckBox autoBox;
    private final Object _waypointLock = new Object(); //deadlock?!??
    boolean failedwp = true;

    public int wpcount = 0;
    public String wpstirng = "";
    public int channel =0;
    public double[] data;
    SensorData Data;
    public String sensorV = "Loading...";
    public static int counter = 0;
    public TextView sensorValueBox;
    boolean dialogClosed = false;
    boolean sensorReady = false;
    public static TextView log;
    public boolean Auto = false;
    private String MapID = "shantanuv.nkob79p0";
    Dialog connectDialog;

    List<ILatLng> waypointList = new ArrayList<ILatLng>();
    List<Marker> markerList = new ArrayList(); //List of all the
    //markers on the map
    //corresponding to the
    //given way

    OfflineMapDownloader offlineMapDownloader;
    TilesOverlay offlineMapOverlay;
    LatLng OfflineCenter = null ;

    private static final String logTag = TeleOpPanel.class.getName();

    protected void onCreate(Bundle savedInstanceState)   {
        super.onCreate(savedInstanceState);

        //this.setContentView(R.layout.tabletlayout);
        this.setContentView(R.layout.tabletlayout);

        ipAddressBox = (TextView) this.findViewById(R.id.printIpAddress);
        //thrust = (SeekBar) this.findViewById(R.id.thrustBar);
        //rudder = (SeekBar) this.findViewById(R.id.rudderBar);
        linlay = (RelativeLayout) this.findViewById(R.id.linlay);
        thrustProgress = (TextView) this.findViewById(R.id.getThrustProgress);
        rudderProgress = (TextView) this.findViewById(R.id.getRudderProgress);
        // test = (TextView) this.findViewById(R.id.test12);
        //tiltButton = (ToggleButton) this.findViewById(R.id.tiltButton);
        waypointButton = (ToggleButton) this.findViewById(R.id.waypointButton);
        deleteWaypoint = (Button) this.findViewById(R.id.waypointDeleteButton);
        connectButton = (Button) this.findViewById(R.id.connectButton);
        log = (TextView) this.findViewById(R.id.log);
       // loadWPFile = (Button)this.findViewById(R.id.loadFileButton);
        autoBox = (CheckBox) this.findViewById(R.id.autonomousBox);
        startWaypoints = (Button) this.findViewById(R.id.waypointStartButton);
        sensorData1 = (TextView) this.findViewById(R.id.SValue1);
        sensorData2 = (TextView) this.findViewById(R.id.SValue2);
        sensorData3 = (TextView) this.findViewById(R.id.SValue3);
        sensorType1 = (TextView) this.findViewById(R.id.sensortype1);
        sensorType2 = (TextView) this.findViewById(R.id.sensortype2);
        sensorType3 = (TextView) this.findViewById(R.id.sensortype3);
        sensorvalueButton = (ToggleButton) this.findViewById(R.id.SensorStart);
        sensorvalueButton.setClickable(sensorReady);
        sensorvalueButton.setTextColor(Color.GRAY);
        battery = (TextView)this.findViewById(R.id.batteryVoltage);

        saveMap = (Button) this.findViewById(R.id.saveMap);
        loadMap = (Button) this.findViewById(R.id.loadMap);
        removeMap = (Button) this.findViewById(R.id.removeMap);
        refreshMap = (Button) this.findViewById(R.id.refreshMap);
        progressBar = (ProgressBar) this.findViewById(R.id.progressBar);
        //sensorvalueButton.setTextColor(Color.BLACK);
        //sensorvalueButton.setText("Show SensorData");
      //  sensorValueBox = (TextView) this.findViewById(R.id.SensorValue);
        //thrust.setProgress(0); //initially set thrust to 0
        //rudder.setProgress(50); //initially set rudder to center (50)
     // *****************//
     //      Joystick   //
     // ****************//
        joystick = (JoystickView) findViewById(R.id.joystickView);
        joystick.setYAxisInverted(false);


        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer,
                SensorManager.SENSOR_DELAY_NORMAL);
//
//        //cameraStream.setImageResource(R.drawable.streamnotfound);
        //File file = new File("/Android/data/com.example.platypuscontrolapp/cache/mapbox_tiles_cache");
        //final MBTilesLayer mbTilesLayer = new MBTilesLayer(file);
        mv = (MapView) findViewById(R.id.mapview);
        offlineMapDownloader = OfflineMapDownloader.getOfflineMapDownloader(this);
        mv.setDiskCacheEnabled(true);
        //offlineMapDownloader.addOfflineMapDownloaderListener(this);
        //tile source uses network

        // If has Internet connection, using online map, else using offline map

            Thread thread = new Thread() {
                public void run() {
                    if (isInternetAvailable()) {
                        try {

                            mv.setAccessToken("pk.eyJ1Ijoic2hhbnRhbnV2IiwiYSI6ImNpZmZ0Zzd5Mjh4NW9zeG03NGMzNDI4ZGUifQ.QJgnm41kA9Wo3CJU-xZLTA");
                            //mv.setTileSource(new MapboxTileLayer("mapbox.streets"));
                            mv.setTileSource(new MapboxTileLayer(MapID));
                            // mv.setTileSource(mbTilesLayer);

                            mv.setCenter(new ILatLng() {
                                @Override
                                public double getLatitude() {
                                    return pHollowStartingPoint.getLatitude();
                                }

                                @Override
                                public double getLongitude() {

                                    return pHollowStartingPoint.getLongitude();
                                }

                                @Override
                                public double getAltitude() {
                                    return 0;
                                }
                            });

                            mv.setZoom(14);


//                    mv.setOnTouchListener(new View.OnTouchListener() {
//                        @Override
//                        public boolean onTouch(View v, MotionEvent event) {
//                            if (waypointButton.isChecked()) {
//                                try {
//                                    Thread.sleep(1000);
//                                } catch (Exception e) {
//
//                                }
//                                Projection proj = ((MapView) v).getProjection();
//                                ILatLng touchedloc = proj.fromPixels(event.getX(), event.getY());
//                                System.out.println("Location: " + touchedloc.toString());
//                                LatLng templocc = new LatLng(touchedloc.getLatitude(), touchedloc.getLongitude());
//                                Log.i(logTag, "Waypoint "+ templocc );
//                                mv.addMarker(new Marker("First", " ", templocc));
//                            }
//                            return false;
//                        }
//                    });


                        }catch(Exception e){
                            System.err.println(e);
                        }
                    }
                    else{
                        if(offlineMapDownloader.getMutableOfflineMapDatabases() != null & offlineMapDownloader.getMutableOfflineMapDatabases().size()>0){
                            mv.setCenter(pHollowStartingPoint);
                            //Thread threadoffline = new Thread(){
                            //   public void run(){
                            loadOfflineMap();
                            //    }
                            //  };
                            //   threadoffline.start();
                        }
                        else{
                            Toast.makeText(getApplicationContext(),"No OfflineMap available, Connect to Internet", Toast.LENGTH_LONG).show();
                        }
                    }
                }
            };
            thread.start();




        connectButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                connectBox();
            }
        });



        connectBox();

//        loadWPFile.setOnClickListener(
//                new OnClickListener() {
//                    @Override
//                    public void onClick(View view) {
//                        try {
//
//                            if(setWaypointsFromFile()==false) {
//                                failedwp = true;
////
//                            }
//                            else
//                            {
//                                failedwp = false;
//                            }
//                        }
//                        catch(Exception e)
//                        {
//
//                        }
//                    }
//                });

//        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
//        alertDialog.setTitle("Add Waypoints from File");
//        if (failedwp == true)
//        {
//            alertDialog.setMessage("Waypoint File was in the incorrect formatting. \n No Current Waypoints");
//            waypointList.clear();
//            for (Marker i : markerList) {
//                i.remove();
//            }
//        }
//        else {
//            alertDialog.setMessage("Waypoints Added and Started");
//        }

//        alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int which) {
//                alertDialog.dismiss();
//            }
//        });
        //alertDialog.show();
        //actual = true;

        /*
         * This gets called when a boat is connected
         * Note it has to draw the boat somewhere initially until it gets a gps loc so it draws it
         * on PantherHollow lake until it gets a new gps loc and will then update to the current
         * position
         */

        startWaypoints.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Thread thread = new Thread(){
                    public void run(){
                        if (currentBoat.isConnected() == true)
                        {
                            System.out.println("called");
                            // System.out.println("waypointList > 0");
                            checktest = true;
                            UtmPose tempUtm = convertLatLngUtm(waypointList.get(waypointList.size() - 1));

                            waypointStatus = tempUtm.toString();

                            //System.out.println("wps" + waypointStatus);
                            currentBoat.addWaypoint(tempUtm.pose, tempUtm.origin);
                            UtmPose[] wpPose = new UtmPose[waypointList.size()];
                            synchronized (_waypointLock) {
                                //wpPose[0] = new UtmPose(tempUtm.pose, tempUtm.origin);
                                for (int i = 0; i < waypointList.size(); i++)
                                {
                                    wpPose[i] = convertLatLngUtm(waypointList.get(i));
                                }
                            }

                            checkAndSleepForCmd();
                            currentBoat.returnServer().startWaypoints(wpPose, "POINT_AND_SHOOT", new FunctionObserver<Void>() {
                                @Override
                                public void completed(Void aVoid) {
                                    System.out.println("completed");
                                }

                                @Override
                                public void failed(FunctionError functionError) {
                                    isCurrentWaypointDone = false;
                                    System.out.println("asdf");
                                    // = waypointStatus + "\n" + functionError.toString();
                                    // System.out.println(waypointStatus);
                                }
                            });
                            currentBoat.returnServer().getWaypoints(new FunctionObserver<UtmPose[]>() {
                                @Override
                                public void completed(UtmPose[] wps) {
                                    for (UtmPose i : wps)
                                    {
                                        System.out.println("wp");
                                        System.out.println(i.toString());
                                    }
                                }
                                @Override
                                public void failed(FunctionError functionError) {
                                    System.out.println("shit");
                                }
                            });
                        }

                    }
                };
                thread.start();
            }
        });

        waypointButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Thread thread = new Thread() {
                    public void run() {
                        if (waypointButton.isChecked()) {
                            Auto = true;
                        } else {
                            Auto = false;
                        }
                        if (Auto) {
                            currentBoat.returnServer().setAutonomous(true, null);
                        } else {
                            currentBoat.returnServer().setAutonomous(false, null);
                        }

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
                    }
                };
                thread.start();
            }
        });
        // download offline map
        saveMap.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(offlineMapDownloader.isMapIdAlreadyAnOfflineMapDatabase(MapID)){
                    Toast.makeText(getApplicationContext(), "MapID has already been downloaded.\n" +
                            "Please remove it before trying to download again", Toast.LENGTH_SHORT).show();
                }
                Thread thread = new Thread(){
                    public void run(){
                        saveOfflineMap();
                    }
                };
                thread.start();
            }
        });
        // display offline map
        loadMap.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //loadOfflineMap();
                mv.setCenter(pHollowStartingPoint);
                Thread thread = new Thread(){
                    public void run(){
                        loadOfflineMap();
                    }
                };
                thread.start();
            }
        });
        // switch to online map
        refreshMap.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                        if(isInternetAvailable()){
                            mv.setAccessToken("pk.eyJ1Ijoic2hhbnRhbnV2IiwiYSI6ImNpZmZ0Zzd5Mjh4NW9zeG03NGMzNDI4ZGUifQ.QJgnm41kA9Wo3CJU-xZLTA");
                            //mv.setTileSource(new MapboxTileLayer("mapbox.streets"));
                            mv.setTileSource(new MapboxTileLayer(MapID));
                        }
                        else{
                            Toast.makeText(getApplicationContext(),"Need Internet Connection", Toast.LENGTH_LONG).show();
                        }


            }
        });
        // remove offline map database
        removeMap.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                offlineMapDownloader.removeOfflineMapDatabaseWithID(MapID);
                Toast.makeText(getApplicationContext(), "Removed OfflineMap", Toast.LENGTH_SHORT).show();
                progressBar.setProgress(0);

            }
        });




    }

    private void saveOfflineMap (){
        offlineMapDownloader = OfflineMapDownloader.getOfflineMapDownloader(this);
       // mv.setCenter(pHollowStartingPoint);
       // mv.setZoom(17);
        BoundingBox boundingBox = new BoundingBox(new LatLng(40.435203, -79.951636), new LatLng(40.439345, -79.944796));
        CoordinateSpan span = new CoordinateSpan(boundingBox.getLatitudeSpan(), boundingBox.getLongitudeSpan());
        CoordinateRegion coordinateRegion = new CoordinateRegion(pHollowStartingPoint, span);
        offlineMapDownloader.beginDownloadingMapID(MapID, coordinateRegion, 11, 19);

        OfflineMapDownloaderListener listener = new OfflineMapDownloaderListener() {
            @Override
            public void stateChanged(OfflineMapDownloader.MBXOfflineMapDownloaderState newState) {
                Log.i(logTag, String.format(MapboxConstants.MAPBOX_LOCALE, "stateChanged to %s", newState));
            }

            @Override
            public void initialCountOfFiles(Integer numberOfFiles) {
                Log.i(logTag, String.format(MapboxConstants.MAPBOX_LOCALE, "File number = %d", numberOfFiles));
            }

            @Override
            public void progressUpdate(final Integer numberOfFilesWritten, final Integer numberOfFilesExcepted) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(progressBar.getVisibility() == View.GONE){
                            progressBar.setVisibility(View.VISIBLE);
                        }
                        progressBar.setMax(numberOfFilesExcepted);
                        progressBar.setProgress(numberOfFilesWritten);

                        if(numberOfFilesExcepted == numberOfFilesWritten){
                            progressBar.setVisibility(View.GONE);
                        }
                    }
                });

            }

            @Override
            public void networkConnectivityError(Throwable error) {

            }

            @Override
            public void sqlLiteError(Throwable error) {

            }

            @Override
            public void httpStatusError(Throwable error) {

            }

            @Override
            public void completionOfOfflineDatabaseMap(OfflineMapDatabase offlineMapDatabase) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Finish Saving Map", Toast.LENGTH_LONG).show();
                    }
                });


            }
        };
        offlineMapDownloader.addOfflineMapDownloaderListener(listener);
    }

    private void loadOfflineMap(){
        ArrayList<OfflineMapDatabase> offlineMapDatabases = offlineMapDownloader.getMutableOfflineMapDatabases();
        if(offlineMapDatabases != null & offlineMapDatabases.size()>0) {
           // OfflineMapDatabase db = offlineMapDatabases.get(0);

           // OfflineMapTileProvider tp = new OfflineMapTileProvider(getApplicationContext(), db);
           // offlineMapOverlay = new TilesOverlay(tp);

            //mv.addOverlay(offlineMapOverlay);


            // mv.setZ(1.0f);

            // File file = new File("/data/data/com.example.platypuscontrolapp/databases/shantanuv.nkob79p0");

           // mv.setCenter(pHollowStartingPoint);
            //mv.setZoom(17);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Marker marker = new Marker("Test", "", pHollowStartingPoint);

                    mv.setTileSource(new MBTilesLayer(getApplicationContext(), "shantanuv.nkob79p0.mblite"));
                    //mv.addOverlay(offlineMapOverlay);
                   // mv.addMarker(marker);
                    //mv.setTranslationZ(0);
                    Toast.makeText(getApplicationContext(), "Loading OfflineMap", Toast.LENGTH_SHORT).show();
                }
            });
        }
        else{
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "OfflineMap is Unavailable", Toast.LENGTH_LONG).show();
                }
            });

        }
    }
    // This method checks the wifi connection but not Internet access
    public static boolean isNetworkAvailable(final Context context) {
        final ConnectivityManager connectivityManager = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
        return connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected();
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
    // Really Check Internet access
    public Boolean isInternetAvailable() {
        try {
            Process p1 = java.lang.Runtime.getRuntime().exec("ping -c 1    www.google.com");
            int returnVal = p1.waitFor();
            boolean reachable = (returnVal==0);
            if(reachable){
                Log.i(logTag, "Internet access");
                return reachable;
            }
            else{
                Log.i(logTag, "No Internet access");
            }

        } catch (Exception e) {

            e.printStackTrace();
        }
        return false;
    }
    // *******************************
    //  JoystickView listener
    // *******************************
    private JoystickMovedListener _listener = new JoystickMovedListener() {
        @Override
        public void OnMoved(int x, int y) {
            thrustTemp = fromProgressToRange(y, THRUST_MIN, THRUST_MAX);
            rudderTemp = fromProgressToRange(x, RUDDER_MIN,RUDDER_MAX);
            Log.i(logTag,"Y:" + y + "\tX:" + x);
            Log.i(logTag, "Thrust" + thrustTemp + "\t Rudder" + rudderTemp);

        }

        @Override
        public void OnReleased() {

        }

        @Override
        public void OnReturnedToCenter() {

        }
    };

    public void dialogClose()
    {
        if (getBoatType() == true) {
            //log.append("asdf");

            //waypoint on click listener
            /*
             * if the add waypoint button is pressed and new marker where ever they click
             */

            mv.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (waypointButton.isChecked()) {

                        Projection proj = ((MapView) v).getProjection();
                        ILatLng touchedloc = proj.fromPixels(event.getX(), event.getY());
                        LatLng wpLoc = new LatLng(touchedloc.getLatitude(), touchedloc.getLongitude());

                        mv.addMarker(new Marker("", "", wpLoc));
                        waypointList.add(wpLoc);
                        wpstirng = wpLoc.toString();
                        //SendEmail();
                        markerList.add(new Marker("", "", wpLoc));

                        Log.i(logTag, "Waypoint "+ wpLoc);
                    }
                    return false;
                }

            });

            /*
             * If they press delete wayponts delete all markers off the map and delete waypoints
             */
            deleteWaypoint.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {

                    // ConnectScreen.boat.cancelWaypoint)
                    stopWaypoints = true;
                    //mv.clear();
                    //mv.removeMarkers(markerList);
                    waypointList.clear();
                    isCurrentWaypointDone = true;

                    try {
                        mv.clear();
                        LatLng curLoc = new LatLng(latlongloc.latitudeValue(SI.RADIAN) * 180 / Math.PI, latlongloc.longitudeValue(SI.RADIAN) * 180 / Math.PI);
                        boat2 = new Marker(currentBoat.getIpAddress().toString(), "Boat", curLoc);
                        boat2.setPoint(curLoc);
                        mv.animate();
                        mv.addMarker(boat2);
                    } catch (Exception e) {
                        boat2 = new Marker(currentBoat.getIpAddress().toString(), "Boat", new LatLng(pHollowStartingPoint.getLatitude(), pHollowStartingPoint.getLongitude()));
                    }
                    System.out.println("called delete");
                }
            });


            networkThread = new NetworkAsync().execute(); //launch networking asnyc task

        }
        else if (getBoatType() == false) {
            log.append("Simulated Boat");
            ipAddressBox.setText("Simulated Phone");
            simulatedBoat();
        }
        else
        {
            log.append("fail");
        }

        try {
            //boat2 = new Marker(currentBoat.getIpAddress().toString(), "Boat", new LatLng(pHollowStartingPoint.getLatitude(), pHollowStartingPoint.getLongitude()));
            boat2 = new Marker("Boat", "Boat", new LatLng(pHollowStartingPoint.getLatitude(), pHollowStartingPoint.getLongitude()));
            mv.addMarker(boat2);
            mv.setCenter(new ILatLng() {
                @Override
                public double getLatitude() {
                    return pHollowStartingPoint.getLatitude();
                }

                @Override
                public double getLongitude() {
                    return pHollowStartingPoint.getLongitude();
                }

                @Override
                public double getAltitude() {
                    return 0;
                }
            });
        }
        catch(Exception e)
        {

        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // turns the thrust and rudder off when you pause the activity
       // thrust.setProgress(0);
        //rudder.setProgress(50);
        //networkThread.cancel(true);
    }

//    @Override
//    public void onResume() {
//        super.onResume();
//        //Intent intent = new Intent(this, TeleOpPanel.class);
//        //startActivity(intent);
//        if (networkThread.isCancelled()) //figure out how to resume asnyc task?
//        {
//            //    networkThread.execute();
//        }
//    }

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

    public void updateVelocity(Boat a) { //taken right from desktop client for updating velocity
        // ConnectScreen.boat.setVelocity(thrust.getProgress(),
        // rudder.getProgress());
        if (a.returnServer() != null) {
            //Twist twist = new Twist();
            twist.dx(thrustTemp >= -1 & thrustTemp <= 1 ? thrustTemp : 0);
            if (Math.abs(rudderTemp - 0) < .05) {
                tempThrustValue = 0;
                twist.drz(fromProgressToRange((int) tempThrustValue, RUDDER_MIN,
                        RUDDER_MAX));

            } else {
                twist.drz(rudderTemp >= -1 & rudderTemp <= 1 ? rudderTemp : 0);
            }
            a.returnServer().setVelocity(twist, null);
        }
    }

    /*
     * this async task handles all of the networking on the boat since networking has to be done on
     * a different thread and since gui updates have to be updated on the main thread ....
     */

    private class NetworkAsync extends AsyncTask<String, Integer, String> {
        long oldTime = 0;
        long oldTime1 = 0;
        String tester = "done";
        boolean connected = false;
        boolean firstTime = true;


        @Override
        protected void onPreExecute()
        {

        }
        @Override
        protected String doInBackground(String... arg0) {
//            if (currentBoat == null)
//            {
//                currentBoat = new Boat();
//            }

        //    offlineMapDownloader.addOfflineMapDownloaderListener(this);
            PoseListener pl = new PoseListener() { //gets the location of the boat
                public void receivedPose(UtmPose upwcs) {

                    UtmPose _pose = upwcs.clone();
                    {
                        xValue = _pose.pose.getX();
                        yValue = _pose.pose.getY();
                        zValue = _pose.pose.getZ();
                        rotation = String.valueOf(Math.PI / 2
                                - _pose.pose.getRotation().toYaw());
                        rot =  Math.PI/2 - _pose.pose.getRotation().toYaw();

                        zone = String.valueOf(_pose.origin.zone);

                        latlongloc = UTM.utmToLatLong(UTM.valueOf(
                                        _pose.origin.zone, 'T', _pose.pose.getX(),
                                        _pose.pose.getY(), SI.METER),
                                ReferenceEllipsoid.WGS84);

                        //Log.i(logTag, "rot:" + rot);
                    }
                }
            };

            currentBoat.returnServer().addPoseListener(pl, null);
            testWaypointListener();




            final SensorListener sensorListener = new SensorListener() {
                @Override
                public void receivedSensor(SensorData sensorData) {
                    Data = sensorData;

                    sensorV = Arrays.toString(Data.data);
                    sensorV = sensorV.substring(1, sensorV.length()-1);
                    sensorReady = true;
                    //Log.i("Platypus","Get sensor Data");
                }
            };

            currentBoat.returnServer().getNumSensors(new FunctionObserver<Integer>() {
                @Override
                public void completed(Integer num) {
                    Log.i(logTag, "Sensor Number:" + Integer.toString(num));
                    for (channel = 0; channel < num; channel++) {
                        currentBoat.returnServer().addSensorListener(channel, sensorListener, null);
                    }
//                    currentBoat.returnServer().addSensorListener(4, sensorListener, new FunctionObserver<Void>() {
//                        @Override
//                        public void completed(Void aVoid) {
//                            Log.i(logTag, "Adding battery listener");
//                        }
//
//                        @Override
//                        public void failed(FunctionError functionError) {
//
//                        }
//                    });
                }

                @Override
                public void failed(FunctionError functionError) {

                }
            });

            // For test, comment this when using for real implementation
            //publishProgress();
            // setVelListener();
            // InitSensorData();
            while (true) { //constantly looping
                if (currentBoat != null) {
                    if (System.currentTimeMillis() % 100 == 0
                            && oldTime != System.currentTimeMillis()) {

                        counter++; // if counter == 10 (1000ms), update sensor value
                        if (currentBoat.isConnected() == true) {
                            connected = true;
                        }
                        if (currentBoat.isConnected() == false) {
                            connected = false;
                        }

                        if (old_thrust != thrustTemp) { //update velocity
                            updateVelocity(currentBoat);
                        }

                        if (old_rudder != rudderTemp) { //update rudder
                            updateVelocity(currentBoat);
                        }

//                    }
                        //make this a method
                        if (stopWaypoints == true) {
                            currentBoat.returnServer().stopWaypoints(null);
                            stopWaypoints = false;
                        }



                        old_thrust = thrustTemp;
                        old_rudder = rudderTemp;
                        oldTime = System.currentTimeMillis();



                        publishProgress();

                    }
//
                }
            }


        }

        @Override
        protected void onProgressUpdate(Integer... result) {

            //cameraStream.setImageBitmap(currentImage);
            try
            {
            Projection tempproj = mv.getProjection();
            LatLng curLoc = new LatLng(latlongloc.latitudeValue(SI.RADIAN) * 180 / Math.PI, latlongloc.longitudeValue(SI.RADIAN) * 180 / Math.PI);
            boat2.setPoint(curLoc);
            mv.animate();


            if (firstTime == true) {
                try {
                    mv.getController().setCenter(new ILatLng() {
                        @Override
                        public double getLatitude() {
                            return latlongloc.latitudeValue(SI.RADIAN) * 180 / Math.PI;

                        }

                        @Override
                        public double getLongitude() {
                            return latlongloc.longitudeValue(SI.RADIAN) * 180 / Math.PI;

                        }

                        @Override
                        public double getAltitude() {
                            return 0;
                        }
                    });
                    mv.animate();
                    firstTime = false;

                } catch (Exception e) {
                    firstTime = true; //for false/fake lat long values until the phone gets real location values
                }
            }



        }
            catch(Exception e)
            {

            }


        if (connected == true) {
            ipAddressBox.setBackgroundColor(Color.GREEN);
        }
        if (connected == false) {
            ipAddressBox.setBackgroundColor(Color.RED);
        }



        if(sensorReady == true) {
            sensorvalueButton.setClickable(sensorReady);
            sensorvalueButton.setTextColor(Color.BLACK);
            sensorvalueButton.setText("Show SensorData");
            if(Data.channel == 4){
                String[] batteries = sensorV.split(",");
                battery.setText(batteries[0]);
            }

            if (sensorvalueButton.isChecked()) {
              //  sensorValueBox.setBackgroundColor(Color.GREEN);
                switch (Data.channel) {
                    case 4:
//                        String[] batteries = sensorV.split(",");
//                        battery.setText(batteries[0]);
                        break;
                    case 1:
                        sensorData1.setText(sensorV);
                        sensorType1.setText("" + Data.type);
                        break;
                    case 2:
                        sensorData2.setText(sensorV);
                        sensorType2.setText("DO \nmg/L");
                        break;
                    case 3:
                        sensorData3.setText(sensorV);
                        sensorType3.setText(" ES2 \nEC(µS/cm)\nTE(°C)");
                        break;
                    case 9:
                        break;
                    default:
                        sensorData1.setText("Waiting");
                        sensorData2.setText("Waiting");
                        sensorData3.setText("Waiting");
                }

            }
            if (!sensorvalueButton.isChecked()) {
                //sensorV = "";
                sensorData1.setText("----");
                sensorData2.setText("----");
                sensorData3.setText("----");
                //sensorValueBox.setBackgroundColor(Color.DKGRAY);
            }
        }
        else{
            sensorvalueButton.setText("Sensor Unavailable");
            sensorData1.setText("----");
            sensorData2.setText("----");
            sensorData3.setText("----");
        }
//********************************//
// Adding Joystick move listener//
// ******************************//
        joystick.setOnJostickMovedListener(_listener);

        DecimalFormat velFormatter = new DecimalFormat("####.###");

        //thrustTemp = fromProgressToRange(thrust.getProgress(), THRUST_MIN, THRUST_MAX);
       // rudderTemp = fromProgressToRange(rudder.getProgress(), RUDDER_MIN, RUDDER_MAX);
        thrustProgress.setText(velFormatter.format(thrustTemp * 100.0) + "%");
        rudderProgress.setText(velFormatter.format(rudderTemp * 100.0) + "%");

        log.setText("Waypoint Status: \n" + boatwaypoint);
        autoBox.setChecked(isAutonomous);



    }
}

    public void simulatedBoat() {
//        boat2 = map.addMarker(new MarkerOptions().anchor(.5f, .5f) //add boat to panther hollow
//                .rotation(270).title("Boat 1")
//                .snippet("IP Address: 192.168.1.1")
//                .position(pHollowStartingPoint).title("Boat 1")
//                .snippet("127.0.0.1 (localhost)")
//                        //.icon(BitmapDescriptorFactory.fromResource(R.drawable.airboat))
//                .flat(true));
//
//        lat = pHollowStartingPoint.latitude;
//        lon = pHollowStartingPoint.longitude;
//        map.setMyLocationEnabled(true);
//        map.moveCamera(CameraUpdateFactory.newLatLngZoom(pHollowStartingPoint,
//                15));
//        map.animateCamera(CameraUpdateFactory.zoomTo(17.0f));
//
//        boat2.setRotation((float) (heading * (180 / Math.PI)));
//        handlerRudder.post(new Runnable() { //control the boat
//            @Override
//            public void run() {
//                if (thrust.getProgress() > 0) {
//                    lat += Math.cos(heading) * (thrust.getProgress() - 50)
//                            * .0000001;
//                    lon += Math.sin(heading) * (thrust.getProgress())
//                            * .0000001;
//                    heading -= (rudder.getProgress() - 50) * .001;
//                    boat2.setRotation((float) (heading * (180 / Math.PI)));
//                }
//                boat2.setPosition(new LatLng(lat, lon));
//                handlerRudder.postDelayed(this, 200);
//            }
//        });
    }

    public void setVelListener() {
        currentBoat.returnServer().addVelocityListener(
                new VelocityListener() {
                    public void receivedVelocity(Twist twist) {
                        thrust.setProgress(fromRangeToProgress(twist.dx(),
                                THRUST_MIN, THRUST_MAX));
                        rudder.setProgress(fromRangeToProgress(twist.drz(),
                                RUDDER_MIN, RUDDER_MAX));
                    }
                }, null);

    }

    // Converts from progress bar value to linear scaling between min and
// max
    private double fromProgressToRange(int progress, double min, double max) {
        return ((max - min) * ((double) progress) / 20.0);
    }

    // Converts from progress bar value to linear scaling between min and
// max
    private int fromRangeToProgress(double value, double min, double max) {
        return (int) (20.0 * (value ) / (max - min));
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

//            if (tiltButton.isChecked()) {
//                if ((curTime - lastUpdate) > 100) {
//                    long diffTime = (curTime - lastUpdate);
//                    lastUpdate = curTime;
//                    float speed = Math
//                            .abs(x + y + z - last_x - last_y - last_z)
//                            / diffTime * 10000;
//
//                    if (speed > SHAKE_THRESHOLD) {
//                    }
//
//                    last_x = y; // rudder switching x and z for tesing orientation
//                    last_y = x;
//                    last_z = z; // thrust
//                    // test.setText("x: " + last_x + "y: " + last_y + "z: "
//                    // + last_z);
//
//                    //updateViaAcceleration(last_x, last_y, last_z);
//                }
//            }
        }
    }

    public void updateViaAcceleration(float xval, float yval, float zval) { //update the thrust via accelerometers
        if (Math.abs(tempX - last_x) > 2.5) {

            if (last_x > 2) {
                thrust.setProgress(thrust.getProgress() - 3);
            }
            if (last_x < 2) {
                thrust.setProgress(thrust.getProgress() + 3);
            }
        }
        if (Math.abs(tempY - last_y) > 1) {
            if (last_y > 2) {
                rudder.setProgress(rudder.getProgress() - 3);
            }
            if (last_y < -2) {
                rudder.setProgress(rudder.getProgress() + 3);
            }
        }
    }

    public void addWayPointFromMap() {
        // when you click you make utm pose... below is fake values
        Pose3D pose = new Pose3D(1, 1, 0, 0.0, 0.0, 10);
        Utm origin = new Utm(17, true);
        // ConnectScreen.boat.addWaypoint(pose, origin);
        UtmPose[] wpPose = new UtmPose[1];
        wpPose[0] = new UtmPose(pose, origin);
        currentBoat.returnServer().startWaypoints(wpPose,
                "POINT_AND_SHOOT", new FunctionObserver<Void>() {
                    public void completed(Void v) {
                        //log.setText("completed"); UNCOMMENT THESE
                    }

                    public void failed(FunctionError fe) {
                        ///log.setText("failed");
                    }
                });

    }

    public LatLng convertUtmLatLng(Pose3D _pose, Utm _origin) {
        LatLong temp = UTM
                .utmToLatLong(
                        UTM.valueOf(_origin.zone, 'T', _pose.getX(),
                                _pose.getY(), SI.METER),
                        ReferenceEllipsoid.WGS84);
        return new LatLng(temp.latitudeValue(SI.RADIAN),
                temp.longitudeValue(SI.RADIAN));
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


    //  public void viewCamera()
//  {
//      ConnectScreen.boat.returnServer().addImageListener(new ImageListener() {
//
//            public void receivedImage(byte[] imageData) {
//                // Take a picture, and put the resulting image into the panel
//                try {
//                  Bitmap image1 = BitmapFactory.decodeByteArray(imageData, 0, 15);
//                    BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(imageData));
//                    if (image != null) {
//                        Image scaledImage = image.getScaledInstance(pictureLabel.getWidth(), pictureLabel.getHeight(), Image.SCALE_DEFAULT);
//                        pictureLabel.setIcon(new ImageIcon(scaledImage));
//                        CameraPanel.this.repaint();
//                    } else {
//                        System.err.println("Failed to decode image.");
//                    }
//                } catch (IOException ex) {
//                    System.err.println("Failed to decode image: " + ex);
//                }
//
//            }
//        }, null);
    public void testCamera() {
        //log.setText("test camera");
        currentBoat.returnServer().addImageListener(new ImageListener() {
            public void receivedImage(byte[] imageData) {
                // Take a picture, and put the resulting image into the panel
                //log.setText("image taken");

                try {
                    Bitmap image1 = BitmapFactory.decodeByteArray(imageData, 0, 15);
                    if (image1 != null) {
                        // a++;
                        //System.out.println("image made");
                        currentImage = image1;

                    }
                } catch (Exception e) {
                    //log.setText(e.toString()); uncomment this
                    e.printStackTrace();
                }
            }
        }, null);
    }

    public void connectBox()
    {
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.connectdialog);
        ipAddress = (EditText) dialog.findViewById(R.id.ipAddress1);

        Button submitButton = (Button) dialog.findViewById(R.id.submit);
        simvsact = (RadioGroup) dialog.findViewById(R.id.simvsactual);
        actualBoat = (RadioButton) dialog.findViewById(R.id.actualBoatRadio);
        simulation = (RadioButton) dialog.findViewById(R.id.simulationRadio);

        direct = (RadioButton) dialog.findViewById(R.id.wifi);
        reg = (RadioButton) dialog.findViewById(R.id.reg);
        ipAddress.setText("registry.senseplatypus.com");

        direct.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(direct.isChecked()){
                    ipAddress.setText("192.168.1.");
                }
            }
        });
        reg.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (reg.isChecked())
                {
                    ipAddress.setText("registry.senseplatypus.com");
                }
                else
                {
                    ipAddress.setText("192.168.1.");
                }
            }
        });
        if (ipAddress.getText() == null || ipAddress.getText().equals("") || ipAddress.getText().length()==0)
        {
            ipAddressBox.setText("IP Address: 127.0.0.1 (localhost)");
        }
        else {
            ipAddressBox.setText("IP Address: " + ipAddress.getText());
        }


        submitButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // int selectedId = simvsact.getCheckedRadioButtonId();
                //int selectedOption = actvsim.getCheckedRadioButtonId();
                //log.append("asdf" + selectedOption);
//                if (boat2 != null) {
                    //boat2.remove();
//                }
                markerList = new ArrayList<Marker>();
                actual = actualBoat.isChecked();

                textIpAddress = ipAddress.getText().toString();
                System.out.println("asdfasdfasdf");
                System.out.println("IP Address entered is: " + textIpAddress);
                if (direct.isChecked()) {
                    if (ipAddress.getText() == null || ipAddress.getText().equals("")) {
                        address = CrwNetworkUtils.toInetSocketAddress("127.0.0.1" + ":11411");
                    }
                    address = CrwNetworkUtils.toInetSocketAddress(textIpAddress + ":11411");
//                    log.append("\n" + address.toString());
                    currentBoat = new Boat(address);
                }
                else if(reg.isChecked())
                {
                    System.out.println("finding ip");
                    FindIP();
                }
                dialog.dismiss();
                dialogClose();
            }
        });

        dialog.show();

    }

    public static InetSocketAddress getAddress()
    {
        return address;
    }
    public static String getIpAddress() {
        return textIpAddress;
    }

    public static boolean getBoatType() {
        return actual;
    }
    public void waypointListenerTest()
    {
        currentBoat.returnServer().addWaypointListener(new WaypointListener() {
            @Override
            public void waypointUpdate(WaypointState waypointState) {
                System.out.println("waypontstate: " + waypointState.toString());
            }
        },null);
    }
    public void testWaypointListener()
    {
        //this gets called on doInBackground() in the async task
        currentBoat.returnServer().addWaypointListener(new WaypointListener() {
            public void waypointUpdate(WaypointState ws) {
                boatwaypoint = ws.toString();
//                currentBoat.returnServer().isAutonomous(new FunctionObserver<Boolean>() {
//                    @Override
//                    public void completed(Boolean aBoolean) {
//                        isAutonomous = aBoolean;
//                        Log.i(logTag, "isAutonomous: "+ isAutonomous);
//                    }
//
//                    @Override
//                    public void failed(FunctionError functionError) {
//
//                    }
                //});
                //System.out.println(boatwaypoint);
            }
        }, null);
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
    public void fromFiletoWPList() throws IOException
    {
        //code for opening window for meantime have tmep folder with one file it accepts for wp list
        File readFile = new File("");
        Scanner fileReader = new Scanner(readFile);
        //set delimeter
        //parse text into latlong
        //waypointList.add(fileReader.next());
    }

    /* at the moment does not validate files! make sure your waypoint file is correctly matched this will be implemented later..*/
    public boolean setWaypointsFromFile() throws IOException {
        File wpFile = null;
        try {
            wpFile = new File("./waypoints.txt");
        }
        catch(Exception e)
        {
            System.out.println(e.toString());
        }
        Scanner fileScanner;
        int valueCounter = 0;
        //first make sure even number of elements

        if (wpFile.exists()) {
            fileScanner = new Scanner(wpFile);
            //first make sure even number of element
            while(fileScanner.hasNext())
            {
                try
                {
                    LatLng temp = new LatLng(Double.parseDouble(fileScanner.next()), Double.parseDouble(fileScanner.next()));
                    waypointList.add(temp);
                    Marker tempMarker = mv.addMarker(new Marker("","",temp));
                    markerList.add(tempMarker);
                }
                catch(Exception e)
                {
                    System.out.println("Invalid LAT/LNG in file");
                }
                System.out.println(fileScanner.next() + " " + fileScanner.next());
                valueCounter+=2;
            }
            System.out.println("amount of elements: " + valueCounter);
            if ((valueCounter % 2) != 0)
            {
                System.out.println("Mismatching lat long vals");
                return false;
            }
            else
            {
                System.out.println("Valid");
            }
        } else
        {
            System.out.println("File not found");
        }
        return true;
    }

//    public void FindIP() {
//
//

//        address = CrwNetworkUtils.toInetSocketAddress(textIpAddress + ":6077");
//
//        Thread thread = new Thread() {
//            public void run() {
//
//                currentBoat = new Boat();
//                UdpVehicleServer tempserver = new UdpVehicleServer();
//                currentBoat.returnServer().setRegistryService(address);
//                currentBoat.returnServer().getVehicleServices(new FunctionObserver<Map<SocketAddress, String>>() {
//                    @Override
//                    public void completed(Map<SocketAddress, String> socketAddressStringMap) {
//                        System.out.println("Completed");
//                        for (Map.Entry<SocketAddress, String> entry : socketAddressStringMap.entrySet()) {
//
//
//                            //newaddressstring = entry.getKey().toString();
//                            //System.out.println(newaddressstring);
//                            currentBoat.returnServer().setVehicleService(entry.getKey());
//
//                            System.out.println(entry.getKey().toString());
//                            System.out.println(entry.getValue().toString());
//
//                        }
//                    }
//
//                    @Override
//                    public void failed(FunctionError functionError) {
//                        System.out.println("No Response");
//                    }
//                });
//                //currentBoat = new Boat(CrwNetworkUtils.toInetSocketAddress(newaddressstring));
//                //System.out.println("Boat address" + currentBoat.getIpAddress());
//                // regcheck.show();
//                //}
//            }
//        };
//
//        thread.start();
//        //System.out.println("print here: " + newaddressstring);
//        //currentBoat = new Boat(CrwNetworkUtils.toInetSocketAddress(newaddressstring));
//
//    }
public void FindIP() {




    Thread thread = new Thread() {

        public void run() {
            //address = CrwNetworkUtils.toInetSocketAddress(textIpAddress + ":6077");
            address = CrwNetworkUtils.toInetSocketAddress(textIpAddress);
            currentBoat = new Boat();
            UdpVehicleServer tempserver = new UdpVehicleServer();
            currentBoat.returnServer().setRegistryService(address);
            currentBoat.returnServer().getVehicleServices(new FunctionObserver<Map<SocketAddress, String>>() {
                @Override
                public void completed(Map<SocketAddress, String> socketAddressStringMap) {
                    System.out.println("Completed");
                    for (Map.Entry<SocketAddress, String> entry : socketAddressStringMap.entrySet()) {
                        //newaddressstring = entry.getKey().toString();
                        //System.out.println(newaddressstring);
                       // currentBoat.returnServer().setVehicleService(entry.getKey());
//                        adapter.add(entry);
//                        adapter.notifyDataSetChanged();


                        currentBoat.returnServer().setVehicleService(entry.getKey());
                        PoseListener pl = new PoseListener() { //gets the location of the boat
                            public void receivedPose(UtmPose upwcs) {

                                UtmPose _pose = upwcs.clone();
                                {
                                    xValue = _pose.pose.getX();
                                    yValue = _pose.pose.getY();
                                    zValue = _pose.pose.getZ();
                                    rotation = String.valueOf(Math.PI / 2
                                            - _pose.pose.getRotation().toYaw());
                                    rot =  Math.PI/2 - _pose.pose.getRotation().toYaw();

                                    zone = String.valueOf(_pose.origin.zone);

                                    latlongloc = UTM.utmToLatLong(UTM.valueOf(
                                                    _pose.origin.zone, 'T', _pose.pose.getX(),
                                                    _pose.pose.getY(), SI.METER),
                                            ReferenceEllipsoid.WGS84);
                                    System.out.println(latlongloc.toString());

                                    //Log.i(logTag, "rot:" + rot);
                                }
                            }
                        };

                        currentBoat.returnServer().addPoseListener(pl, null);




                        System.out.println(entry.getKey().toString());
                        System.out.println(entry.getValue().toString());

                    }
                }

                @Override
                public void failed(FunctionError functionError) {
                    System.out.println("No Response");
                }
            });
//            regcheck.show();
        }
    };
    thread.start();

}

    public void SendEmail()
    {
        Thread thread = new Thread() {
            public void run() {
                Email mail = new Email("platypuslocation@gmail.com", "airboats");
                try {
                    //   mail.sendMail("jeffboat", wpstirng, "shantanu@gmail.com", "platypuslocation@gmail.com");
                }
                catch(Exception e)
                {
                    System.out.println(e.toString());
                    System.out.println("fucked up");
                }

            }
        };
        thread.start();
    }

    public void InitSensorData() {
        while (currentBoat == null) {
        }

        final SensorListener sensorListener = new SensorListener() {
            @Override
            public void receivedSensor(SensorData sensorData) {

                Data = sensorData;
                //data = Data.data;
                //channel = Data.channel;
                sensorV = Arrays.toString(Data.data);
                //sensorV = Integer.toString(Data.channel);
//                if(Data.toString()==null){
//                    sensorV = "No sensor value";
//                }
//                else {
//                    sensorV = Data.toString();
//                }
            }
        };

        //currentBoat.returnServer().getNumSensors(new FunctionObserver<Integer>() {
        // @Override
        //  public void completed(Integer numSensors) {
        //    System.out.println("Sensor num:" + numSensors);
        //  for (int channel = 0; channel < numSensors; ++channel) {
        currentBoat.returnServer().addSensorListener(3, sensorListener, new FunctionObserver<Void>() {
            @Override
            public void completed(Void aVoid) {
                System.out.println("Add Sensorlistener");
            }

            @Override
            public void failed(FunctionError functionError) {
                sensorV = "Failed to get sensor value";
            }
        });
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

}
//
//class
