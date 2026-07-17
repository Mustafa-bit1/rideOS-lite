package com.fanconn.rideOS;

import android.content.Context;
import android.content.SharedPreferences;

public final class SocPreference {

    private static final String PREF_NAME = "vehicle_pref";
    private static final String KEY_BATTERY_LEVEL = "battery_level";

    private SocPreference() {
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Save RAW battery level received from CAN
    public static void saveBatteryLevel(Context context, int batteryLevel) {
        getPrefs(context)
                .edit()
                .putInt(KEY_BATTERY_LEVEL, batteryLevel)
                .apply();
    }

    // Read RAW battery level
    public static int getBatteryLevel(Context context) {
        return getPrefs(context)
                .getInt(KEY_BATTERY_LEVEL, -1);
    }
    public static boolean hasBatteryLevel(Context context) {
        return getBatteryLevel(context) >= 0;
    }

    public static void clear(Context context) {
        getPrefs(context)
                .edit()
                .remove(KEY_BATTERY_LEVEL)
                .apply();
    }
}
