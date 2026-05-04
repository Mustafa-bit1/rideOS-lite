package com.fanconn.rideOS;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.fanconn.rideOS.authen.LoginActivity;
import com.fanconn.rideOS.databinding.ActivityChargingUiBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class chargingUI extends AppCompatActivity {

    private ActivityChargingUiBinding binding;
    private boolean isReceiverRegistered = false;
    private SerialPortManager serialPortManager;
    public static chargingUI instance;
    private Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;

    private Handler socUpdateHandler = new Handler(Looper.getMainLooper());
    private Runnable socUpdateRunnable;
    // Battery constants
    private static  float BATTERY_CAPACITY_AH;
    private static final float RANGE_km_PER_SOC = 1.7f;
    private static final float KM_PER_AH = RANGE_km_PER_SOC * 1.666f;
    private static final float MAX_RANGE_KM = RANGE_km_PER_SOC * 100f;
    // Runtime variables
    private float currentRangeKm = 0f;
    private long lastUpdateTime = 0;
    private final BroadcastReceiver frameReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String frame = intent.getStringExtra("frame_data");
            if (frame.startsWith("FFAA770303001518FF50E5")) {
                resetChargingTimeout();
            }
            if (frame == null || frame.length() < 12) {
                Log.w("chargingUI", "Invalid or null frame received");
                return;
            }

            if (!FrameUtils.verifyFrame(frame)) {
                Log.w("chargingUI", "Frame verification failed: " + frame);
                return;
            }

            // SOC frame — optional logging
//            if (frame.startsWith("FFAA770303001518904001")) {
//                Log.d("chargingUI", "SOC frame: " + frame);
///                String Candata=FrameUtils.getByteSubstring(frame,11,18);
////                int soc=FrameUtils.getByteSubstringAsInt(Candata,6,7);
////                Log.d("chargingUI", "CSOCCCCCCCCCCCCCCCCCCCC"+soc);
////
//// /               if (soc < 100 && soc >= 0) {
/////                    binding.socText.setText(soc + "%");
///                }
//            }
//
            //Reset timeout ONLY if this frame is received
        }
    };
    private MediaPlayer mediaPlayer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        //Log.d("sdsfg","CHARGINGUI CLASS OPENED");
        binding = ActivityChargingUiBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        instance = this;
        serialPortManager = serialApplication.getSerialPortManager();

        // Full screen immersive mode
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        mediaPlayer = MediaPlayer.create(this, R.raw.chargingstartedsound);
        if (mediaPlayer != null) {
            mediaPlayer.start();
        }

        //git addacquisitionTime();
        wakeScreenIfNeeded();
        if (MainActivity.uiUpdater != null) {
            MainActivity.uiUpdater.setChargingUI(this);
        }
//        InactivityManager.getInstance().setCallback(this);
//            InactivityManager.getInstance().stopTimer();
//            InactivityManager.getInstance().startTimer(4);

        // Register BroadcastReceiver
        if (!isReceiverRegistered) {
            LocalBroadcastManager.getInstance(this)
                    .registerReceiver(frameReceiver, new IntentFilter("FRAME_RECEIVED"));
            isReceiverRegistered = true;
        }

        //TelemetryService.shdtelemetrywhenunlocked=true;

        binding.chargingtologinbtn.setOnClickListener(v -> {
            isChargingAnimationRunning = false;
            LoginActivity.ChargingUiClosedByUserFlag = true;

            Intent intent = new Intent(chargingUI.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            // Then finish ChargingUI
            finish();
        });

        ahTrackerManager = AhTrackerManager.getInstance(this);
        startUpdatingSOC();
        resetChargingTimeout(); // start timeout initially
    }//END OF ONCREATE

    private Handler mHandlerTime = new Handler(Looper.getMainLooper());
    private Runnable mRunnableTime;

    private void acquisitionTime() {

        if (mRunnableTime != null) return; // 🔒 PREVENT DUPLICATES

        TimeManager timeManager = TimeManager.getInstance(this);

        mRunnableTime = new Runnable() {
            @Override
            public void run() {
                String formattedTime =
                        timeManager.getFormattedTime("hh:mm a");

                binding.timeTextView.setText(formattedTime);
                mHandlerTime.postDelayed(this, 1000);
            }
        };

        mHandlerTime.post(mRunnableTime);
    }

    private void wakeScreenIfNeeded() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager != null && !powerManager.isInteractive()) {
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "YourApp:ChargingWakeLock"
            );
            wakeLock.acquire(3000); // keep screen on for 3 seconds
        }
    }
    private float actualSOC = 0; // store real SOC from hardware
    private int APUsoc = 0;
    private boolean isChargingAnimationRunning = false;
    AhTrackerManager ahTrackerManager;
    private void startUpdatingSOC() {

        socUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                float rawSOC = serialPortManager.getHb3().getNoLess10BMSBatteryLevel() / 10.0f;
                actualSOC = serialPortManager.getHb3().getBatteryLevel();


                APUsoc = serialPortManager.hb3.getAPUBatteryLevel() / 10;

                runOnUiThread(() -> {

                    if (lastUpdateTime == 0) {
                        initializeRangeFromSOC((int) actualSOC);
                        lastUpdateTime = System.currentTimeMillis();
                    }

                    Heartbeat3 hb3 = serialPortManager.getHb3();
                    float chargingbit = hb3.getChargingbit();

                    BATTERY_CAPACITY_AH = getBatteryCapacity();

                    float socRange = ((actualSOC / 100f) * BATTERY_CAPACITY_AH) * KM_PER_AH;
                    if (socRange > currentRangeKm) {
                        currentRangeKm = socRange;
                    }

                    // ================== NOT CHARGING ==================
                    if (chargingbit == 0) {

                        binding.chargingBatteryArc.setPulseMode(false);
                        binding.socText.setVisibility(View.INVISIBLE);
                        binding.rangeAddedText.setVisibility(View.INVISIBLE);
                        binding.linearLayout2.setVisibility(View.INVISIBLE);
                        binding.etaText.setVisibility(View.INVISIBLE);
                        binding.chagingstartedicon.setVisibility(View.INVISIBLE);
                        binding.plugged.setVisibility(View.VISIBLE);

                        stopBatteryPulseAnimation();
                        isChargingAnimationRunning = false;

                        binding.chargingBatteryArc.setBatteryLevel(actualSOC);
                    }

                    // ================== CHARGING ==================
                    else if (chargingbit > 0) {

                        binding.socText.setVisibility(View.VISIBLE);
                        binding.rangeAddedText.setVisibility(View.VISIBLE);
                        binding.etaText.setVisibility(View.VISIBLE);
                        binding.linearLayout2.setVisibility(View.VISIBLE);
                        binding.plugged.setVisibility(View.INVISIBLE);
                        binding.chagingstartedicon.setVisibility(View.VISIBLE);

                        updateChargingRange(chargingbit);
                        updateChargingETA((int) actualSOC, chargingbit);

                        //  Directly use scaled SOC (already decimal)
                        if (actualSOC >= 0 && actualSOC <= 100) {
                            Log.d("SOC", "BatteryCHARGINGUI SCALED SOC: " + actualSOC);

                            String socValue = String.format("%.2f%%", actualSOC);

                            SpannableString spannable = new SpannableString(socValue);
                            spannable.setSpan(new RelativeSizeSpan(0.45f),
                                    socValue.length() - 1,
                                    socValue.length(),
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                            binding.socText.setText(spannable);
                        }

                        int icon = getChargingIcon(chargingbit);
                        if (icon != 0) {
                            binding.chagingstartedicon.setImageResource(icon);
                        }

                        if (!isChargingAnimationRunning) {
                            isChargingAnimationRunning = true;
                            startBatteryPulseAnimation();
                        }
                    }

                    // ================== FULL / PLUGGED ==================
                    if (actualSOC == 100 && chargingbit == 0) {
                        binding.batfullmsg.setText("Battery Full, Disconnect the charger");
                        binding.batfullmsg.setVisibility(View.VISIBLE);

                    } else if (actualSOC < 100 && chargingbit == 0) {
                        binding.batfullmsg.setText("Plugged In");
                        binding.batfullmsg.setVisibility(View.VISIBLE);

                    } else {
                        binding.batfullmsg.setVisibility(View.INVISIBLE);
                    }

                    // ================== APU ==================
                    if (APUsoc >= 0 && APUsoc <= 100 && UIUpdater.isAPUPresent) {

                        binding.APUbatTV.setVisibility(View.VISIBLE);
                        binding.APUbatTV.setText("APU " + APUsoc + "%");

                        int batteryImageResId = getBatteryImageResource(APUsoc);
                        binding.APUbatteryImageView.setVisibility(View.VISIBLE);
                        binding.apubaticon.setImageResource(batteryImageResId);

                        try {
                            APUupdateBatteryAnimation(APUsoc);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    } else {
                        binding.APUbatteryImageView.setVisibility(View.INVISIBLE);
                        binding.apubaticon.setVisibility(View.INVISIBLE);
                        binding.APUbatTV.setVisibility(View.INVISIBLE);
                    }

                });

                socUpdateHandler.postDelayed(this, 50);
            }
        };

        socUpdateHandler.post(socUpdateRunnable);
    }
    private int getChargingIcon(float chargingbit) {
        if (chargingbit > 0 && chargingbit < 25) {
            return R.drawable.normal_charging;
        } else if (chargingbit >= 25 && chargingbit < 40) {
            return R.drawable.fast_charging;
        } else if (chargingbit >= 40) {
            return R.drawable.flash_charging;
        } else {
            return 0; // no icon
        }
    }
    private float scaleSOCtoLess10(float actualSOC) {

        float minActual = 16f;
        float maxActual = 100f;

        if (actualSOC <= minActual) return 0f;
        if (actualSOC >= maxActual) return 100f;

        return (actualSOC - minActual) * 100f / (maxActual - minActual);
    }


    private int lastAPUBatteryLevel = -1; // Store last battery level to avoid redundant updates

    public void APUupdateBatteryAnimation(int batteryLevel) {
        // Clamp battery level between 0-100 and round to the nearest even number
        int mappedAPUBattery = (batteryLevel > 100) ? 100 : Math.max(0, (batteryLevel / 2) * 2);

        if (mappedAPUBattery == lastAPUBatteryLevel) return; // Skip if battery level hasn't changed

        if (MainActivity.APUbatteryImageMap.containsKey(mappedAPUBattery)) {
            binding.APUbatteryImageView.setImageResource(MainActivity.APUbatteryImageMap.get(mappedAPUBattery)); // Update ImageView
        }

        lastAPUBatteryLevel = mappedAPUBattery;
    }
    private int getBatteryImageResource(int batteryLevel) {
        if (batteryLevel < 25) {
            return R.drawable.battery_level_1;
        } else if (batteryLevel < 60) {
            return R.drawable.battery_level_2;
        } else {
            return R.drawable.battery_level_3;
        }
    }
    private final Handler pulseHandler = new Handler(Looper.getMainLooper());
    private Runnable pulseRunnable;

    private void startBatteryPulseAnimation() {

        pulseRunnable = new Runnable() {

            int displaySOC = (int)actualSOC;
            boolean goingUp = true;

            @Override
            public void run() {

                if (pulseHandler == null || isFinishing() || isDestroyed()) {
                    return;
                }

                if (goingUp) {
                    displaySOC += 1;
                    if (displaySOC >= 100) {
                        displaySOC = 100;
                        goingUp = false;
                    }
                } else {
                    displaySOC -= 1;
                    if (displaySOC <= actualSOC) {
                        displaySOC = (int)actualSOC;
                        goingUp = true;
                    }
                }

                // ✅ THIS is the key change
                binding.chargingBatteryArc.setBatteryLevel(displaySOC);

                pulseHandler.postDelayed(this, 10); // smooth speed
            }
        };

        pulseHandler.post(pulseRunnable);
    }
    private void stopBatteryPulseAnimation() {
        if (pulseRunnable != null) {
            pulseHandler.removeCallbacks(pulseRunnable); // ✅ instead of removeCallbacksAndMessages
            pulseRunnable = null;
        }

        binding.chargingBatteryArc.setPulseMode(false);
        binding.chargingBatteryArc.setPulseValue(0);
    }

    public void resetChargingTimeout() {
        timeoutHandler.removeCallbacks(timeoutRunnable);
        timeoutRunnable = () -> {
            //Log.d("chargingUI", "No FFAA770303001514028001 frame received in 3 seconds — finishing");
//            InactivityManager.getInstance().stopTimer();
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (powerManager != null && !powerManager.isInteractive()) {
                PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                        PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,
                        "MyApp:WakeLockTag"
                );
                wakeLock.acquire(3000);
            }
            TelemetryDataManager.getInstance().setTeleChargingState(0);
            LoginActivity.ChargingUiClosedByUserFlag = false;
            LoginActivity.isChargingUIActive= false;
            finish();
//            UIUpdater.telechargingstate=0;


            Log.d("sdafgd","CANCLINGCHARGING flague");
        };
        timeoutHandler.postDelayed(timeoutRunnable, 7000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //  Start EV lock sequence
        //EVLockService.getInstance().startLockSequence();

        //  Stop charging pulse animation safely
        stopBatteryPulseAnimation();

        //  Stop time update runnable
        if (mRunnableTime != null) {
            mHandlerTime.removeCallbacks(mRunnableTime);
            mRunnableTime = null;
        }
        instance = null;
        //  Stop timeout & SOC update handlers
        timeoutHandler.removeCallbacksAndMessages(null);
        socUpdateHandler.removeCallbacksAndMessages(null);

        //  Unregister BroadcastReceiver
        if (isReceiverRegistered) {
            try {
                LocalBroadcastManager.getInstance(this)
                        .unregisterReceiver(frameReceiver);
            } catch (IllegalArgumentException ignored) {
            }
            isReceiverRegistered = false;
        }

        //  Release MediaPlayer
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            } catch (IllegalStateException ignored) {
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }

        //  Flag for LoginActivity

        //  Navigate to LoginActivity
//        Intent intent = new Intent(this, LoginActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
//                | Intent.FLAG_ACTIVITY_CLEAR_TOP
//                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        acquisitionTime();   // start / resume updates

        //InactivityManager.getInstance().startTimer(4);
        InactivityManager.getInstance().startTimer(4,10000);
//        Log.d("sdfgdfh","RESUMED CHARGINGUI");
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (mRunnableTime != null) {
            mHandlerTime.removeCallbacks(mRunnableTime);
            mRunnableTime = null;
        }
    }

    // Optional for external use
    private boolean seenNonWakeFrameSinceScreenOff = true;

    public void markScreenJustOff() {
        seenNonWakeFrameSinceScreenOff = false;
    }

    public void markNonWakeFrameSeen() {
        seenNonWakeFrameSinceScreenOff = true;
    }

    public boolean hasSeenNonWakeFrame() {
        return seenNonWakeFrameSinceScreenOff;
    }



//    @Override
//    public void onUserInteraction() {
//        super.onUserInteraction();
//        //stopSequence();
//        InactivityManager.getInstance().onUserInteraction(); // Reset timer on touch or key press
//
//    }
//    @Override
//    public void onInactivityTimeout(int code) {
//        switch (code) {
//            case 4:
//                Log.d("InactivityTimeout", "SSSSSSSSSSSSSSSSSSSSSSS: Starting lock and screen-off sequence.");
//
//                // Start EV lock
//                EVLockService.getInstance().startLockSequence();
//                if (MainActivity.instance != null) {
//                    MainActivity.instance.finish();
//                }
//                // Delay screen off slightly to allow lock to complete
//                new Handler(Looper.getMainLooper()).postDelayed(() -> {
//                    try {
//                        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
//                        Method goToSleep = PowerManager.class.getMethod("goToSleep", long.class);
//
//                        Log.d("PowerAction", "Calling goToSleep()");
//                        goToSleep.invoke(powerManager, SystemClock.uptimeMillis());
//                        Log.d("PowerAction", "goToSleep() invoked.");
//                    } catch (Exception e) {
//                        Log.e("PowerAction", "Failed to initiate goToSleep:", e);
//                    }
//                }, 1000); // Wait 1 seconds after lock before sleeping
//                break;
//
//            default:
//                Log.d("InactivityTimeout", "Unhandled timeout code: " + code);
//                break;
//        }
//    }

//    private void attemptScreenOff(PowerManager powerManager, Method goToSleep, int attemptCount) {
//        final int MAX_ATTEMPTS = 10;
//
//        try {
//            Log.d("PowerAction", "Attempt " + (attemptCount + 1) + ": Calling goToSleep...");
//            goToSleep.invoke(powerManager, SystemClock.uptimeMillis());
//            Log.d("PowerAction", "goToSleep() invoked.");
//
//            // Delay before checking if screen is still on
//            new Handler(Looper.getMainLooper()).postDelayed(() -> {
//                if (powerManager.isInteractive()) {
//                    Log.d("PowerAction", "Screen still ON after attempt " + (attemptCount + 1));
//                    if (attemptCount < MAX_ATTEMPTS) {
//                        attemptScreenOff(powerManager, goToSleep, attemptCount + 1);
//                    } else {
//                        Log.e("PowerAction", "Failed to turn off screen after max attempts");
//                    }
//                } else {
//                    Log.d("PowerAction", "Screen confirmed OFF after attempt " + (attemptCount + 1));
//                }
//            }, 800); // Small delay to allow system to actually turn off screen
//
//        } catch (Exception e) {
//            Log.e("PowerAction", "Exception during screen off attempt: ", e);
//        }
//    }
    private void initializeRangeFromSOC(int soc) {

        BATTERY_CAPACITY_AH=getBatteryCapacity();
        float presentAh = (soc / 100f) * BATTERY_CAPACITY_AH;

        currentRangeKm = presentAh * KM_PER_AH;

        // ✅ Clamp to max range
        if (currentRangeKm > MAX_RANGE_KM) {
            currentRangeKm = MAX_RANGE_KM;
        }

        binding.rangeAddedText.setText(
                String.format(Locale.US, "Range %.2f km", currentRangeKm)
        );
    }

    private void updateChargingETA(int soc, float chargingCurrent) {

        BATTERY_CAPACITY_AH=getBatteryCapacity();
        float remainingAh = BATTERY_CAPACITY_AH - (soc * BATTERY_CAPACITY_AH/100f);

        float timeHours = remainingAh / chargingCurrent;
        Log.d("asdfsdgfd","THISISESTIMATED "+remainingAh+"  cc "+chargingCurrent);
        // Add 20% buffer (real-world charging slowdown)
        timeHours *= 1.2f;
        ETADataManager.getInstance().setEtaTime(timeHours);
        int hours = (int) timeHours;
        int minutes = (int) ((timeHours - hours) * 60);

        binding.etaText.setText(
                String.format(Locale.US, "%dHr %02dMin to Full", hours, minutes)
        );
    }
    private void updateChargingRange(float chargingCurrent) {

        long now = System.currentTimeMillis();
        float deltaSeconds = (now - lastUpdateTime) / 1000f;
        lastUpdateTime = now;

        float ahAdded = (chargingCurrent / 3600f) * deltaSeconds;
        float kmAdded = ahAdded * KM_PER_AH;

        currentRangeKm += kmAdded;

        // ✅ Clamp to max range
        if (currentRangeKm >= MAX_RANGE_KM) {
            currentRangeKm = MAX_RANGE_KM;

            // Optional: stop further updates
            binding.rangeAddedText.setText(
                    String.format(Locale.US, "Range %6.2f", currentRangeKm)
            );
            return;
        }

        binding.rangeAddedText.setText(
                String.format(Locale.US, "Range %6.2f", currentRangeKm)
        );
    }
    private float getBatteryCapacity() {
        String model = MetadataManager.get().getModel();

        if (model == null) return 60f; // default safety

        switch (model) {
            case "nx100 Pro":
                return 60f;

            case "nx100 Max":
                return 90f;

            default:
                return 60f; // fallback
        }
    }
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        InactivityManager.getInstance().ShortonUserInteraction(4);
        return super.dispatchTouchEvent(ev);
    }
}
