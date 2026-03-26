package com.fanconn.rideOS;

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
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.fanconn.rideOS.menu.BrightnessManager;
import com.fanconn.rideOS.menu.settings;

public class QuickSettingsView extends FrameLayout {
    private static final int TOUCH_SLOP = 8; // px (small movement threshold)
    private float totalDrag = 0f;
    private BrightnessManager brightnessManager;
    private AudioManager audioManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ImageButton allSetbtn;
    private Button wifiToggle, autoBrightnessToggle, blue_tpmsbtn,new_feature_btn;
    private TextView autoBrightnessText;
    private SeekBar brightnessSlider, volumeSlider;
    private ImageButton muteButton;
    private float startY;
    private boolean dragging;
    private SharedPreferences prefs;
    private ContentObserver brightnessObserver;

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
            volumeSlider.setProgress(vol);
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
        registerObservers(context);

        if(new_feature_btn!=null)
        new_feature_btn.setBackgroundTintList(
                ColorStateList.valueOf(Color.parseColor("#1C78BE"))
        );


        if(blue_tpmsbtn!=null){
            blue_tpmsbtn.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#1C78BE"))
            );
        }

        post(() -> setTranslationY(-getHeight()));
    }

    /* ================= UI ================= */

    private void setupUI(Context context) {

        allSetbtn = findViewById(R.id.allSetbtn);
        allSetbtn.setOnClickListener(v -> {
            Intent intent = new Intent(context, settings.class);
            context.startActivity(intent);

            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).overridePendingTransition(0,0);
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

        /* ---------- VOLUME ---------- */

        volumeSlider = findViewById(R.id.volumeSlider);
        muteButton = findViewById(R.id.muteButton);

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
        blue_tpmsbtn.setOnClickListener(v ->{

//                v.animate()
//                        .scaleX(0.92f)
//                        .scaleY(0.92f)
//                        .setDuration(70)
//                        .withEndAction(() -> v.animate()
//                                .scaleX(1f)
//                                .scaleY(1f)
//                                .setDuration(1))
//                        .start();

            context.startActivity(new Intent(context, TPMS_activity.class));
    } );

        new_feature_btn = findViewById(R.id.new_feature_btn);

        new_feature_btn.setOnClickListener(v -> {

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

            // MCU send
            SerialPortManager spm = SerialPortManager.getInstance();
            if (spm != null) {
                spm.VCUSendToMCU("FFAA550102000A01");
            }
        });

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

                // 👇 TAP (no drag)
                if (!dragging) {
                    smoothClose();   // animate close
                }
                // 👇 DRAG RELEASE
                else {
                    settle();        // animate settle
                }

                dragging = false;
                return true;
        }

        return super.onTouchEvent(event);
    }

    public void smoothClose() {
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
        syncBrightnessUI();
        syncVolumeUI();
        animate()
                .translationY(0)
                .setDuration(120)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();    }
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
}
