package com.fanconn.rideOS.mcuupdate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.fanconn.rideOS.menu.OS_UpdatesFragment;
import com.fanconn.rideOS.serialApplication;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class McuOtaExecutor {

    private static final String TAG = "McuOtaExecutor";
    private static final String TARGET_PKG = "com.fanconn.rideOSfw";

    // ---- OTA PREFS (from McuOtaVersionUtil) ----
    private static final String PREFS_OTA = "ota_prefs";
    private static final String KEY_LAST_MCU_VERSION = "last_mcu_version";

    /* =====================================================
     *  PUBLIC ENTRY
     * ===================================================== */

    public static void startMcuOta(Context context) {
        try {
            Context appCtx = context.getApplicationContext();
            String myPkg = appCtx.getPackageName();

            // 1️⃣ Launch MCU OTA app
            Intent intent = appCtx.getPackageManager()
                    .getLaunchIntentForPackage(TARGET_PKG);

            if (intent == null) {
                Log.e(TAG, "MCU OTA app not found");
                return;
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            appCtx.startActivity(intent);

            // 2️⃣ Force-stop current app
            Runtime.getRuntime().exec(new String[]{
                    "sh", "-c", "am force-stop " + myPkg
            });

            Log.i(TAG, "MCU OTA launched, current app force-stopped");

        } catch (Exception e) {
            Log.e(TAG, "Failed to start MCU OTA", e);
        }
    }

    /* =====================================================
     *  MCU VERSION LOGIC (MERGED)
     * ===================================================== */

    /**
     * Gets effective MCU version:
     * 1. Try live MCU
     * 2. Cache if valid
     * 3. Fallback to last stored version
     */
    public static int getEffectiveLocalMcuVersion(Context context) {

        int liveVersion = 0;

        try {
            if (serialApplication.getSerialPortManager() != null &&
                    serialApplication.getSerialPortManager().mad != null) {

                liveVersion =
                        serialApplication.getSerialPortManager()
                                .mad.getMcuVersions();
            }
        } catch (Exception e) {
            Log.e(TAG, "MCU version read failed", e);
        }

        if (liveVersion > 0) {
            cacheMcuVersion(context, liveVersion);
            return liveVersion;
        }

        return getCachedMcuVersion(context);
    }

    /**
     * Fetch MCU version from server
     */
    public static int fetchServerVersion(String versionUrl) {

        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            URL url = new URL(versionUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "HTTP error: " + connection.getResponseCode());
                return -1;
            }

            reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream())
            );

            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }

            JSONObject obj = new JSONObject(json.toString());
            return obj.getInt("version");

        } catch (Exception e) {
            Log.e(TAG, "Failed to fetch version", e);
            return -1;
        } finally {
            try {
                if (reader != null) reader.close();
            } catch (Exception ignored) {}
            if (connection != null) connection.disconnect();
        }
    }

    /* =====================================================
     *  INTERNAL HELPERS
     * ===================================================== */

    private static void cacheMcuVersion(Context context, int version) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_OTA, Context.MODE_PRIVATE);

        prefs.edit()
                .putInt(KEY_LAST_MCU_VERSION, version)
                .apply();
    }

    private static int getCachedMcuVersion(Context context) {
        return context.getSharedPreferences(PREFS_OTA, Context.MODE_PRIVATE)
                .getInt(KEY_LAST_MCU_VERSION, 0);
    }
}
