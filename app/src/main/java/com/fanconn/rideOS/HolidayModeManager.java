
package com.fanconn.rideOS;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.fanconn.rideOS.authen.LoginActivity;

public final class HolidayModeManager {

    private static final String TAG = "HolidayMode";

    private static final String PREF_NAME = "feature_prefs";
    private static final String KEY_HOLIDAY_MODE =
            "holiday_mode_enabled";
    private static final String KEY_CHARGING_STOPPED =
            "holiday_charging_stopped";

    private static final String STOP_CHARGING =
            "FFAA55010300151807E5EF0000000001000000";

    private static final String PREVENT_RIDING =
            "FFAA550103001518FF50E50000000000000000";

    private static final long INTERVAL_MS = 800L;
    private static final long RIDING_RETRY_DELAY_MS = 250L;

    private static volatile HolidayModeManager instance;

    private final SharedPreferences prefs;
    private final HandlerThread thread;
    private final Handler handler;

    // Accessed only on HolidayModeWorker.
    private boolean running = false;
    private boolean chargingStopped;

    private final Runnable monitorTask = new Runnable() {
        @Override
        public void run() {
            if (!running) {
                return;
            }

            try {
                monitorHolidayMode();
            } catch (Exception e) {
                Log.e(TAG, "Monitoring cycle failed", e);
            }

            if (running) {
                handler.postDelayed(this, INTERVAL_MS);
            }
        }
    };

    private HolidayModeManager(Context context) {
        Context appContext = context.getApplicationContext();

        prefs = appContext.getSharedPreferences(
                PREF_NAME, Context.MODE_PRIVATE);

        chargingStopped = prefs.getBoolean(
                KEY_CHARGING_STOPPED, false);

        thread = new HandlerThread("HolidayModeWorker");
        thread.start();

        handler = new Handler(thread.getLooper());

        Log.i(TAG, "Manager created; chargingStopped="
                + chargingStopped);
    }

    public static HolidayModeManager getInstance(Context context) {
        if (instance == null) {
            synchronized (HolidayModeManager.class) {
                if (instance == null) {
                    instance = new HolidayModeManager(context);
                }
            }
        }

        return instance;
    }

    public void start() {
        Log.i(TAG, "start() requested");

        handler.post(() -> {
            if (running) {
                Log.d(TAG, "Already running");
                return;
            }

            if (!prefs.getBoolean(KEY_HOLIDAY_MODE, false)) {
                Log.w(TAG, "Cannot start: Holiday Mode is disabled");
                return;
            }

            chargingStopped = prefs.getBoolean(
                    KEY_CHARGING_STOPPED, false);

            running = true;

            Log.i(TAG, "HOLIDAY MODE STARTED");

            monitorTask.run();
        });
    }

    public void stop() {
        Log.i(TAG, "stop() requested");

        handler.post(() -> {
            running = false;
            handler.removeCallbacks(monitorTask);

            Log.i(TAG, "HOLIDAY MODE STOPPED");
        });
    }

    private void monitorHolidayMode() {
        if (!prefs.getBoolean(KEY_HOLIDAY_MODE, false)) {
            Log.w(TAG, "Holiday Mode disabled; stopping monitor");

            running = false;
            handler.removeCallbacks(monitorTask);
            return;
        }

        // 1. Riding prevention: independent of SOC validity.
        boolean scooterOn = MainActivity.MainisActive;

        Log.d(TAG, "Scooter ON=" + scooterOn);

        if (scooterOn) {
            sendCommand(PREVENT_RIDING, "riding-prevention");

            // One additional attempt after 250 ms.
            // Recheck the conditions before retrying.
            handler.postDelayed(() -> {
                if (!running) {
                    return;
                }

                if (!prefs.getBoolean(KEY_HOLIDAY_MODE, false)) {
                    return;
                }

                if (!MainActivity.MainisActive) {
                    Log.d(TAG, "Riding retry skipped: scooter is OFF");
                    return;
                }

                sendCommand(
                        PREVENT_RIDING,
                        "riding-prevention-retry");
            }, RIDING_RETRY_DELAY_MS);

        } else {
            Log.d(TAG, "Riding command skipped: scooter is OFF");
        }

        // 2. Do not change charging decisions until SOC is valid.
        boolean validSoc =
                LoginActivity.hasReceivedValidSocinLoginPage();

        Log.d(TAG, "Valid SOC received=" + validSoc);

        if (!validSoc) {
            // Preserve the saved decision. If charging was already
            // stopped, keep sending the stop command during this gap.
            if (chargingStopped) {
                sendCommand(STOP_CHARGING,
                        "charging-stop-last-known-state");
            } else {
                Log.d(TAG,
                        "SOC invalid; retaining charging-allowed state");
            }

            return;
        }

        // 3. Read current battery data.
        SerialPortManager serialManager =
                SerialPortManager.getInstance();

        if (serialManager == null || serialManager.getHb3() == null) {
            Log.w(TAG, "Battery data unavailable");

            if (chargingStopped) {
                sendCommand(STOP_CHARGING,
                        "charging-stop-last-known-state");
            }

            return;
        }

        double soc = serialManager.getHb3().getBatteryLevel();
        double voltage =
                serialManager.getHb3().getbatvoltage() / 10.0;

        Log.d(TAG, "SOC=" + soc + "%, voltage=" + voltage);

        boolean validBatteryData =
                !Double.isNaN(soc)
                        && !Double.isInfinite(soc)
                        && soc >= 0.0
                        && soc <= 100.0
                        && !Double.isNaN(voltage)
                        && !Double.isInfinite(voltage)
                        && voltage > 0.0;

        if (!validBatteryData) {
            Log.w(TAG, "Invalid battery data; retaining last decision");

            if (chargingStopped) {
                sendCommand(STOP_CHARGING,
                        "charging-stop-last-known-state");
            }

            return;
        }

        // 4. Apply charging thresholds.
        boolean previousState = chargingStopped;

        if (soc >= 80.0) {
            chargingStopped = true;
            Log.i(TAG, "SOC >= 80%: stop charging");
        } else if (soc <= 20.0) {
            chargingStopped = false;
            Log.i(TAG, "SOC <= 20%: allow charging");
        } else {
            Log.d(TAG, "SOC between thresholds; retain previous decision");
        }

        // 5. Persist changes.
        if (previousState != chargingStopped) {
            prefs.edit()
                    .putBoolean(KEY_CHARGING_STOPPED, chargingStopped)
                    .apply();

            Log.i(TAG, "Charging state changed: "
                    + previousState + " -> " + chargingStopped);
        }

        // 6. Maintain the required charging state.
        if (chargingStopped) {
            sendCommand(STOP_CHARGING, "charging-stop");
        } else {
            Log.d(TAG, "Charging-stop command not required");
        }
    }

    private void sendCommand(String command, String description) {
        try {
            SerialPortManager serialManager =
                    SerialPortManager.getInstance();

            if (serialManager == null) {
                Log.e(TAG, description
                        + ": SerialPortManager is null");
                return;
            }

            Log.i(TAG, "TX attempt [" + description + "]: " + command);

            serialManager.VCUSendToMCU(command);

            Log.i(TAG, "VCUSendToMCU returned [" + description + "]");

        } catch (Exception e) {
            Log.e(TAG, "TX failed [" + description + "]", e);
        }
    }
}
