package com.fanconn.rideOS.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import com.fanconn.rideOS.MainActivity;

public class NightlyRebootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        // Schedule tomorrow first.
        NightlyRebootScheduler.scheduleNext(context);

        // Reboot only if scooter is locked.
        if (!MainActivity.MainisActive) {

            PowerManager pm =
                    (PowerManager) context.getSystemService(Context.POWER_SERVICE);

            if (pm != null) {
                pm.reboot(null);
            }
        }
    }
}