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

import com.fanconn.rideOS.*;
import com.fanconn.rideOS.authen.LoginActivity;

public class BLEScanService extends Service
        implements KeyPresenceManager.PresenceListener {

    private static final String TAG = "SMARTKEY-Service";
    private static final long SCAN_INTERVAL_MS = 4000;

    private BluetoothLeScanner scanner;
    private Handler handler;
    private String targetMac;

    /** 🔐 Key lost while riding → lock AFTER stop */
    private boolean pendingLockAfterStop = false;

    // =========================================================
    // ================= SERVICE ===============================
    // =========================================================
    @Override
    public void onCreate() {
        super.onCreate();

        handler = new Handler(Looper.getMainLooper());
        targetMac = MetadataManager.get().getSmartKeyId();

        startForegroundNotification();

        BluetoothManager bm =
                ContextCompat.getSystemService(this, BluetoothManager.class);
        BluetoothAdapter adapter = bm != null ? bm.getAdapter() : null;

        if (adapter == null || !adapter.isEnabled()) {
            Log.e(TAG, "SMARTKEY Bluetooth unavailable");
            stopSelf();
            return;
        }

        scanner = adapter.getBluetoothLeScanner();
        KeyPresenceManager.get().setListener(this);

        Log.i(TAG, "SMARTKEY BLEScanService STARTED");
        handler.post(scanRunnable);
    }

    // =========================================================
    // ================= SCAN LOOP =============================
    // =========================================================
    private final Runnable scanRunnable = new Runnable() {
        @Override
        public void run() {

            scanner.startScan(null, buildScanSettings(), scanCallback);

            handler.postDelayed(() -> {
                scanner.stopScan(scanCallback);

                KeyPresenceManager.get()
                        .tickCheck(System.currentTimeMillis());

                checkPendingLockOnStop(); // 🔑 IMPORTANT

                handler.postDelayed(scanRunnable, 1000);
            }, SCAN_INTERVAL_MS);
        }
    };

    // =========================================================
    // ================= BLE CALLBACK ==========================
    // =========================================================
    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int type, ScanResult result) {
            if (result == null || result.getDevice() == null) return;
            if (!targetMac.equals(result.getDevice().getAddress())) return;

            KeyPresenceManager.get()
                    .onAdvertisement(result.getRssi(),
                            System.currentTimeMillis());
        }
    };

    // =========================================================
    // ================= SMARTKEY EVENTS =======================
    // =========================================================

    /**
     * 🔓 First detection of key (allowed unlock)
     */
    @Override
    public void onFirstInRange() {

        Log.i(TAG, "SMARTKEY FIRST IN RANGE → UNLOCK");

        // Cancel pending lock if any
        if (pendingLockAfterStop) {
            Log.i(TAG, "SMARTKEY Pending lock cancelled (key returned)");
            pendingLockAfterStop = false;
        }

        if (UIUpdater.getInstance() != null) {
            UIUpdater.getInstance().stopKeylostRedAlertAnimation();
            UIUpdater.getInstance().stopBeeping();
        }

        if (LoginActivity.instance != null) {
            LoginActivity.instance.loginSuccess("SmartKey Rider");
        }

        if (MainActivity.MainisActive) {
            SerialPortManager.getInstance()
                    .VCUSendToMCU("FFAA550107000A01");

            new Handler(Looper.getMainLooper()).postDelayed(
                    () -> SerialPortManager.getInstance()
                            .VCUSendToMCU("FFAA550101000A01"),
                    500
            );
        }
    }

    /**
     * 🔴 Key confirmed OUT of range
     */
    @Override
    public void onConfirmedOut() {

        int rpm = SerialPortManager.getInstance()
                .getHb3().getBSG2_BYTE78();

        Log.e(TAG, "SMARTKEY CONFIRMED OUT | RPM=" + rpm);

        if (rpm > 0) {
            // 🚨 Riding → warn only
            pendingLockAfterStop = true;

            Log.w(TAG,
                    "SMARTKEY Key lost while riding → wait for stop");

            if (UIUpdater.getInstance() != null) {
                UIUpdater.getInstance()
                        .startKeylostRedAlertAnimation();
                UIUpdater.getInstance().startBeeping(4);
            }
            return;
        }

        // 🛑 Already stopped → lock now
        lockScooterAndLaunchLogin();
    }

    // =========================================================
    // ================= RPM CHECK =============================
    // =========================================================
    private void checkPendingLockOnStop() {

        if (!pendingLockAfterStop) return;

        int rpm = SerialPortManager.getInstance()
                .getHb3().getBSG2_BYTE78();

        if (rpm == 0 && !KeyPresenceManager.get().isInRange()) {

            Log.e(TAG,
                    "SMARTKEY STOPPED + key still lost → LOCKING");

            pendingLockAfterStop = false;
            lockScooterAndLaunchLogin();
        }
    }

    // =========================================================
    // ================= LOCK + LOGIN ==========================
    // =========================================================
    private void lockScooterAndLaunchLogin() {

        Log.e(TAG, "SMARTKEY LOCKING SCOOTER");

        EVLockService.getInstance().startLockSequence();

        if (UIUpdater.getInstance() != null) {
            UIUpdater.getInstance()
                    .stopKeylostRedAlertAnimation();
            UIUpdater.getInstance().stopBeeping();
        }

        Intent i = new Intent(this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
    }

    // =========================================================
    // ================= UTILS ================================
    // =========================================================
    private ScanSettings buildScanSettings() {
        return new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
    }

    private void startForegroundNotification() {

        String ch = "smartkey_channel";

        NotificationChannel nc = new NotificationChannel(
                ch, "SmartKey",
                NotificationManager.IMPORTANCE_LOW);

        NotificationManager nm =
                getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(nc);

        Notification n = new NotificationCompat.Builder(this, ch)
                .setContentTitle("SmartKey Active")
                .setContentText("Monitoring key presence")
                .setSmallIcon(
                        android.R.drawable.stat_sys_data_bluetooth)
                .build();

        startForeground(1, n);
    }

    // =========================================================
    // ================= CLEANUP ===============================
    // =========================================================
    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (scanner != null) scanner.stopScan(scanCallback);
        KeyPresenceManager.get().setListener(null);
        Log.i(TAG, "SMARTKEY BLEScanService STOPPED");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
