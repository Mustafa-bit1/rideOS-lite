package com.fanconn.rideOS;

public final class UptimeManager {

    private UptimeManager() {
    }

    public static String getUptime() {

        long uptimeMillis =
                System.currentTimeMillis() - MyApp.appStartTime;

        if (uptimeMillis < 0) {
            uptimeMillis = 0;
        }

        long totalSeconds = uptimeMillis / 1000;

        long days = totalSeconds / 86400;
        long hours = (totalSeconds / 3600) % 24;
        long minutes = (totalSeconds / 60) % 60;
        long seconds = totalSeconds % 60;

        return String.format(
                "%dd %02d:%02d:%02d",
                days,
                hours,
                minutes,
                seconds
        );
    }
}