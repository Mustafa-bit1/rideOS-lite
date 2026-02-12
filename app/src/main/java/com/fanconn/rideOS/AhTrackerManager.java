package com.fanconn.rideOS;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class AhTrackerManager {

    private static AhTrackerManager instance;   //  Singleton instance

    private float rideOSInAh;
    private float rideOSOutAh;
    private float prevBMSInAh;
    private float prevBMSOutAh;

    private static final float BATTERY_CAPACITY = 60f;   // Total pack capacity (Ah)
    private final SharedPreferences prefs;

    //  Private constructor (Singleton)
    private AhTrackerManager(Context ctx) {
        prefs = ctx.getSharedPreferences("AH_DATA", Context.MODE_PRIVATE);

        rideOSInAh = prefs.getFloat("rideOSInAh", 0f);
        rideOSOutAh = prefs.getFloat("rideOSOutAh", 0f);
        prevBMSInAh = prefs.getFloat("prevBMSInAh", 0f);
        prevBMSOutAh = prefs.getFloat("prevBMSOutAh", 0f);
    }

    //  Public getInstance method (SAFE)
    public static synchronized AhTrackerManager getInstance(Context ctx) {
        if (instance == null) {
            instance = new AhTrackerManager(ctx.getApplicationContext());
        }
        return instance;
    }

    // Update Charging Ah
    public void updateChargingAh(float bmsInAh) {
        Log.d("AH_TRACKER", "ChargingAh input = " + bmsInAh);

        if (bmsInAh < prevBMSInAh) {
            // BMS counter reset happened
            rideOSInAh += bmsInAh;
        } else {
            // Normal increase
            rideOSInAh += (bmsInAh - prevBMSInAh);
        }

        prevBMSInAh = bmsInAh;
        save();
    }

    //  Update Discharging Ah
    public void updateDischargingAh(float bmsOutAh) {
        Log.d("AH_TRACKER", "DischargingAh input = " + bmsOutAh);

        if (bmsOutAh < prevBMSOutAh) {
            rideOSOutAh += bmsOutAh;
        } else {
            rideOSOutAh += (bmsOutAh - prevBMSOutAh);
        }

        prevBMSOutAh = bmsOutAh;
        save();
    }

    // Remaining Ah in battery
    public float getRemainingAh() {
        float rem = rideOSInAh - rideOSOutAh;
        float capacity = getBatteryCapacity();

        // Physical limits
        if (rem < 0f) rem = 0f;
        if (rem > capacity) rem = capacity;   //HARD LOCK

        Log.d("AH_TRACKER", "RemainingAh(clamped) = " + rem);
        return rem;
    }


    // SOC (%) value
    public float getSOC() {
        float rem = getRemainingAh();
        float capacity = getBatteryCapacity();

        // Safety checks
        if (capacity <= 0f) {
            Log.e("AH_TRACKER", "Invalid battery capacity: " + capacity);
            return 0f;
        }

        if (rem < 0f) {
            //Log.w("AH_TRACKER", "Remaining Ah is negative: " + rem);
            rem = 0f;
        }

        float soc = (rem / capacity) * 100f;

        // Clamp SOC between 0–100
        if (soc < 0f) soc = 0f;
        else if (soc > 100f) soc = 100f;
//
//        Log.d("AH_TRACKER",
//                "Model=" + MetadataManager.get().getModel()
//                        + " RemAh=" + rem
//                        + " Capacity=" + capacity
//                        + " SOC=" + soc);

        return soc;
    }

    private float getBatteryCapacity() {
        String model = MetadataManager.get().getModel();

        if (model == null) return 60f; // default safety

        switch (model) {
            case "nx100 Pro":
                return 60f;

            case "nx100 Max":
                return 90f;

            default:
                return 60f; // fallback
        }
    }

    // Save to SharedPreferences
    private void save() {
        prefs.edit()
                .putFloat("rideOSInAh", rideOSInAh)
                .putFloat("rideOSOutAh", rideOSOutAh)
                .putFloat("prevBMSInAh", prevBMSInAh)
                .putFloat("prevBMSOutAh", prevBMSOutAh)
                .apply();
    }

    //  RESET everything fully
    public void resetAll() {
        rideOSInAh = 0f;
        rideOSOutAh = 0f;
        prevBMSInAh = 0f;
        prevBMSOutAh = 0f;

        prefs.edit().clear().apply();   // clear all stored AH values
        save();                          // apply new cleared values

        Log.d("AH_TRACKER", "RESET completed. All AH values cleared.");
    }
}
