package com.fanconn.rideOS;

import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.os.PowerManager.WakeLock;


import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.List;

import tp.xmaihh.serialport.bean.ComBean;

public class serialApplication extends MyApp {
    public static SerialPortManager serialPortManager;

    private final String sPort = "/dev/ttyHS1";
    private final int iBaudRate = 115200;
    private final int RX_type = 0; // 0 = Hex
    private String rxText = "";
    private WakeLock wakeLock;
    private final Handler mHandler = new Handler(msg -> {
        ComBean comBean = (ComBean) msg.obj;
        // Append new data to buffer
        rxText += SerialPortManager.processReceivedData(comBean, RX_type);

        // Process immediately and continuously
        List<String> frames = SerialPortManager.splitIntoFrames(rxText);

        for (String frame : frames) {
//            if (frame.startsWith("FFAA660101")) {
//                Log.d("DATA FRAME", "Frame: " + frame);
            if(frame.startsWith("FFAA770301"))
                Log.d("sdfgh","FRAME------"+frame);

            Intent intent = new Intent("FRAME_RECEIVED");
            intent.putExtra("frame_data", frame);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
            // }
            // Handle other frame types as needed
        }

        // Reset buffer
        rxText = "";

        return true;
    });

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize only once for global access
        serialPortManager = SerialPortManager.getInstance(sPort, iBaudRate, RX_type, mHandler);
        serialPortManager.initSerialConfig(this);
        Log.d("MyApplication", "SerialPortManager initialized globally and reading continuously");

        String vin = MetadataManager.get().getVin();

        if(!MetadataManager.get().getVin().startsWith("UNKNOWN_VIN"))
        {
            Log.d("MainActivity", "🚀 Starting Telemetry for VIN: " + vin);
            Intent intent = new Intent(this, TelemetryService.class);
            intent.putExtra("VIN_NUMBER", vin);
            startService(intent);
        }

        // ✅ Acquire partial wake lock to keep CPU alive
//        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
//        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "rideOS::SerialLock");
//        wakeLock.acquire(); // Now CPU will stay on

    }

    public static SerialPortManager getSerialPortManager() {
        return serialPortManager;
    }
    private String canData;
    public void setCanDataLogin(String canData) {
        this.canData = canData;
    }
    public String getCanData(){return this.canData;}
}