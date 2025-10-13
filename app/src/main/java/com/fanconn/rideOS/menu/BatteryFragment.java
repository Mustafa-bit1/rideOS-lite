package com.fanconn.rideOS;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class BatteryFragment extends Fragment {

    private SerialPortManager serialPortManager;

    // Main Battery Views
    private TextView mainBatteryLevel, mainBatteryVoltage, mainRemainingAh;
    private TextView NTC1, NTC2, NTC3, NTC4;

    // Aux / APU Battery Views
    private TextView apuBatteryLevel, apuBatteryVoltage, apuRemainingAh;

    // Top Section Views
    private LinearLayout topMainContainer, topAuxContainer;
    private TextView topCardTitle, topCardValue, Rangetext, RangeValue;
    private TextView topAuxTitle, topAuxValue, topAuxRangeText, topAuxRangeValue;

    // Other Common Views
    private TextView chargingStatus, mainMosState, apuMosState;

    // Scroll Containers
    private LinearLayout mainBatteryContainer, auxBatteryContainer;

    // Radio buttons
    private RadioButton btnBattery, btnAuxillary;

    private Handler uiHandler = new Handler(Looper.getMainLooper());

    private Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (serialPortManager != null && serialPortManager.getHb3() != null) {
                Heartbeat3 hb3 = serialPortManager.getHb3();

                if (mainBatteryContainer.getVisibility() == View.VISIBLE) {
                    // Update Main Battery Top Section
                    int battery = hb3.getBatteryLevel() / 10;
                    int range = battery * 2;

                    SpannableString batteryText = new SpannableString(battery + "%");
                    batteryText.setSpan(new RelativeSizeSpan(0.7f),
                            batteryText.length() - 1,
                            batteryText.length(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    topCardValue.setText(batteryText);

                    SpannableString rangeText = new SpannableString(range + " km");
                    rangeText.setSpan(new RelativeSizeSpan(0.7f),
                            rangeText.length() - 2,
                            rangeText.length(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    RangeValue.setText(rangeText);

                    topCardTitle.setText("Battery Level");
                    Rangetext.setText("Range");

                    // Update Main Battery Details
                    mainBatteryLevel.setText(battery + "%");
                    mainBatteryVoltage.setText(String.format("%.1f V", hb3.getbatvoltage() / 10.0));
                    mainRemainingAh.setText(String.format("%.1f Ah", hb3.getreamainingAH() / 1000.0));

                    NTC1.setText(hb3.getNTC1() - 40 + "°C");
                    NTC2.setText(hb3.getNTC2() - 40 + "°C");
                    NTC3.setText(hb3.getNTC3() - 40 + "°C");
                    NTC4.setText(hb3.getNTC4() - 40 + "°C");

                    mainMosState.setText("Main Charge: " + hb3.getMainChargeMosState() +
                            " | Main Discharge: " + hb3.getMainDisChargeMosState());

                } else if (auxBatteryContainer.getVisibility() == View.VISIBLE) {
                    // Update Aux / APU Battery Top Section
                    int apuBattery = hb3.getAPUBatteryLevel() / 10;

                    topAuxValue.setText(apuBattery + "%");
                    topAuxRangeValue.setText("-- km"); // Replace if you have range calculation for APU
                    topAuxTitle.setText("APU Battery Level");
                    topAuxRangeText.setText("Range");

                    // Update Aux Battery Details
                    apuBatteryLevel.setText(apuBattery + "%");
                    apuBatteryVoltage.setText(String.format("%.1f V", hb3.getbatvoltage() / 10.0));
                    apuRemainingAh.setText(String.format("%.1f Ah", (float) hb3.getBMSlifecycles()));

                    apuMosState.setText("APU Charge: " + hb3.getApuChargeMosState() +
                            " | APU Discharge: " + hb3.getApuDisChargeMosState());
                }

                // Charging status (common)
                chargingStatus.setText(hb3.getChargingbit() == 1 ? "Charging" : "Not Charging");
            }

            uiHandler.postDelayed(this, 1000);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_battery_fragment, container, false);

        serialPortManager = SerialPortManager.peekInstance();

        // Bind Top Containers
        topMainContainer = view.findViewById(R.id.topMainContainer);
        topAuxContainer = view.findViewById(R.id.topAuxContainer);

        // Main Top Views
        topCardTitle = view.findViewById(R.id.topCardTitle);
        topCardValue = view.findViewById(R.id.topCardValue);
        Rangetext = view.findViewById(R.id.Rangetext);
        RangeValue = view.findViewById(R.id.RangeValue);

        // Aux Top Views
        topAuxTitle = view.findViewById(R.id.topAuxTitle);
        topAuxValue = view.findViewById(R.id.topAuxValue);
        topAuxRangeText = view.findViewById(R.id.topAuxRangeText);
        topAuxRangeValue = view.findViewById(R.id.topAuxRangeValue);

        // Bind other views
        mainBatteryLevel = view.findViewById(R.id.mainBatteryLevel);
        mainBatteryVoltage = view.findViewById(R.id.mainBatteryVoltage);
        mainRemainingAh = view.findViewById(R.id.mainRemainingAh);
        NTC1 = view.findViewById(R.id.NTC1);
        NTC2 = view.findViewById(R.id.NTC2);
        NTC3 = view.findViewById(R.id.NTC3);
        NTC4 = view.findViewById(R.id.NTC4);

        apuBatteryLevel = view.findViewById(R.id.apuBatteryLevel);
        apuBatteryVoltage = view.findViewById(R.id.apuBatteryVoltage);
        apuRemainingAh = view.findViewById(R.id.apuRemainingAh);

        chargingStatus = view.findViewById(R.id.chargingStatus);
        mainMosState = view.findViewById(R.id.mainMosState);
        apuMosState = view.findViewById(R.id.apuMosState);

        mainBatteryContainer = view.findViewById(R.id.mainBatteryContainer);
        auxBatteryContainer = view.findViewById(R.id.auxBatteryContainer);

        btnBattery = view.findViewById(R.id.btnBattery);
        btnAuxillary = view.findViewById(R.id.btnAuxillary);

        // RadioGroup toggle listener
        RadioGroup topToggleGroup = view.findViewById(R.id.topToggleGroup);
        topToggleGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.btnBattery) {
                mainBatteryContainer.setVisibility(View.VISIBLE);
                auxBatteryContainer.setVisibility(View.GONE);
                topMainContainer.setVisibility(View.VISIBLE);
                topAuxContainer.setVisibility(View.GONE);
            } else if (checkedId == R.id.btnAuxillary) {
                mainBatteryContainer.setVisibility(View.GONE);
                auxBatteryContainer.setVisibility(View.VISIBLE);
                topMainContainer.setVisibility(View.GONE);
                topAuxContainer.setVisibility(View.VISIBLE);
            }
        });

        // Start updating UI
        uiHandler.post(updateRunnable);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        uiHandler.removeCallbacks(updateRunnable);
    }
}
