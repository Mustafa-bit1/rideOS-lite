package com.fanconn.rideOS;

import android.os.Handler;
import android.os.Looper;

public class UptimeManager {

    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static String currentUptime = "0d 00:00:00";

    private static final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            long uptime = System.currentTimeMillis() - MyApp.appStartTime;

            long days = uptime / (1000 * 60 * 60 * 24);
            long hours = (uptime / (1000 * 60 * 60)) % 24;
            long minutes = (uptime / (1000 * 60)) % 60;
            long seconds = (uptime / 1000) % 60;

            currentUptime = String.format("%dd %02d:%02d:%02d",
                    days, hours, minutes, seconds);

            handler.postDelayed(this, 1000); // update every second
        }
    };

    // Call this once in Application or onStart
    public static void start() {
        handler.post(runnable);
    }

    public static String getUptime() {
        return currentUptime;
    }
}

