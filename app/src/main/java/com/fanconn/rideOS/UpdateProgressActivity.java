package com.fanconn.rideOS;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.fanconn.rideOS.R;

public class UpdateProgressActivity extends Activity {
    private ProgressBar progressBar;
    private TextView statusText;
    private Handler handler;
    private boolean countdownStarted = false;

    private final BroadcastReceiver progressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int progress = intent.getIntExtra("progress", 0);
            progressBar.setProgress(progress);

            if (progress < 10) {
                statusText.setText("Powering the future!");
            } else if (progress < 17) {
                statusText.setText("Ready to download . . .");
            } else if (progress < 70) {
                statusText.setText("Downloading . . .");
            } else if (progress < 100) {
                statusText.setText("Installing... your upgrade is almost there!");
            } else {
                statusText.setText("Your EV is Upgraded!");
                if (!countdownStarted) {
                    countdownStarted = true;
                    showCountdown();
                }
            }
            if (progress == -1) {
                statusText.setText("Update failed. Try again later.");

                TextView okBtn = new TextView(UpdateProgressActivity.this);
                okBtn.setText("OK");
                okBtn.setTextSize(20f);
                okBtn.setTextColor(getResources().getColor(android.R.color.white));
                okBtn.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
                okBtn.setPadding(30, 20, 30, 20);
                okBtn.setClickable(true);

                ((LinearLayout) findViewById(R.id.rootLayout)).addView(okBtn);

                okBtn.setOnClickListener(v -> finish());
                return;
            }

        }
    };

    private void showCountdown() {
        final int[] countdown = {5};
        //Log.d("UpdateProgress", "Starting countdown");

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (countdown[0] > 0) {
                    String msg = "Restarting in " + countdown[0] + "s";
                    //Log.d("UpdateProgress", msg);
                    statusText.setText(msg);
                    countdown[0]--;
                    handler.postDelayed(this, 1000);
                } else {
                    //Log.d("UpdateProgress", "Countdown complete, showing success UI");
                    showSuccessPrompt(); // 🔄 Instead of finish()
                }
            }
        });
    }
    private void showSuccessPrompt() {
        statusText.setText("Successfully Updated!");

        TextView rebootBtn = new TextView(this);
        rebootBtn.setText("REBOOT");
        rebootBtn.setTextSize(22f);
        rebootBtn.setTextColor(getResources().getColor(android.R.color.white));
        rebootBtn.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
        rebootBtn.setPadding(40, 20, 40, 20);
        rebootBtn.setGravity(View.TEXT_ALIGNMENT_CENTER);
        rebootBtn.setClickable(true);

        ((LinearLayout) findViewById(R.id.rootLayout)).addView(rebootBtn);

        rebootBtn.setOnClickListener(v -> {
            //Log.d("UpdateProgress", "Restarting app...");
            try {
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(launchIntent);
                }
                android.os.Process.killProcess(android.os.Process.myPid());
            } catch (Exception e) {
                //Log.e("UpdateProgress", "Reboot failed: " + e.getMessage());
                statusText.setText("Restart failed. Please relaunch manually.");
                rebootBtn.setVisibility(View.GONE);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fullscreen
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        setContentView(R.layout.activity_update_progress);

        progressBar = findViewById(R.id.progressBar);
        statusText = findViewById(R.id.statusText);
        handler = new Handler();

        progressBar.setMax(100);

        // Register for progress updates
        LocalBroadcastManager.getInstance(this).registerReceiver(progressReceiver, new IntentFilter("com.fanconn.UPDATE_PROGRESS"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister
        LocalBroadcastManager.getInstance(this).unregisterReceiver(progressReceiver);
    }
}
