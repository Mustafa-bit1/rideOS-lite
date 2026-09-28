package com.fanconn.rideOS.menu;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.fanconn.rideOS.InactivityManager;
import com.fanconn.rideOS.MetadataManager;
import com.fanconn.rideOS.R;
import com.fanconn.rideOS.SoundPlayer;
import com.fanconn.rideOS.authen.PatternViewRegister;
import com.fanconn.rideOS.authen.SecureStorageUtil;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class riderProfilesFragment extends Fragment {

    // Shared prefs
    private SharedPreferences sharedPreferences;
    private Button btnClearPattern;

    Button btnKeepPin;
    Button btnKeepPattern;

    // Containers & UI
    private View initialContainer;
    private View settingsParent;

    private LinearLayout ridersContainer;
    private ImageButton btnAddRider;

    private LinearLayout basicContainer,
            advancedContainer,
            securityContainer,
            pinContainer,
            patternContainer;

    // BASIC
    private EditText etUsername, etPhone, etAge;

    // ADVANCED
    private SeekBar seekBarMaxSpeed, seekBarTorque;
    private TextView tvMaxspeedValue, tvTorqueLevel;

    private String selectedTorqueLevel = "Level 3";
    private int maxSpeedValue = 60;

    // SECURITY
    private RadioButton securitybtn;

    private EditText etPin, etPinConfirm;

    private TextView tvEnterPin,
            tvConfirmPin,
            TVdrawpat,
            TVconfirmpat;

    private PatternViewRegister patternViewRegister,
            patternViewConfirm;

    private ImageButton btnRegister;

    // Pattern state
    private final List<Integer> firstPattern = new ArrayList<>();
    private final List<Integer> confirmPattern = new ArrayList<>();
    private final List<Integer> selectedPattern = new ArrayList<>();

    private boolean isPatternConfirmed = false;

    // Focused keypad target
    private EditText currentFocusedEditText;

    private Context ctx;

    private int currentStep = 0;

    private ImageButton btnBack,
            btnNext;

    SeekBar seekBarPickup,
            seekBarBraking;

    TextView tvPickupLevel,
            tvBrakingLevel;

    private int pickupLevel = 2;
    private int brakingLevel = 3;

    private boolean keepExistingPin = false;
    private boolean keepExistingPattern = false;

    private boolean isEditMode = false;
    private String editingUsername = "";

    private boolean pinMatched = false;


    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.activity_rider_profiles,
                container,
                false
        );

        ctx = requireContext();

        // ============================================================
        // BIND CONTAINERS
        // ============================================================

        initialContainer =
                view.findViewById(R.id.initialContainer);

        settingsParent =
                view.findViewById(R.id.settingsParent);

        ridersContainer =
                view.findViewById(R.id.ridersContainer);

        btnAddRider =
                view.findViewById(R.id.btnRegister);

        basicContainer =
                view.findViewById(R.id.basicContainer);

        advancedContainer =
                view.findViewById(R.id.advancedContainer);

        securityContainer =
                view.findViewById(R.id.securityContainer);

        pinContainer =
                view.findViewById(R.id.pinContainer);

        patternContainer =
                view.findViewById(R.id.patternContainer);

        btnBack =
                view.findViewById(R.id.btnBack);

        btnNext =
                view.findViewById(R.id.btnNext);


        // ============================================================
        // TOP TOGGLE TABS
        // ============================================================

        RadioButton basicBtn =
                view.findViewById(R.id.basicbtn);

        RadioButton advancedBtn =
                view.findViewById(R.id.advancedbtn);

        RadioButton securityBtn =
                view.findViewById(R.id.securitybtn);

        disableToggle(basicBtn);
        disableToggle(advancedBtn);
        disableToggle(securityBtn);


        // ============================================================
        // INITIAL SCREEN
        // ============================================================

        initialContainer.setVisibility(View.VISIBLE);
        settingsParent.setVisibility(View.GONE);

        btnBack.setVisibility(View.GONE);
        btnNext.setVisibility(View.GONE);


        // ============================================================
        // BASIC FIELDS
        // ============================================================

        etUsername =
                view.findViewById(R.id.etUsername);

        etPhone =
                view.findViewById(R.id.etPhone);

        etPhone.addTextChangedListener(
                new TextWatcher() {

                    private boolean isUpdating = false;

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count) {
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s) {

                        if (isUpdating)
                            return;

                        isUpdating = true;

                        String input =
                                s.toString();

                        String digitsOnly =
                                input.replaceAll(
                                        "[^\\d]",
                                        ""
                                );

                        if (digitsOnly.length() > 10) {

                            digitsOnly =
                                    digitsOnly.substring(
                                            0,
                                            10
                                    );
                        }

                        if (!digitsOnly.equals(input)) {

                            etPhone.setText(
                                    digitsOnly
                            );

                            etPhone.setSelection(
                                    digitsOnly.length()
                            );

                            isUpdating = false;
                            return;
                        }

                        if (digitsOnly.length() == 10) {

                            hideKeyboard();

                            etAge.requestFocus();
                        }

                        isUpdating = false;

                        updateNextButtonState();
                    }
                }
        );


        etAge =
                view.findViewById(R.id.etAge);

        etAge.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count) {
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s) {

                        if (s.length() == 2) {

                            hideKeyboard();

                            etAge.clearFocus();
                        }

                        updateNextButtonState();
                    }
                }
        );


        // ============================================================
        // ADVANCED
        // ============================================================

        seekBarMaxSpeed =
                view.findViewById(
                        R.id.seekBarMaxSpeed
                );

        seekBarTorque =
                view.findViewById(
                        R.id.seekBarTorque
                );

        tvMaxspeedValue =
                view.findViewById(
                        R.id.tvMaxspeedValue
                );

        tvTorqueLevel =
                view.findViewById(
                        R.id.tvTorqueLevel
                );

        seekBarPickup =
                view.findViewById(
                        R.id.seekBarPickup
                );

        seekBarBraking =
                view.findViewById(
                        R.id.seekBarBraking
                );

        tvPickupLevel =
                view.findViewById(
                        R.id.tvPickupLevel
                );

        tvBrakingLevel =
                view.findViewById(
                        R.id.tvBrakingLevel
                );


        // ============================================================
        // SECURITY
        // ============================================================

        securitybtn =
                view.findViewById(
                        R.id.securitybtn
                );

        etPin =
                view.findViewById(
                        R.id.etPin
                );

        etPinConfirm =
                view.findViewById(
                        R.id.etPinConfirm
                );

        tvEnterPin =
                view.findViewById(
                        R.id.tvEnterPin
                );

        tvConfirmPin =
                view.findViewById(
                        R.id.tvConfirmPin
                );

        TVdrawpat =
                view.findViewById(
                        R.id.TVdrawpat
                );

        TVconfirmpat =
                view.findViewById(
                        R.id.TVconfirmpat
                );

        patternViewRegister =
                view.findViewById(
                        R.id.patternViewRegister
                );

        patternViewConfirm =
                view.findViewById(
                        R.id.patternViewConfirm
                );

        btnClearPattern =
                view.findViewById(
                        R.id.btnClearPattern
                );

        btnKeepPin =
                view.findViewById(
                        R.id.btnKeepPin
                );

        btnKeepPattern =
                view.findViewById(
                        R.id.btnKeepPattern
                );

        btnRegister =
                view.findViewById(
                        R.id.btnsave
                );

        btnRegister.setVisibility(
                View.GONE
        );

        basicContainer.setVisibility(
                View.GONE
        );

        advancedContainer.setVisibility(
                View.GONE
        );

        securityContainer.setVisibility(
                View.GONE
        );


        // ============================================================
        // LOAD RIDERS
        // ============================================================

        new Thread(() -> {

            List<String> riders =
                    new ArrayList<>();

            if (sharedPreferences == null)
                return;

            Map<String, ?> all =
                    sharedPreferences.getAll();

            for (Map.Entry<String, ?> e :
                    all.entrySet()) {

                if (e.getKey().startsWith("user_")
                        && e.getKey().endsWith("_age")) {

                    riders.add(
                            e.getKey()
                                    .replace(
                                            "user_",
                                            ""
                                    )
                                    .replace(
                                            "_age",
                                            ""
                                    )
                    );
                }
            }

            if (isAdded()) {

                requireActivity()
                        .runOnUiThread(() -> {

                            ridersContainer
                                    .removeAllViews();

                            for (String r :
                                    riders) {

                                addRiderCard(r);
                            }
                        });
            }

        }).start();


        // ============================================================
        // MAX SPEED
        // ============================================================

        seekBarMaxSpeed.setMax(80);

        seekBarMaxSpeed.setProgress(80);

        maxSpeedValue =
                20 +
                        seekBarMaxSpeed.getProgress();

        tvMaxspeedValue.setText(
                maxSpeedValue + " Kmph"
        );

        seekBarMaxSpeed.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser) {

                        maxSpeedValue =
                                20 + progress;

                        tvMaxspeedValue.setText(
                                maxSpeedValue +
                                        " Kmph"
                        );
                    }

                    @Override
                    public void onStartTrackingTouch(
                            SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(
                            SeekBar seekBar) {
                    }
                }
        );


        // ============================================================
        // TORQUE
        // ============================================================

        seekBarTorque.setMax(4);

        seekBarTorque.setProgress(1);

        selectedTorqueLevel =
                "Level 1";

        tvTorqueLevel.setText(
                selectedTorqueLevel
        );

        seekBarTorque.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser) {

                        switch (progress) {

                            case 0:
                                selectedTorqueLevel =
                                        "Level 1";
                                break;

                            case 1:
                                selectedTorqueLevel =
                                        "Level 2";
                                break;

                            case 2:
                                selectedTorqueLevel =
                                        "Level 3";
                                break;

                            case 3:
                                selectedTorqueLevel =
                                        "Level 4";
                                break;

                            case 4:
                                selectedTorqueLevel =
                                        "Level 5";
                                break;
                        }

                        tvTorqueLevel.setText(
                                selectedTorqueLevel
                        );
                    }

                    @Override
                    public void onStartTrackingTouch(
                            SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(
                            SeekBar seekBar) {
                    }
                }
        );


        // ============================================================
        // PICKUP
        // ============================================================

        seekBarPickup.setMax(2);

        seekBarPickup.setProgress(1);

        pickupLevel = 2;

        tvPickupLevel.setText(
                "Level 2"
        );

        seekBarPickup.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser) {

                        pickupLevel =
                                progress + 1;

                        tvPickupLevel.setText(
                                "Level " +
                                        pickupLevel
                        );
                    }

                    @Override
                    public void onStartTrackingTouch(
                            SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(
                            SeekBar seekBar) {
                    }
                }
        );


        // ============================================================
        // BRAKING
        // ============================================================

        seekBarBraking.setMax(5);

        seekBarBraking.setProgress(3);

        brakingLevel = 3;

        tvBrakingLevel.setText(
                "Level 3"
        );

        seekBarBraking.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser) {

                        brakingLevel =
                                progress + 1;

                        tvBrakingLevel.setText(
                                "Level " +
                                        brakingLevel
                        );
                    }

                    @Override
                    public void onStartTrackingTouch(
                            SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(
                            SeekBar seekBar) {
                    }
                }
        );


        // ============================================================
        // ADD RIDER
        // ============================================================

        btnAddRider.setOnClickListener(v -> {

            String vin =
                    MetadataManager
                            .get()
                            .getVin();

            if (vin == null
                    || vin.equals("UNKNOWN_VIN")
                    || vin.trim().isEmpty()) {

                showCustomToasterror(
                        "Empty VIN"
                );
            }

            if (getRegisteredRiderCount() >= 4) {

                showCustomToasterror(
                        "Maximum 4 Riders"
                );

                return;
            }

            initialContainer.setVisibility(
                    View.GONE
            );

            settingsParent.setVisibility(
                    View.VISIBLE
            );

            currentStep = 0;

            showBasicTab();

            basicBtn.setChecked(true);

            btnBack.setVisibility(
                    View.VISIBLE
            );

            btnNext.setVisibility(
                    View.VISIBLE
            );

            btnNext.setEnabled(false);

            btnNext.setAlpha(0.1f);

            clearEntryFields();

            btnKeepPin.setVisibility(
                    View.GONE
            );

            btnKeepPattern.setVisibility(
                    View.GONE
            );

            keepExistingPin = false;
            keepExistingPattern = false;

            isEditMode = false;

            InactivityManager
                    .getInstance()
                    .stopTimer();
        });


        // ============================================================
        // SAVE RIDER
        // ============================================================

        btnRegister.setOnClickListener(v -> {

            hideKeyboard();

            if (currentStep != 2) {

                showCustomToasterror(
                        "Complete all steps before saving."
                );

                return;
            }

            handleRegisterSave();
        });


        // ============================================================
        // SECURITY BUTTON
        // ============================================================

        securitybtn.setOnClickListener(
                v -> security()
        );


        setupFocusListeners();

        setPatternListeners();


        // ============================================================
        // CLEAR PATTERN
        // ============================================================

        btnClearPattern.setOnClickListener(v -> {

            resetPatterns();

            patternViewConfirm.setVisibility(
                    View.GONE
            );

            TVconfirmpat.setVisibility(
                    View.GONE
            );

            patternViewRegister.setVisibility(
                    View.VISIBLE
            );

            TVdrawpat.setVisibility(
                    View.VISIBLE
            );

            btnKeepPattern.setVisibility(
                    isEditMode
                            ? View.VISIBLE
                            : View.GONE
            );

            TVdrawpat.setText(
                    "Draw Pattern"
            );

            TVdrawpat.setTextColor(
                    Color.parseColor("#9D9D9D")
            );

            btnClearPattern.setVisibility(
                    View.GONE
            );

            showCustomToast(
                    "Pattern cleared"
            );
        });


        // ============================================================
        // PIN WATCHER
        // ============================================================

        etPin.addTextChangedListener(
                new TextWatcher() {

                    private boolean isUpdating = false;

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count) {
                    }

                    @Override
                    public void afterTextChanged(
                            Editable editable) {

                        if (isUpdating)
                            return;

                        isUpdating = true;

                        String pin =
                                editable.toString();

                        if (pin.length() > 4) {

                            pin =
                                    pin.substring(
                                            0,
                                            4
                                    );

                            etPin.setText(pin);

                            etPin.setSelection(
                                    pin.length()
                            );
                        }

                        if (pin.length() == 4) {

                            if (isPinAlreadyUsed(pin)) {

                                playErrorSound();

                                showCustomToasterror(
                                        "Invalid PIN. Try a different PIN "
                                );

                                etPin.setText("");

                                etPin.requestFocus();

                                isUpdating = false;

                                return;
                            }

                            etPinConfirm
                                    .requestFocus();
                        }

                        isUpdating = false;
                    }
                }
        );


        // ============================================================
        // PIN CONFIRM
        // ============================================================

        etPinConfirm.addTextChangedListener(
                new TextWatcher() {

                    private boolean isUpdating = false;

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count) {
                    }

                    @Override
                    public void afterTextChanged(
                            Editable editable) {

                        if (isUpdating)
                            return;

                        isUpdating = true;

                        String pinConfirm =
                                editable.toString();

                        if (pinConfirm.length() > 4) {

                            pinConfirm =
                                    pinConfirm.substring(
                                            0,
                                            4
                                    );

                            etPinConfirm
                                    .setText(pinConfirm);

                            etPinConfirm
                                    .setSelection(
                                            pinConfirm.length()
                                    );
                        }

                        if (pinConfirm.length() == 4) {

                            String pin =
                                    etPin.getText()
                                            .toString()
                                            .trim();

                            if (pin.length() != 4) {

                                isUpdating = false;

                                return;
                            }

                            if (!pin.equals(
                                    pinConfirm)) {

                                etPin.setText("");
                                etPinConfirm.setText("");

                                etPin.requestFocus();

                                playErrorSound();

                                showCustomToasterror(
                                        "PINs do not match."
                                );

                                isUpdating = false;

                                return;
                            }

                            hideKeyboard();

                            pinMatched = true;

                            pinContainer
                                    .setVisibility(
                                            View.GONE
                                    );

                            patternContainer
                                    .setVisibility(
                                            View.VISIBLE
                                    );

                            patternViewRegister
                                    .setVisibility(
                                            View.VISIBLE
                                    );

                            TVdrawpat
                                    .setVisibility(
                                            View.VISIBLE
                                    );

                            TVdrawpat.setText(
                                    "Draw Pattern"
                            );

                            patternViewConfirm
                                    .setVisibility(
                                            View.GONE
                                    );

                            TVconfirmpat
                                    .setVisibility(
                                            View.GONE
                                    );

                            btnKeepPattern
                                    .setVisibility(
                                            isEditMode
                                                    ? View.VISIBLE
                                                    : View.GONE
                                    );

                            resetPatterns();
                        }

                        isUpdating = false;
                    }
                }
        );


        // ============================================================
        // NEXT
        // ============================================================

        btnNext.setOnClickListener(v -> {

            hideKeyboard();

            if (currentStep == 0) {

                String name =
                        etUsername.getText()
                                .toString()
                                .trim();

                String phone =
                        etPhone.getText()
                                .toString()
                                .trim();

                String ageStr =
                        etAge.getText()
                                .toString()
                                .trim();

                if (name.isEmpty()) {

                    showCustomToasterror(
                            "Please enter your name"
                    );

                    return;
                }

                if (phone.isEmpty()) {

                    showCustomToasterror(
                            "Please enter phone number"
                    );

                    return;
                }

                if (!phone.matches(
                        "\\d{10}"
                )) {

                    showCustomToasterror(
                            "Phone number must be exactly 10 digits"
                    );

                    return;
                }

                if (ageStr.isEmpty()) {

                    showCustomToasterror(
                            "Please enter your age"
                    );

                    return;
                }

                int age =
                        Integer.parseInt(ageStr);

                if (age < 18 || age > 110) {

                    showCustomToasterror(
                            "Invalid age"
                    );

                    return;
                }

                if (isActiveRiderNameExists(
                        name
                )) {

                    if (!(isEditMode
                            && name.equals(
                            editingUsername))) {

                        playErrorSound();

                        showCustomToasterror(
                                "Rider name already exists"
                        );

                        showBasicTab();

                        basicBtn.setChecked(
                                true
                        );

                        return;
                    }
                }

                currentStep = 1;

                showAdvancedTab();

                advancedBtn.setChecked(
                        true
                );

                return;
            }

            if (currentStep == 1) {

                currentStep = 2;

                showSecurityTab();

                securityBtn.setChecked(
                        true
                );
            }
        });


        // ============================================================
        // BACK
        // ============================================================

        btnBack.setOnClickListener(v -> {

            hideKeyboard();

            if (currentStep == 0) {

                settingsParent.setVisibility(
                        View.GONE
                );

                initialContainer.setVisibility(
                        View.VISIBLE
                );

                softResetFields();

                etUsername.setEnabled(
                        true
                );

                btnBack.setVisibility(
                        View.GONE
                );

                btnNext.setVisibility(
                        View.GONE
                );

            } else if (currentStep == 1) {

                currentStep = 0;

                basicBtn.setChecked(
                        true
                );

                showBasicTab();

                btnNext.setVisibility(
                        View.VISIBLE
                );

            } else if (currentStep == 2) {

                currentStep = 1;

                advancedBtn.setChecked(
                        true
                );

                showAdvancedTab();

                btnNext.setVisibility(
                        View.VISIBLE
                );
            }
        });


        // ============================================================
        // KEEP EXISTING PIN
        // ============================================================

        btnKeepPin.setOnClickListener(v -> {

            keepExistingPin = true;

            pinContainer.setVisibility(
                    View.GONE
            );

            patternContainer.setVisibility(
                    View.VISIBLE
            );

            patternViewRegister
                    .setVisibility(
                            View.VISIBLE
                    );

            TVdrawpat.setVisibility(
                    View.VISIBLE
            );

            TVdrawpat.setText(
                    "Draw Pattern"
            );

            patternViewConfirm
                    .setVisibility(
                            View.GONE
                    );

            TVconfirmpat.setVisibility(
                    View.GONE
            );

            btnKeepPattern.setVisibility(
                    isEditMode
                            ? View.VISIBLE
                            : View.GONE
            );

            showCustomToast(
                    "Existing PIN will be set"
            );
        });


        // ============================================================
        // KEEP EXISTING PATTERN
        // ============================================================

        btnKeepPattern.setOnClickListener(v -> {

            keepExistingPattern = true;

            patternViewRegister
                    .setVisibility(
                            View.GONE
                    );

            patternViewConfirm
                    .setVisibility(
                            View.GONE
                    );

            TVconfirmpat.setVisibility(
                    View.GONE
            );

            btnClearPattern.setVisibility(
                    View.GONE
            );

            btnKeepPattern.setVisibility(
                    View.GONE
            );

            TVdrawpat.setVisibility(
                    View.VISIBLE
            );

            TVdrawpat.setText(
                    "Verified Successfully, Please save your profile"
            );

            TVdrawpat.setTextColor(
                    getResources().getColor(
                            android.R.color.holo_blue_bright
                    )
            );

            btnRegister.setVisibility(
                    View.VISIBLE
            );

            showCustomToast(
                    "Existing Pattern will be set"
            );
        });


        return view;
    }


    // ================================================================
    // UI HELPERS
    // ================================================================

    private void disableToggle(
            RadioButton btn) {

        btn.setClickable(false);
        btn.setFocusable(false);
        btn.setLongClickable(false);

        btn.setOnTouchListener(
                (v, event) -> true
        );
    }


    private void showBasicTab() {

        basicContainer.setVisibility(
                View.VISIBLE
        );

        advancedContainer.setVisibility(
                View.GONE
        );

        securityContainer.setVisibility(
                View.GONE
        );

        btnRegister.setVisibility(
                View.GONE
        );

        btnNext.setVisibility(
                View.VISIBLE
        );
    }


    private void hideKeyboard() {

        InputMethodManager imm =
                (InputMethodManager)
                        requireContext()
                                .getSystemService(
                                        Context.INPUT_METHOD_SERVICE
                                );

        View view =
                requireActivity()
                        .getCurrentFocus();

        if (view == null) {

            view =
                    new View(
                            requireContext()
                    );
        }

        imm.hideSoftInputFromWindow(
                view.getWindowToken(),
                0
        );
    }


    private void showAdvancedTab() {

        basicContainer.setVisibility(
                View.GONE
        );

        advancedContainer.setVisibility(
                View.VISIBLE
        );

        securityContainer.setVisibility(
                View.GONE
        );

        btnRegister.setVisibility(
                View.GONE
        );

        btnNext.setVisibility(
                View.VISIBLE
        );
    }


    private void showSecurityTab() {

        basicContainer.setVisibility(
                View.GONE
        );

        advancedContainer.setVisibility(
                View.GONE
        );

        securityContainer.setVisibility(
                View.VISIBLE
        );

        btnNext.setVisibility(
                View.GONE
        );

        pinContainer.setVisibility(
                View.VISIBLE
        );

        patternContainer.setVisibility(
                View.GONE
        );

        if (isEditMode) {

            btnKeepPin.setVisibility(
                    View.VISIBLE
            );

        } else {

            btnKeepPin.setVisibility(
                    View.GONE
            );
        }

        etPin.setHint(
                isEditMode
                        ? "● ● ● ●"
                        : "Enter PIN"
        );

        etPinConfirm.setHint(
                isEditMode
                        ? "● ● ● ●"
                        : "Confirm PIN"
        );

        btnKeepPattern.setVisibility(
                View.GONE
        );

        etPin.setVisibility(
                View.VISIBLE
        );

        etPinConfirm.setVisibility(
                View.VISIBLE
        );

        tvEnterPin.setVisibility(
                View.VISIBLE
        );

        tvConfirmPin.setVisibility(
                View.VISIBLE
        );

        resetPatterns();
    }


    private void clearEntryFields() {

        etUsername.setText("");
        etPhone.setText("");
        etAge.setText("");

        seekBarMaxSpeed.setProgress(
                80
        );

        tvMaxspeedValue.setText(
                "100 Kmph"
        );

        seekBarTorque.setProgress(
                4
        );

        selectedTorqueLevel =
                "Level 5";

        tvTorqueLevel.setText(
                "Level 5"
        );

        etPin.setText("");
        etPinConfirm.setText("");

        resetPatterns();

        patternViewRegister
                .setVisibility(
                        View.GONE
                );

        patternViewConfirm
                .setVisibility(
                        View.GONE
                );

        TVdrawpat.setVisibility(
                View.GONE
        );

        TVconfirmpat.setVisibility(
                View.GONE
        );

        seekBarPickup.setProgress(
                1
        );

        pickupLevel = 2;

        tvPickupLevel.setText(
                "Level 2"
        );

        pinMatched = false;

        isPatternConfirmed = false;

        selectedPattern.clear();
        firstPattern.clear();
        confirmPattern.clear();

        updateNextButtonState();
    }


    // ================================================================
    // RIDER CARDS
    // ================================================================

    private void loadUsers() {

        if (ridersContainer == null)
            return;

        ridersContainer.removeAllViews();

        Set<String> list =
                new HashSet<>();

        for (Map.Entry<String, ?> e :
                sharedPreferences
                        .getAll()
                        .entrySet()) {

            if (e.getKey().startsWith(
                    "user_"
            )
                    && e.getKey().contains(
                    "_age"
            )) {

                list.add(
                        e.getKey()
                                .replace(
                                        "user_",
                                        ""
                                )
                                .replace(
                                        "_age",
                                        ""
                                )
                );
            }
        }

        for (String name : list) {

            addRiderCard(name);
        }
    }


    private void addRiderCard(
            String username) {

        View card =
                LayoutInflater.from(ctx)
                        .inflate(
                                R.layout.rider_card,
                                ridersContainer,
                                false
                        );

        TextView nameText =
                card.findViewById(
                        R.id.riderName
                );

        TextView tvMaxSpeed =
                card.findViewById(
                        R.id.tvMaxSpeed
                );

        TextView tvTorque =
                card.findViewById(
                        R.id.tvTorque
                );

        ImageButton deleteBtn =
                card.findViewById(
                        R.id.deleteBtn
                );

        ImageButton editBtn =
                card.findViewById(
                        R.id.editBtn
                );

        nameText.setText(username);

        String maxSpeed =
                sharedPreferences.getString(
                        "user_" + username +
                                "_maxspeed",
                        "60"
                );

        String torque =
                sharedPreferences.getString(
                        "user_" + username +
                                "_torque",
                        "Level 2"
                );

        tvMaxSpeed.setText(
                "Max speed : " +
                        maxSpeed +
                        " Kmph"
        );

        tvTorque.setText(
                "Torque level : " +
                        torque +
                        "/5"
        );

        String currentRider =
                getCurrentRider();


        if (username.equals(
                currentRider)) {

            card.setAlpha(1.0f);

            deleteBtn.setEnabled(
                    true
            );

            editBtn.setEnabled(
                    true
            );

        } else {

            card.setAlpha(0.3f);

            deleteBtn.setEnabled(
                    false
            );

            editBtn.setEnabled(
                    false
            );
        }


        // ============================================================
        // DELETE
        // ============================================================

        deleteBtn.setOnClickListener(v -> {

            if (!username.equals(
                    getCurrentRider())) {

                showCustomToasterror(
                        "You are not allowed to delete this rider"
                );

                return;
            }

            if (getRegisteredRiderCount()
                    <= 1) {

                showCustomToasterror(
                        "At least one rider must remain"
                );

                return;
            }

            View dialogView =
                    LayoutInflater.from(ctx)
                            .inflate(
                                    R.layout.popupdialog,
                                    null
                            );

            AlertDialog.Builder builder =
                    new AlertDialog.Builder(ctx);

            builder.setView(
                    dialogView
            );

            AlertDialog dialog =
                    builder.create();

            if (dialog.getWindow()
                    != null) {

                dialog.getWindow()
                        .setBackgroundDrawableResource(
                                android.R.color.transparent
                        );
            }

            ((TextView)
                    dialogView.findViewById(
                            R.id.dialogTitle
                    )).setText(
                    "Confirm Deletion"
            );

            ((TextView)
                    dialogView.findViewById(
                            R.id.dialogMessage
                    )).setText(
                    "Delete rider \"" +
                            username +
                            "\" ?"
            );

            Button yes =
                    dialogView.findViewById(
                            R.id.btnYes
                    );

            Button no =
                    dialogView.findViewById(
                            R.id.btnNo
                    );

            yes.setText("Delete");
            no.setText("Cancel");

            yes.setOnClickListener(
                    b -> {

                        dialog.dismiss();

                        deleteUser(
                                username
                        );
                    }
            );

            no.setOnClickListener(
                    b -> dialog.dismiss()
            );

            dialog.show();
        });


        // ============================================================
        // EDIT
        // ============================================================

        editBtn.setOnClickListener(v -> {

            if (!username.equals(
                    getCurrentRider())) {

                showCustomToasterror(
                        "Only current rider can be edited"
                );

                return;
            }

            startEditFlow(username);
        });

        ridersContainer.addView(
                card
        );
    }


    private void startEditFlow(
            String username) {

        etUsername.setEnabled(
                false
        );

        initialContainer.setVisibility(
                View.GONE
        );

        settingsParent.setVisibility(
                View.VISIBLE
        );

        btnBack.setVisibility(
                View.VISIBLE
        );

        btnNext.setVisibility(
                View.VISIBLE
        );

        currentStep = 0;

        showBasicTab();


        String age =
                sharedPreferences.getString(
                        "user_" + username +
                                "_age",
                        ""
                );

        String phone =
                sharedPreferences.getString(
                        "user_" + username +
                                "_phone",
                        ""
                );

        String maxSpeed =
                sharedPreferences.getString(
                        "user_" + username +
                                "_maxspeed",
                        "60"
                );

        String torque =
                sharedPreferences.getString(
                        "user_" + username +
                                "_torque",
                        "Level 2"
                );

        String pickup =
                sharedPreferences.getString(
                        "user_" + username +
                                "_pickup",
                        "2"
                );

        String braking =
                sharedPreferences.getString(
                        "user_" + username +
                                "_braking",
                        "3"
                );


        etUsername.setText(
                username
        );

        etPhone.setText(
                phone
        );

        etAge.setText(
                age
        );


        int speed =
                Integer.parseInt(
                        maxSpeed
                );

        seekBarMaxSpeed.setProgress(
                speed - 20
        );

        tvMaxspeedValue.setText(
                speed +
                        " Kmph"
        );


        switch (torque) {

            case "Level 1":
                seekBarTorque.setProgress(0);
                break;

            case "Level 2":
                seekBarTorque.setProgress(1);
                break;

            case "Level 3":
                seekBarTorque.setProgress(2);
                break;

            case "Level 4":
                seekBarTorque.setProgress(3);
                break;

            case "Level 5":
                seekBarTorque.setProgress(4);
                break;
        }

        selectedTorqueLevel =
                torque;

        tvTorqueLevel.setText(
                torque
        );


        pickupLevel =
                Integer.parseInt(
                        pickup
                );

        brakingLevel =
                Integer.parseInt(
                        braking
                );

        seekBarPickup.setProgress(
                pickupLevel - 1
        );

        tvPickupLevel.setText(
                "Level " +
                        pickupLevel
        );

        seekBarBraking.setProgress(
                brakingLevel - 1
        );

        tvBrakingLevel.setText(
                "Level " +
                        brakingLevel
        );


        isEditMode = true;

        editingUsername =
                username;

        btnKeepPin.setVisibility(
                View.VISIBLE
        );

        btnKeepPattern.setVisibility(
                View.GONE
        );

        keepExistingPin = false;
        keepExistingPattern = false;
    }


    private int getRegisteredRiderCount() {

        Set<String> usernames =
                new HashSet<>();

        for (Map.Entry<String, ?> entry :
                sharedPreferences
                        .getAll()
                        .entrySet()) {

            if (entry.getKey()
                    .startsWith("user_")
                    && entry.getKey()
                    .endsWith("_age")) {

                usernames.add(
                        entry.getKey()
                                .replace(
                                        "user_",
                                        ""
                                )
                                .replace(
                                        "_age",
                                        ""
                                )
                );
            }
        }

        return usernames.size();
    }


    // ================================================================
    // DELETE RIDER
    // ================================================================

    private void deleteUser(
            String username) {

        try {

            /*
             * IMPORTANT:
             * Read rider_id BEFORE removing it from
             * SharedPreferences.
             */

            String riderId =
                    sharedPreferences.getString(
                            "user_" + username +
                                    "_rider_id",
                            null
                    );


            /*
             * Send DELETE to backend.
             *
             * RiderMqttManager will:
             *
             * MQTT available
             *      -> publish immediately
             *
             * MQTT unavailable
             *      -> save DELETE locally
             */

            if (riderId != null
                    && !riderId.isEmpty()) {

                RiderMqttManager
                        .getInstance(ctx)
                        .deleteRider(
                                riderId
                        );
            }


            // ========================================================
            // DELETE LOCAL RIDER DATA
            // ========================================================

            SharedPreferences.Editor editor =
                    sharedPreferences.edit();

            String[] keys = {

                    "user_" + username +
                            "_age",

                    "user_" + username +
                            "_phone",

                    "user_" + username +
                            "_lucky",

                    "user_" + username +
                            "_maxspeed",

                    "user_" + username +
                            "_torque",

                    "user_" + username +
                            "_pin",

                    "user_" + username +
                            "_salt",

                    "user_" + username +
                            "_pattern",

                    "user_" + username +
                            "_rider_id"
            };

            for (String key : keys) {

                editor.remove(key);
            }

            editor.apply();


            SoundPlayer.playOnce(
                    ctx,
                    R.raw.deletesound
            );

            loadUsers();

            showCustomToast(
                    "Rider Deleted " +
                            username
            );

        } catch (Exception e) {

            e.printStackTrace();

            showCustomToasterror(
                    "Error deleting " +
                            username
            );
        }
    }


    // ================================================================
    // SAVE / REGISTER RIDER
    // ================================================================

    private void handleRegisterSave() {

        etUsername.setEnabled(
                true
        );

        String username =
                etUsername.getText()
                        .toString()
                        .trim();

        String age =
                etAge.getText()
                        .toString()
                        .trim();

        String phone =
                etPhone.getText()
                        .toString()
                        .trim();

        String maxspeed =
                String.valueOf(
                        maxSpeedValue
                );

        String torque =
                selectedTorqueLevel;

        String pin =
                etPin.getText()
                        .toString()
                        .trim();

        String pinConfirm =
                etPinConfirm.getText()
                        .toString()
                        .trim();

        String pickup =
                String.valueOf(
                        pickupLevel
                );

        String braking =
                String.valueOf(
                        brakingLevel
                );


        // ============================================================
        // BASIC VALIDATION
        // ============================================================

        if (TextUtils.isEmpty(username)
                || TextUtils.isEmpty(phone)
                || TextUtils.isEmpty(age)) {

            playErrorSound();

            showCustomToasterror(
                    "Please enter name and age"
            );

            return;
        }


        if (!phone.matches(
                "\\d{10}"
        )) {

            playErrorSound();

            showCustomToasterror(
                    "Invalid phone number"
            );

            return;
        }


        if (TextUtils.isEmpty(maxspeed)
                || TextUtils.isEmpty(torque)) {

            playErrorSound();

            showCustomToasterror(
                    "Please set speed and torque"
            );

            return;
        }


        // ============================================================
        // PIN VALIDATION
        // ============================================================

        if (!keepExistingPin) {

            if (TextUtils.isEmpty(pin)
                    || TextUtils.isEmpty(
                    pinConfirm)) {

                playErrorSound();

                showCustomToasterror(
                        "Please enter PIN"
                );

                return;
            }


            if (!pin.equals(
                    pinConfirm)) {

                playErrorSound();

                etPin.setText("");
                etPinConfirm.setText("");

                showCustomToasterror(
                        "PIN does not match"
                );

                return;
            }
        }


        // ============================================================
        // PATTERN VALIDATION
        // ============================================================

        if (!keepExistingPattern) {

            if (!isPatternConfirmed
                    || selectedPattern.size()
                    < 4) {

                playErrorSound();

                showCustomToasterror(
                        "Please draw and confirm the pattern"
                );

                return;
            }
        }


        // ============================================================
        // SAVE
        // ============================================================

        try {

            SharedPreferences.Editor editor =
                    sharedPreferences.edit();


            String finalUsername =
                    isEditMode
                            ? editingUsername
                            : username;


            /*
             * ========================================================
             * STABLE RIDER ID
             * ========================================================
             *
             * ADD:
             *      generate UUID once
             *
             * UPDATE:
             *      reuse existing UUID
             *
             * This same ID is used by Go backend for
             * ADD / UPDATE / DELETE.
             */

            String riderId;
            boolean firstCloudSync = false;

            if (isEditMode) {

                riderId =
                        sharedPreferences.getString(
                                "user_" +
                                        finalUsername +
                                        "_rider_id",
                                null
                        );

                // Old rider created by previous app version
                if (riderId == null || riderId.isEmpty()) {

                    riderId =
                            RiderMqttManager
                                    .getInstance(ctx)
                                    .generateRiderId();

                    firstCloudSync = true;

                    Log.d(
                            "RiderSync",
                            "Old rider detected. Generated new rider ID: "
                                    + riderId
                    );
                }

            } else {

                riderId =
                        RiderMqttManager
                                .getInstance(ctx)
                                .generateRiderId();
            }


            /*
             * IMPORTANT:
             * Save rider_id locally so it survives
             * future UPDATE and DELETE operations.
             */

            editor.putString(
                    "user_" +
                            finalUsername +
                            "_rider_id",
                    riderId
            );


            // ========================================================
            // SAVE RIDER DETAILS LOCALLY
            // ========================================================

            editor.putString(
                    "user_" +
                            finalUsername +
                            "_age",
                    age
            );

            editor.putString(
                    "user_" +
                            finalUsername +
                            "_phone",
                    phone
            );

            editor.putString(
                    "user_" +
                            finalUsername +
                            "_maxspeed",
                    maxspeed
            );

            editor.putString(
                    "user_" +
                            finalUsername +
                            "_torque",
                    torque
            );

            editor.putString(
                    "user_" +
                            finalUsername +
                            "_pickup",
                    String.valueOf(
                            pickupLevel
                    )
            );

            editor.putString(
                    "user_" +
                            finalUsername +
                            "_braking",
                    String.valueOf(
                            brakingLevel
                    )
            );


            // ========================================================
            // PIN
            // ========================================================

            if (!keepExistingPin) {

                String salt =
                        generateSalt();

                String hashedPin =
                        hashWithSalt(
                                pin,
                                salt
                        );

                editor.putString(
                        "user_" +
                                finalUsername +
                                "_salt",
                        salt
                );

                editor.putString(
                        "user_" +
                                finalUsername +
                                "_pin",
                        hashedPin
                );
            }


            // ========================================================
            // PATTERN
            // ========================================================

            if (!keepExistingPattern) {

                editor.putString(
                        "user_" +
                                finalUsername +
                                "_pattern",
                        TextUtils.join(
                                "",
                                selectedPattern
                        )
                );
            }


            editor.apply();


            // ========================================================
            // NEW RIDER MQTT ARCHITECTURE
            // ========================================================

            RiderMqttManager riderMqttManager =
                    RiderMqttManager
                            .getInstance(ctx);


            if (isEditMode && !firstCloudSync) {

                // Existing rider already known to cloud
                riderMqttManager.updateRider(
                        riderId,
                        finalUsername,
                        phone,
                        Integer.parseInt(age),
                        Float.parseFloat(maxspeed),
                        torque,
                        pickupLevel,
                        brakingLevel
                );

            } else {

                // New rider OR old rider being migrated
                riderMqttManager.addRider(
                        riderId,
                        finalUsername,
                        phone,
                        Integer.parseInt(age),
                        Float.parseFloat(maxspeed),
                        torque,
                        pickupLevel,
                        brakingLevel
                );
            }


            // ========================================================
            // FINISH UI
            // ========================================================

            isEditMode = false;

            editingUsername = "";

            SoundPlayer.playOnce(
                    ctx,
                    R.raw.locksound2
            );

            showCustomToast(
                    "Registered Successfully"
            );

            currentStep = 0;

            btnNext.setVisibility(
                    View.GONE
            );

            btnRegister.setVisibility(
                    View.GONE
            );

            btnBack.setVisibility(
                    View.GONE
            );

            settingsParent.setVisibility(
                    View.GONE
            );

            initialContainer.setVisibility(
                    View.VISIBLE
            );

            softResetFields();

            loadUsers();

        } catch (Exception e) {

            e.printStackTrace();

            playErrorSound();

            showCustomToasterror(
                    "Error saving user"
            );
        }
    }


    @Override
    public void onResume() {

        super.onResume();
    }


    // ================================================================
    // RESET
    // ================================================================

    private void softResetFields() {

        etUsername.setEnabled(
                true
        );

        etUsername.setText("");
        etPhone.setText("");
        etAge.setText("");


        seekBarMaxSpeed.setProgress(
                80
        );

        tvMaxspeedValue.setText(
                "100 Kmph"
        );


        seekBarTorque.setProgress(
                1
        );

        selectedTorqueLevel =
                "Level 2";

        tvTorqueLevel.setText(
                "Level 2"
        );


        etPin.setText("");
        etPinConfirm.setText("");


        keepExistingPin = false;
        keepExistingPattern = false;


        btnKeepPin.setVisibility(
                View.GONE
        );

        btnKeepPattern.setVisibility(
                View.GONE
        );


        isEditMode = false;


        pinMatched = false;

        isPatternConfirmed = false;


        selectedPattern.clear();
        firstPattern.clear();
        confirmPattern.clear();


        TVdrawpat.setText(
                "Draw Pattern"
        );

        TVdrawpat.setTextColor(
                Color.parseColor(
                        "#9D9D9D"
                )
        );


        TVconfirmpat.setText(
                "Confirm Pattern"
        );

        TVconfirmpat.setTextColor(
                Color.parseColor(
                        "#9D9D9D"
                )
        );


        patternViewRegister
                .setVisibility(
                        View.GONE
                );

        patternViewConfirm
                .setVisibility(
                        View.GONE
                );

        TVdrawpat.setVisibility(
                View.GONE
        );

        TVconfirmpat.setVisibility(
                View.GONE
        );

        btnClearPattern.setVisibility(
                View.GONE
        );


        etPin.setVisibility(
                View.VISIBLE
        );

        etPinConfirm.setVisibility(
                View.VISIBLE
        );

        tvEnterPin.setVisibility(
                View.VISIBLE
        );

        tvConfirmPin.setVisibility(
                View.VISIBLE
        );


        updateNextButtonState();
    }


    // ================================================================
    // SECURITY
    // ================================================================

    private void security() {

        String ageText =
                etAge.getText()
                        .toString()
                        .trim();

        String newUsername =
                etUsername.getText()
                        .toString()
                        .trim();

        String phone =
                etPhone.getText()
                        .toString()
                        .trim();


        if (ageText.isEmpty()
                || newUsername.isEmpty()
                || phone.isEmpty()) {

            showCustomToasterror(
                    "Please enter all details"
            );

            playErrorSound();

            return;
        }


        if (!phone.matches(
                "\\d{10}"
        )) {

            showCustomToasterror(
                    "Invalid phone number"
            );

            playErrorSound();

            return;
        }


        try {

            int ageValue =
                    Integer.parseInt(
                            ageText
                    );

            if (ageValue < 18) {

                showCustomToasterror(
                        "Invalid rider age, must be 18 or above"
                );

                playErrorSound();

                return;
            }

        } catch (NumberFormatException e) {

            showCustomToasterror(
                    "Enter a valid age"
            );

            playErrorSound();

            return;
        }


        if (isActiveRiderNameExists(
                newUsername
        )) {

            showCustomToasterror(
                    "Rider name already exists"
            );

            playErrorSound();

            return;
        }


        pinContainer.setVisibility(
                View.VISIBLE
        );

        patternContainer.setVisibility(
                View.GONE
        );

        tvEnterPin.setVisibility(
                View.VISIBLE
        );

        tvConfirmPin.setVisibility(
                View.VISIBLE
        );

        TVconfirmpat.setVisibility(
                View.GONE
        );

        TVdrawpat.setVisibility(
                View.GONE
        );

        resetPatterns();

        etPin.requestFocus();
    }


    // ================================================================
    // SECURITY HELPERS
    // ================================================================

    private String generateSalt() {

        byte[] salt =
                new byte[16];

        new SecureRandom()
                .nextBytes(salt);

        return Base64.encodeToString(
                salt,
                Base64.NO_WRAP
        );
    }


    private String hashWithSalt(
            String input,
            String saltBase64) {

        try {

            byte[] saltBytes =
                    Base64.decode(
                            saltBase64,
                            Base64.NO_WRAP
                    );

            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            digest.update(
                    saltBytes
            );

            byte[] hash =
                    digest.digest(
                            input.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            StringBuilder hex =
                    new StringBuilder();

            for (byte b : hash) {

                hex.append(
                        String.format(
                                "%02x",
                                b
                        )
                );
            }

            return hex.toString();

        } catch (Exception e) {

            e.printStackTrace();

            return null;
        }
    }


    // ================================================================
    // PATTERN
    // ================================================================

    private void setPatternListeners() {

        if (patternViewRegister != null) {

            patternViewRegister
                    .setOnPatternCompleteListener(
                            pattern -> {

                                firstPattern.clear();

                                firstPattern.addAll(
                                        pattern
                                );


                                String patternString =
                                        TextUtils.join(
                                                "",
                                                firstPattern
                                        );


                                if (isPatternAlreadyUsed(
                                        patternString
                                )) {

                                    playErrorSound();

                                    showCustomToasterror(
                                            "Invalid Pattern, Try a different Pattern "
                                    );

                                    resetPatterns();

                                    patternViewRegister
                                            .clearPattern();

                                    return;
                                }


                                patternViewRegister
                                        .clearPattern();


                                patternViewRegister
                                        .setVisibility(
                                                View.GONE
                                        );

                                TVdrawpat.setVisibility(
                                        View.GONE
                                );


                                patternViewConfirm
                                        .setVisibility(
                                                View.VISIBLE
                                        );

                                TVconfirmpat
                                        .setVisibility(
                                                View.VISIBLE
                                        );


                                if (TVconfirmpat
                                        .getVisibility()
                                        == View.VISIBLE) {

                                    btnKeepPattern
                                            .setVisibility(
                                                    View.GONE
                                            );

                                    btnClearPattern
                                            .setVisibility(
                                                    View.VISIBLE
                                            );
                                }
                            }
                    );
        }


        if (patternViewConfirm != null) {

            patternViewConfirm
                    .setOnPatternCompleteListener(
                            pattern -> {

                                confirmPattern.clear();

                                confirmPattern.addAll(
                                        pattern
                                );


                                if (!confirmPattern
                                        .equals(
                                                firstPattern
                                        )) {

                                    playErrorSound();

                                    showCustomToasterror(
                                            "Patterns do not match "
                                    );

                                    resetPatterns();


                                    patternViewConfirm
                                            .setVisibility(
                                                    View.GONE
                                            );

                                    TVconfirmpat
                                            .setVisibility(
                                                    View.GONE
                                            );


                                    patternViewRegister
                                            .setVisibility(
                                                    View.VISIBLE
                                            );

                                    TVdrawpat
                                            .setVisibility(
                                                    View.VISIBLE
                                            );

                                    TVdrawpat.setText(
                                            "Draw Pattern Again"
                                    );

                                    return;
                                }


                                isPatternConfirmed =
                                        true;

                                selectedPattern.clear();

                                selectedPattern.addAll(
                                        firstPattern
                                );


                                patternViewConfirm
                                        .setVisibility(
                                                View.GONE
                                        );

                                TVconfirmpat
                                        .setVisibility(
                                                View.GONE
                                        );

                                btnClearPattern
                                        .setVisibility(
                                                View.GONE
                                        );

                                btnKeepPattern
                                        .setVisibility(
                                                View.GONE
                                        );


                                TVdrawpat
                                        .setVisibility(
                                                View.VISIBLE
                                        );

                                TVdrawpat.setText(
                                        "Verified Successfully, Please save your profile"
                                );

                                TVdrawpat.setTextColor(
                                        getResources()
                                                .getColor(
                                                        android.R.color.holo_blue_bright
                                                )
                                );


                                btnRegister
                                        .setVisibility(
                                                View.VISIBLE
                                        );
                            }
                    );
        }
    }


    private void resetPatterns() {

        firstPattern.clear();

        confirmPattern.clear();

        selectedPattern.clear();

        isPatternConfirmed =
                false;

        if (patternViewRegister != null) {

            patternViewRegister
                    .clearPattern();
        }

        if (patternViewConfirm != null) {

            patternViewConfirm
                    .clearPattern();
        }

        if (btnClearPattern != null) {

            btnClearPattern.setVisibility(
                    View.GONE
            );
        }
    }


    // ================================================================
    // KEYPAD
    // ================================================================

    private void setupFocusListeners() {

        OnFocusChangeListener focusListener =
                (v, hasFocus) -> {

                    if (hasFocus
                            && v instanceof EditText) {

                        currentFocusedEditText =
                                (EditText) v;
                    }
                };


        etPin.setOnFocusChangeListener(
                focusListener
        );

        etPinConfirm.setOnFocusChangeListener(
                focusListener
        );

        etAge.setOnFocusChangeListener(
                focusListener
        );

        etPhone.setOnFocusChangeListener(
                focusListener
        );

        etUsername.setOnFocusChangeListener(
                focusListener
        );
    }


    private void disableSoftKeyboard(
            EditText... editTexts) {

        for (EditText et :
                editTexts) {

            try {

                et.setShowSoftInputOnFocus(
                        false
                );

            } catch (Exception ignored) {
            }
        }
    }


    public void onKeypadButtonClick(
            View view) {

        if (!(view instanceof Button)
                && !(view instanceof ImageButton)) {

            return;
        }


        Object tagObj =
                view.getTag();

        if (tagObj == null)
            return;


        String tag =
                String.valueOf(
                        tagObj
                );


        EditText focused =
                currentFocusedEditText;

        if (focused == null)
            return;


        if ("del".equals(tag)) {

            String text =
                    focused.getText()
                            .toString();

            if (!text.isEmpty()) {

                focused.setText(
                        text.substring(
                                0,
                                text.length() - 1
                        )
                );
            }

        } else {

            focused.append(tag);
        }
    }


    // ================================================================
    // TOAST
    // ================================================================

    private void showCustomToast(
            String message) {

        View layout =
                LayoutInflater.from(ctx)
                        .inflate(
                                R.layout.toast_custom,
                                null
                        );

        ((ImageView)
                layout.findViewById(
                        R.id.toast_icon
                )).setImageResource(
                R.drawable.toastlogo
        );

        ((TextView)
                layout.findViewById(
                        R.id.toast_text
                )).setText(
                message
        );


        Toast t =
                new Toast(ctx);

        t.setDuration(
                Toast.LENGTH_SHORT
        );

        t.setView(layout);

        t.show();
    }


    private void showCustomToasterror(
            String message) {

        View layout =
                LayoutInflater.from(ctx)
                        .inflate(
                                R.layout.custom_toast_errror,
                                null
                        );

        ((ImageView)
                layout.findViewById(
                        R.id.toast_icon
                )).setImageResource(
                R.drawable.toastlogo
        );

        ((TextView)
                layout.findViewById(
                        R.id.toast_text
                )).setText(
                message
        );


        Toast t =
                new Toast(ctx);

        t.setDuration(
                Toast.LENGTH_SHORT
        );

        t.setView(layout);

        t.show();
    }


    private void playErrorSound() {

        try {

            MediaPlayer mediaPlayer =
                    MediaPlayer.create(
                            ctx,
                            R.raw.errorsoundtrim
                    );

            if (mediaPlayer != null) {

                mediaPlayer.setOnCompletionListener(
                        MediaPlayer::release
                );

                mediaPlayer.start();
            }

        } catch (Exception ignored) {
        }
    }


    // ================================================================
    // CREATE
    // ================================================================

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        sharedPreferences = SecurePrefs.get();

        SecureStorageUtil.init(requireContext());

        RiderMqttManager
                .getInstance(requireContext())
                .connect();
    }

    // ================================================================
    // CURRENT RIDER
    // ================================================================

    private String getCurrentRider() {

        if (getContext() == null) {

            return "";
        }


        SharedPreferences prefs =
                getContext()
                        .getSharedPreferences(
                                "app_session",
                                Context.MODE_PRIVATE
                        );


        return prefs.getString(
                "current_rider",
                ""
        );
    }


    // ================================================================
    // DUPLICATE RIDER NAME
    // ================================================================

    private boolean isActiveRiderNameExists(
            String newUsername) {

        if (ridersContainer == null) {

            return false;
        }


        int childCount =
                ridersContainer.getChildCount();


        for (int i = 0;
             i < childCount;
             i++) {

            View card =
                    ridersContainer
                            .getChildAt(i);


            TextView riderName =
                    card.findViewById(
                            R.id.riderName
                    );


            if (riderName == null) {

                continue;
            }


            String existingName =
                    riderName
                            .getText()
                            .toString()
                            .trim();


            if (existingName.equals(
                    newUsername
            )) {

                if (isEditMode
                        && existingName.equals(
                        editingUsername
                )) {

                    continue;
                }

                return true;
            }
        }


        return false;
    }


    // ================================================================
    // NEXT BUTTON
    // ================================================================

    private void updateNextButtonState() {

        String name =
                etUsername.getText()
                        .toString()
                        .trim();

        String phone =
                etPhone.getText()
                        .toString()
                        .trim();

        String age =
                etAge.getText()
                        .toString()
                        .trim();


        boolean valid = true;


        if (name.isEmpty()) {

            valid = false;
        }


        if (!phone.matches(
                "\\d{10}"
        )) {

            valid = false;
        }


        if (age.isEmpty()) {

            valid = false;

        } else {

            try {

                int a =
                        Integer.parseInt(
                                age
                        );

                if (a < 18 || a > 110) {

                    valid = false;
                }

            } catch (Exception e) {

                valid = false;
            }
        }


        btnNext.setEnabled(
                valid
        );

        btnNext.setAlpha(
                valid
                        ? 1f
                        : 0.4f
        );
    }


    // ================================================================
    // PIN DUPLICATE
    // ================================================================

    private boolean isPinAlreadyUsed(
            String enteredPin) {

        Map<String, ?> all =
                sharedPreferences
                        .getAll();


        for (Map.Entry<String, ?> entry :
                all.entrySet()) {

            if (!entry.getKey()
                    .endsWith("_pin")) {

                continue;
            }


            String username =
                    entry.getKey()
                            .replace(
                                    "user_",
                                    ""
                            )
                            .replace(
                                    "_pin",
                                    ""
                            );


            if (isEditMode
                    && username.equals(
                    editingUsername
            )) {

                continue;
            }


            String salt =
                    sharedPreferences.getString(
                            "user_" +
                                    username +
                                    "_salt",
                            ""
                    );


            if (salt.isEmpty()) {

                continue;
            }


            String enteredHash =
                    hashWithSalt(
                            enteredPin,
                            salt
                    );


            if (enteredHash != null
                    && enteredHash.equals(
                    entry.getValue()
            )) {

                return true;
            }
        }


        return false;
    }


    // ================================================================
    // PATTERN DUPLICATE
    // ================================================================

    private boolean isPatternAlreadyUsed(
            String pattern) {

        Map<String, ?> all =
                sharedPreferences
                        .getAll();


        for (Map.Entry<String, ?> entry :
                all.entrySet()) {

            if (!entry.getKey()
                    .endsWith("_pattern")) {

                continue;
            }


            String username =
                    entry.getKey()
                            .replace(
                                    "user_",
                                    ""
                            )
                            .replace(
                                    "_pattern",
                                    ""
                            );


            if (isEditMode
                    && username.equals(
                    editingUsername
            )) {

                continue;
            }


            String savedPattern =
                    String.valueOf(
                            entry.getValue()
                    );


            if (pattern.equals(
                    savedPattern
            )) {

                return true;
            }
        }


        return false;
    }


    private class AppCompatRadioGroup {
    }
}