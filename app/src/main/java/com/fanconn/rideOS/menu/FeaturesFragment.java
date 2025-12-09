package com.fanconn.rideOS.menu;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.fanconn.rideOS.R;
import com.fanconn.rideOS.SerialPortManager;
import com.fanconn.rideOS.serialApplication;

import java.util.Locale;

public class FeaturesFragment extends Fragment {

//    private SwitchCompat cruiseControlSwitch, boostSwitch, rollProtectSwitch;
    private SwitchCompat   rollProtectSwitch;

    private SharedPreferences sharedPreferences;
    private SerialPortManager serialPortManager;
    private SeekBar volumeSlider;
    private ImageButton muteButton;
    private AudioManager audioManager;
    private SharedPreferences prefs;
    private boolean isMuted;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {


        View view = inflater.inflate(R.layout.activity_features_fragment, container, false);
        serialPortManager= serialApplication.getSerialPortManager();
        // Initialize switches
//        cruiseControlSwitch = view.findViewById(R.id.cruisecontrolSwitch);
//        boostSwitch = view.findViewById(R.id.boostSwitch);
        rollProtectSwitch = view.findViewById(R.id.rollprotectSwitch);

        // Load stored state
        sharedPreferences = requireContext().getSharedPreferences("feature_prefs", Context.MODE_PRIVATE);
//        cruiseControlSwitch.setChecked(sharedPreferences.getBoolean("cruise_enabled", false));
//        boostSwitch.setChecked(sharedPreferences.getBoolean("boost_enabled", false));
        rollProtectSwitch.setChecked(sharedPreferences.getBoolean("roll_enabled", false));

        // Set custom styling for all switches
//        Drawable thumbDrawable1 = ContextCompat.getDrawable(requireContext(), R.drawable.custom_switch_thumb).mutate();
//        Drawable trackDrawable1 = ContextCompat.getDrawable(requireContext(), R.drawable.custom_switch_track).mutate();
        Drawable thumbDrawable2 = ContextCompat.getDrawable(requireContext(), R.drawable.custom_switch_thumb).mutate();
        Drawable trackDrawable2 = ContextCompat.getDrawable(requireContext(), R.drawable.custom_switch_track).mutate();
        Drawable thumbDrawable3 = ContextCompat.getDrawable(requireContext(), R.drawable.custom_switch_thumb).mutate();
        Drawable trackDrawable3 = ContextCompat.getDrawable(requireContext(), R.drawable.custom_switch_track).mutate();

//        cruiseControlSwitch.setThumbDrawable(thumbDrawable1);
//        cruiseControlSwitch.setTrackDrawable(trackDrawable1);
//
//        boostSwitch.setThumbDrawable(thumbDrawable2);
//        boostSwitch.setTrackDrawable(trackDrawable2);

        rollProtectSwitch.setThumbDrawable(thumbDrawable3);
        rollProtectSwitch.setTrackDrawable(trackDrawable3);

        // Reflect state on label
//        cruiseControlSwitch.setText(cruiseControlSwitch.isChecked() ? "Enabled" : "Disabled");
//        boostSwitch.setText(boostSwitch.isChecked() ? "Enabled" : "Disabled");
        rollProtectSwitch.setText(rollProtectSwitch.isChecked() ? "Enabled" : "Disabled");

        // Cruise control toggle logic
//        cruiseControlSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
//            sharedPreferences.edit().putBoolean("cruise_enabled", isChecked).apply();
//            cruiseControlSwitch.setText(isChecked ? "Enabled" : "Disabled");
//
//            if (isChecked) {
//                // TODO: Add logic to enable cruise control
//                if(serialPortManager!=null)serialPortManager.VCUSendToMCU("FFAA550103001510F8A807050000020E000000");
//                new android.os.Handler().postDelayed(() -> {
//                    if (serialPortManager != null) {
//                        serialPortManager.VCUSendToMCU("FFAA550103001510F8A8070000000000007387");
//                    }
//                }, 3000); // 3000 milliseconds = 3 seconds
//
//            } else {
//                // TODO: Add logic to disable cruise control
//                if(serialPortManager!=null)serialPortManager.VCUSendToMCU("FFAA550103001510F8A8070500000200000000");
//                new android.os.Handler().postDelayed(() -> {
//                    if (serialPortManager != null) {
//                        serialPortManager.VCUSendToMCU("FFAA550103001510F8A8070000000000007387");
//                    }
//                }, 3000); // 3000 milliseconds = 3 seconds
//            }
//        });

        // Boost toggle logic
//        boostSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
//            sharedPreferences.edit().putBoolean("boost_enabled", isChecked).apply();
//            boostSwitch.setText(isChecked ? "Enabled" : "Disabled");
//
//            if (isChecked) {
//                // TODO: Add logic to enable boost
//            } else {
//                // TODO: Add logic to disable boost
//            }
//        });

        // Roll Protect toggle logic
        rollProtectSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("roll_enabled", isChecked).apply();
            rollProtectSwitch.setText(isChecked ? "Enabled" : "Disabled");

            if (isChecked) {
                // TODO: Add logic to enable roll protection
            } else {
                // TODO: Add logic to disable roll protection
            }
        });

        SeekBar seekBar = view.findViewById(R.id.seekBar);
        TextView valueText = view.findViewById(R.id.valueText);
        Handler handler = new Handler();

        final int min = 5;

// SharedPreferences for persistent storage
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("seekbar_prefs", Context.MODE_PRIVATE);
        int savedProgress = sharedPreferences.getInt("seekbar_progress", 0); // default to 0

        Runnable stableRunnable = new Runnable() {
            @Override
            public void run() {
                int seekBarValue = seekBar.getProgress() + min;
                double numerator = seekBarValue * 3.3125 * 1000;
                double denominator = 60.0 * 1.4825;
                double x = numerator / denominator;
                int xRounded = (int) Math.round(x);


// Convert to 2-byte hex (big endian: MM NN)
                String hexString = String.format("%04X", xRounded); // always 4 hex digits
                String mm = hexString.substring(0, 2);
                String nn = hexString.substring(2, 4);

// Insert into the frame
                String originalFrame = "FFAA550103001510FDA8070000MMNN00000000";
                String finalFrame = originalFrame.replace("MM", nn).replace("NN", mm);

// Log and send
                Log.d("SeekBarStable", "SeekBar=" + seekBarValue + ", x=" + xRounded + ", Hex=" + mm + " " + nn);
                Log.d("SeekBarStable", "Final Frame: " + finalFrame);


                if (serialPortManager != null) {
                    serialPortManager.VCUSendToMCU(finalFrame);
                }
//                new android.os.Handler().postDelayed(() -> {
//                    if (serialPortManager != null) {
//                        serialPortManager.VCUSendToMCU("FFAA550103001510F8A8070000000000007387");
//                    }
//                }, 1000); // 3000 milliseconds = 3 seconds
                // Optional: display value
                // valueText.setText(seekBarValue + " ✓");
            }
        };

        seekBar.setMax(10); // because 15 - 5 = 10
        seekBar.setProgress(savedProgress); // restore saved value
        valueText.setText(String.valueOf(savedProgress + min)); // show saved value

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = progress + min;
                valueText.setText(String.valueOf(value));

                // Save to SharedPreferences
                sharedPreferences.edit().putInt("seekbar_progress", progress).apply();

                handler.removeCallbacks(stableRunnable);
                handler.postDelayed(stableRunnable, 2000); // wait 4 seconds
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                handler.removeCallbacks(stableRunnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Optional: save again or run logic
                // handler.postDelayed(stableRunnable, 4000);
            }
        });
//--------------------------------boostsetting ----------------------------------------------------------------------
        SharedPreferences BoostsettingsharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = BoostsettingsharedPreferences.edit();

        RadioGroup boostDurationGroup = view.findViewById(R.id.boostduration);
        RadioGroup boostIntervalGroup = view.findViewById(R.id.boostlasting);

// Restore previously selected RadioButtons from SharedPreferences
        int savedDurationId = BoostsettingsharedPreferences.getInt("boost_duration_id", -1);
        int savedIntervalId = BoostsettingsharedPreferences.getInt("boost_interval_id", -1);

        if (savedDurationId != -1) {
            boostDurationGroup.check(savedDurationId);
        }

        if (savedIntervalId != -1) {
            boostIntervalGroup.check(savedIntervalId);
        }


        boostDurationGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton selected = group.findViewById(checkedId);
            if (selected != null) {
                int durationSec = parseDurationSeconds(selected.getText().toString());
                String durationHex = toTwoDigitHex(durationSec);

                editor.putInt("boost_duration_value", durationSec);
                editor.putString("boost_duration_hex", durationHex);
                editor.putInt("boost_duration_id", checkedId);
                editor.apply();

                updateAndSendFrame(BoostsettingsharedPreferences);
            }
        });

        boostIntervalGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton selected = group.findViewById(checkedId);
            if (selected != null) {
                int intervalSec = parseDurationSeconds(selected.getText().toString());
                String intervalHex = toTwoDigitHex(intervalSec);

                editor.putInt("boost_interval_value", intervalSec);
                editor.putString("boost_interval_hex", intervalHex);
                editor.putInt("boost_interval_id", checkedId);
                editor.apply();

                updateAndSendFrame(BoostsettingsharedPreferences);
            }
        });

//===============================================================================
// At the top of onCreateView()
        volumeSlider = view.findViewById(R.id.volumeSlider);
        muteButton = view.findViewById(R.id.muteButton);

// Reuse BrightnessManager for volume
        BrightnessManager manager = new BrightnessManager(requireContext());
        manager.applyToVolumeSlider(volumeSlider, muteButton);




        return view;
    }

    private int parseDurationSeconds(String text) {
        text = text.toLowerCase(Locale.ROOT);
        if (text.contains("min")) {
            return (int)(Float.parseFloat(text.replaceAll("[^0-9.]", "")) * 60);
        } else {
            return Integer.parseInt(text.replaceAll("[^0-9]", ""));
        }
    }
    private String toTwoDigitHex(int value) {
        String hex = Integer.toHexString(value).toUpperCase();
        return hex.length() == 1 ? "0" + hex : hex;
    }
    private void updateAndSendFrame(SharedPreferences sharedPreferences) {
        // 1. Get boost duration and interval from regular shared prefs
        String durationHex = sharedPreferences.getString("boost_duration_hex", "00");
        String intervalHex = sharedPreferences.getString("boost_interval_hex", "00");

        // 2. Load EncryptedSharedPreferences for user-specific values
        SharedPreferences securePrefs;
        try {
            securePrefs = EncryptedSharedPreferences.create(
                    "user_data",  // Must match file name used in LoginActivity
                    MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
                    requireContext(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            Log.e("SharedPrefError", "Failed to load EncryptedSharedPreferences", e);
            return;
        }

        // 3. Get last logged-in username
        String username = securePrefs.getString("last_logged_in_user", null);
        if (username == null) {
            Log.e("SpeedBytes", "No user is logged in.");
            return;
        }

        // 4. Get stored speed bytes for this user
        String speedByte01 = securePrefs.getString("user_" + username + "_speedByte01", "00");
        String speedByte02 = securePrefs.getString("user_" + username + "_speedByte02", "00");

        Log.d("SpeedBytes", "Username: " + username);
        Log.d("SpeedBytes", "Speed Byte01 (low): " + speedByte01 + ", Speed Byte02 (high): " + speedByte02);

        // 5. Construct the boost frame using retrieved values
        String boostFrame = "FFAA550103001510FCA807" + speedByte01 + speedByte02 + "00000000" + durationHex + intervalHex;
        Log.d("FrameOut", "Final boost frame: " + boostFrame);

        // 6. Send the frame via Serial/UART if available
        if (serialPortManager != null) {
            serialPortManager.VCUSendToMCU(boostFrame);
        } else {
            Log.e("FrameOut", "serialPortManager is null, frame not sent");
        }

//        new Handler().postDelayed(() -> {
//            if (serialPortManager != null) {
//                serialPortManager.VCUSendToMCU("FFAA550103001510F8A8070000000000007387");
//            }
//        }, 1000);
    }
}
