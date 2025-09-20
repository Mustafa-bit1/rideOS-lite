package com.fanconn.rideOS;

import android.net.TrafficStats;
import android.util.Log;

public class DataUsageTracker {
    private long startTxBytes;
    private long startRxBytes;

    public void startTracking() {
        int uid = android.os.Process.myUid();
        startTxBytes = TrafficStats.getUidTxBytes(uid);
        startRxBytes = TrafficStats.getUidRxBytes(uid);
        if (startTxBytes == TrafficStats.UNSUPPORTED || startRxBytes == TrafficStats.UNSUPPORTED) {
            Log.e("DataUsageTracker", "TrafficStats not supported on this device.");
        }
    }

    public void logUsage(String tag) {
        int uid = android.os.Process.myUid();
        long txBytes = TrafficStats.getUidTxBytes(uid) - startTxBytes;
        long rxBytes = TrafficStats.getUidRxBytes(uid) - startRxBytes;

        double txKB = txBytes / 1024.0;
        double rxKB = rxBytes / 1024.0;

        Log.d("DataUsageTracker", tag + " ➝ Sent: " + txKB + " KB | Received: " + rxKB + " KB");
    }
}
