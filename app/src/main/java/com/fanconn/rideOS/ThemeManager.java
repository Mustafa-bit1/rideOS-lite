package com.fanconn.rideOS;

import android.content.Context;
import android.content.SharedPreferences;

public class ThemeManager {

    private static final String PREF = "theme_pref";
    private static final String KEY_THEME = "is_light";

    public static void setLightMode(Context context, boolean isLight) {

        SharedPreferences prefs =
                context.getSharedPreferences(PREF, Context.MODE_PRIVATE);

        prefs.edit().putBoolean(KEY_THEME, isLight).apply();
    }

    public static boolean isLightMode(Context context) {

        SharedPreferences prefs =
                context.getSharedPreferences(PREF, Context.MODE_PRIVATE);

        return prefs.getBoolean(KEY_THEME, false);
    }
}
