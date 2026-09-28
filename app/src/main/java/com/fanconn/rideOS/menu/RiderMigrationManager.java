package com.fanconn.rideOS.menu;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.fanconn.rideOS.MetadataManager;

import java.util.Map;

public class RiderMigrationManager {

    private static final String TAG = "RiderMigrationManager";

    private static RiderMigrationManager instance;

    private final Context context;
    private final SharedPreferences sharedPreferences;

    private RiderMigrationManager(
            Context context,
            SharedPreferences sharedPreferences) {

        this.context = context.getApplicationContext();
        this.sharedPreferences = sharedPreferences;
    }

    public static synchronized RiderMigrationManager getInstance(
            Context context,
            SharedPreferences sharedPreferences) {

        if (instance == null) {
            instance = new RiderMigrationManager(
                    context,
                    sharedPreferences
            );
        }

        return instance;
    }

    /**
     * Migrates old riders that do not have a rider_id.
     *
     * Existing riders:
     *     rider_id exists -> skip
     *
     * Old riders:
     *     rider_id missing
     *          -> generate UUID
     *          -> save UUID locally
     *          -> send ADD through RiderMqttManager
     *
     * If MQTT is unavailable, RiderMqttManager will queue the
     * rider using RiderQueueManager.
     */
    public void migrateExistingRiders() {

        String vin = MetadataManager.get().getVin();

        if (vin == null
                || vin.trim().isEmpty()
                || vin.equals("UNKNOWN_VIN")) {

            Log.d(TAG, "VIN unavailable. Migration skipped.");
            return;
        }

        Map<String, ?> allEntries = sharedPreferences.getAll();

        if (allEntries == null || allEntries.isEmpty()) {
            Log.d(TAG, "No local rider data found.");
            return;
        }

        RiderMqttManager riderMqttManager =
                RiderMqttManager.getInstance(context);

        for (String key : allEntries.keySet()) {

            /*
             * Every rider has:
             *
             * user_<username>_age
             *
             * Use this to find existing riders.
             */
            if (!key.startsWith("user_")
                    || !key.endsWith("_age")) {

                continue;
            }

            String username = key.substring(
                    "user_".length(),
                    key.length() - "_age".length()
            );

            if (username.trim().isEmpty()) {
                continue;
            }

            String riderIdKey =
                    "user_" + username + "_rider_id";

            /*
             * Already migrated.
             */
            String existingRiderId =
                    sharedPreferences.getString(
                            riderIdKey,
                            null
                    );

            if (existingRiderId != null
                    && !existingRiderId.trim().isEmpty()) {

                Log.d(
                        TAG,
                        "Already migrated: " + username
                );

                continue;
            }

            try {

                /*
                 * Generate permanent UUID.
                 */
                String riderId =
                        riderMqttManager.generateRiderId();

                /*
                 * Read existing rider data.
                 */
                int age = sharedPreferences.getInt(
                        "user_" + username + "_age",
                        0
                );

                String phone = sharedPreferences.getString(
                        "user_" + username + "_phone",
                        ""
                );

                float topSpeed = getFloatPreference(
                        "user_" + username + "_maxspeed",
                        0f
                );

                String topTorque = sharedPreferences.getString(
                        "user_" + username + "_torque",
                        ""
                );

                int pickupLevel = sharedPreferences.getInt(
                        "user_" + username + "_pickup",
                        0
                );

                int brakingLevel = sharedPreferences.getInt(
                        "user_" + username + "_braking",
                        0
                );

                /*
                 * Save rider ID BEFORE sending.
                 *
                 * This prevents the same old rider from being
                 * migrated again on the next app launch.
                 */
                sharedPreferences.edit()
                        .putString(riderIdKey, riderId)
                        .apply();

                Log.d(
                        TAG,
                        "Migrating rider: "
                                + username
                                + " | ID: "
                                + riderId
                );

                /*
                 * RiderMqttManager handles:
                 *
                 * MQTT available
                 *      -> publish
                 *
                 * MQTT unavailable
                 *      -> RiderQueueManager
                 */
                riderMqttManager.addRider(
                        riderId,
                        username,
                        phone,
                        age,
                        topSpeed,
                        topTorque,
                        pickupLevel,
                        brakingLevel
                );

                Log.d(
                        TAG,
                        "Migration ADD queued/sent: "
                                + username
                );

            } catch (Exception e) {

                Log.e(
                        TAG,
                        "Migration failed for: " + username,
                        e
                );
            }
        }
    }

    /**
     * Safely reads a float preference.
     */
    private float getFloatPreference(
            String key,
            float defaultValue) {

        Object value =
                sharedPreferences
                        .getAll()
                        .get(key);

        if (value == null) {
            return defaultValue;
        }

        try {

            if (value instanceof Float) {
                return (Float) value;
            }

            if (value instanceof Double) {
                return ((Double) value).floatValue();
            }

            if (value instanceof Integer) {
                return ((Integer) value).floatValue();
            }

            if (value instanceof Long) {
                return ((Long) value).floatValue();
            }

            if (value instanceof String) {
                return Float.parseFloat((String) value);
            }

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Failed to read preference: " + key,
                    e
            );
        }

        return defaultValue;
    }
}