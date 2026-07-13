package com.fanconn.rideOS.menu;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.fanconn.rideOS.AhTrackerManager;
import com.fanconn.rideOS.Heartbeat3;
import com.fanconn.rideOS.MetadataManager;
import com.fanconn.rideOS.R;
import com.fanconn.rideOS.SerialPortManager;
import com.fanconn.rideOS.UIUpdater;

import java.util.Locale;

public class BatteryFragment extends Fragment {

    private SerialPortManager serialPortManager;
//    private Button ChargeMOS_ON, ChargeMOS_OFF;
    // Main Battery Views
    private TextView mainBatteryVoltage, mainRemainingAh,maincurrentcon;
    private TextView BMSMosTemp,Main_Battry_Size,NTC1, NTC2, NTC3, NTC4;
    // Aux / APU Battery Views
    private TextView apuBatteryVoltage, apuRemainingAh,apucurrentcon;
    private TextView AuxNTC1, AuxNTC2, AuxNTC3, AuxNTC4;
    // Top Section Views
    private LinearLayout topMainContainer, topAuxContainer;
    private TextView topCardValue, Rangetext, RangeValue;
    private TextView topAuxValue, topAuxRangeText, topAuxRangeValue;
    private TextView Batterytv, topAuxBatteryText;
    // Other Common Views
    private TextView  mainMosState, apuMosState;

    // Scroll Containers
    private ScrollView mainScroll, auxScroll;
    // Radio buttons
    private RadioButton btnBattery, btnAuxillary;
    private Handler uiHandler = new Handler(Looper.getMainLooper());
    private double RANGE_km_PER_SOC =1.7;
    // Enum to track selected battery
    private enum BatteryType { MAIN, AUX }
    private BatteryType currentBattery = BatteryType.MAIN;
    AhTrackerManager ahTrackerManager;
    private Runnable updateRunnable = new Runnable() {
        @SuppressLint("DefaultLocale")
        @Override
        public void run() {
            updateApuButtonState();
            if (serialPortManager != null && serialPortManager.getHb3() != null) {
                Heartbeat3 hb3 = serialPortManager.getHb3();

                if (!isAdded()) return;

                requireActivity().runOnUiThread(() -> {
                    if (currentBattery == BatteryType.MAIN) {
                        // ----------------- MAIN BATTERY -----------------
                        float battery = hb3.getBatteryLevel();
//                        double battery = roundTo2Decimal(ahTrackerManager.getSOC());

                        double range = battery * RANGE_km_PER_SOC;

                        SpannableString batteryText = new SpannableString(battery + "%");
                        batteryText.setSpan(new RelativeSizeSpan(0.7f),
                                batteryText.length() - 1,
                                batteryText.length(),
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        topCardValue.setText(batteryText);

                        int fixedRange = (int) range; // removes decimal part

                        SpannableString rangeText = new SpannableString(fixedRange + " km");
                        rangeText.setSpan(new RelativeSizeSpan(0.7f),
                                rangeText.length() - 2,
                                rangeText.length(),
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                        RangeValue.setText(rangeText);


                        Batterytv.setText("battery level");
                        Rangetext.setText("range");

                        mainBatteryVoltage.setText(String.format("%.1f V", hb3.getbatvoltage() / 10.0));
//                        float rawCurrent = hb3.getrealCurrent();
//
//                        if (rawCurrent == 0) {
//                            maincurrentcon.setText("0.0 A");
//                        } else {
//                            double current = (rawCurrent - 30000) / 10.0; // correct calculation
//                            maincurrentcon.setText(String.format("%.1f A", current));
//                        }
                        float rawCurrent = hb3.getChargingbit();

                        if (rawCurrent == 0) {
                            maincurrentcon.setText("0.0 A");
                        } else {
                            maincurrentcon.setText(String.format("%.1f A", rawCurrent));
                        }

                        float remAh = ahTrackerManager.getRemainingAh();

                        // truncate to 1 decimal place (no rounding)
                        float clippedAh = Math.min(remAh, getBatteryCapacity());
                        float truncated = (float) (Math.floor(clippedAh * 10) / 10.0);
                        mainRemainingAh.setText(String.format("%.1f Ah", truncated));

                        int BMS_Mos_Temp = hb3.getBMS_MOS_Temp();
                        BMSMosTemp.setText(BMS_Mos_Temp + "°C");



                        int ntc1 = hb3.getNTC1();
                        NTC1.setText(ntc1 < 40 ? "--" : (ntc1 - 40) + "°C");

                        int ntc2 = hb3.getNTC2();
                        NTC2.setText(ntc2 < 40 ? "--" : (ntc2 - 40) + "°C");

                        int ntc3 = hb3.getNTC3();
                        NTC3.setText(ntc3 < 40 ? "--" : (ntc3 - 40) + "°C");

                        int ntc4 = hb3.getNTC4();
                        NTC4.setText(ntc4 < 40 ? "--" : (ntc4 - 40) + "°C");


                        String chargeState = (hb3.getMainChargeMosState() == 1) ? "ON" : "OFF";
                        String dischargeState = (hb3.getMainDisChargeMosState() == 1) ? "ON" : "OFF";

                        mainMosState.setText("Charge Switch: " + chargeState +
                                " | Discharge Switch: " + dischargeState);

                    } else {
                        // ----------------- AUX BATTERY -----------------
                        int apuBattery = hb3.getAPUBatteryLevel() / 10; // use main values for now
                        double apuRange = apuBattery * 0.15;

                        SpannableString apuBatteryText = new SpannableString(apuBattery + "%");
                        apuBatteryText.setSpan(new RelativeSizeSpan(0.7f),
                                apuBatteryText.length() - 1,
                                apuBatteryText.length(),
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        topAuxValue.setText(apuBatteryText);
                        String formattedRange = String.format(Locale.getDefault(), "%.1f", apuRange);

                        SpannableString apuRangeText = new SpannableString(formattedRange + " km");
                        apuRangeText.setSpan(new RelativeSizeSpan(0.7f),
                                apuRangeText.length() - 2,
                                apuRangeText.length(),
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        topAuxRangeValue.setText(apuRangeText);

                        topAuxRangeText.setText("range");
                        topAuxBatteryText.setText("battery level");

                        int ntc5 = hb3.getNTC5();
                        AuxNTC1.setText(ntc5 == 0 ? "0°C" : (ntc5 - 40) + "°C");

                        int ntc6 = hb3.getNTC6();
                        AuxNTC2.setText(ntc6 == 0 ? "0°C" : (ntc6 - 40) + "°C");

                        int ntc7 = hb3.getNTC7();
                        AuxNTC3.setText(ntc7 == 0 ? "0°C" : (ntc7 - 40) + "°C");

                        int ntc8 = hb3.getNTC8();
                        AuxNTC4.setText(ntc8 == 0 ? "0°C" : (ntc8 - 40) + "°C");


                        String chargeState = (hb3.getApuChargeMosState() == 1) ? "ON" : "OFF";
                        String dischargeState = (hb3.getApuDisChargeMosState() == 1) ? "ON" : "OFF";

                        apuMosState.setText("Charge Switch: " + chargeState +
                                " | Discharge Switch: " + dischargeState);

                        apuBatteryVoltage.setText(String.format("%.1f V", hb3.getAPUbatvoltage() / 10.0));
                        apuRemainingAh.setText(String.format("%.1f Ah", hb3.getAPUremainingAH() / 1000.0));
                        float rawCurrent = hb3.getAPUrealCurrent();

                        if (rawCurrent == 0) {
                            apucurrentcon.setText("0.0 A");
                        } else {
                            double current = (rawCurrent - 30000) / 10.0; // correct calculation
                            apucurrentcon.setText(String.format("%.1f A", current));
                        }
                    }

                });
            }

            uiHandler.postDelayed(this, 1000);
        }
    };

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
    private float getBatterykWh() {
        String model = MetadataManager.get().getModel();

        if (model == null) return 4.41f; // default safety

        switch (model) {
            case "nx100 Pro":
                return 4.41f;

            case "nx100 Max":
                return 6.62f;

            default:
                return 4.41f; // fallback
        }
    }
    float Battery_Size;
    private double roundTo2Decimal(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_battery_fragment, container, false);

        serialPortManager = SerialPortManager.peekInstance();
        ahTrackerManager=AhTrackerManager.getInstance(requireContext());
        // Top Section
        topMainContainer = view.findViewById(R.id.topMainContainer);
        topAuxContainer = view.findViewById(R.id.topAuxContainer);

        topCardValue = view.findViewById(R.id.topCardValue);
        Rangetext = view.findViewById(R.id.Rangetext);
        RangeValue = view.findViewById(R.id.RangeValue);
        Batterytv = view.findViewById(R.id.Batterytv);

        topAuxValue = view.findViewById(R.id.topAuxValue);
        topAuxRangeText = view.findViewById(R.id.topAuxRangeText);
        topAuxRangeValue = view.findViewById(R.id.topAuxRangeValue);
        topAuxBatteryText = view.findViewById(R.id.topAuxBatteryText);
//        ChargeMOS_OFF = view.findViewById(R.id.MainChargeMOSOFF);
//        ChargeMOS_ON = view.findViewById(R.id.MainChargeMOSON);
//        ChargeMOS_OFF.setOnClickListener(v -> serialPortManager.VCUSendToMCU("FFAA5501030015162A01800001020304050607"));
//        ChargeMOS_ON.setOnClickListener(v -> serialPortManager.VCUSendToMCU("FFAA5501030015162A01800101020304050607"));

        // Main Battery
        mainBatteryVoltage = view.findViewById(R.id.mainBatteryVoltage);
        maincurrentcon = view.findViewById(R.id.maincurrentconsumption);
        mainRemainingAh = view.findViewById(R.id.mainBatteryCapacity);
        BMSMosTemp = view.findViewById(R.id.BMSMosTemp);
        Main_Battry_Size= view.findViewById(R.id.Main_Battry_Size);
        NTC1 = view.findViewById(R.id.NTC1);
        NTC2 = view.findViewById(R.id.NTC2);
        NTC3 = view.findViewById(R.id.NTC3);
        NTC4 = view.findViewById(R.id.NTC4);

        // Aux Battery
        apuBatteryVoltage = view.findViewById(R.id.auxBatteryVoltage);
        apucurrentcon = view.findViewById(R.id.auxcurrentconsumption);
        apuRemainingAh = view.findViewById(R.id.auxBatteryCapacity);
        AuxNTC1 = view.findViewById(R.id.auxNTC1);
        AuxNTC2 = view.findViewById(R.id.auxNTC2);
        AuxNTC3 = view.findViewById(R.id.auxNTC3);
        AuxNTC4 = view.findViewById(R.id.auxNTC4);

        // Common
        mainMosState = view.findViewById(R.id.mainMosState);
        apuMosState = view.findViewById(R.id.apuMosState);

        // Scroll Containers
        mainScroll = view.findViewById(R.id.mainBatteryScroll);
        auxScroll = view.findViewById(R.id.auxBatteryScroll);

        // Radio buttons
        btnBattery = view.findViewById(R.id.btnBattery);
        btnAuxillary = view.findViewById(R.id.btnAuxillary);
        updateApuButtonState();
        btnBattery.setChecked(true);

        RadioGroup topToggleGroup = view.findViewById(R.id.topToggleGroup);
        topToggleGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.btnBattery) {
                currentBattery = BatteryType.MAIN;
                mainScroll.setVisibility(View.VISIBLE);
                auxScroll.setVisibility(View.GONE);
                topMainContainer.setVisibility(View.VISIBLE);
                topAuxContainer.setVisibility(View.GONE);
            } else if (checkedId == R.id.btnAuxillary) {

                if (!UIUpdater.isAPUPresent) {
                    btnBattery.setChecked(true);
                    return;
                }

                currentBattery = BatteryType.AUX;
                mainScroll.setVisibility(View.GONE);
                auxScroll.setVisibility(View.VISIBLE);
                topMainContainer.setVisibility(View.GONE);
                topAuxContainer.setVisibility(View.VISIBLE);
            }
        });

        uiHandler.postDelayed(updateRunnable, 1000);
        Battery_Size = getBatterykWh();
        Main_Battry_Size.setText("Battery capacity: "+Battery_Size + "kWh");
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        uiHandler.removeCallbacksAndMessages(null);
    }

    private void updateApuButtonState() {

        if (btnAuxillary == null) return;

        if (UIUpdater.isAPUPresent) {

            btnAuxillary.setEnabled(true);
            btnAuxillary.setAlpha(1.0f);   // normal

        } else {

            btnAuxillary.setEnabled(false);
            btnAuxillary.setAlpha(0.2f);   // greyed out

            // if currently showing APU page, switch back
            btnBattery.setChecked(true);
        }
    }
}
