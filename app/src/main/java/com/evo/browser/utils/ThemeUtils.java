package com.evo.browser.utils;

import androidx.preference.PreferenceManager;
import com.evo.browser.App;
import com.evo.browser.R;

public class ThemeUtils {

    public static int getCurrentTheme() {
        if (PreferenceManager.getDefaultSharedPreferences(App.getInstance().getApplicationContext()).getBoolean("night", false)) {
            return R.style.AppThemeDark;
        } else {
            return R.style.AppTheme;
        }
    }
    public static int getCurrentBottomTheme() {
        if (PreferenceManager.getDefaultSharedPreferences(App.getInstance().getApplicationContext()).getBoolean("night", false)) {
            return R.style.BottomSheetDark;
        } else {
            return R.style.BottomSheetLight;
        }
    }
}