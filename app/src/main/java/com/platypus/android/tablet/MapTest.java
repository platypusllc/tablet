package com.platypus.android.tablet;

//import com.google.android.gms.maps.CameraUpdateFactory;
//import com.google.android.gms.maps.GoogleMap;

//import com.google.android.gms.maps.model.BitmapDescriptorFactory;
//import com.google.android.gms.maps.model.LatLng;
//import com.google.android.gms.maps.model.MarkerOptions;
//import com.google.android.gms.maps.*;
//import com.google.android.gms.maps.model.*;

import android.app.Activity;

import android.os.Bundle;
import android.os.Handler;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;

public class MapTest extends Activity
{

	static SeekBar thrust = null;
	static SeekBar rudder = null;
	static TextView thrustProgress = null;
	static TextView rudderProgress = null;

	static TextView loca = null;
	CheckBox autonomous = null;
//	static Marker boat;
	//static Marker boat2;
//	static LatLng pHollowStartingPoint = new LatLng((float) 40.436871, (float) -79.948825);
	static double lat;
	static double lon;
	static Handler handlerRudder = new Handler();
	public static int thrustCurrent;
	public static int rudderCurrent;
	public static double heading = Math.PI / 2.;
//	public static GoogleMap map;

	@Override
	protected void onCreate(Bundle savedInstanceState)
		{
			super.onCreate(savedInstanceState);
			setContentView(R.layout.maptest);

			// if (ConnectScreen.getBoatType() == false)
				{
					thrust = (SeekBar) this.findViewById(R.id.thrustBar);
					rudder = (SeekBar) this.findViewById(R.id.rudderBar);
					thrustProgress = (TextView) this.findViewById(R.id.getThrustProgress);
					rudderProgress = (TextView) this.findViewById(R.id.getRudderProgress);
					autonomous = (CheckBox) this.findViewById(R.id.Autonomous);
					loca = (TextView) this.findViewById(R.id.location);
					// Get a handle to the Map Fragment
//					map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
					rudder.setProgress(50);
					// thrust.setProgress(50);
//					map.moveCamera(CameraUpdateFactory.newLatLngZoom(pHollowStartingPoint, 15));
					//makeBoatMarker();
		
					if (ConnectScreen.simul == true)
						{
							//simulatedBoat();
						}
				}
		}

	public static void updateThrust()
		{
			thrust.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
				{
					@Override
					public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
						{
							// TODO Auto-generated method stub
							thrustCurrent = progress;
							thrustProgress.setText(String.valueOf(progress));
						} 	

					@Override
					public void onStartTrackingTouch(SeekBar seekBar)
						{
						}

					@Override
					public void onStopTrackingTouch(SeekBar seekBar)
						{
						}
				});
		}

	public static void updateRudder()
		{
			rudder.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
				{
					@Override
					public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
						{
							// TODO Auto-generated method stub
							rudderCurrent = progress;
							rudderProgress.setText(String.valueOf(progress));
						}

					@Override
					public void onStartTrackingTouch(SeekBar seekBar)
						{
						}

					@Override
					public void onStopTrackingTouch(SeekBar seekBar)
						{
						}
				});
		}

//	public static void makeBoatMarker()
//		{
//			map.setMyLocationEnabled(true);
//			//map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(ConnectScreen.boat.getPoseX(),ConnectScreen.boat.getPoseY()), 15));
//			map.animateCamera(CameraUpdateFactory.zoomTo(17.0f));
//
////			Handler handle = new Handler();
////			handle.post(new Runnable()
////				{
////					@Override
////					public void run()
////						{
//			//LatLng boatLoc = new LatLng(ConnectScreen.boat.getPoseX(),ConnectScreen.boat.getPoseY());
//			LatLng boatLoc = new LatLng(0588399.2171,4477511.8839);
//			boat2 = map.addMarker(new MarkerOptions()
//			.anchor(.5f, .5f).flat(true)
//			.rotation(270).title("Boat 1")
//			.snippet("IP Address: 192.168.1.1")
//			.position(boatLoc)
//			.flat(true)
//
//		// .icon(BitmapDescriptorFactory.fromResource(R.drawable.arrow))
//		);
//
//		}
//	public static void simulatedBoat()
//		{
//			updateThrust();
//			updateRudder();
//
//
//
//			boat2 = map.addMarker(new MarkerOptions()
//						.anchor(.5f, .5f).flat(true)
//						.rotation(270).title("Boat 1")
//						.snippet("IP Address: 192.168.1.1")
//						.position(pHollowStartingPoint)
//						.flat(true)
//					// .icon(BitmapDescriptorFactory.fromResource(R.drawable.arrow))
//					);
//
//			lat = pHollowStartingPoint.latitude;
//			lon = pHollowStartingPoint.longitude;
//			map.setMyLocationEnabled(true);
//			map.moveCamera(CameraUpdateFactory.newLatLngZoom(pHollowStartingPoint, 15));
//			map.animateCamera(CameraUpdateFactory.zoomTo(17.0f));
//
//
//			handlerRudder.post(new Runnable()
//				{
//					@Override
//					public void run()
//						{
//							heading -= (rudderCurrent - 50) * .001;
//							lat += Math.cos(heading) * (thrustCurrent - 50) * .0000001;
//							lon += Math.sin(heading) * (thrustCurrent) * .0000001;
//							boat2.setPosition(new LatLng(lat, lon));
//							loca.setText(lat + "\n" + lon);
//							boat2.setRotation((float) (heading * (180 / Math.PI)));
//							handlerRudder.postDelayed(this, 300);
//						}
//				});
//
//		}

}

