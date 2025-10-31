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
import android.util.Log;
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

    private Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;

    private Handler socUpdateHandler = new Handler(Looper.getMainLooper());
    private Runnable socUpdateRunnable;

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
////                String Candata=FrameUtils.getByteSubstring(frame,11,18);
////                int soc=FrameUtils.getByteSubstringAsInt(Candata,6,7);
////                Log.d("chargingUI", "CSOCCCCCCCCCCCCCCCCCCCC"+soc);
////
////                if (soc < 100 && soc >= 0) {
////                    binding.socText.setText(soc + "%");
////                }
//            }

            // ⚠️ Reset timeout ONLY if this frame is received

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

        acquisitionTime();
        wakeScreenIfNeeded();
        if (MainActivity.uiUpdater != null) {
            MainActivity.uiUpdater.setChargingUI(this);
        }
//        InactivityManager.getInstance().setCallback(this);
        if(true){Log.d("sadfsg","TIMERE$");
            InactivityManager.getInstance().stopTimer();
            InactivityManager.getInstance().startTimer(4);}

        // Register BroadcastReceiver
        if (!isReceiverRegistered) {
            LocalBroadcastManager.getInstance(this)
                    .registerReceiver(frameReceiver, new IntentFilter("FRAME_RECEIVED"));
            isReceiverRegistered = true;
        }

        TelemetryService.shdtelemetrywhenunlocked=true;

        binding.chargingtologinbtn.setOnClickListener(v -> {
            isChargingAnimationRunning = false;
            Intent intent = new Intent(chargingUI.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            // Then finish ChargingUI
            finish();
        });


        startUpdatingSOC();
        resetChargingTimeout(); // start timeout initially
    }//END OF ONCREATE

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

    private int actualSOC = 0; // store real SOC from hardware
    private int APUsoc = 0;
    private boolean isChargingAnimationRunning = false;

    private void startUpdatingSOC() {
        //InactivityManager.getInstance().startTimer(4);
        socUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                actualSOC = serialPortManager.getHb3().getBatteryLevel() / 10;
//                Log.d("SOC", "Battery SOC: " + actualSOC);
                APUsoc = serialPortManager.hb3.getAPUBatteryLevel() / 10;


                runOnUiThread(() -> {
                    Heartbeat3 hb3 = serialPortManager.getHb3();
                    int chargingbit = hb3.getChargingbit();
                    if(chargingbit==0){
                        chargingUiBatteryAnimation(actualSOC);
                        binding.socText.setVisibility(View.INVISIBLE);
                        binding.chagingstartedicon.setVisibility(View.INVISIBLE);
                        binding.plugged.setVisibility(View.VISIBLE);
                        stopBatteryPulseAnimation();
                        isChargingAnimationRunning = false;
                    }
                    if(chargingbit>0){

                        binding.socText.setVisibility(View.VISIBLE);
                        binding.plugged.setVisibility(View.INVISIBLE);
                        binding.chagingstartedicon.setVisibility(View.VISIBLE);
                        if (actualSOC >= 0 && actualSOC <= 100) {
                            binding.socText.setText(actualSOC + "%");
                        }
                        if (!isChargingAnimationRunning) {
                            isChargingAnimationRunning = true;
                            startBatteryPulseAnimation();
                        }
                    }

                    if(actualSOC==100 && chargingbit==0){
                        binding.batfullmsg.setText("Battery Full, Disconnect the charger"); //
                        binding.batfullmsg.setVisibility(View.VISIBLE);

                    }
                    else if(actualSOC<100 && chargingbit==0){
                        binding.batfullmsg.setText("Plugged In"); //
                        binding.batfullmsg.setVisibility(View.VISIBLE);

                    }
                    else binding.batfullmsg.setVisibility(View.INVISIBLE);

                    if (APUsoc >= 0 && APUsoc <= 100 && UIUpdater.isAPUPresent) {
                        binding.APUbatTV.setVisibility(View.VISIBLE);
                        binding.APUbatTV.setText("APU "+APUsoc + "%");
                    }

                    if (UIUpdater.isAPUPresent){

                        int batteryImageResId = getBatteryImageResource(APUsoc);
                        binding.apubaticon.setVisibility(View.VISIBLE);
                        binding.APUbatteryImageView.setVisibility(View.VISIBLE);
                        binding.apubaticon.setImageResource(batteryImageResId);
                        try {
                            if (APUsoc>=0 && APUsoc<=100) { // ✅ Only update if animation is done
                                APUupdateBatteryAnimation(APUsoc);
                            }
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    }
                    else {
                        binding.APUbatteryImageView.setVisibility(View.INVISIBLE);
                        binding.apubaticon.setVisibility(View.INVISIBLE);
                        binding.APUbatTV.setVisibility(View.INVISIBLE);
                    }

                    // Start animation loop only once

                });
                socUpdateHandler.postDelayed(this, 500); // update actual SOC
            }
        };

        socUpdateHandler.post(socUpdateRunnable);
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
    private Handler pulseHandler;
    private Runnable pulseRunnable;

    private void startBatteryPulseAnimation() {
        pulseHandler = new Handler(Looper.getMainLooper());

        pulseRunnable = new Runnable() {
            boolean goingUp = true;
            int displaySOC = actualSOC;
            boolean pausedAtActualSOC = false;

            @Override
            public void run() {
                if (goingUp) {
                    displaySOC += 2;
                    if (displaySOC >= 100) {
                        goingUp = false;
                    }
                } else {
                    displaySOC -= 2;
                    if (displaySOC <= actualSOC) {
                        displaySOC = actualSOC;
                        chargingUiBatteryAnimation(displaySOC);

                        if (!pausedAtActualSOC) {
                            pausedAtActualSOC = true;
                            pulseHandler.postDelayed(this, 1000);
                            return;
                        } else {
                            pausedAtActualSOC = false;
                            goingUp = true;
                            displaySOC = actualSOC;
                        }
                    }
                }

                chargingUiBatteryAnimation(displaySOC);
                pulseHandler.postDelayed(this, 20);
            }
        };

        pulseHandler.post(pulseRunnable);
    }

    private void stopBatteryPulseAnimation() {
        if (pulseHandler != null && pulseRunnable != null) {
            pulseHandler.removeCallbacks(pulseRunnable);
        }
    }

    private int chargingUiLastlevel = -1; // Store last battery level to avoid redundant updates

    public void chargingUiBatteryAnimation(int batteryLevel) {
        // Clamp battery level between 0-100 and round to the nearest even number
        int mappedChargingUiBattery = (batteryLevel > 100) ? 100 : Math.max(0, (batteryLevel / 2) * 2);

        if (mappedChargingUiBattery == chargingUiLastlevel) return; // Skip if battery level hasn't changed

        if (MainActivity.APUbatteryImageMap.containsKey(mappedChargingUiBattery)) {
            binding.chargingUiBatterybar.setImageResource(MainActivity.batteryImageMap.get(mappedChargingUiBattery)); // Update ImageView
        }

        chargingUiLastlevel = mappedChargingUiBattery;
    }


    public void resetChargingTimeout() {
        timeoutHandler.removeCallbacks(timeoutRunnable);
        timeoutRunnable = () -> {
            //Log.d("chargingUI", "No FFAA770303001514028001 frame received in 3 seconds — finishing");
            InactivityManager.getInstance().stopTimer();
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (powerManager != null && !powerManager.isInteractive()) {
                PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                        PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,
                        "MyApp:WakeLockTag"
                );
                wakeLock.acquire(3000);
            }
            finish();
//            UIUpdater.telechargingstate=0;
            TelemetryDataManager.getInstance().setTeleChargingState(0);

            Log.d("sdafgd","CANCLINGCHARGING");
        };
        timeoutHandler.postDelayed(timeoutRunnable, 7000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EVLockService.getInstance().startLockSequence();

        //Log.d("ChargingUI", "onDestroy called — launching LoginActivity...");

        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);

        //Log.d("chargingUI", "Now destroyed");
        mHandlerTime.removeCallbacks(mRunnableTime);
        LoginActivity.ChargingUiClosedByUserFlag = true;

        if (isReceiverRegistered) {
            try {
                LocalBroadcastManager.getInstance(this).unregisterReceiver(frameReceiver);
            } catch (IllegalArgumentException e) {
                //Log.e("chargingUI", "Receiver not registered", e);
            }
            isReceiverRegistered = false;
        }

        timeoutHandler.removeCallbacks(timeoutRunnable);
        socUpdateHandler.removeCallbacks(socUpdateRunnable);

        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //InactivityManager.getInstance().startTimer(4);
        InactivityManager.getInstance().startTimer(4);
//        Log.d("sdfgdfh","RESUMED CHARGINGUI");
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

    private Handler mHandlerTime = new Handler(Looper.getMainLooper());
    private Runnable mRunnableTime;

    private void acquisitionTime() {
        mRunnableTime = new Runnable() {
            @Override
            public void run() {
                Date currentTime = new Date();
                SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                String formattedTime = timeFormat.format(currentTime);

                binding.timeTextView.setText(formattedTime);

                mHandlerTime.postDelayed(this, 1000); // update every second
            }
        };

        mHandlerTime.post(mRunnableTime);

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

}
