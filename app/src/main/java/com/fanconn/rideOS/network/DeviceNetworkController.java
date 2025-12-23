package com.fanconn.rideOS.network;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.lang.reflect.Method;

public class DeviceNetworkController {

    private static final String TAG = "DeviceNetworkController";

    /* ================= MOBILE DATA ================= */

    public static boolean setMobileData(Context context, boolean enabled) {
        try {
            TelephonyManager tm =
                    (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            if (tm == null) {
                Log.e(TAG, "❌ TelephonyManager is null");
                return false;
            }

            Method setDataEnabled =
                    TelephonyManager.class.getDeclaredMethod(
                            "setDataEnabled",
                            boolean.class
                    );

            setDataEnabled.setAccessible(true);
            setDataEnabled.invoke(tm, enabled);

            Log.d(TAG, "📶 Mobile data set to: " + enabled);
            return true;

        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to toggle mobile data", e);
            return false;
        }
    }

    /* ================= AIRPLANE MODE ================= */

    public static boolean setAirplaneMode(Context context, boolean enabled) {
        try {
            Settings.Global.putInt(
                    context.getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON,
                    enabled ? 1 : 0
            );

            Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            intent.putExtra("state", enabled);
            context.sendBroadcast(intent);

            Log.d(TAG, "✈ Airplane mode set to: " + enabled);
            return true;

        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to toggle airplane mode", e);
            return false;
        }
    }
    public static void enableAirplaneModeWithAutoOff(Context context, int delayMs) {

        boolean enabled = setAirplaneMode(context, true);
        if (!enabled) return;

        //Log.d("AIRPLANE", "✈ Airplane mode enabled, will auto-disable in " + delayMs + "ms");

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            setAirplaneMode(context, false);
           // Log.d("AIRPLANE", "✈ Airplane mode auto-disabled");
        }, delayMs);
    }


    /* ================= DEVICE RESTART ================= */

    /**
     * Reboots device AFTER delay.
     * ACK must be sent BEFORE calling this.
     */
    public static boolean restartWithDelay(Context context, long delayMillis) {
        try {
            Log.d(TAG, "🔄 Restart scheduled in " + delayMillis + " ms");

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    PowerManager pm =
                            (PowerManager) context.getSystemService(Context.POWER_SERVICE);

                    if (pm == null) {
                        Log.e(TAG, "❌ PowerManager is null");
                        return;
                    }

                    Log.w(TAG, "⚠ REBOOTING DEVICE NOW");
                    pm.reboot(null);

                } catch (Exception e) {
                    Log.e(TAG, "❌ Reboot failed", e);
                }
            }, delayMillis);

            return true;

        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to schedule reboot", e);
            return false;
        }
    }
}
