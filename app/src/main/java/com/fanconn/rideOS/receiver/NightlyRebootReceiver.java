package com.fanconn.rideOS.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.util.Log;

import com.fanconn.rideOS.MainActivity;

public class NightlyRebootReceiver extends BroadcastReceiver {

    private static final String TAG = "NightlyReboot";

    @Override
    public void onReceive(Context context, Intent intent) {

        // Always schedule tomorrow's reboot check first.
        NightlyRebootScheduler.scheduleNext(context);

        // Check whether Holiday Mode is enabled.
        SharedPreferences prefs = context.getSharedPreferences(
                "feature_prefs", Context.MODE_PRIVATE);

        boolean isHolidayModeOn =
                prefs.getBoolean("holiday_mode_enabled", false);

        if (isHolidayModeOn) {
            Log.i(TAG,
                    "Reboot skipped: Holiday Mode is ON");
            return;
        }

        // Reboot only if the scooter is locked.
        if (!MainActivity.MainisActive) {

            Log.i(TAG,
                    "Holiday Mode is OFF and scooter is locked; rebooting");

            PowerManager pm = (PowerManager)
                    context.getSystemService(Context.POWER_SERVICE);

            if (pm != null) {
                pm.reboot(null);
            } else {
                Log.e(TAG, "PowerManager is unavailable");
            }

        } else {
            Log.i(TAG,
                    "Reboot skipped: scooter is active");
        }
    }
}