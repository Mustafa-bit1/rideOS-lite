package com.fanconn.rideOS.Smartkey;


import android.util.Log;

import java.util.ArrayDeque;
import java.util.Deque;

public class KeyPresenceManager {
    private static final String TAG = "KeyPresenceManager";
    private static final int MAX_SAMPLES = 12;
    private static final double EMA_ALPHA = 0.3;
    private static final int OUT_OF_RANGE_MISSES = 1;
    private static final long OUT_OF_RANGE_MS = 5000L;
    // Unlock only if key is strong signal (> -65),
// but don't immediately lock until it's much weaker (< -75)
    private static final int RSSI_IN_RANGE_THRESHOLD = -65;
    private static final int RSSI_OUT_RANGE_THRESHOLD = -75;
    private static KeyPresenceManager instance;
    private final Deque<Integer> samples = new ArrayDeque<>();
    private double ema = Double.NaN;
    private int consecutiveMisses = 0;
    private boolean isInRange = false;
    private long lastSeenTs = 0;

    public interface PresenceListener {
        void onInRange();
        void onOutOfRange();
    }

    private PresenceListener listener;

    private KeyPresenceManager() {}

    public static synchronized KeyPresenceManager get() {
        if (instance == null) instance = new KeyPresenceManager();
        return instance;
    }

    public void setListener(PresenceListener l) {
        this.listener = l;
    }

    /**
     * Called whenever an advertisement from the target key is received.
     */
    public synchronized void onAdvertisement(int rssi, long ts) {
        lastSeenTs = ts;
        consecutiveMisses = 0;

        // Update Exponential Moving Average (EMA)
        if (Double.isNaN(ema)) ema = rssi;
        else ema = EMA_ALPHA * rssi + (1 - EMA_ALPHA) * ema;

        samples.addLast(rssi);
        if (samples.size() > MAX_SAMPLES) samples.removeFirst();

        // Detect in-range transition
        // --- Hysteresis logic for smoother detection ---
        if (!isInRange && ema > RSSI_IN_RANGE_THRESHOLD && samples.size() >= 2) {
            // Become in-range when signal is strong enough
            isInRange = true;
            Log.i(TAG, "🟢 SMARTKEY: Key entered IN RANGE (EMA " + ema + " dBm)");
            if (listener != null) listener.onInRange();
        }
        else if (isInRange && ema < RSSI_OUT_RANGE_THRESHOLD) {
            // Mark out of range only if signal drops below lower threshold
            isInRange = false;
            Log.i(TAG, "🔴 SMARTKEY: Key dropped OUT OF RANGE (EMA " + ema + " dBm)");
            if (listener != null) listener.onOutOfRange();
        }

    }

    /**
     * Called periodically (e.g., from BLEScanService every second) to detect timeout.
     */
    public synchronized void tickCheck(long nowTs) {
        if (nowTs - lastSeenTs > 1000) {
            consecutiveMisses++;
        }

        if (isInRange) {
            if (consecutiveMisses >= OUT_OF_RANGE_MISSES && (nowTs - lastSeenTs) >= OUT_OF_RANGE_MS) {
                isInRange = false;
                Log.i(TAG, "🔴 notifyOutOfRange() — Key timed out");
                if (listener != null) listener.onOutOfRange();
            }
        }
    }

    /**
     * Manual trigger for IN RANGE (used by BLEScanService).
     */
    public synchronized void notifyInRange() {
        if (!isInRange) {
            isInRange = true;
            consecutiveMisses = 0;
            lastSeenTs = System.currentTimeMillis();
            Log.i(TAG, "🟢 notifyInRange() called manually");
            if (listener != null) listener.onInRange();
        }
    }

    /**
     * Manual trigger for OUT OF RANGE (used by BLEScanService).
     */
    public synchronized void notifyOutOfRange() {
        if (isInRange) {
            isInRange = false;
            Log.i(TAG, "🔴 notifyOutOfRange() called manually");
            if (listener != null) listener.onOutOfRange();
        }
    }

    public synchronized boolean getIsInRange() {
        return isInRange;
    }
}
