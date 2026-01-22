package com.fanconn.rideOS.menu;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.fanconn.rideOS.R;

public class RideCamFragment extends Fragment {

    private SwitchCompat autorecording, autoAudioRecbtn;
    private RadioButton q480, q720, q1080;
    private Context ctx;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_ride_cam_fragment, container, false);
        ctx = requireContext();

        // Switches
        autorecording = view.findViewById(R.id.autoRecordingbtn);
        autoAudioRecbtn = view.findViewById(R.id.autoAudioRecbtn);

        autorecording.setThumbDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.custom_switch_thumb));
        autorecording.setTrackDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.custom_switch_track));

        autoAudioRecbtn.setThumbDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.custom_switch_thumb));
        autoAudioRecbtn.setTrackDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.custom_switch_track));

        // Radio Buttons
        q480 = view.findViewById(R.id.Q480);
        q720 = view.findViewById(R.id.Q720);
        q1080 = view.findViewById(R.id.Q1080);

        // Always keep 480p selected
        q480.setChecked(true);

        // 720p → Not supported
        q720.setOnClickListener(v -> {

            showCustomToast("rideCam not found on your scooter");

            q480.setChecked(true);
        });

        // 1080p → Not supported
        q1080.setOnClickListener(v -> {

            showCustomToast("rideCam not found on your scooter");

            q480.setChecked(true);
        });

        return view;
    }
    private void showCustomToast(String message) {
        View layout = LayoutInflater.from(ctx).inflate(R.layout.toast_custom, null);
        ((ImageView) layout.findViewById(R.id.toast_icon)).setImageResource(R.drawable.toastlogo);
        ((TextView) layout.findViewById(R.id.toast_text)).setText(message);
        Toast t = new Toast(ctx);
        t.setDuration(Toast.LENGTH_SHORT);
        t.setView(layout);
        t.show();
    }
}
