package com.fanconn.rideOS;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class Maps extends AppCompatActivity {

    private ImageButton returnbtn;
    private Button btnRequest;
    private TextView tvCharges;
    private ProgressBar progressBuffer;

    public static final String PREF_NAME = "rideos_prefs";
    public static final String KEY_REQUEST_STATUS = "request_status";

    // Handler safety (prevents WindowLeaked)
    private final Handler handler = new Handler();
    private final Runnable popupRunnable = this::showResultPopup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otaupdates);
        setFullScreen();

        returnbtn = findViewById(R.id.returnbtn);
        btnRequest = findViewById(R.id.btnRequest);
        tvCharges = findViewById(R.id.tvCharges);
        progressBuffer = findViewById(R.id.progressBuffer);

        returnbtn.setOnClickListener(v -> finish());

        btnRequest.setOnClickListener(v -> {

            // Persist requestStatus = 1
            setRequestStatus(this, 1);

            btnRequest.setVisibility(View.GONE);
            tvCharges.setVisibility(View.GONE);
            progressBuffer.setVisibility(View.VISIBLE);

            handler.postDelayed(popupRunnable, 2000);
        });
    }

    // -------------------- SharedPreferences --------------------

    public static void setRequestStatus(Context context, int status) {
        SharedPreferences prefs =
                context.getApplicationContext()
                        .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        prefs.edit().putInt(KEY_REQUEST_STATUS, status).apply();
    }

    public static int getRequestStatus(Context context) {
        SharedPreferences prefs =
                context.getApplicationContext()
                        .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        return prefs.getInt(KEY_REQUEST_STATUS, 0);
    }

    // -------------------- Popup --------------------

    private void showResultPopup() {
        if (isFinishing() || isDestroyed()) return;

        progressBuffer.setVisibility(View.GONE);

        Dialog dialog = new Dialog(this);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        dialog.setContentView(R.layout.popupdialog);
        dialog.setCancelable(false);

        TextView title = dialog.findViewById(R.id.dialogTitle);
        TextView message = dialog.findViewById(R.id.dialogMessage);
        Button btnYes = dialog.findViewById(R.id.btnYes);
        Button btnNo = dialog.findViewById(R.id.btnNo);

        title.setText("rideOS Maps");
        message.setText("Thank you for showing interest\nWe recieved your request for Maps\nOur Team will contact you shortly.");

        btnYes.setText("OK");
        btnNo.setVisibility(View.GONE);

        btnYes.setOnClickListener(v -> {
            dialog.dismiss();
            //finish();
        });

        dialog.show();
    }

    // -------------------- Gesture --------------------

    private final float[] startY = new float[1];

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (GestureHelper.handleGesture(this, ev, startY)) {
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    // -------------------- Fullscreen --------------------

    private void setFullScreen() {
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        decorView.setSystemUiVisibility(uiOptions);
    }

    // -------------------- Cleanup --------------------

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(popupRunnable);
        super.onDestroy();
    }
}
