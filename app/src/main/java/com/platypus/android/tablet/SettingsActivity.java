package com.platypus.android.tablet;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.Activity;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import java.util.Map;

import static com.platypus.android.tablet.R.id.map;


public class SettingsActivity extends PreferenceActivity {

    public static final String KEY_PREF_DEFAULT_IP = "pref_boat_ip";
    public static final String KEY_PREF_PORT = "pref_boat_port";
    public static final String KEY_PREF_COMMAND_RATE = "pref_command_update_rate";
    public static final String KEY_PREF_LAT = "pref_latitude";
    public static final String KEY_PREF_LON = "pref_longitude";
    public static final String KEY_PREF_SAVE_MAP = "pref_save_location";

    public static final String KEY_PREF_THRUST_MIN = "pref_thrust_min";
    public static final String KEY_PREF_THRUST_MAX = "pref_thrust_MAX";
    public static final String KEY_PREF_RUDDER_MAX = "pref_rudder_MAX";
    public static final String KEY_PREF_RUDDER_MIN = "pref_rudder_min";

    public static final String KEY_PREF_PID_THRUST_P = "pref_pid_thrust_p";
    public static final String KEY_PREF_PID_THRUST_I= "pref_pid_thrust_i";
    public static final String KEY_PREF_PID_THRUST_D= "pref_pid_thrust_d";

    public static final String KEY_PREF_PID_RUDDER_P = "pref_pid_rudder_p";
    public static final String KEY_PREF_PID_RUDDER_I= "pref_pid_rudder_i";
    public static final String KEY_PREF_PID_RUDDER_D= "pref_pid_rudder_d";

    public static final String KEY_PREF_PID_LOW_THRUST_P = "pref_pid_low_thrust_p";
    public static final String KEY_PREF_PID_LOW_THRUST_I= "pref_pid_low_thrust_i";
    public static final String KEY_PREF_PID_LOW_THRUST_D= "pref_pid_low_thrust_d";

    public static final String KEY_PREF_PID_LOW_RUDDER_P = "pref_pid_low_rudder_p";
    public static final String KEY_PREF_PID_LOW_RUDDER_I= "pref_pid_low_rudder_i";
    public static final String KEY_PREF_PID_LOW_RUDDER_D= "pref_pid_low_rudder_d";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        Preference pref = findPreference("pref_category");
        SharedPreferences sharedpref = pref.getSharedPreferences();
        System.out.println("sharedpref: " + sharedpref.getString("pref_pid_low_rudder_p","0.4"));

        Map<String, ?> listOfPref = sharedpref.getAll();

        for (Map.Entry<String, ?> entry : listOfPref.entrySet())
        {
            System.out.println("Key : " + entry.getKey() + " Value : " + entry.getValue());
            Preference currentPref = findPreference(entry.getKey());
            if (currentPref instanceof EditTextPreference)
            {
                currentPref.setSummary(entry.getValue().toString());
            }
        }
    }
}
