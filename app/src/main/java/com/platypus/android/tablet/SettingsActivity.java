package com.platypus.android.tablet;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;

import java.util.Map;

import static com.platypus.android.tablet.R.id.map;

/*
* Only things that should be automatically saved from the teleop panel activity when its closed:
* IP Address
* Port
* Map Center location (where map autopans to on next connect)
* */

public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static TeleOpPanel tpanel;
    public static final void set_TeleOpPanel(TeleOpPanel tpanel_) { tpanel = tpanel_;}

    public static final String KEY_PREF_VEHICLE_TYPE = "pref_vehicle_type";
    public static final String KEY_PREF_SPEED = "pref_vehicle_speed";

    public static final String KEY_PREF_IP = "pref_boat_ip";
    public static final String KEY_PREF_PORT = "pref_boat_port";
    public static final String KEY_PREF_COMMAND_RATE = "pref_command_update_rate";
    public static final String KEY_PREF_LAT = "pref_latitude";
    public static final String KEY_PREF_LON = "pref_longitude";
    public static final String KEY_PREF_SAVE_MAP = "pref_save_location";

    public static final String KEY_PREF_THRUST_MIN = "pref_thrust_min";
    public static final String KEY_PREF_THRUST_MAX = "pref_thrust_max";

    public static final String KEY_PREF_RUDDER_MIN = "pref_rudder_min";
    public static final String KEY_PREF_RUDDER_MAX = "pref_rudder_max";

    public static final String KEY_PREF_PID_THRUST_P = "pref_pid_thrust_p";
    public static final String KEY_PREF_PID_THRUST_I= "pref_pid_thrust_i";
    public static final String KEY_PREF_PID_THRUST_D= "pref_pid_thrust_d";

    public static final String KEY_PREF_PID_RUDDER_P = "pref_pid_rudder_p";
    public static final String KEY_PREF_PID_RUDDER_I= "pref_pid_rudder_i";
    public static final String KEY_PREF_PID_RUDDER_D= "pref_pid_rudder_d";

    public static final String KEY_PREF_VOLTAGE_ALERT = "pref_voltage_alert";
    public static final String KEY_PREF_VOLTAGE_ALARM = "pref_voltage_alarm";
    public static final String KEY_PREF_SNOOZE = "pref_snooze";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        ListView v = getListView();
        Button restore_defaults_button = new Button(this);
        restore_defaults_button.setText("Restore defaults  (long press)");
        restore_defaults_button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = sharedPref.edit();
                String vehicle_type = sharedPref.getString(KEY_PREF_VEHICLE_TYPE, "PROP");
                switch (vehicle_type) {
                    case "PROP":
                        editor.putString(KEY_PREF_PID_THRUST_P, Double.toString(0.2));
                        editor.putString(KEY_PREF_PID_THRUST_I, Double.toString(0.0));
                        editor.putString(KEY_PREF_PID_THRUST_D, Double.toString(0.0));
                        editor.putString(KEY_PREF_PID_RUDDER_P, Double.toString(1.0));
                        editor.putString(KEY_PREF_PID_RUDDER_I, Double.toString(0.0));
                        editor.putString(KEY_PREF_PID_RUDDER_D, Double.toString(0.2));
                        editor.putString(KEY_PREF_THRUST_MIN, Double.toString(-1.0));
                        editor.putString(KEY_PREF_THRUST_MAX, Double.toString(0.3));
                        editor.putString(KEY_PREF_RUDDER_MIN,Double.toString(-1.0));
                        editor.putString(KEY_PREF_RUDDER_MAX,Double.toString(1.0));
                        break;

                    case "AIR":
                        editor.putString(KEY_PREF_PID_THRUST_P, Double.toString(0.4));
                        editor.putString(KEY_PREF_PID_THRUST_I, Double.toString(0.0));
                        editor.putString(KEY_PREF_PID_THRUST_D, Double.toString(0.0));
                        editor.putString(KEY_PREF_PID_RUDDER_P, Double.toString(0.75));
                        editor.putString(KEY_PREF_PID_RUDDER_I, Double.toString(0.0));
                        editor.putString(KEY_PREF_PID_RUDDER_D, Double.toString(0.9));
                        editor.putString(KEY_PREF_THRUST_MIN, Double.toString(-1.0));
                        editor.putString(KEY_PREF_THRUST_MAX, Double.toString(0.7));
                        editor.putString(KEY_PREF_RUDDER_MIN,Double.toString(-1.0));
                        editor.putString(KEY_PREF_RUDDER_MAX,Double.toString(1.0));
                        break;

                    default:
                        break;
                }
                editor.putString(KEY_PREF_COMMAND_RATE, "500");
                editor.putString(KEY_PREF_VOLTAGE_ALERT, "15.0");
                editor.putString(KEY_PREF_VOLTAGE_ALARM, "14.0");
                editor.putString(KEY_PREF_SNOOZE, "5");
                editor.putBoolean(KEY_PREF_SAVE_MAP, true);
                editor.apply();
                editor.commit();
                return false;
            }
        });

        v.addHeaderView(restore_defaults_button);

        Preference pref = findPreference("pref_category");
        SharedPreferences sharedpref = PreferenceManager.getDefaultSharedPreferences(this);
        Map<String, ?> listOfPref = sharedpref.getAll();
        for (Map.Entry<String, ?> entry : listOfPref.entrySet())
        {
            System.out.println("key found : " + entry.getKey());
            Preference currentPref = findPreference(entry.getKey());
            if (currentPref instanceof EditTextPreference)
            {
                currentPref.setSummary(entry.getValue().toString());
                System.out.println("entry: " + entry.getValue().toString());
            }
        }

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
        Preference pref = findPreference(key);
        if (key.equalsIgnoreCase(KEY_PREF_SAVE_MAP))
        {
            boolean state = sharedPreferences.getBoolean(key, true);
            CheckBoxPreference cbpref = (CheckBoxPreference) pref;
            cbpref.setChecked(state);
        }
        else
        {
            pref.setSummary(sharedPreferences.getString(key,"default"));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

}

