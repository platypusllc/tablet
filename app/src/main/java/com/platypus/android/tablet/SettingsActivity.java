package com.platypus.android.tablet;

import android.os.Bundle;
import android.app.Activity;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity {

    public static final String KEY_PREF_DEFAULT_IP = "pref_boat_ip";
    public static final String KEY_PREF_PORT = "pref_boat_port";
    public static final String KEY_PREF_COMMAND_RATE = "pref_command_update_rate";
    public static final String KEY_PREF_LAT = "pref_latitude";
    public static final String KEY_PREF_LON = "pref_longitude";
    public static final String KEY_PREF_SAVE_MAP = "pref_save_location";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

}
