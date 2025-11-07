package com.fanconn.rideOS.Smartkey;


import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.util.Log;

public class BLEScanCallback extends ScanCallback {
    private final String targetMac;
    public BLEScanCallback(String mac) { this.targetMac = mac; }

    @Override
    public void onScanResult(int callbackType, ScanResult result) {
        if (result == null || result.getDevice() == null) return;
        String mac = result.getDevice().getAddress();
        if (mac == null) return;

        if (mac.equalsIgnoreCase(targetMac)) {
            int rssi = result.getRssi();
            long ts = System.currentTimeMillis();
            KeyPresenceManager.get().onAdvertisement(rssi, ts);
            // optional: log raw
            Log.d("BLEScanCallback", "Found key " + mac + " rssi=" + rssi);
        }
    }

    @Override
    public void onScanFailed(int errorCode) {
        Log.e("BLEScanCallback", "Scan failed: " + errorCode);
    }
}
