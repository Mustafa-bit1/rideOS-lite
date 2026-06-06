package com.fanconn.rideOS;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class InactivityManager {
    private static InactivityManager instance;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Handler countdownHandler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;
    private Runnable countdownRunnable;
    private long timeoutMillis = 30000; // 40 seconds
    private int currentCode = -1;
    private InactivityCallback callback;
    private int secondsRemaining = 0;
    private boolean isRunning = false;
    private InactivityManager() {}
    public static final String PREFS_NAME = "rideos_settings";
    public static final String KEY_INACTIVITY_TIMEOUT_SEC = "inactivity_timeout_sec";

    public static synchronized InactivityManager getInstance() {
        if (instance == null) {
            instance = new InactivityManager();
        }
        return instance;
    }

    public void setCallback(InactivityCallback callback) {
        this.callback = callback;
    }
    public void init(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences("rideos_settings", Context.MODE_PRIVATE);

        int timeoutSec = prefs.getInt("inactivity_timeout_sec", 30); // default 20s
        timeoutMillis = timeoutSec * 1000L;
//        Log.d("TOUCHTIMER",
//               "Loaded inactivity timeout = " + timeoutSec + " seconds");
    }
    public void setTimeoutSeconds(Context context, int timeoutSec) {

        timeoutMillis = timeoutSec * 1000L;

        SharedPreferences prefs =
                context.getSharedPreferences("rideos_settings", Context.MODE_PRIVATE);

        prefs.edit()
                .putInt("inactivity_timeout_sec", timeoutSec)
                .apply();
//        Log.d("TOUCHTIMER",
//                "Timeout updated & saved = " + timeoutSec + " seconds");

        // Restart timer if active
        if (isRunning && currentCode != -1) {
            startTimer(currentCode);
        }
    }
    public int getTimeoutSeconds() {
        return (int) (timeoutMillis / 1000);
    }

    public void startTimer(int code) {
        stopTimer(); // Cancel any existing timer
        currentCode = code;
        isRunning = true;

        timeoutRunnable = () -> {
//           Log.d("TOUCHTIMER", " Timeout TOUCHTIMER triggered for code: " + currentCode);
            isRunning = false;
            if (callback != null) {
                callback.onInactivityTimeout(currentCode);
            }
        };

        // Start main timeout
        handler.postDelayed(timeoutRunnable, timeoutMillis);

        // Start countdown logger
        startCountdown((int) (timeoutMillis / 1000));
    }
    public void startTimer(int code, long customTimeoutMillis) {
        stopTimer();

        currentCode = code;
        isRunning = true;

        timeoutRunnable = () -> {
//            Log.d("TOUCHTIMER", " Timeout TOUCHTIMER triggered for code: " + currentCode);
            isRunning = false;
            if (callback != null) {
                callback.onInactivityTimeout(currentCode);
            }
        };

        handler.postDelayed(timeoutRunnable, customTimeoutMillis);

        startCountdown((int)(customTimeoutMillis / 1000));
    }
    private void startCountdown(int seconds) {
        secondsRemaining = seconds;
        countdownHandler.removeCallbacksAndMessages(null);

        countdownRunnable = new Runnable() {
            @Override
            public void run() {
//                Log.d("TOUCHTIMER","TOUCHTIMER Entered startcountdown");
                if (!isRunning) return; // Stop if timer stopped
              //  Log.d("TOUCHTIMER","TOUCHTIMER Entered startcountdown and not resturnd");

//                Log.d("TOUCHTIMER", " " + secondsRemaining + "s TOUCHTIMER remaining");
                secondsRemaining--;

                if (secondsRemaining >= 0) {
//                   Log.d("TOUCHTIMER","TOUCHTIMER Entered startcountdown and Entered cundownhandler");

                    countdownHandler.postDelayed(this, 1000);
                }
            }
        };
        countdownHandler.post(countdownRunnable);
    }

    public void resetTimer() {
        if (currentCode != -1) {
//            Log.d("TOUCHTIMER", " TOUCHTIMER Interaction detected — timer reset");
            startTimer(currentCode);
        }
        else{
//            Log.d("TOUCHTIMER","onUserInteraction() TOUCHTIMER NETER ELSEPARTOF RESTTIMER");
        }
    }
    public void ShortresetTimer(int code) {
        if (code != -1) {
//            Log.d("TOUCHTIMER", " TOUCHTIMER Short timer reset");
            startTimer(code,15000);
        }
        else{
//            Log.d("TOUCHTIMER","onUserInteraction() TOUCHTIMER ENTER ELSEPARTOF SHORTRESET");
        }
    }

    public void stopTimer() {
//        Log.d("TOUCHTIMER","TOUCHTIMER ENTER STOPTIMER");
        isRunning = false;
        handler.removeCallbacksAndMessages(null);
        countdownHandler.removeCallbacksAndMessages(null);
        timeoutRunnable = null;
        countdownRunnable = null;
        currentCode = -1;
    }

    public void onUserInteraction() {
        resetTimer();
    }
    public void ShortonUserInteraction(int code) {
        ShortresetTimer(code);
    }

    public void updateSpeed(double speed) {
        if (speed != 0) {
            resetTimer();
        }
    }


    public interface InactivityCallback {
        void onInactivityTimeout(int code);
    }
}
