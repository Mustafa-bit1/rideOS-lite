package com.fanconn.rideOS.menu;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.fanconn.rideOS.R;
import com.fanconn.rideOS.menu.BrightnessManager;

public class DisplayFragment extends Fragment {
    private SeekBar brightnessSlider;
    private SwitchCompat autoBrightnessSwitch;
    private TextView brightnessValue;
    private BrightnessManager brightnessManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_display, container, false);

        // Initialize views
        brightnessSlider = view.findViewById(R.id.brightnessSlider);
        brightnessValue = view.findViewById(R.id.brightnessValue);
        autoBrightnessSwitch = view.findViewById(R.id.autoBrightnessSwitch);

        // Set custom switch drawables
        autoBrightnessSwitch.setThumbDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.custom_switch_thumb));
        autoBrightnessSwitch.setTrackDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.custom_switch_track));

        // Request system write permission
        if (!Settings.System.canWrite(requireContext())) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

        // Init BrightnessManager
        brightnessManager = new BrightnessManager(requireContext());

        // Apply slider behavior from shared utility
        brightnessManager.applyToSeekBar(brightnessSlider, brightnessValue);

        // Reflect saved state in switch
        boolean savedState = brightnessManager.isAutoBrightnessEnabled();
        autoBrightnessSwitch.setChecked(savedState);
        updateSwitchUI(savedState);

        // Switch toggle logic
        autoBrightnessSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            brightnessManager.setAutoBrightnessEnabled(isChecked);
            updateSwitchUI(isChecked);
            brightnessManager.applyToSeekBar(brightnessSlider, brightnessValue); // reapply config
        });

        return view;
    }

    private void updateSwitchUI(boolean isChecked) {
        autoBrightnessSwitch.setText(isChecked ? "Enabled" : "Disabled");
        brightnessSlider.setEnabled(!isChecked);

        if (isChecked) {
            brightnessValue.setText("Auto Brightness: ON");
        } else {
            int percent = (int) ((brightnessSlider.getProgress() / 255f) * 100);
            brightnessValue.setText("Adjust overall screen brightness : " + percent + "%");
        }
    }
}
