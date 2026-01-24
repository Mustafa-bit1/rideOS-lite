package com.fanconn.rideOS.network;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public final class AppRestartManager {

    private AppRestartManager() {} // no instances

    public static void relaunchAppFresh(Context context) {

        Context appContext = context.getApplicationContext();

        Intent intent = appContext.getPackageManager()
                .getLaunchIntentForPackage(appContext.getPackageName());

        if (intent == null) return;

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        PendingIntent pendingIntent = PendingIntent.getActivity(
                appContext,
                0,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager =
                (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);

        alarmManager.setExact(
                AlarmManager.RTC,
                System.currentTimeMillis() + 300,
                pendingIntent
        );

        // 🔥 Kill app process = true cold start
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }
}

