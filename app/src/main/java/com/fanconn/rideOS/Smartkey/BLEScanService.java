package com.fanconn.rideOS.Smartkey;

import android.app.*;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.Intent;
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
    private static final long SCAN_INTERVAL_MS = 4000;
    private static final int OUT_OF_RANGE_THRESHOLD_COUNT = 3;

    private static final int RSSI_IN_RANGE_THRESHOLD = -70;
    private static final int RSSI_OUT_RANGE_THRESHOLD = -100;

    private BluetoothLeScanner scanner;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private boolean keyInRange = false;
    private String unlockedBy = null;

    private int consecutiveOutOfRangeCount = 0;
    private long lastSeenTime = 0;
    private boolean keyLostWhileMoving = false;

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

            // ---------- UNLOCK ----------
            if (!keyInRange) {

                if (isSmartKey && rssi >= RSSI_IN_RANGE_THRESHOLD) {
                    unlock("SMARTKEY", mac, rssi);
                } else if (isAndroidKey && rssi >= RSSI_IN_RANGE_THRESHOLD) {
                    unlock(TARGET_NAME, deviceName, rssi);
                } else {
                    //Log.d(TAG, "❌ Weak RSSI ignored (" + rssi + ")");
                }
                return;
            }

            // ---------- MONITOR ----------
            boolean correctKey =
                    ("SMARTKEY".equals(unlockedBy) && isSmartKey) ||
                            (TARGET_NAME.equals(unlockedBy) && isAndroidKey);

            if (!correctKey) return;

            if (rssi >= RSSI_IN_RANGE_THRESHOLD) {
                lastSeenTime = now;
                consecutiveOutOfRangeCount = 0;
                Log.d(TAG, "✅ " + unlockedBy + " IN RANGE (RSSI " + rssi + ")");
                return;
            }

            if (rssi > RSSI_OUT_RANGE_THRESHOLD) {
                lastSeenTime = now;
                Log.d(TAG, "➖ " + unlockedBy + " MID RSSI ignored (" + rssi + ")");
                return;
            }

            consecutiveOutOfRangeCount++;
            Log.w(TAG, "⚠️ " + unlockedBy + " possible out of range (RSSI " + rssi + ") (" +
                    consecutiveOutOfRangeCount + "/" + OUT_OF_RANGE_THRESHOLD_COUNT + ")");

            if (consecutiveOutOfRangeCount >= OUT_OF_RANGE_THRESHOLD_COUNT) {
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

        Log.i(TAG, "🟢 " + keyType + " detected → UNLOCK (RSSI " + rssi + ")");

        if (MainActivity.instance != null)
            MainActivity.instance.updateKeyImage(true);

        if (UIUpdater.getInstance() != null) {
            UIUpdater.getInstance().stopKeylostRedAlertAnimation();
            UIUpdater.getInstance().stopBeeping();
        }

        if (LoginActivity.instance != null)
            LoginActivity.instance.loginSuccess("Key Rider");

        if (MainActivity.MainisActive && "SMARTKEY".equals(keyType)) {
            SerialPortManager.getInstance().VCUSendToMCU("FFAA550107000A01");
            handler.postDelayed(() ->
                    SerialPortManager.getInstance().VCUSendToMCU("FFAA550101000A01"), 500);
        }
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
            if (scanner != null) {
                scanner.startScan(null, buildScanSettings(), scanCallback);
            }

            handler.postDelayed(() -> {
                if (scanner != null) {
                    scanner.stopScan(scanCallback);
                }
                checkTimeout();
                handler.postDelayed(scanRunnable, 1000);
            }, SCAN_INTERVAL_MS);
        }
    };

    private void checkTimeout() {
        if (!keyInRange) return;

        long now = System.currentTimeMillis();
        if ((now - lastSeenTime) > SCAN_INTERVAL_MS) {
            consecutiveOutOfRangeCount++;
            Log.w(TAG, "⚠️ " + unlockedBy + " timeout (" +
                    consecutiveOutOfRangeCount + "/" + OUT_OF_RANGE_THRESHOLD_COUNT + ")");
            if (consecutiveOutOfRangeCount >= OUT_OF_RANGE_THRESHOLD_COUNT) {
                handleOutOfRange();
            }
        }
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
        handler.post(scanRunnable);
        handler.post(rpmCheckRunnable);

        Log.i(TAG, "⏳ BLEScanService started");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (scanner != null) scanner.stopScan(scanCallback);
        handler.removeCallbacksAndMessages(null);
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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
