package com.fanconn.rideOS;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class InactivityManager {
    private static InactivityManager instance;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;
    private long timeoutMillis = 40000; // 30 seconds
    private int currentCode = -1;
    private InactivityCallback callback;
    private InactivityManager() {}

    public static synchronized InactivityManager getInstance() {
        if (instance == null) {
            instance = new InactivityManager();
        }
        return instance;
    }

    // Set the callback listener
    public void setCallback(InactivityCallback callback) {
        this.callback = callback;
    }

    // Start the inactivity timer
    public void startTimer(int code) {
        stopTimer(); // Cancel any existing timer
        currentCode = code;

        timeoutRunnable = () -> {
//            Log.d("InactivityManager", "Timeout triggered for code: " + currentCode);
            if (callback != null) {
                callback.onInactivityTimeout(currentCode);
            }
        };

        handler.postDelayed(timeoutRunnable, timeoutMillis);
        //Log.d("InactivityManager", "Timer started with code: " + code);
    }

    public void resetTimer() {
        if (currentCode != -1) {
            startTimer(currentCode);
        }
    }

    // ✋ Stop the timer
    public void stopTimer() {
        if (timeoutRunnable != null) {
            handler.removeCallbacks(timeoutRunnable);
            timeoutRunnable = null;
        }
        currentCode = -1;
        //Log.d("InactivityManager", "Timer stopped.");
    }

    public void onUserInteraction() {
        resetTimer();
        //Log.d("InactivityManager", "User interaction detected. Timer reset.");
    }
    public void updateSpeed(double speed) {
        if (speed != 0) {
            resetTimer();
        }
    }
    // Interface for activity callback
    public interface InactivityCallback {
        void onInactivityTimeout(int code);
    }
}
