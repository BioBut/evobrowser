package com.evo.browser.utils;

import android.os.AsyncTask;
import android.util.Log;

import com.evo.browser.BuildConfig;
import com.evo.browser.activities.MainActivity;
import com.evo.browser.managers.SettingsManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

public class UpdateUtils extends AsyncTask<MainActivity, Void, Void> {

    private Exception exception;

    protected Void doInBackground(MainActivity... activity) {
        checkUpdates(activity[0]);
        return null;
    }

    Integer getLastAppVersion() {
        try {
            URL url = new URL("https://raw.githubusercontent.com/dmitrylaas/Evolution-Browser/master/ota/update.gradle");
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String str;
            while ((str = in.readLine()) != null) {
                int f = str.indexOf("versionCode");
                if (f != -1) {
                    str = str.substring(f + ("versionCode").length()).trim();
                    Log.d("Evolution", "Last release version: " + str);
                    return Integer.parseInt(str);
                }
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    void checkUpdates(final MainActivity activity) {
        final Integer lastAppVersion = getLastAppVersion();
        if (lastAppVersion == null)
            return;
        if (lastAppVersion <= BuildConfig.VERSION_CODE) {
            return;
        }
        String li = SettingsManager.get(activity, "LastIgnoredUpdateVersion");
        if (li != null) {
            Integer liInt = Integer.parseInt(li);
            if (liInt >= lastAppVersion)
                return;
        }
        activity.Update(lastAppVersion);
    }

}
