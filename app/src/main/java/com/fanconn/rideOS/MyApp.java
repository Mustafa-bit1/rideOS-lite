package com.fanconn.rideOS;

import android.app.Application;
import android.content.Intent;

public class MyApp extends Application {
    public static long appStartTime;
    @Override
    public void onCreate() {
        super.onCreate();
        new InactivityHandler(this); // Register the central handler
        registerActivityLifecycleCallbacks(new TouchTracker());
        MetadataManager.init(this);
        appStartTime = System.currentTimeMillis();
        UptimeManager.start(); // Start uptime tracking immediately
        startBleKeyService();
    }

    private void startBleKeyService() {
        try {
            Intent svc = new Intent(this, com.fanconn.rideOS.Smartkey.BLEScanService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(svc);
            } else {
                startService(svc);
            }
            android.util.Log.i("MyApp", "🔑 BLE Key detection service started");
        } catch (Exception e) {
            android.util.Log.e("MyApp", "❌ Failed to start BLE key service", e);
        }
    }
}
