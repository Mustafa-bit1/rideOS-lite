package com.fanconn.rideOS;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
    private ProgressBar progressBar;
    private TextView statusText;
    private Handler handler;
    private boolean countdownStarted = false;
    private boolean finalUiShown = false;

    /* ================= PROGRESS RECEIVER ================= */
    private final BroadcastReceiver progressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            int progress = intent.getIntExtra("progress", 0);
            //Log.d(TAG, "Progress broadcast = " + progress);

            if (finalUiShown) return;

            if (progress == -1) {
                showFailureUI();
                return;
            }

            progressBar.setProgress(progress);

            if (progress < 10) {
                statusText.setText("Powering the future...");
            }
            else if (progress < 20) {
                statusText.setText("Preparing system for update...");
            }
            else if (progress < 70) {
                statusText.setText("Downloading...");
            }
            else if (progress < 90) {
                statusText.setText("Finalizing download...");
            }
            else if (progress < 100) {
                statusText.setText("Update ready. Preparing installation...");
            }
            else {
                if (!countdownStarted) {
                    countdownStarted = true;
                    statusText.setText("Optimizing system for update");
                    startCountdown();
                }
            }

        }
    };

    /* ================= COUNTDOWN ================= */

    private void startCountdown() {
        final int[] sec = {5};

        //  Show success image when countdown starts
        otsSuccessImg.setVisibility(View.VISIBLE);
        Log.i(TAG, "OTA success image shown (countdown started)");

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (sec[0] > 0) {
                    statusText.setText("Installing update in " + sec[0] + "s");
                    sec[0]--;
                    handler.postDelayed(this, 1000);
                } else {
                    Log.i(TAG, "Countdown finished → starting install");

                    //  Hide image before installation
                    otsSuccessImg.setVisibility(View.GONE);
                    Log.i(TAG, "OTA success image hidden (installation started)");

                    statusText.setText("Restarting...");

                    //  Trigger silent install
                    Intent installIntent =
                            new Intent(UpdateService.ACTION_INSTALL_UPDATE);

                    LocalBroadcastManager
                            .getInstance(UpdateProgressActivity.this)
                            .sendBroadcast(installIntent);
                }
            }
        }, 1000);
    }


    /* ================= OK BUTTON ================= */

    private void showOkButton() {
        finalUiShown = true;
        statusText.setText("Ready to install update");

        TextView ok = new TextView(this);
        ok.setText("OK");
        ok.setTextSize(22f);
        ok.setTextColor(getResources().getColor(android.R.color.white));
        ok.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
        ok.setPadding(50, 25, 50, 25);

        LinearLayout root = findViewById(R.id.rootLayout);
        root.addView(ok);

        ok.setOnClickListener(v -> {
            Log.i(TAG, "User confirmed installation");

            Intent installIntent =
                    new Intent(UpdateService.ACTION_INSTALL_UPDATE);

            LocalBroadcastManager
                    .getInstance(this)
                    .sendBroadcast(installIntent);

            statusText.setText("Installing update...");
            ok.setEnabled(false);
        });
    }

    /* ================= FAILURE ================= */

    private void showFailureUI() {
        finalUiShown = true;
        statusText.setText("Update failed");

        TextView ok = new TextView(this);
        ok.setText("OK");
        ok.setTextColor(getResources().getColor(android.R.color.white));
        ok.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
        ok.setPadding(40, 20, 40, 20);

        ((LinearLayout) findViewById(R.id.rootLayout)).addView(ok);
        ok.setOnClickListener(v -> finish());
    }

    /* ================= LIFECYCLE ================= */
    private ImageView otsSuccessImg;

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
        handler = new Handler(Looper.getMainLooper());

        progressBar.setMax(100);
        InactivityManager.getInstance().stopTimer();
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(
                        progressReceiver,
                        new IntentFilter("com.fanconn.UPDATE_PROGRESS")
                );
        otsSuccessImg = findViewById(R.id.otssuccesimg);

        Log.d(TAG, "UpdateProgressActivity started");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(progressReceiver);
        Log.d(TAG, "UpdateProgressActivity destroyed");
    }
}
