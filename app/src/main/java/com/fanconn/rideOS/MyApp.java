package com.fanconn.rideOS;

import android.app.Application;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.fanconn.rideOS.menu.SecurePrefs;
import com.fanconn.rideOS.network.ScooterMqttClient;
import com.fanconn.rideOS.receiver.NightlyRebootScheduler;

public class MyApp extends Application {

    private static final String TAG = "MyApp";
    private ScooterMqttClient scooterMqttClient;
    public static long appStartTime;

    @Override
    public void onCreate() {
        super.onCreate();

        MetadataManager.init(this);
        appStartTime = System.currentTimeMillis();

        NightlyRebootScheduler.scheduleNext(this);

        UptimeManager.start();
        InactivityManager.getInstance().init(this);
        SecurePrefs.init(this);
        new InactivityHandler(this);
        registerActivityLifecycleCallbacks(new TouchTracker());

        String vin = MetadataManager.get().getVin();
//        String brokerUrl = "tcp://192.168.1.134:1883";
        String brokerUrl = "tcp://sb.rivotmotors.com:1883";


        if (vin != null && !vin.isEmpty() && !"UNKNOWN".equalsIgnoreCase(vin)) {
            scooterMqttClient = new ScooterMqttClient(this, brokerUrl, vin);
            startSmartKeyService();

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                scooterMqttClient.connectAndSubscribe();
                //Log.d(TAG, "✅ SCOOTER_MQTT MQTT initialized for VIN: " + vin);
            }, 1500);
        }
        else {
            //Log.e(TAG, "❌  SCOOTER_MQTT VIN missing, MQTT not started");
        }

    }

    private void startSmartKeyService() {
        try {
            Intent svc = new Intent(this,
                    com.fanconn.rideOS.Smartkey.BLEScanService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(svc);
            } else {
                startService(svc);
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ SmartKey service failed", e);
        }
    }
}
