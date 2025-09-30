package com.fanconn.rideOS;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TpmsScanService extends Service {
    private static final String TAG = "TPMS-Service";

    // Broadcast constants
    public static final String ACTION_TPMS_UPDATE = "com.fanconn.rideOS.ACTION_TPMS_UPDATE";
    public static final String ACTION_TPMS_REQUEST = "com.fanconn.rideOS.ACTION_TPMS_REQUEST";
    public static final String ACTION_TPMS_RESTART = "com.fanconn.rideOS.ACTION_TPMS_RESTART";
    public static final String EXTRA_MAC = "extra_mac";
    public static final String EXTRA_MINOR = "extra_minor";

    private BluetoothLeScanner scanner;
    private final Set<String> validMacs = new HashSet<>();
    private final Map<String, Integer> latestMinorMap = new HashMap<>();

    private final BroadcastReceiver tpmsRequestReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_TPMS_REQUEST.equals(action)) {
                Log.d(TAG, "📡 ACTION_TPMS_REQUEST → sending cached values...");
                for (Map.Entry<String, Integer> entry : latestMinorMap.entrySet()) {
                    Intent update = new Intent(ACTION_TPMS_UPDATE);
                    update.putExtra(EXTRA_MAC, entry.getKey());
                    update.putExtra(EXTRA_MINOR, entry.getValue());
                    LocalBroadcastManager.getInstance(TpmsScanService.this).sendBroadcast(update);
                }
            } else if (ACTION_TPMS_RESTART.equals(action)) {
                Log.d(TAG, "🔄 ACTION_TPMS_RESTART → restarting BLE scan...");
                if (scanner != null) {
                    try {
                        scanner.stopScan(scanCallback);
                    } catch (Exception ignore) {}
                }
                startScanning();
            }
        }
    };

    private final MetadataManager metadata = MetadataManager.get();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: Service created");

        // Add your TPMS MACs
        validMacs.add(metadata.getFrontTPMS());
        validMacs.add(metadata.getRearTPMS());
        Log.d(TAG, "onCreate: Valid TPMS MACs = " + validMacs);

        startForegroundService();
        startScanning();

        // Register request & restart receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_TPMS_REQUEST);
        filter.addAction(ACTION_TPMS_RESTART);
        LocalBroadcastManager.getInstance(this).registerReceiver(tpmsRequestReceiver, filter);
    }

    private void startForegroundService() {
        String channelId = "tpms_scan_channel";
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "TPMS Scanning",
                    NotificationManager.IMPORTANCE_LOW
            );
            manager.createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("TPMS Service")
                .setContentText("Scanning for TPMS sensors...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOngoing(true)
                .build();

        startForeground(101, notification);
    }

    private void startScanning() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            Log.e(TAG, "startScanning: Bluetooth not available or not enabled ❌");
            return;
        }

        scanner = adapter.getBluetoothLeScanner();
        if (scanner == null) {
            Log.e(TAG, "startScanning: BLE scanner not available ❌");
            return;
        }

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        scanner.startScan(null, settings, scanCallback);
        Log.d(TAG, "startScanning: Started BLE scan for TPMS ✅");
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (result == null || result.getScanRecord() == null) return;

            byte[] data = result.getScanRecord().getBytes();
            if (data == null || data.length < 29) return;

            int major = ((data[25] & 0xFF) << 8) | (data[26] & 0xFF);
            int minor = ((data[27] & 0xFF) << 8) | (data[28] & 0xFF);

            String mac = result.getDevice().getAddress();
            if (!validMacs.contains(mac)) return;

            String last4 = mac.replace(":", "").toUpperCase();
            last4 = last4.substring(last4.length() - 4);

            String majorHex = String.format("%04X", major);

            if (majorHex.equals(last4)) {
                latestMinorMap.put(mac, minor);

                Log.d(TAG, "✅ TPMS detected | MAC=" + mac +
                        " | Major=" + major + " (Hex=" + majorHex + ")" +
                        " | Minor=" + minor);

                Intent intent = new Intent(ACTION_TPMS_UPDATE);
                intent.putExtra(EXTRA_MAC, mac);
                intent.putExtra(EXTRA_MINOR, minor);
                LocalBroadcastManager.getInstance(TpmsScanService.this).sendBroadcast(intent);
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (scanner != null) {
            try {
                scanner.stopScan(scanCallback);
            } catch (Exception e) {
                Log.e(TAG, "onDestroy: Failed to stop scan", e);
            }
        }

        LocalBroadcastManager.getInstance(this).unregisterReceiver(tpmsRequestReceiver);
        //Log.d(TAG, "onDestroy: Service destroyed, scan stopped, receiver unregistered ✅");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
