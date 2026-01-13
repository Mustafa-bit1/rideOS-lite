package com.fanconn.rideOS;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class UpdateProgressActivity extends Activity {

    private static final String TAG = "UpdateProgressActivity";

    /* ================= TIMEOUT CONFIG ================= */
    private static final long STUCK_TIMEOUT_MS = 20_000;      // 20 seconds
    private static final long WATCHDOG_INTERVAL_MS = 1000;    // 1 second

    /* ================= UI ================= */
    private ProgressBar progressBar;
    private TextView statusText;
    private TextView statuspercentText;
    private ImageView otsSuccessImg;

    /* ================= STATE ================= */
    private Handler handler;
    private boolean countdownStarted = false;
    private boolean finalUiShown = false;

    private int lastProgressValue = -1;
    private long lastProgressTime = 0;

    private Runnable watchdogRunnable;

    /* ================= PROGRESS RECEIVER ================= */
    private final BroadcastReceiver progressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            int progress = intent.getIntExtra("progress", 0);

            if (finalUiShown) return;

            /* ===== FAILURE FROM SERVICE ===== */
            if (progress == -1) {
                Log.e(TAG, "Service reported download failure");
                showFailureAndRestart();
                return;
            }

            /* ===== UPDATE WATCHDOG STATE ===== */
            if (progress != lastProgressValue) {
                lastProgressValue = progress;
                lastProgressTime = System.currentTimeMillis();
            }

            /* ===== UI UPDATE ===== */
            progressBar.setProgress(progress);
            statuspercentText.setText(progress + "%");
            InactivityManager.getInstance().stopTimer();

            if (progress < 80) {
                statusText.setText("Downloading...");
            } else if (progress < 100) {
                statusText.setText("Installing update...");
            } else if (!countdownStarted) {
                countdownStarted = true;
                startCountdown();
            }
        }
    };

    /* ================= WATCHDOG ================= */
    private void startWatchdog() {
        lastProgressTime = System.currentTimeMillis();

        watchdogRunnable = new Runnable() {
            @Override
            public void run() {

                if (finalUiShown) return;

                long now = System.currentTimeMillis();

                if (now - lastProgressTime >= STUCK_TIMEOUT_MS) {
                    Log.e(TAG, "WATCHDOG: Download stuck at " + lastProgressValue + "%");
                    showFailureAndRestart();
                    return;
                }

                handler.postDelayed(this, WATCHDOG_INTERVAL_MS);
            }
        };

        handler.postDelayed(watchdogRunnable, WATCHDOG_INTERVAL_MS);
    }

    /* ================= COUNTDOWN ================= */
    private void startCountdown() {
        final int[] sec = {5};

        otsSuccessImg.setVisibility(View.VISIBLE);
        Log.i(TAG, "OTA success image shown");

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (sec[0] > 0) {
                    statusText.setText("Restarting system in " + sec[0]);
                    sec[0]--;
                    handler.postDelayed(this, 1000);
                } else {
                    Log.i(TAG, "Countdown finished → installing");

                    Intent installIntent =
                            new Intent(UpdateService.ACTION_INSTALL_UPDATE);

                    LocalBroadcastManager
                            .getInstance(UpdateProgressActivity.this)
                            .sendBroadcast(installIntent);

                    otsSuccessImg.setVisibility(View.GONE);
                }
            }
        }, 1000);
    }

    /* ================= FAILURE + RESTART ================= */
    private void showFailureAndRestart() {
        if (finalUiShown) return;

        finalUiShown = true;
        statusText.setTextColor(Color.RED);
        statusText.setText("Download failed");

        handler.postDelayed(this::restartAppFresh, 3000);
    }

    private void restartAppFresh() {
        Log.i(TAG, "Restarting app fresh");

        Intent launchIntent =
                getPackageManager().getLaunchIntentForPackage(getPackageName());

        if (launchIntent != null) {
            launchIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
            );
            startActivity(launchIntent);
        }

        finish();
        Runtime.getRuntime().exit(0);
    }

    /* ================= LIFECYCLE ================= */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        setContentView(R.layout.activity_update_progress);

        progressBar = findViewById(R.id.progressBar);
        statusText = findViewById(R.id.statusText);
        statuspercentText = findViewById(R.id.statuspercentText);
        otsSuccessImg = findViewById(R.id.otssuccesimg);

        handler = new Handler(Looper.getMainLooper());
        progressBar.setMax(100);

        InactivityManager.getInstance().stopTimer();

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(
                        progressReceiver,
                        new IntentFilter("com.fanconn.UPDATE_PROGRESS")
                );

        startWatchdog();

        Log.d(TAG, "UpdateProgressActivity started");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(progressReceiver);

        if (handler != null && watchdogRunnable != null) {
            handler.removeCallbacks(watchdogRunnable);
        }

        Log.d(TAG, "UpdateProgressActivity destroyed");
    }
}
