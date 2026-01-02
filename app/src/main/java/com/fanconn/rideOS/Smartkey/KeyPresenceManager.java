package com.fanconn.rideOS.Smartkey;

import android.util.Log;
import java.util.ArrayDeque;
import java.util.Deque;

public class KeyPresenceManager {

    private static final String TAG = "SMARTKEY-KeyMgr";

    // ================= CONFIG =================
    private static final int MAX_SAMPLES = 12;
    private static final double EMA_ALPHA = 0.3;

    private static final int RSSI_IN_RANGE_THRESHOLD = -75;
    private static final int RSSI_OUT_RANGE_THRESHOLD = -80;

    private static final long OUT_OF_RANGE_TIMEOUT_MS = 5000;
    private static final int OUT_OF_RANGE_CONFIRM_COUNT = 3;

    // ================= STATE =================
    private static KeyPresenceManager instance;

    private final Deque<Integer> samples = new ArrayDeque<>();
    private double ema = Double.NaN;

    private boolean keyInRange = false;

    /**
     * 🔑 Unlock allowed ONLY when this is true
     * - TRUE after confirmed out-of-range
     * - FALSE after unlock OR manual lock
     */
    private boolean unlockArmed = true;

    private long lastSeenTs = 0;
    private int timeoutMisses = 0;
    private int weakRssiMisses = 0;

    // ================= LISTENER =================
    public interface PresenceListener {
        void onFirstInRange();   // unlock
        void onConfirmedOut();   // key lost
    }

    private PresenceListener listener;

    private KeyPresenceManager() {}

    public static synchronized KeyPresenceManager get() {
        if (instance == null) instance = new KeyPresenceManager();
        return instance;
    }

    public synchronized void setListener(PresenceListener l) {
        this.listener = l;
    }

    // =========================================================
    // ================= BLE ADVERTISEMENT =====================
    // =========================================================
    public synchronized void onAdvertisement(int rssi, long ts) {

        lastSeenTs = ts;
        timeoutMisses = 0;

        // ---------- EMA ----------
        if (Double.isNaN(ema)) ema = rssi;
        else ema = EMA_ALPHA * rssi + (1 - EMA_ALPHA) * ema;

        samples.addLast(rssi);
        if (samples.size() > MAX_SAMPLES) samples.removeFirst();

        Log.d(TAG, "SMARTKEY ADV rssi=" + rssi + " ema=" + String.format("%.1f", ema));

        // ---------------- FIRST IN-RANGE ----------------
        if (!keyInRange && ema > RSSI_IN_RANGE_THRESHOLD && samples.size() >= 2) {

            keyInRange = true;
            weakRssiMisses = 0;

            if (unlockArmed) {
                Log.i(TAG, "SMARTKEY FIRST IN-RANGE → UNLOCK EVENT");
                unlockArmed = false;
                if (listener != null) listener.onFirstInRange();
            } else {
                Log.i(TAG, "SMARTKEY IN-RANGE (unlock blocked)");
            }
            return;
        }

        // ---------------- WEAK RSSI DETECTION ----------------
        if (keyInRange && ema < RSSI_OUT_RANGE_THRESHOLD) {

            weakRssiMisses++;

            Log.w(TAG, "SMARTKEY Weak RSSI "
                    + weakRssiMisses + "/" + OUT_OF_RANGE_CONFIRM_COUNT);

            if (weakRssiMisses >= OUT_OF_RANGE_CONFIRM_COUNT) {
                confirmOutOfRange("RSSI weak x" + weakRssiMisses);
            }
        } else {
            weakRssiMisses = 0;
        }
    }

    // =========================================================
    // ================= TIMEOUT CHECK =========================
    // =========================================================
    public synchronized void tickCheck(long now) {

        if (!keyInRange) return;

        if (now - lastSeenTs > 1000) {
            timeoutMisses++;

            Log.w(TAG, "SMARTKEY Timeout miss "
                    + timeoutMisses + "/" + OUT_OF_RANGE_CONFIRM_COUNT);
        }

        if ((now - lastSeenTs) >= OUT_OF_RANGE_TIMEOUT_MS
                && timeoutMisses >= OUT_OF_RANGE_CONFIRM_COUNT) {

            confirmOutOfRange("timeout " + (now - lastSeenTs) + "ms");
        }
    }

    // =========================================================
    // ================= CONFIRM OUT ===========================
    // =========================================================
    private void confirmOutOfRange(String reason) {

        if (!keyInRange) return;

        keyInRange = false;
        unlockArmed = true;
        weakRssiMisses = 0;
        timeoutMisses = 0;

        Log.e(TAG, "SMARTKEY CONFIRMED OUT → " + reason
                + " | unlock ARMED");

        if (listener != null) listener.onConfirmedOut();
    }

    // =========================================================
    // ================= MANUAL LOCK ===========================
    // =========================================================
    public synchronized void notifyManualLock() {
        unlockArmed = false;
        Log.w(TAG,
                "SMARTKEY MANUAL LOCK → unlock DISABLED until key exits range");
    }

    // =========================================================
    // ================= GETTERS ===============================
    // =========================================================
    public synchronized boolean isInRange() {
        return keyInRange;
    }
}
