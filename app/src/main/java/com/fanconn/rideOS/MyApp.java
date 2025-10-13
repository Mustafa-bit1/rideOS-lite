package com.fanconn.rideOS;

import android.app.Application;

public class MyApp extends Application {
    public static long appStartTime;
    @Override
    public void onCreate() {
        super.onCreate();
        new InactivityHandler(this); // Register the central handler
        registerActivityLifecycleCallbacks(new TouchTracker());
        MetadataManager.init(this);
        appStartTime = System.currentTimeMillis();

    }
}
