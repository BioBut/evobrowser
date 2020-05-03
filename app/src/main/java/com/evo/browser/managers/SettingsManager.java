package com.evo.browser.managers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

public class SettingsManager {

    public static String get(Context mContext, String key) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
        String data = settings.getString(key, null);
        if (data == null)
            Log.d("SettingsManager", "No settings " + key + " is stored! ");
        else
            Log.d("SettingsManager", "Got settings " + key + " equal to " + data);
        return data;
    }

    @SuppressLint({"CommitPrefEdits", "ApplySharedPref"})
    public static void put(Context mContext, String key, String value) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);
        editor.commit();
    }
}
