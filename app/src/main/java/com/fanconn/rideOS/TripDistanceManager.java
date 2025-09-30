package com.fanconn.rideOS;

import android.content.Context;
import android.content.SharedPreferences;

public class TripDistanceManager {

    private static final String PREF_NAME = "TripPrefs";
    private static final String KEY_TRIP_DISTANCE = "trip_distance";
    private static final String KEY_ODO_DISTANCE = "odo_distance";
    private static final String KEY_LAST_TIME = "last_update_time";
    private static final double RPM_THRESHOLD = 40.0;
    private double tripDistance; // in km
    private double odoDistance;  // in km (never resets)
    private long lastUpdateTime; // in ms
    private SharedPreferences preferences;
    public TripDistanceManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        tripDistance = Double.longBitsToDouble(preferences.getLong(KEY_TRIP_DISTANCE, Double.doubleToLongBits(0.0)));
        odoDistance = Double.longBitsToDouble(preferences.getLong(KEY_ODO_DISTANCE, Double.doubleToLongBits(0.0)));
        lastUpdateTime = preferences.getLong(KEY_LAST_TIME, System.currentTimeMillis());
    }

    public void updateTrip(double rpm) {
        long currentTime = System.currentTimeMillis();

        // Skip update if RPM is too low or app just started
        if (rpm < RPM_THRESHOLD) {
            lastUpdateTime = currentTime;
            saveLastTime();
            return;
        }

        double deltaTimeHours = (currentTime - lastUpdateTime) / 3600000.0;

        // Speed formula (convert rpm to km/h)
        double speed = (rpm * 60.0 * 1.46) / 3.3125 * 0.95 / 1000.0; // km/h
        double deltaDistance = speed * deltaTimeHours;

        // Update trip + odo
        tripDistance += deltaDistance;
        odoDistance += deltaDistance;

        lastUpdateTime = currentTime;

        saveTripData();
    }

    private void saveTripData() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(KEY_TRIP_DISTANCE, Double.doubleToLongBits(tripDistance));
        editor.putLong(KEY_ODO_DISTANCE, Double.doubleToLongBits(odoDistance));
        editor.putLong(KEY_LAST_TIME, lastUpdateTime);
        editor.apply();
    }

    private void saveLastTime() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(KEY_LAST_TIME, lastUpdateTime);
        editor.apply();
    }

    public double getTripDistance() {
        return tripDistance;
    }

    public double getOdoDistance() {
        return odoDistance;
    }

    public void resetTrip() {
        tripDistance = 0.0;
        lastUpdateTime = System.currentTimeMillis();
        saveTripData();
    }
}
