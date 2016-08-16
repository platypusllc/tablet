package com.platypus.android.tablet;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.platypus.crw.SensorListener;
import com.platypus.crw.data.SensorData;

import java.util.Arrays;

/**
 * Created by zeshengxi on 2/1/16.
 */
public class fragment_sensor extends Fragment {

    TextView sensorData1 = null;
    TextView sensorData2 = null;
    TextView sensorData3 = null;

    TextView sensorType1 = null;
    TextView sensorType2 = null;
    TextView sensorType3 = null;
    boolean sensorReady =false;
    TextView battery = null;
    TextView Title = null;

    ToggleButton sensorvalueButton = null;


    private static final String logTag = fragment_sensor.class.getSimpleName();
    private SensorListener sl;
    SensorData Data;
    private String sensorV = "Loading...";
    @Override
    public void onCreate(Bundle savedInstanceState){
        //*******************************************************************************
        //  Initialize Sensorlistener
        //*******************************************************************************
        sl = new SensorListener() {
            @Override
            public void receivedSensor(SensorData sensorData) {
                Data = sensorData;

                sensorV = Arrays.toString(Data.data);
                sensorV = sensorV.substring(1, sensorV.length()-1);
                sensorReady = true;
                //Log.i("Platypus","Get sensor Data");
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sensor,container, false);
        sensorData1 = (TextView) view.findViewById(R.id.SValue1);
        sensorData2 = (TextView) view.findViewById(R.id.SValue2);
        sensorData3 = (TextView) view.findViewById(R.id.SValue3);
        sensorType1 = (TextView) view.findViewById(R.id.sensortype1);
        sensorType2 = (TextView) view.findViewById(R.id.sensortype2);
        sensorType3 = (TextView) view.findViewById(R.id.sensortype3);
        sensorvalueButton = (ToggleButton) view.findViewById(R.id.SensorStart);
        sensorvalueButton.setClickable(sensorReady);
        sensorvalueButton.setTextColor(Color.GRAY);

        return view;
    }
}
