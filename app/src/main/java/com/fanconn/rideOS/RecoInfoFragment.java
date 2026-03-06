package com.fanconn.rideOS;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import org.jetbrains.annotations.Nullable;

public class RecoInfoFragment extends Fragment {

    private TextView tripRecoKmTv;
    private TextView totalRecoKmTv;
    private TextView lastRecokmTv;


    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_reco_info, container, false);

        tripRecoKmTv = view.findViewById(R.id.tripRecoKm);
        totalRecoKmTv = view.findViewById(R.id.totalRecoKm);
        lastRecokmTv = view.findViewById(R.id.lasttripRecoKm);

        View blurBg = view.findViewById(R.id.blurBg);
        View rightPanel = view.findViewById(R.id.rightPanel);

        blurBg.setOnClickListener(v -> close());

        // Slide in animation
        rightPanel.setTranslationX(500f);
        rightPanel.animate()
                .translationX(0f)
                .setDuration(300)
                .start();

        updateRecoValues();
        InactivityManager.getInstance().stopTimer();
        Log.d("dsfsg","ENTERED RECOENGINE");
        return view;
    }

    private void updateRecoValues() {
        UIUpdater uiUpdater = UIUpdater.getInstance();
        if (uiUpdater == null) return;

        float tripKm = uiUpdater.getTripRecoKm();
        float totalKm = uiUpdater.getTotalRecoKm();
        float lastKm =uiUpdater.getLastTripRecoKm();
        tripRecoKmTv.setText(String.format("+%.1f km (Current Trip)", tripKm));
        lastRecokmTv.setText(String.format("+%.1f km (Last trip)",lastKm));
        totalRecoKmTv.setText(String.format("+%.1f km (Total)", totalKm));
    }

    private void close() {
        if (getActivity() == null || getView() == null) return;

        View rightPanel = getView().findViewById(R.id.rightPanel);
        rightPanel.animate()
                .translationX(rightPanel.getWidth())
                .setDuration(250)
                .withEndAction(() -> {
                    getActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .remove(this)
                            .commitAllowingStateLoss();

                    View overlay = getActivity().findViewById(R.id.overlayContainer);
                    if (overlay != null) overlay.setVisibility(View.GONE);
                    if (UIUpdater.directionalGears==0)
                    {
                        InactivityManager.getInstance().startTimer(1);
                    }
                })
                .start();
    }
}
