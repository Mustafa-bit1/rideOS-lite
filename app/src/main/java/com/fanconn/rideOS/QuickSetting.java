package com.fanconn.rideOS;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.fanconn.rideOS.menu.BrightnessManager;
import com.fanconn.rideOS.menu.settings;

import java.lang.reflect.Method;

public class QuickSetting extends AppCompatActivity {

    private GestureDetector gestureDetector;
    private BrightnessManager brightnessManager;

    private final Handler handler = new Handler(Looper.getMainLooper());
    ImageButton tpmsBtn,allSetbtn;

    private Button wifiToggle, autoBrightnessToggle,blue_tpmsbtn;
    //private Button Settingbtn;
    //private Button lteToggle;
    //private TextView wifiStatusText, autoBrightnessText, lteStatusText,autoBrightnessStatus;
    private TextView autoBrightnessText;
    private SeekBar brightnessSlider;

    /* ---------------- WIFI RECEIVER ---------------- */

    private final BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            if (wm != null) {
                updateWifiUI(wm);
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(wifiReceiver, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(wifiReceiver);
    }

    /* ---------------- ACTIVITY ---------------- */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_quick_setting);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, 0);
            return insets;
        });

        gestureDetector = new GestureDetector(this, new QuickGestureListener());

        // WRITE_SETTINGS permission
        if (!Settings.System.canWrite(this)) {
            startActivity(new Intent(
                    Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:" + getPackageName())
            ));
        }
//        tpmsBtn = findViewById(R.id.tpmsbtn);
        allSetbtn = findViewById(R.id.allSetbtn);
        allSetbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(QuickSetting.this,settings.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });
        /* ---------------- BRIGHTNESS ---------------- */

        brightnessManager = new BrightnessManager(this);
        brightnessSlider = findViewById(R.id.brightnessSlider);
        autoBrightnessText = findViewById(R.id.autoBrightnessText);
        //autoBrightnessStatus = findViewById(R.id.autoBrightnessStatus);
        autoBrightnessToggle = findViewById(R.id.autoBrightnessToggle);

        brightnessManager.applyToSeekBar(brightnessSlider, autoBrightnessText);
        syncBrightnessUI();

        autoBrightnessToggle.setOnClickListener(v -> {
            boolean wasAuto = brightnessManager.isAutoBrightnessEnabled();

            brightnessManager.setAutoBrightnessEnabled(!wasAuto);
            syncBrightnessUI();

            // 🔥 VERY IMPORTANT
            if (wasAuto) {
                // Auto → Manual transition
                rebindBrightnessSlider();
            }
        });

        /* ---------------- VOLUME ---------------- */

        SeekBar volumeSlider = findViewById(R.id.volumeSlider);
        ImageButton muteButton = findViewById(R.id.muteButton);
        brightnessManager.applyToVolumeSlider(volumeSlider, muteButton);

        /* ---------------- WIFI ---------------- */

        wifiToggle = findViewById(R.id.wifiToggle);
       // wifiStatusText = findViewById(R.id.wifiStatusText);

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        updateWifiUI(wifiManager);

        wifiToggle.setOnClickListener(v -> {
            boolean currentlyEnabled = wifiManager.isWifiEnabled();

            // 🔥 Instant UI feedback
            setWifiUiImmediate(!currentlyEnabled);

            // Actual system toggle (async)
            wifiManager.setWifiEnabled(!currentlyEnabled);
        });


        wifiToggle.setOnLongClickListener(v -> {
            Intent i = new Intent(this, com.fanconn.rideOS.menu.menu.class);
            i.putExtra("openFragment", "wireless");
            startActivity(i);
            finish();
            return true;
        });

        /* ---------------- SETTINGS & TPMS ---------------- */

        //Settingbtn = findViewById(R.id.Settingbtn);
        blue_tpmsbtn = findViewById(R.id.blue_tpmsbtn);

//        Settingbtn.setOnClickListener(v -> {
//            startActivity(new Intent(this, settings.class));
//            finish();
//        });

        blue_tpmsbtn.setOnClickListener(v -> {
            startActivity(new Intent(this, TPMS_activity.class));
            finish();
        });

        /* ---------------- LTE ---------------- */

//        lteToggle = findViewById(R.id.ltetoggle);
//        lteStatusText = findViewById(R.id.ltetoggleStatusText);
//
//        updateMobileDataUI();
//        lteToggle.setOnClickListener(v -> toggleMobileData());
    }

    /* ---------------- BRIGHTNESS UI ---------------- */

    private void syncBrightnessUI() {
        boolean auto = brightnessManager.isAutoBrightnessEnabled();

        int on = Color.parseColor("#1C78BE");
        int off = Color.parseColor("#555555");

        autoBrightnessToggle.setBackgroundTintList(
                ColorStateList.valueOf(auto ? on : off)
        );
        autoBrightnessToggle.setText(auto ? "Auto Brightness ON" : "Auto Brightness OFF");
        //autoBrightnessStatus.setText((auto ? "Auto Brightness ON" : "Auto Brightness OFF"));
        if(auto){
            autoBrightnessText.setText("Auto brightness ON");
        }
        brightnessSlider.setEnabled(!auto);
        brightnessSlider.setAlpha(auto ? 0.4f : 1.0f);
    }

    /* ---------------- WIFI UI ---------------- */

    private void updateWifiUI(WifiManager wm) {
        int on = Color.parseColor("#1C78BE");
        int off = Color.parseColor("#555555");

        if (wm.isWifiEnabled()) {
            wifiToggle.setBackgroundTintList(ColorStateList.valueOf(on));
//            WifiInfo info = wm.getConnectionInfo();
//            wifiStatusText.setText(
//                    info != null && info.getNetworkId() != -1
//                            ? info.getSSID().replace("\"", "")
//                            : "Not connected"
//            );
        } else {
            wifiToggle.setBackgroundTintList(ColorStateList.valueOf(off));
//            wifiStatusText.setText("");
        }
    }

    /* ---------------- MOBILE DATA ---------------- */

    private void toggleMobileData() {
        if (!hasSimCard()) {
            Toast.makeText(this, "No SIM card detected", Toast.LENGTH_SHORT).show();
            return;
        }
        setMobileDataEnabled(!getMobileDataState());
        updateMobileDataUI();
    }

    private void updateMobileDataUI() {
//        int on = Color.parseColor("#1C78BE");
//        int off = Color.parseColor("#555555");
//
//        boolean enabled = getMobileDataState();
//        lteToggle.setBackgroundTintList(ColorStateList.valueOf(enabled ? on : off));
//        lteStatusText.setText(enabled ? "Data On" : "Data Off");
    }

    private boolean hasSimCard() {
        TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        return tm != null && tm.getSimState() == TelephonyManager.SIM_STATE_READY;
    }

    private boolean getMobileDataState() {
        try {
            TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            Method m = TelephonyManager.class.getDeclaredMethod("getDataEnabled");
            m.setAccessible(true);
            return (boolean) m.invoke(tm);
        } catch (Exception e) {
            return false;
        }
    }

    private void setMobileDataEnabled(boolean enabled) {
        try {
            TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            Method m = TelephonyManager.class.getDeclaredMethod("setDataEnabled", boolean.class);
            m.setAccessible(true);
            m.invoke(tm, enabled);
        } catch (Exception e) {
            Toast.makeText(this, "Requires system privileges", Toast.LENGTH_SHORT).show();
        }
    }

    /* ---------------- GESTURES ---------------- */

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.slide_out_up);
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private class QuickGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
            if (e2.getY() - e1.getY() < -100) {
                finish();
                return true;
            }
            return false;
        }
    }
    private void rebindBrightnessSlider() {
        brightnessSlider.setOnSeekBarChangeListener(null);

        brightnessManager.applyToSeekBar(
                brightnessSlider,
                autoBrightnessText
        );

        // Force one write to system
        int current = Settings.System.getInt(
                getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS,
                128
        );
        Settings.System.putInt(
                getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS,
                current
        );
    }
    private void setWifiUiImmediate(boolean turningOn) {
        int on = Color.parseColor("#1C78BE");
        int off = Color.parseColor("#555555");

        if (turningOn) {
            wifiToggle.setBackgroundTintList(ColorStateList.valueOf(on));
//            wifiStatusText.setText("Turning ON...");
        } else {
            wifiToggle.setBackgroundTintList(ColorStateList.valueOf(off));
//            wifiStatusText.setText("Turning OFF...");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncBrightnessUI();
    }
}
