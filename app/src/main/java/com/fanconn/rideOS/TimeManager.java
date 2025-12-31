package com.fanconn.rideOS;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimeManager {
    private static final String PREF_NAME = "time_prefs";
    private static final String KEY_OFFSET = "time_offset";
    private static TimeManager instance;
    private final SharedPreferences prefs;
    private TimeManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    // Singleton instance
    public static synchronized TimeManager getInstance(Context context) {
        if (instance == null) {
            instance = new TimeManager(context);
        }
        return instance;
    }

    /**
     * Call this ONLY when internet is available (NTP / server time)
     */

    public void updateTimeOffset(long networkTimeMillis) {
        long systemTime = System.currentTimeMillis();
        long offset = networkTimeMillis - systemTime;
        prefs.edit().putLong(KEY_OFFSET, offset).apply();
    }

    /**
     * Core corrected time (use this everywhere)
     */
    public long getCurrentTimeMillis() {
        long offset = prefs.getLong(KEY_OFFSET, 0);
        return System.currentTimeMillis() + offset;
    }

    /**
     * Convenience method for UI
     */
    public String getFormattedTime(String pattern) {
        SimpleDateFormat sdf =
                new SimpleDateFormat(pattern, Locale.getDefault());
        return sdf.format(new Date(getCurrentTimeMillis()));
    }
}
