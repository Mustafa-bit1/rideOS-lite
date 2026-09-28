package com.fanconn.rideOS;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.database.ContentObserver;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.fanconn.rideOS.menu.BrightnessManager;
import com.fanconn.rideOS.menu.settings;
import com.fanconn.rideOS.telemetry.VehicleTelemetryManager;

public class QuickSettingsView extends FrameLayout {
    private static final int TOUCH_SLOP = 8; // px (small movement threshold)
    private float totalDrag = 0f;
    private BrightnessManager brightnessManager;
    private AudioManager audioManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Button allSetbtn;
    private Button wifiToggle, autoBrightnessToggle, blue_tpmsbtn,GPS_btn;
    private TextView autoBrightnessText;
    private SeekBar brightnessSlider, volumeSlider;
    private ImageButton muteButton;
    private float startY;
    private boolean dragging;
    private SharedPreferences prefs;
    private ContentObserver brightnessObserver;
    private TextView currentRiderTV;
    private static final String PREF_GPS_TELEMETRY =
            "pref_gps_telemetry";

    /* ================= WIFI RECEIVER ================= */

    private final BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            WifiManager wm =
                    (WifiManager) context.getApplicationContext()
                            .getSystemService(Context.WIFI_SERVICE);
            if (wm != null) updateWifiUI(wm);
        }
    };

    /* ================= VOLUME RECEIVER ================= */

    private final BroadcastReceiver volumeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handler.post(this::syncVolumeUI);
        }

        private void syncVolumeUI() {

            int vol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int sliderValue = brightnessManager.systemToSliderVolume(vol, 100);
            volumeSlider.setProgress(vol == 0 ? 0 : sliderValue);
            updateMuteIcon(vol == 0);
        }
    };

    /* ================= CONSTRUCTORS ================= */

    public QuickSettingsView(Context context) {
        super(context);
        init(context);
    }

    public QuickSettingsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /* ================= INIT ================= */

    private void init(Context context) {
        LayoutInflater.from(context)
                .inflate(R.layout.view_quick_settings, this, true);

        setClickable(true);
        setFocusable(true);
        prefs = context.getSharedPreferences("menu", Context.MODE_PRIVATE);
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        brightnessManager = new BrightnessManager(context);

        if (!Settings.System.canWrite(context)) {
            context.startActivity(new Intent(
                    Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:" + context.getPackageName()))
            );
        }

        setupUI(context);
        updateTpmsButtonColor();
        registerObservers(context);

        post(() -> setTranslationY(-getHeight()));
    }

    /* ================= UI ================= */

    private void setupUI(Context context) {

        allSetbtn = findViewById(R.id.allSetbtn);

//        allSetbtn.setOnClickListener(v -> {
//            Intent intent = new Intent(context, settings.class);
//            context.startActivity(intent);
//
//            if (context instanceof android.app.Activity) {
//                ((android.app.Activity) context).overridePendingTransition(0,0);
//            }
//        });
        allSetbtn.setOnClickListener(v -> {

            Intent intent = new Intent(context, settings.class);
            context.startActivity(intent);

            if (context instanceof Activity) {
                ((Activity) context).overridePendingTransition(0, 0);
            }

        });
        /* ---------- BRIGHTNESS ---------- */

        brightnessSlider = findViewById(R.id.brightnessSlider);
        autoBrightnessText = findViewById(R.id.autoBrightnessText);
        autoBrightnessToggle = findViewById(R.id.autoBrightnessToggle);

        brightnessManager.applyToSeekBar(brightnessSlider, autoBrightnessText);
        syncBrightnessUI();

        autoBrightnessToggle.setOnClickListener(v -> {
            boolean auto = brightnessManager.isAutoBrightnessEnabled();
            brightnessManager.setAutoBrightnessEnabled(!auto);
            syncBrightnessUI();
            rebindBrightnessSlider();
        });

        //-------GPS--------

        GPS_btn = findViewById(R.id.GPS_btn);
        boolean gpsEnabled =
                prefs.getBoolean(PREF_GPS_TELEMETRY, true);


        VehicleTelemetryManager.getInstance()
                .setGpsTelemetryEnabled(gpsEnabled);

        syncGpsUI();
        GPS_btn.setOnClickListener(v -> {

//            boolean enabled =
//                    TelemetryDataManager.getInstance()
//                            .isGpsTelemetryEnabled();
            boolean enabled =
                    VehicleTelemetryManager.getInstance()
                            .isGpsTelemetryEnabled();

            enabled = !enabled;

            VehicleTelemetryManager.getInstance()
                    .setGpsTelemetryEnabled(enabled);

            prefs.edit()
                    .putBoolean(PREF_GPS_TELEMETRY, enabled)
                    .apply();

            syncGpsUI();
        });

        /* ---------- VOLUME ---------- */

        volumeSlider = findViewById(R.id.volumeSlider);
        muteButton = findViewById(R.id.muteButton);

        currentRiderTV = findViewById(R.id.currentRiderTV);
        String riderName = getCurrentRider();
        if (!riderName.isEmpty()) {
            currentRiderTV.setText("Current rider: "+riderName);
        } else {
            currentRiderTV.setText("No Rider");
        }

        brightnessManager.applyToVolumeSlider(volumeSlider, muteButton);
        syncVolumeUI();

        /* ---------- WIFI ---------- */

        wifiToggle = findViewById(R.id.wifiToggle);
        WifiManager wifiManager =
                (WifiManager) context.getApplicationContext()
                        .getSystemService(Context.WIFI_SERVICE);

        updateWifiUI(wifiManager);

        wifiToggle.setOnClickListener(v -> {
            boolean enabled = wifiManager.isWifiEnabled();
            wifiManager.setWifiEnabled(!enabled);
            setWifiUiImmediate(!enabled);
        });
        wifiToggle.setOnLongClickListener(v -> {

            v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);

            Context ctx = getContext();

            Intent intent = new Intent(ctx, com.fanconn.rideOS.menu.menu.class);
            intent.putExtra("openFragment", "wireless");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            ctx.startActivity(intent);

            smoothClose();

            return true;
        });

        /* ---------- TPMS ---------- */

        blue_tpmsbtn = findViewById(R.id.blue_tpmsbtn);

        blue_tpmsbtn.setOnClickListener(v -> {

            // Click animation
            v.animate()
                    .scaleX(0.92f)
                    .scaleY(0.92f)
                    .setDuration(70)
                    .withEndAction(() -> v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(20))
                    .start();

            // TPMS not available
            if (!TpmsState.sensorAvailable) {

                showCustomToast("No TPMS sensor detected");

                return;
            }

            // Open TPMS screen
            context.startActivity(
                    new Intent(context, TPMS_activity.class)
            );
        });

    }

    private void animateButton(View v) {
        v.animate()
                .scaleX(0.92f)
                .scaleY(0.92f)
                .setDuration(70)
                .withEndAction(() -> v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(80))
                .start();
    }
    private void showCustomToast(String message) {

        View layout = LayoutInflater.from(getContext())
                .inflate(R.layout.toast_custom, null);

        ((ImageView) layout.findViewById(R.id.toast_icon))
                .setImageResource(R.drawable.toastlogo);

        ((TextView) layout.findViewById(R.id.toast_text))
                .setText(message);

        Toast toast = new Toast(getContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }
    /* ================= TOUCH DRAG ================= */

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getActionMasked()) {

            case MotionEvent.ACTION_DOWN:
                startY = event.getRawY();
                totalDrag = 0f;
                dragging = false;
                return true;

            case MotionEvent.ACTION_MOVE:

                float dy = event.getRawY() - startY;
                totalDrag = Math.abs(dy);

                // start dragging only after small movement
                if (totalDrag > TOUCH_SLOP) {
                    dragging = true;
                }

                if (dragging) {
                    float moveY = Math.max(0, dy * 1.2f);
                    setTranslationY(-getHeight() + moveY);
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:

                if (!dragging) {
                    smoothClose();   // animate close
                }
                else {
                    settle();        // animate settle
                }

                dragging = false;
                return true;
        }

        return super.onTouchEvent(event);
    }

    public void smoothClose() {
        tpmsHandler.removeCallbacks(tpmsRunnable);

        animate()
                .translationY(-getHeight())
                .setDuration(180)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
    }

    private void settle() {
        if (getTranslationY() > -getHeight() / 2f) open();
        else close();
    }

    public void open() {
        MainActivity.isVisible = false;
        syncBrightnessUI();
        syncGpsUI();
        syncVolumeUI();
        updateCurrentRiderUI();
        animate()
                .translationY(0)
                .setDuration(120)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
        tpmsHandler.post(tpmsRunnable);


    }

    public void close() {
        smoothClose();
    }


    /* ================= BRIGHTNESS ================= */

    private void syncBrightnessUI() {

        boolean auto = brightnessManager.isAutoBrightnessEnabled();

        int on = Color.parseColor("#1C78BE");
        int off = Color.parseColor("#555555");

        // 🔹 Button color (same pattern as Wi-Fi)
        autoBrightnessToggle.setBackgroundTintList(
                ColorStateList.valueOf(auto ? on : off)
        );

        // 🔹 Button text
        autoBrightnessToggle.setText(
                auto ? "Auto Brightness ON" : "Auto Brightness OFF"
        );

        // 🔹 Status text
        if (auto) {
            autoBrightnessText.setText("Auto brightness ON");
        } else {
            autoBrightnessText.setText("Auto brightness OFF");
        }

        // 🔹 Slider state
        brightnessSlider.setEnabled(!auto);
        brightnessSlider.setAlpha(auto ? 0.2f : 1.0f);

        // 🔹 Sync slider value when manual
        if (!auto) {
            brightnessSlider.setProgress(
                    brightnessManager.getCurrentBrightness()
            );
        }
    }

    private void syncGpsUI() {

//        boolean enabled =
//                TelemetryDataManager.getInstance()
//                        .isGpsTelemetryEnabled();
        boolean enabled =
                VehicleTelemetryManager.getInstance()
                        .isGpsTelemetryEnabled();

        int on = Color.parseColor("#1C78BE");
        int off = Color.parseColor("#555555");

        GPS_btn.setBackgroundTintList(
                ColorStateList.valueOf(
                        enabled ? on : off
                )
        );

        GPS_btn.setText(
                enabled ? "Ride Location ON" : "Ride Location OFF"
        );
    }

    private void rebindBrightnessSlider() {
        brightnessSlider.setOnSeekBarChangeListener(null);
        brightnessManager.applyToSeekBar(brightnessSlider, autoBrightnessText);
    }

    /* ================= VOLUME ================= */

    private void syncVolumeUI() {
        int vol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        volumeSlider.setProgress(vol);
        updateMuteIcon(vol == 0);
    }

    private void updateMuteIcon(boolean muted) {
        muteButton.setImageResource(
                muted ? R.drawable.mutted : R.drawable.unmutted
        );
    }

    /* ================= WIFI ================= */

    private void updateWifiUI(WifiManager wm) {
        wifiToggle.setBackgroundTintList(
                ColorStateList.valueOf(
                        wm.isWifiEnabled() ?
                                Color.parseColor("#1C78BE") :
                                Color.parseColor("#555555")));
    }

    private void setWifiUiImmediate(boolean on) {
        wifiToggle.setBackgroundTintList(
                ColorStateList.valueOf(
                        on ? Color.parseColor("#1C78BE") :
                                Color.parseColor("#555555")));
    }

    /* ================= OBSERVERS ================= */

    private void registerObservers(Context context) {

        // Brightness observer
        brightnessObserver = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                handler.post(() -> {
                    if (!brightnessManager.isAutoBrightnessEnabled()) {
                        brightnessSlider.setProgress(
                                brightnessManager.getCurrentBrightness());
                    }
                });
            }

        };
        context.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE),
                false,
                new ContentObserver(handler) {
                    @Override
                    public void onChange(boolean selfChange) {
                        handler.post(() -> syncBrightnessUI());
                    }
                }
        );

        context.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS),
                false,
                brightnessObserver
        );

        // Volume receiver
        context.registerReceiver(
                volumeReceiver,
                new IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        );

        // Wifi receiver
        IntentFilter wifi = new IntentFilter();
        wifi.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        context.registerReceiver(wifiReceiver, wifi);
    }

    public void release(Context context) {

        context.unregisterReceiver(wifiReceiver);
        context.unregisterReceiver(volumeReceiver);

        context.getContentResolver()
                .unregisterContentObserver(brightnessObserver);
    }
    private String getCurrentRider() {

        SharedPreferences prefs =
                getContext().getSharedPreferences(
                        "app_session",
                        Context.MODE_PRIVATE
                );

        return prefs.getString("current_rider", "");
    }
    private void updateCurrentRiderUI() {

        String riderName = getCurrentRider();

        if (!riderName.isEmpty()) {
            currentRiderTV.setText("Current rider: "+riderName);
        } else {
            currentRiderTV.setText("No Rider");
        }
    }
    private void updateTpmsButtonColor() {

        boolean tpmsAvailable = TpmsState.sensorAvailable;

//        Log.d("TPMS_STATUS",
//                "QuickSettings Read = " + tpmsAvailable);

        if (blue_tpmsbtn == null)
            return;

        if (tpmsAvailable) {

//            Log.d("TPMS_STATUS", "SETTING BLUE");


            blue_tpmsbtn.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#1C78BE"))
            );

        } else {

//            Log.d("TPMS_STATUS", "SETTING GREY");


            blue_tpmsbtn.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#555555"))
            );
        }
    }
    private final Handler tpmsHandler = new Handler(Looper.getMainLooper());

    private final Runnable tpmsRunnable = new Runnable() {
        @Override
        public void run() {

            updateTpmsButtonColor();

            tpmsHandler.postDelayed(this, 1000);
        }
    };
}
