package com.fanconn.rideOS.Smartkey;

import android.app.*;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.Context;
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

import java.util.concurrent.atomic.AtomicBoolean;

public class BLEScanService extends Service {
    private static final String TAG = "BLEKeyService";
    private SerialPortManager serialPortManager;
    private static final String TARGET_MAC = MetadataManager.get().getSmartKeyId();
    private static final long SCAN_INTERVAL_MS = 4000;
    private static final long OUT_OF_RANGE_TIMEOUT_MS = 6000;
    private static final int OUT_OF_RANGE_THRESHOLD_COUNT = 3;

    private static final int RSSI_IN_RANGE_THRESHOLD = -85;
    private static final int RSSI_OUT_RANGE_THRESHOLD = -90;

    private BluetoothLeScanner scanner;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long lastSeenTime = 0;
    private AtomicBoolean keyInRange = new AtomicBoolean(false);
    private int consecutiveOutOfRangeCount = 0;
    private boolean outOfRangeNotified = false;

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (result == null || result.getDevice() == null) return;

            String mac = result.getDevice().getAddress();
            if (!TARGET_MAC.equals(mac)) return;

            int rssi = result.getRssi();
            long now = System.currentTimeMillis();

            // Always update last seen time
            lastSeenTime = now;

            if (rssi > RSSI_OUT_RANGE_THRESHOLD) {
                // Strong enough signal → reset
                if (!keyInRange.get() && rssi > RSSI_IN_RANGE_THRESHOLD) {
                    keyInRange.set(true);
                    consecutiveOutOfRangeCount = 0;
                    outOfRangeNotified = false;
                    Log.i(TAG, "🟢 SMARTKEY Key detected close (RSSI: " + rssi + ")");

                    if (UIUpdater.getInstance() != null) {
                        UIUpdater.getInstance().stopKeylostRedAlertAnimation();
                        UIUpdater.getInstance().stopBeeping();
                    }

                    if (LoginActivity.instance != null) {
                        LoginActivity.instance.loginSuccess("Key Rider");
                        Log.d(TAG, "✅ SMARTKEY loginSuccess() called directly");
                    } else {
                        Log.w(TAG, "⚠️ SMARTKEY LoginActivity not active — cannot call loginSuccess()");
                    }

                    KeyPresenceManager.get().notifyInRange();
                } else {
                    // Still in range
                    consecutiveOutOfRangeCount = 0;
                    outOfRangeNotified = false;
                    Log.d(TAG, "✅ SMARTKEY RSSI update: " + rssi);

                    SerialPortManager.getInstance().VCUSendToMCU("FFAA550107000A01");
                    new Handler(Looper.getMainLooper()).postDelayed(
                            () -> SerialPortManager.getInstance().VCUSendToMCU("FFAA550101000A01"), 500);
                }

            } else {
                // Weak RSSI
                consecutiveOutOfRangeCount++;
                Log.w(TAG, "⚠️ SMARTKEY Possible out of range (" + rssi + " dBm) (" +
                        consecutiveOutOfRangeCount + "/" + OUT_OF_RANGE_THRESHOLD_COUNT + ")");

                if (consecutiveOutOfRangeCount >= OUT_OF_RANGE_THRESHOLD_COUNT && !outOfRangeNotified) {
                    handleActualOutOfRange();
                }
            }
        }

        @Override
        public void onBatchScanResults(java.util.List<ScanResult> results) {
            for (ScanResult r : results)
                onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, r);
        }
    };

    private final Runnable scanRunnable = new Runnable() {
        @Override
        public void run() {
            if (scanner == null) return;
            scanner.startScan(null, buildScanSettings(), scanCallback);
            handler.postDelayed(() -> {
                scanner.stopScan(scanCallback);
                checkKeyTimeout();
                handler.postDelayed(scanRunnable, 1000);
            }, SCAN_INTERVAL_MS);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundNotification();

        BluetoothManager manager = ContextCompat.getSystemService(this, BluetoothManager.class);
        BluetoothAdapter adapter = manager != null ? manager.getAdapter() : null;
        if (adapter == null || !adapter.isEnabled()) {
            Log.e(TAG, "❌ SMARTKEY Bluetooth not available or disabled");
            stopSelf();
            return;
        }

        scanner = adapter.getBluetoothLeScanner();
        Log.i(TAG, "⏳ SMARTKEY Waiting for key...");
        handler.post(scanRunnable);
    }

    private void checkKeyTimeout() {
        long now = System.currentTimeMillis();

        // If already notified as out-of-range, just wait silently
        if (outOfRangeNotified) {
            Log.i(TAG, "⏳ SMARTKEY Waiting for key to return...");
            return;
        }

        // If no signal recently — count as possible timeout
        if ((now - lastSeenTime) > OUT_OF_RANGE_TIMEOUT_MS) {
            consecutiveOutOfRangeCount++;
            Log.w(TAG, "⚠️ SMARTKEY Possible out of range by timeout (" +
                    consecutiveOutOfRangeCount + "/" + OUT_OF_RANGE_THRESHOLD_COUNT + ")");

            if (consecutiveOutOfRangeCount >= OUT_OF_RANGE_THRESHOLD_COUNT) {
                handleActualOutOfRange();
            }
        }
    }


    private void handleActualOutOfRange() {
        if (!keyInRange.get() || outOfRangeNotified) return;

        keyInRange.set(false);
        outOfRangeNotified = true;   // ✅ prevent duplicate triggers
        consecutiveOutOfRangeCount = 0; // ✅ reset counter for next detection

        Log.e(TAG, "🔴 SMARTKEY Actual OUT OF RANGE confirmed after " +
                OUT_OF_RANGE_THRESHOLD_COUNT + " checks");

        int rpm = SerialPortManager.getInstance().getHb3().getBSG2_BYTE78();

        if (MainActivity.MainisActive) {
            if (rpm == 0) {
                // 🚦 Vehicle stopped → Lock immediately
                Log.e(TAG, "🔒 SMARTKEY Key lost & RPM=0 → Locking scooter");
                EVLockService.getInstance().startLockSequence();

                Context context2 = MainActivity.instance;
                if (context2 != null) {
                    Intent intent = new Intent(context2, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    context2.startActivity(intent);
                }

                if (UIUpdater.getInstance() != null) {
                    UIUpdater.getInstance().stopKeylostRedAlertAnimation();
                    UIUpdater.getInstance().stopBeeping();
                }
            } else {
                // 🏍 Vehicle moving → Warning animation + beep
                Log.w(TAG, "⚠️ SMARTKEY Key lost while driving (RPM>0)");
                if (UIUpdater.getInstance() != null) {
                    UIUpdater.getInstance().startKeylostRedAlertAnimation();
                    UIUpdater.getInstance().startBeeping(4);
                }
            }
        } else {
            Log.e(TAG, "⚠️ SMARTKEY MainActivity not active, skipping UI update");
        }

        KeyPresenceManager.get().notifyOutOfRange();
    }

    private ScanSettings buildScanSettings() {
        return new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
    }

    private void startForegroundNotification() {
        String channelId = "blekey_channel";
        NotificationChannel channel =
                new NotificationChannel(channelId, "BLE Key", NotificationManager.IMPORTANCE_LOW);
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(channel);

        Notification notif = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("Scooter Key Active")
                .setContentText("Monitoring BLE key presence...")
                .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .build();

        startForeground(1, notif);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (scanner != null) scanner.stopScan(scanCallback);
        handler.removeCallbacksAndMessages(null);
        Log.i(TAG, "🛑 SMARTKEY BLE key service stopped");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
