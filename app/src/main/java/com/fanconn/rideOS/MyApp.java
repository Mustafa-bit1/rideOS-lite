package com.fanconn.rideOS;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import java.util.Objects;

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
        if(!Objects.equals(MetadataManager.get().getSmartKeyId(), "UNKNOWN_KEY")){
           // Log.d("SMKEYVALUE","SMARTKEY"+MetadataManager.get().getSmartKeyId());
            startSmartKeyService();
        }
        //else Log.d("sadfdg","SMARTKEY FEATUREDISABLED");
    }

    private void startSmartKeyService() {
        try {
            Intent svc = new Intent(this, com.fanconn.rideOS.Smartkey.BLEScanService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(svc);
            } else {
                startService(svc);
            }
            //android.util.Log.i("MyApp", "🔑 SMARTKEY  Key detection service started");
        } catch (Exception e) {
            //android.util.Log.e("MyApp", "❌ SMARTKEY Failed to start key service", e);
        }
    }
}
