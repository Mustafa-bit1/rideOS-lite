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
import java.util.Objects;
import java.util.Set;

public class TpmsScanService extends Service {

    private static final String TAG = "TPMS-Service";
    public static final String ACTION_TPMS_RESYNC =
            "com.fanconn.rideOS.ACTION_TPMS_RESYNC";
    // Broadcast constants
    public static final String ACTION_TPMS_UPDATE  = "com.fanconn.rideOS.ACTION_TPMS_UPDATE";
    public static final String ACTION_TPMS_REQUEST = "com.fanconn.rideOS.ACTION_TPMS_REQUEST";
    public static final String ACTION_TPMS_RESTART = "com.fanconn.rideOS.ACTION_TPMS_RESTART";

    public static final String EXTRA_MAC    = "extra_mac";
    public static final String EXTRA_MINOR  = "extra_minor";   // legacy
    public static final String EXTRA_PSI    = "extra_psi";
    public static final String EXTRA_TEMP   = "extra_temp";
    public static final String EXTRA_VOLT   = "extra_voltage";
    public static final String EXTRA_LOWBAT = "extra_low_battery";

    private BluetoothLeScanner scanner;

    private final Set<String> validMacs = new HashSet<>();
    private final Map<String, Integer> latestMinorMap = new HashMap<>();

    private final MetadataManager metadata = MetadataManager.get();

    /* ================= BROADCAST RECEIVER ================= */

    private final BroadcastReceiver tpmsRequestReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (ACTION_TPMS_REQUEST.equals(intent.getAction())) {

                //Log.d(TAG, " ACTION_TPMS_REQUEST → sending cached values");

                for (Map.Entry<String, Integer> e : latestMinorMap.entrySet()) {
                    Intent update = new Intent(ACTION_TPMS_UPDATE);
                    update.putExtra(EXTRA_MAC, e.getKey());
                    update.putExtra(EXTRA_MINOR, e.getValue());
                    LocalBroadcastManager
                            .getInstance(TpmsScanService.this)
                            .sendBroadcast(update);
                }

            } else if (ACTION_TPMS_RESTART.equals(intent.getAction())) {

               // Log.d(TAG, " ACTION_TPMS_RESTART → restarting scan");
                restartScanSafely();
            }
            else if (ACTION_TPMS_RESYNC.equals(intent.getAction())) {

               // Log.d(TAG, "TPMS RESYNC");

                // Forget all old packets
                latestMinorMap.clear();

                // Reset state
                TpmsState.sensorAvailable = false;

                // Restart BLE scan
                restartScanSafely();
            }

        }
    };

    /* ================= SERVICE LIFECYCLE ================= */

    @Override
    public void onCreate() {
        super.onCreate();

        validMacs.add(metadata.getFrontTPMS());
        validMacs.add(metadata.getRearTPMS());

       // Log.d(TAG, "Valid TPMS MACs = " + validMacs);

        startForegroundService();
        startScanning();

        IntentFilter f = new IntentFilter();
        f.addAction(ACTION_TPMS_REQUEST);
        f.addAction(ACTION_TPMS_RESTART);
        f.addAction(ACTION_TPMS_RESYNC);
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(tpmsRequestReceiver, f);
        lastScanTime = System.currentTimeMillis();
        scanWatchdog.post(scanWatchdogRunnable);

    }

    /* ================= FOREGROUND ================= */

    private void startForegroundService() {

        String channelId = "tpms_scan_channel";
        NotificationManager nm =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    channelId,
                    "TPMS Scanning",
                    NotificationManager.IMPORTANCE_LOW
            );
            nm.createNotificationChannel(ch);
        }

        Notification n = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("TPMS Service")
                .setContentText("Scanning TPMS sensors")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOngoing(true)
                .build();

        startForeground(101, n);
    }

    /* ================= BLE SCANNING ================= */
    private boolean isScanning = false;

    private synchronized void startScanning() {

        if (isScanning) return;

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            Log.e(TAG, "Bluetooth unavailable");
            return;
        }

        scanner = adapter.getBluetoothLeScanner();
        if (scanner == null) {
            Log.e(TAG, "BLE scanner unavailable");
            return;
        }

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        scanner.startScan(null, settings, scanCallback);

        isScanning = true;
        lastScanTime = System.currentTimeMillis();

     //   Log.d(TAG, "BLE scan started");
    }

    private long lastScanTime = 0;
    private long lastTpmsPacketTime = 0;
    private static final long TPMS_TIMEOUT_MS = 15000; // 15 sec
    private final ScanCallback scanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {

            if (result == null || result.getScanRecord() == null) return;
            lastScanTime = System.currentTimeMillis();

            String mac = result.getDevice().getAddress();
            if (!validMacs.contains(mac)) return;

            TpmsParsedData d =
                    TpmsParser.parse(result.getScanRecord().getBytes());

            if (!d.valid) return;

            TpmsState.sensorAvailable = true;
            lastTpmsPacketTime = System.currentTimeMillis();
//            Log.d("TPMS_STATUS",
//                    "SERVICE SET sensorAvailable = true");

            int psiInt  = (int) Math.round(d.psi);
            int tempInt = d.temperature;

            // Legacy compatibility (minor-style)
            int minorCompat = ((psiInt & 0xFF) << 8) | (tempInt & 0xFF);
            latestMinorMap.put(mac, minorCompat);

            Intent i = new Intent(ACTION_TPMS_UPDATE);
            i.putExtra(EXTRA_MAC, mac);
            i.putExtra(EXTRA_MINOR, minorCompat);
            i.putExtra(EXTRA_PSI, d.psi);
            i.putExtra(EXTRA_TEMP, d.temperature);
            i.putExtra(EXTRA_VOLT, d.voltage);
            i.putExtra(EXTRA_LOWBAT, d.lowBattery);

            LocalBroadcastManager
                    .getInstance(TpmsScanService.this)
                    .sendBroadcast(i);

//            Log.d(TAG,
//                    "TPMSBATTAERY | " + mac +
//                            " | PSI=" + String.format("%.1f", d.psi) +
//                            " | TEMP=" + d.temperature +
//                            " | BAT=" + (d.voltage > 0
//                            ? String.format("%.2fV", d.voltage)
//                            : (d.lowBattery ? "LOW" : "OK")) +
//                            " | TYPE=" + d.type
//            );
            if(d.voltage>0  && Objects.equals(mac, metadata.getFrontTPMS())){
              //  Log.d("asfaf","THSIISTPMSBATTERYVOATGE FRONT "+String.format("%.2fV", d.voltage));
                TelemetryDataManager.getInstance()
                        .setTeleFTirePressureVoltage(String.format("%.2fV", d.voltage));

            }
            else if (d.voltage>0  && Objects.equals(mac, metadata.getRearTPMS())) {
              //  Log.d("asfaf","THSIISTPMSBATTERYVOATGE REAR "+String.format("%.2fV", d.voltage));
                TelemetryDataManager.getInstance()
                        .setTeleRTirePressureVoltage(String.format("%.2fV", d.voltage));
            }
        }
    };
    private static final long SCAN_STALL_TIMEOUT_MS = 15_000; // 15 sec

    private final android.os.Handler scanWatchdog =
            new android.os.Handler(android.os.Looper.getMainLooper());
    private long lastRestartTime = 0;

    private final Runnable scanWatchdogRunnable = new Runnable() {
        @Override
        public void run() {


            long now = System.currentTimeMillis();

            if (System.currentTimeMillis() - lastTpmsPacketTime > TPMS_TIMEOUT_MS) {

                TpmsState.sensorAvailable = false;

//                Log.d("TPMS_STATUS",
//                        "SERVICE SET sensorAvailable = false");
            }
            if (now - lastScanTime > SCAN_STALL_TIMEOUT_MS
                    && now - lastRestartTime > 30_000) {

                lastRestartTime = now;
                restartScanSafely();
            }


            scanWatchdog.postDelayed(this, 5000);
        }
    };
    private synchronized void restartScanSafely() {

        stopScanningSafely();

        // Let BLE stack cool down
        scanWatchdog.postDelayed(this::startScanning, 1000);
    }
    private synchronized void stopScanningSafely() {
        try {
            if (scanner != null) {
                scanner.stopScan(scanCallback);
            }
        } catch (Exception ignored) {}

        scanner = null;
        isScanning = false;
    }


    /* ================= CLEANUP ================= */

    @Override
    public void onDestroy() {

        scanWatchdog.removeCallbacksAndMessages(null);
        stopScanningSafely();

        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(tpmsRequestReceiver);

        super.onDestroy();
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
