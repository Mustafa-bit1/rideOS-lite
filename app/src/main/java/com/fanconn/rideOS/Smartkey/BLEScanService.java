package com.fanconn.rideOS.Smartkey;

import android.app.*;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.*;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.fanconn.rideOS.EVLockService;
import com.fanconn.rideOS.MainActivity;
import com.fanconn.rideOS.MetadataManager;
import com.fanconn.rideOS.SerialPortManager;
import com.fanconn.rideOS.UIUpdater;
import com.fanconn.rideOS.authen.LoginActivity;

public class BLEScanService extends Service {
    private static final String TAG = "BLEKeyService";
    private static final String TARGET_MAC = MetadataManager.get().getSmartKeyId();
    private static final String TARGET_NAME = MetadataManager.get().getVin();//"ME9NC1202F1172002";//ME9NC1202F1172001
    private static final long SCAN_INTERVAL_MS = 1000;
    private long lastTimeoutIncrementTime = 0;
    private static final int OUT_OF_RANGE_THRESHOLD_COUNT = 3;
    private static final int RSSI_IN_RANGE_THRESHOLD = -82;
    private static final int RSSI_OUT_RANGE_THRESHOLD = -100;
    private BluetoothLeScanner scanner;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean keyInRange = false;
    private String unlockedBy = null;
    private int consecutiveOutOfRangeCount = 0;
    private long lastSeenTime = 0;
    private boolean keyLostWhileMoving = false;
    private boolean isScanning = false;

    private int weakSignalCount = 0;
    private int missedScanCount = 0;
    private boolean signalSeenThisScan = false;

    // ---------------- BLE Scan Callback ----------------
    private final ScanCallback scanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {

            if (result == null || result.getDevice() == null) return;

            String deviceName =
                    result.getScanRecord() != null ? result.getScanRecord().getDeviceName() : null;

            String mac = result.getDevice().getAddress();
            int rssi = result.getRssi();
            long now = System.currentTimeMillis();

            boolean isSmartKey = TARGET_MAC.equals(mac);
            boolean isAndroidKey = TARGET_NAME.equals(deviceName);

            // Ignore all other BLE devices
            if (!isSmartKey && !isAndroidKey) return;

            // Mark that the correct key was seen in this scan window
            signalSeenThisScan = true;

            // ---------- UNLOCK ----------
            if (!keyInRange) {

                if (isSmartKey && rssi >= RSSI_IN_RANGE_THRESHOLD) {
                    unlock("SMARTKEY", mac, rssi);
                }
                else if (isAndroidKey && rssi >= RSSI_IN_RANGE_THRESHOLD) {
                    unlock(TARGET_NAME, deviceName, rssi);
                }
                else {
                    Log.d(TAG, "❌ Weak RSSI ignored (" + rssi + ")");
                }

                return;
            }

            // ---------- MONITOR ----------
            boolean correctKey =
                    ("SMARTKEY".equals(unlockedBy) && isSmartKey) ||
                            (TARGET_NAME.equals(unlockedBy) && isAndroidKey);

            if (!correctKey) return;

            // ---------- STRONG SIGNAL ----------
            if (rssi >= RSSI_IN_RANGE_THRESHOLD) {

                weakSignalCount = 0;
                missedScanCount = 0;
                lastSeenTime = now;

                Log.d(TAG, "✅ " + unlockedBy +
                        " STRONG SIGNAL (" + rssi + " dBm) → counters reset");

                return;
            }

            // ---------- WEAK SIGNAL ----------
            weakSignalCount++;
            missedScanCount = 0;

            Log.w(TAG, "⚠️ " + unlockedBy +
                    " WEAK RSSI (" + rssi + " dBm) (" +
                    weakSignalCount + "/3)");

            if (weakSignalCount >= 3) {

                Log.e(TAG, "🔴 " + unlockedBy +
                        " 3 consecutive WEAK RSSI → OUT OF RANGE");

                handleOutOfRange();
            }
        }
    };
    // ---------------- Unlock ----------------
    private void unlock(String keyType, String id, int rssi) {

        keyInRange = true;
        unlockedBy = keyType;
        consecutiveOutOfRangeCount = 0;
        lastSeenTime = System.currentTimeMillis();
        keyLostWhileMoving = false;
        weakSignalCount = 0;
        missedScanCount = 0;
        signalSeenThisScan = false;
        Log.i(TAG, "🟢 " + keyType + " detected → UNLOCK (RSSI " + rssi + ")");

        if (MainActivity.instance != null)
            MainActivity.instance.updateKeyImage(true);
        else Log.d("BLEKeyService","DETCETDBUTELSE OF111");

        if (UIUpdater.getInstance() != null) {
            UIUpdater.getInstance().stopKeylostRedAlertAnimation();
            UIUpdater.getInstance().stopBeeping();
        }
        else Log.d("BLEKeyService","DETCETDBUTELSE OF222");

        if (LoginActivity.instance != null)
            LoginActivity.instance.loginSuccess("Key Rider");
        else Log.d("BLEKeyService","DETCETDBUTELSE OF333");

        if (MainActivity.MainisActive && "SMARTKEY".equals(keyType)) {
            SerialPortManager.getInstance().VCUSendToMCU("FFAA550107000A01");
            handler.postDelayed(() ->
                    SerialPortManager.getInstance().VCUSendToMCU("FFAA550101000A01"), 500);
            Log.d("BLEKeyService","DETCETDBUTIFFF OF444");

        }
        else Log.d("BLEKeyService","DETCETDBUTELSE OF444");
    }

    // ---------------- Out-of-range ----------------
    private void handleOutOfRange() {

        Log.e(TAG, "🔴 " + unlockedBy + " OUT OF RANGE");
        keyInRange = false;
        consecutiveOutOfRangeCount = 0;

        int rpm = SerialPortManager.getInstance().getHb3().getBSG2_BYTE78();

        if (MainActivity.MainisActive) {

            if (rpm == 0) {
                EVLockService.getInstance().startLockSequence();
                if (UIUpdater.getInstance() != null) {
                    UIUpdater.getInstance().onDoubleTapAction();
                    UIUpdater.getInstance().stopKeylostRedAlertAnimation();
                    UIUpdater.getInstance().stopBeeping();
                }
            } else {
                keyLostWhileMoving = true;
                if (UIUpdater.getInstance() != null) {
                    UIUpdater.getInstance().startKeylostRedAlertAnimation();
                    UIUpdater.getInstance().startBeeping(4);
                }
            }
        }

        if (MainActivity.instance != null)
            MainActivity.instance.updateKeyImage(false);
        weakSignalCount = 0;
        missedScanCount = 0;
        signalSeenThisScan = false;
    }

    // ---------------- RPM Watcher ----------------
    private final Runnable rpmCheckRunnable = new Runnable() {
        @Override
        public void run() {
            if (keyLostWhileMoving) {
                int rpm = SerialPortManager.getInstance().getHb3().getBSG2_BYTE78();
                if (rpm == 0) {
                    EVLockService.getInstance().startLockSequence();
                    if (UIUpdater.getInstance() != null) {
                        UIUpdater.getInstance().onDoubleTapAction();
                        UIUpdater.getInstance().stopKeylostRedAlertAnimation();
                        UIUpdater.getInstance().stopBeeping();
                    }
                    keyLostWhileMoving = false;
                }
            }
            handler.postDelayed(this, 500);
        }
    };

    // ---------------- Scan Loop ----------------
    private final Runnable scanRunnable = new Runnable() {
        @Override
        public void run() {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

            if (adapter == null || !adapter.isEnabled()) {
                Log.w(TAG, "⚠️ Bluetooth disabled, skipping scan");
                handler.postDelayed(this, 1000);
                return;
            }

            scanner = adapter.getBluetoothLeScanner();
            if (scanner == null) {
                handler.postDelayed(this, 1000);
                return;
            }

            try {
                if (!isScanning) {
                    scanner.startScan(null, buildScanSettings(), scanCallback);
                    isScanning = true;
                    Log.d(TAG, "▶️ BLE scan started");
                }
            } catch (IllegalStateException e) {
                Log.w(TAG, "⚠️ startScan failed (BT state change)");
            }


            handler.postDelayed(() -> {
                safeStopScan();
                checkTimeout();
                handler.postDelayed(scanRunnable, 1000);
            }, SCAN_INTERVAL_MS);
        }
    };

    private final BroadcastReceiver btReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) return;

            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);

            if (state == BluetoothAdapter.STATE_OFF) {
                Log.w(TAG, "🔴 Bluetooth OFF → stopping BLE scan");

                handler.removeCallbacks(scanRunnable);
                safeStopScan();

                // Optional: treat as key lost
                if (keyInRange) {
                    handleOutOfRange();
                }
            }

            else if (state == BluetoothAdapter.STATE_ON) {
                Log.i(TAG, "🔵 Bluetooth ON → restarting BLE scan");
                handler.removeCallbacks(scanRunnable);
                handler.post(scanRunnable);
            }
        }
    };


    private void checkTimeout() {

        if (!keyInRange) return;

        if (!signalSeenThisScan) {

            missedScanCount++;

            Log.w(TAG, "⚠️ " + unlockedBy +
                    " NO SIGNAL this scan (" +
                    missedScanCount + "/3)");

            if (missedScanCount >= 3) {

                Log.e(TAG, "🔴 " + unlockedBy +
                        " 3 consecutive MISSED SCANS → OUT OF RANGE");

                handleOutOfRange();
            }

        } else {
            missedScanCount = 0;
        }

        signalSeenThisScan = false;
    }


    private ScanSettings buildScanSettings() {
        return new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
    }

    // ---------------- Lifecycle ----------------
    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundNotification();

        BluetoothManager manager =
                ContextCompat.getSystemService(this, BluetoothManager.class);

        BluetoothAdapter adapter =
                manager != null ? manager.getAdapter() : null;

        if (adapter == null || !adapter.isEnabled()) {
            stopSelf();
            return;
        }

        scanner = adapter.getBluetoothLeScanner();

        // ✅ Prevent duplicate scan loops
        handler.removeCallbacks(scanRunnable);
        handler.post(scanRunnable);

        handler.post(rpmCheckRunnable);

        registerReceiver(btReceiver,
                new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        Log.i(TAG, "⏳ BLEScanService started");
    }



    @Override
    public void onDestroy() {
        super.onDestroy();

        handler.removeCallbacksAndMessages(null);
        safeStopScan();

        try {
            unregisterReceiver(btReceiver);
        } catch (Exception ignored) {}

        Log.i(TAG, "🛑 BLEScanService stopped");
    }

    private void startForegroundNotification() {
        String channelId = "blekey_channel";

        NotificationChannel channel =
                new NotificationChannel(channelId, "BLE Key",
                        NotificationManager.IMPORTANCE_LOW);

        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(channel);

        Notification notification =
                new NotificationCompat.Builder(this, channelId)
                        .setContentTitle("Scooter Key Active")
                        .setContentText("Monitoring BLE key presence")
                        .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                        .build();

        startForeground(1, notification);
    }
    private void safeStopScan() {
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter != null && adapter.isEnabled() && scanner != null && isScanning) {
                scanner.stopScan(scanCallback);
                isScanning = false;
                Log.d(TAG, "⏹️ BLE scan stopped");
            }
        } catch (IllegalStateException e) {
            Log.w(TAG, "⚠️ stopScan ignored (Bluetooth OFF)");
        }
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
