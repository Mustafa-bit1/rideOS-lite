package com.fanconn.rideOS;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.fanconn.rideOS.telemetry.VehicleTelemetryManager;
import com.fanconn.rideOS.util.NumberUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TripStatsManager {
    private static final String KEY_YESTERDAY_DISTANCE = "yesterday_distance";

    private static final String TAG = "TripStats";

    private static final String PREF_NAME = "TripStats";

    // Current Trip
    private static final String KEY_CURRENT_TRIP_DISTANCE = "current_trip_distance";
    private static final String KEY_TRIP_RUNNING = "trip_running";
    private static final String KEY_START_TRIP = "start_trip";
    private static final String KEY_START_SOC = "start_soc";
    private static final String KEY_TOP_SPEED = "current_trip_top_speed";

    // Last Trip
    private static final String KEY_LAST_DISTANCE = "last_distance";
    private static final String KEY_LAST_TOP_SPEED = "last_top_speed";
    private static final String KEY_LAST_ENERGY_WH = "last_energy_wh";

    // Today
    private static final String KEY_TODAY_START_ODO = "today_start_odo";
    private static final String KEY_TODAY_DATE = "today_date";
    private static final String KEY_LAST_ODO = "last_odo";

    private static final double BATTERY_CAPACITY_KWH = 4.4;
    private static final double BATTERY_CAPACITY_WH =
            BATTERY_CAPACITY_KWH * 1000.0;

    private static final double MIN_VALID_TRIP_DISTANCE = 0.1; // 100 metres

    private static TripStatsManager instance;

    private final SharedPreferences prefs;
    private final VehicleTelemetryManager tm;

    private boolean tripRunning;
    private double startTripKm;
    private float startSoc;
    private int topSpeed;

    private TripStatsManager(Context context) {

        prefs = context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
        );

        tm = VehicleTelemetryManager.getInstance();

        tripRunning = prefs.getBoolean(
                KEY_TRIP_RUNNING,
                false
        );

        startTripKm = Double.longBitsToDouble(
                prefs.getLong(
                        KEY_START_TRIP,
                        Double.doubleToLongBits(0.0)
                )
        );

        startSoc = prefs.getFloat(
                KEY_START_SOC,
                0.0f
        );

        topSpeed = prefs.getInt(
                KEY_TOP_SPEED,
                0
        );

        checkNewDay();
    }

    public static synchronized TripStatsManager getInstance(Context context) {

        if (instance == null) {
            instance = new TripStatsManager(
                    context.getApplicationContext()
            );
        }

        return instance;
    }

    public static TripStatsManager getInstance() {

        if (instance == null) {
            throw new IllegalStateException(
                    "TripStatsManager not initialized."
            );
        }

        return instance;
    }

    /**
     * Call ONLY once after successful unlock.
     */
    public synchronized void startTrip() {

        if (tripRunning) {
            Log.d(TAG, "Trip already running.");
            return;
        }

        checkNewDay();

        /*
         * Vehicle telemetry may not be available immediately
         * when unlock happens.
         *
         * Therefore never directly call:
         *
         * tm.getTripKm()
         * tm.getSoc().floatValue()
         *
         * without checking for null.
         */

        Double currentTripKm = tm.getTripKm();
        Double currentSoc = tm.getSoc();

        if (currentTripKm == null) {
            Log.w(TAG,
                    "Cannot start trip: TripKm is null.");

            return;
        }

        if (currentSoc == null) {
            Log.w(TAG,
                    "Cannot start trip: SOC is null.");

            return;
        }

        startTripKm = currentTripKm;
        startSoc = currentSoc.floatValue();

        tripRunning = true;
        topSpeed = 0;

        prefs.edit()
                .putBoolean(KEY_TRIP_RUNNING, true)
                .putLong(
                        KEY_START_TRIP,
                        Double.doubleToLongBits(startTripKm)
                )
                .putFloat(KEY_START_SOC, startSoc)
                .putInt(KEY_TOP_SPEED, 0)
                .apply();

        Log.d(
                TAG,
                "START TRIP | startTripKm="
                        + startTripKm
                        + " | startSoc="
                        + startSoc
        );
    }

    /**
     * Called continuously while CAN data is received.
     */
    public synchronized void update(int currentSpeed) {

        checkNewDay();

        /*
         * Store latest odometer only when it is available.
         */
        Double odometer = tm.getOdometer();

        if (odometer != null) {

            prefs.edit()
                    .putFloat(
                            KEY_LAST_ODO,
                            odometer.floatValue()
                    )
                    .apply();
        }

        if (!tripRunning) {
            return;
        }

        /*
         * Update top speed.
         */
        if (currentSpeed > topSpeed) {

            topSpeed = currentSpeed;

            prefs.edit()
                    .putInt(
                            KEY_TOP_SPEED,
                            topSpeed
                    )
                    .apply();
        }

        /*
         * Get current telemetry safely.
         */
        Double currentTripKmValue = tm.getTripKm();
        Double currentSocValue = tm.getSoc();

        if (currentTripKmValue == null) {

            Log.w(
                    TAG,
                    "Trip update skipped: TripKm is null."
            );

            return;
        }

        if (currentSocValue == null) {

            Log.w(
                    TAG,
                    "Trip update skipped: SOC is null."
            );

            return;
        }

        /*
         * Calculate trip distance.
         */
        double currentTripDistance =
                currentTripKmValue - startTripKm;

        if (currentTripDistance < 0) {
            currentTripDistance = 0;
        }

        float trip = round2(currentTripDistance);

        /*
         * Calculate energy consumed.
         */
        float currentSoc = currentSocValue.floatValue();

        float socUsed = startSoc - currentSoc;

        if (socUsed < 0) {
            socUsed = 0;
        }

        float energyWh = round2(
                (socUsed / 100f)
                        * (float) BATTERY_CAPACITY_WH
        );

        SharedPreferences.Editor editor =
                prefs.edit();

        /*
         * Always keep current trip distance updated.
         */
        editor.putFloat(
                KEY_CURRENT_TRIP_DISTANCE,
                trip
        );

        /*
         * Only save as a valid last trip when
         * distance reaches the minimum threshold.
         */
        if (trip >= MIN_VALID_TRIP_DISTANCE) {

            editor.putFloat(
                    KEY_LAST_DISTANCE,
                    trip
            );

            editor.putInt(
                    KEY_LAST_TOP_SPEED,
                    topSpeed
            );

            editor.putFloat(
                    KEY_LAST_ENERGY_WH,
                    energyWh
            );
        }

        editor.apply();

//        Log.d(
//                TAG,
//                "UPDATE | distance="
//                        + trip
//                        + " km"
//                        + " | speed="
//                        + topSpeed
//                        + " km/h"
//                        + " | energy="
//                        + energyWh
//                        + " Wh"
//        );
    }

    public double getCurrentTripDistance() {

        return prefs.getFloat(
                KEY_CURRENT_TRIP_DISTANCE,
                0.0f
        );
    }

    /**
     * Call ONLY once after successful lock.
     */
    public synchronized void endTrip() {

        if (!tripRunning) {
            return;
        }

        Double endTripKmValue = tm.getTripKm();

        /*
         * If telemetry is unavailable during lock,
         * don't crash the application.
         */
        if (endTripKmValue == null) {

            Log.w(
                    TAG,
                    "Cannot end trip: TripKm is null."
            );

            /*
             * Keep tripRunning state so that the next
             * update can still process the trip.
             */
            return;
        }

        double endTripKm = endTripKmValue;

        double distance =
                endTripKm - startTripKm;

        if (distance < 0) {
            distance = 0;
        }

        if (distance < MIN_VALID_TRIP_DISTANCE) {

            Log.d(
                    TAG,
                    "Trip too short: "
                            + distance
                            + " km"
            );

            tripRunning = false;

            prefs.edit()
                    .putBoolean(
                            KEY_TRIP_RUNNING,
                            false
                    )
                    .remove(KEY_CURRENT_TRIP_DISTANCE)
                    .remove(KEY_START_TRIP)
                    .remove(KEY_START_SOC)
                    .remove(KEY_TOP_SPEED)
                    .apply();

            return;
        }

        /*
         * Save final trip values.
         */
        float finalDistance = round2(distance);

        prefs.edit()
                .putFloat(
                        KEY_LAST_DISTANCE,
                        finalDistance
                )
                .putInt(
                        KEY_LAST_TOP_SPEED,
                        topSpeed
                )
                .remove(KEY_CURRENT_TRIP_DISTANCE)
                .remove(KEY_START_TRIP)
                .remove(KEY_START_SOC)
                .remove(KEY_TOP_SPEED)
                .putBoolean(
                        KEY_TRIP_RUNNING,
                        false
                )
                .apply();

        tripRunning = false;

        Log.d(
                TAG,
                "END TRIP"
                        + " | endTripKm="
                        + endTripKm
                        + " | distance="
                        + finalDistance
                        + " km"
                        + " | topSpeed="
                        + topSpeed
        );
    }

    /**
     * Automatically resets today's ODO after midnight.
     */
    private void checkNewDay() {

        String today = new SimpleDateFormat(
                "yyyyMMdd",
                Locale.getDefault()
        ).format(new Date());

        String savedDate = prefs.getString(
                KEY_TODAY_DATE,
                ""
        );

        if (!today.equals(savedDate)) {

            Double odometer = tm.getOdometer();

            /*
             * Prefer the last known stored odometer.
             * If it doesn't exist, use current telemetry.
             * If both are unavailable, use 0.
             */
            float defaultOdo = 0.0f;

            if (odometer != null) {
                defaultOdo = odometer.floatValue();
            }

            float lastKnownOdo = prefs.getFloat(
                    KEY_LAST_ODO,
                    defaultOdo
            );

            float yesterdayDistance = lastKnownOdo - prefs.getFloat(KEY_TODAY_START_ODO, lastKnownOdo);
            if (yesterdayDistance < 0)
                yesterdayDistance = 0;


            prefs.edit()
                    .putFloat(KEY_YESTERDAY_DISTANCE, round2(yesterdayDistance))
                    .putString(KEY_TODAY_DATE, today)
                    .putFloat(KEY_TODAY_START_ODO, lastKnownOdo)
                    .apply();

            Log.d(
                    TAG,
                    "New day | todayStartOdo="
                            + lastKnownOdo
            );
        }
    }
    public double getYesterdayDistance() {
                return NumberUtils.truncate1(
                                prefs.getFloat(KEY_YESTERDAY_DISTANCE, 0));
            }
    public double getTodayDistance() {

        /*
         * First try the stored starting ODO.
         */
        float startOdo = prefs.getFloat(
                KEY_TODAY_START_ODO,
                Float.NaN
        );

        /*
         * Get current odometer safely.
         */
        Double currentOdoValue = tm.getOdometer();

        if (currentOdoValue == null) {

            Log.w(
                    TAG,
                    "Today's distance unavailable: Odometer is null."
            );

            return 0.0;
        }

        /*
         * If today's starting ODO doesn't exist yet,
         * initialize it using the current ODO.
         */
        if (Float.isNaN(startOdo)) {

            startOdo = currentOdoValue.floatValue();

            prefs.edit()
                    .putFloat(
                            KEY_TODAY_START_ODO,
                            startOdo
                    )
                    .apply();

            return 0.0;
        }

        double today =
                currentOdoValue - startOdo;

        if (today < 0) {
            today = 0;
        }

        return NumberUtils.truncate1(
                round2(today)
        );
    }

    public double getLastTripDistance() {

        return NumberUtils.truncate1(
                prefs.getFloat(
                        KEY_LAST_DISTANCE,
                        0.0f
                )
        );
    }

    public int getLastTripTopSpeed() {

        return prefs.getInt(
                KEY_LAST_TOP_SPEED,
                0
        );
    }

    /**
     * Returns energy in Wh.
     */
    public double getLastTripEnergyWh() {

        return NumberUtils.truncate1(
                prefs.getFloat(
                        KEY_LAST_ENERGY_WH,
                        0.0f
                )
        );
    }

    /**
     * Returns energy in kWh.
     */
    public double getLastTripEnergyKWh() {

        return NumberUtils.round(
                getLastTripEnergyWh() / 1000.0,
                2
        );
    }

    public boolean isTripRunning() {

        return tripRunning;
    }

    private float round2(double value) {

        return (float) (
                Math.round(value * 100.0) / 100.0
        );
    }

    private double round3(double value) {

        return Math.round(value * 1000.0) / 1000.0;
    }
}