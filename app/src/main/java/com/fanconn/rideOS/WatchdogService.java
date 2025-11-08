package com.fanconn.rideOS;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;

public class WatchdogService extends Service {

    private final Handler watchdogHandler = new Handler();
    private final Runnable watchdogRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                // 1️⃣ Cleanup resources before restart
                if (UIUpdater.getInstance() != null) {
                    UIUpdater.getInstance().release();
                }

                // 2️⃣ Restart the app silently
                Context ctx = getApplicationContext();
                Intent restartIntent = ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName());

                if (restartIntent != null) {
                    restartIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    PendingIntent pendingIntent = PendingIntent.getActivity(
                            ctx,
                            0,
                            restartIntent,
                            PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    );

                    AlarmManager alarmManager = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
                    alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            System.currentTimeMillis() + 2000, // restart after 2 seconds
                            pendingIntent
                    );

                    System.exit(0); // Kill process
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Schedule restart every 24 hours
        watchdogHandler.postDelayed(watchdogRunnable, 1 * 30 * 60 * 1000);
        return START_STICKY; // Restart service if killed
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not binding to UI
    }
}
