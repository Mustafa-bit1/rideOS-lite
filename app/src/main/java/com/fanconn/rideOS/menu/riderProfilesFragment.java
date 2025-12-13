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
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.fanconn.rideOS.InactivityManager;
import com.fanconn.rideOS.R;
import com.fanconn.rideOS.SoundPlayer;
import com.fanconn.rideOS.authen.SecureStorageUtil;
import com.fanconn.rideOS.authen.PatternViewRegister;

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

    // Containers & UI
    private View initialContainer;
    private View settingsParent;

    private LinearLayout ridersContainer;
    private ImageButton btnAddRider;      // initial screen add button

    private LinearLayout basicContainer, advancedContainer, securityContainer;

    // BASIC
    private EditText etUsername, etAge;

    // ADVANCED
    private SeekBar seekBarMaxSpeed, seekBarTorque;
    private TextView tvMaxspeedValue, tvTorqueLevel;
    private String selectedTorqueLevel = "Medium";
    private int maxSpeedValue = 60;
    // SECURITY
    private RadioButton securitybtn;
    private EditText etPin, etPinConfirm;
    private TextView tvEnterPin, tvConfirmPin, TVdrawpat, TVconfirmpat;
    private PatternViewRegister patternViewRegister, patternViewConfirm;
//    private View customKeypad;
    private ImageButton btnRegister; // this is the save button in your layout

    // Pattern state
    private final List<Integer> firstPattern = new ArrayList<>();
    private final List<Integer> confirmPattern = new ArrayList<>();
    private final List<Integer> selectedPattern = new ArrayList<>();
    private boolean isPatternConfirmed = false;

    // Focused keypad target
    private EditText currentFocusedEditText;

    private Context ctx;
    private int currentStep = 0;
    private ImageButton btnBack, btnNext;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_rider_profiles, container, false);
        ctx = requireContext();

        // ----- bind containers -----
        initialContainer = view.findViewById(R.id.initialContainer);
        settingsParent = view.findViewById(R.id.settingsParent);

        ridersContainer = view.findViewById(R.id.ridersContainer);
        btnAddRider = view.findViewById(R.id.btnRegister);

        basicContainer = view.findViewById(R.id.basicContainer);
        advancedContainer = view.findViewById(R.id.advancedContainer);
        securityContainer = view.findViewById(R.id.securityContainer);

        btnBack = view.findViewById(R.id.btnBack);
        btnNext = view.findViewById(R.id.btnNext);

        // ----- top toggle tabs -----
        RadioButton basicBtn = view.findViewById(R.id.basicbtn);
        RadioButton advancedBtn = view.findViewById(R.id.advancedbtn);
        RadioButton securityBtn = view.findViewById(R.id.securitybtn);

        // DISABLE MANUAL TOGGLE SWITCHING
        disableToggle(basicBtn);
        disableToggle(advancedBtn);
        disableToggle(securityBtn);

        // initial screen visible
        initialContainer.setVisibility(View.VISIBLE);
        settingsParent.setVisibility(View.GONE);

        btnBack.setVisibility(View.GONE);
        btnNext.setVisibility(View.GONE);

        // ----- BIND FIELDS -----
        etUsername = view.findViewById(R.id.etUsername);
        etAge = view.findViewById(R.id.etAge);

        seekBarMaxSpeed = view.findViewById(R.id.seekBarMaxSpeed);
        seekBarTorque = view.findViewById(R.id.seekBarTorque);
        tvMaxspeedValue = view.findViewById(R.id.tvMaxspeedValue);
        tvTorqueLevel = view.findViewById(R.id.tvTorqueLevel);

        securitybtn = view.findViewById(R.id.securitybtn);
        etPin = view.findViewById(R.id.etPin);
        etPinConfirm = view.findViewById(R.id.etPinConfirm);
        tvEnterPin = view.findViewById(R.id.tvEnterPin);
        tvConfirmPin = view.findViewById(R.id.tvConfirmPin);

        TVdrawpat = view.findViewById(R.id.TVdrawpat);
        TVconfirmpat = view.findViewById(R.id.TVconfirmpat);
        patternViewRegister = view.findViewById(R.id.patternViewRegister);
        patternViewConfirm = view.findViewById(R.id.patternViewConfirm);

        btnRegister = view.findViewById(R.id.btnsave);
        btnRegister.setVisibility(View.GONE);

        basicContainer.setVisibility(View.GONE);
        advancedContainer.setVisibility(View.GONE);
        securityContainer.setVisibility(View.GONE);

        // --- SECURE STORAGE ---
        try {
            sharedPreferences = EncryptedSharedPreferences.create(
                    "user_data",
                    MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
                    ctx,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            SecureStorageUtil.init(ctx);
            loadUsers();
        } catch (Exception e) {
            e.printStackTrace();
            showCustomToasterror("Error initializing secure storage");
        }

        // ----- SEEKBARS -----
        seekBarMaxSpeed.setMax(80);
        seekBarMaxSpeed.setProgress(40);
        maxSpeedValue = 20 + seekBarMaxSpeed.getProgress();
        tvMaxspeedValue.setText(maxSpeedValue + " Kmph");

        seekBarMaxSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                maxSpeedValue = 20 + progress;
                tvMaxspeedValue.setText(maxSpeedValue + " Kmph");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekBarTorque.setMax(3);
        seekBarTorque.setProgress(1);
        tvTorqueLevel.setText(selectedTorqueLevel);

        seekBarTorque.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                switch (progress) {
                    case 0: selectedTorqueLevel = "Easy"; break;
                    case 1: selectedTorqueLevel = "Medium"; break;
                    case 2: selectedTorqueLevel = "Rush"; break;
                    default: selectedTorqueLevel = "Insane"; break;
                }
                tvTorqueLevel.setText(selectedTorqueLevel);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // ----- ADD RIDER BUTTON -----
        btnAddRider.setOnClickListener(v -> {
            if (getRegisteredRiderCount() >= 4) {
                showCustomToasterror("Maximum 4 Riders");
                return;
            }

            initialContainer.setVisibility(View.GONE);
            settingsParent.setVisibility(View.VISIBLE);

            currentStep = 0;
            showBasicTab();
            basicBtn.setChecked(true);

            btnBack.setVisibility(View.VISIBLE);
            btnNext.setVisibility(View.VISIBLE);

            clearEntryFields();
            InactivityManager.getInstance().stopTimer();
        });

        // ----- SAVE BUTTON -----
        btnRegister.setOnClickListener(v -> {
            hideKeyboard();

            // Save must only work in Step 2 (Security)
            if (currentStep != 2) {
                showCustomToasterror("Complete all steps before saving.");
                return;
            }

            handleRegisterSave();
        });

        // Security button -> show PIN UI
        securitybtn.setOnClickListener(v -> security());

        setupFocusListeners();
        setPatternListeners();

        // PIN watcher
        etPin.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable editable) {
                if (editable.length() == 4) etPinConfirm.requestFocus();
            }
        });

        // PIN CONFIRM watcher
        etPinConfirm.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override public void afterTextChanged(Editable editable) {
                if (editable.length() == 4) {

                    String pin = etPin.getText().toString().trim();
                    String pinConfirm = etPinConfirm.getText().toString().trim();

                    if (!pin.equals(pinConfirm)) {
                        etPin.setText("");
                        etPinConfirm.setText("");
                        etPin.requestFocus();
                        playErrorSound();
                        showCustomToasterror("PINs do not match.");
                        return;
                    }

                    // PIN SUCCESS
                    hideKeyboard();
                    pinMatched = true;

                    // show pattern setup
                    patternViewRegister.setVisibility(View.VISIBLE);
                    TVdrawpat.setVisibility(View.VISIBLE);

                    patternViewConfirm.setVisibility(View.GONE);
                    TVconfirmpat.setVisibility(View.GONE);

                    // hide PIN inputs
                    etPin.setVisibility(View.GONE);
                    etPinConfirm.setVisibility(View.GONE);
                    tvEnterPin.setVisibility(View.GONE);
                    tvConfirmPin.setVisibility(View.GONE);

                    resetPatterns();
                }
            }
        });


        // NEXT BUTTON
        btnNext.setOnClickListener(v -> {
            hideKeyboard();

            if (currentStep == 0) {

                String name = etUsername.getText().toString().trim();
                String ageStr = etAge.getText().toString().trim();

                // ---- Validation ----
                if (name.isEmpty()) {
                    showCustomToast("Please enter your name");
                    return;
                }

                if (ageStr.isEmpty()) {
                    showCustomToast("Please enter your age");
                    return;
                }

                int age = Integer.parseInt(ageStr);
                if (age < 18 || age > 110) {
                    showCustomToast("Invalid age");
                    return;
                }

                // If all good -> go to next step
                currentStep = 1;
                showAdvancedTab();
                advancedBtn.setChecked(true);
                return;
            }

            // Step 1 -> Step 2
            if (currentStep == 1) {
                currentStep = 2;
                showSecurityTab();
                securityBtn.setChecked(true);
                return;
            }

        });

        // BACK BUTTON
        btnBack.setOnClickListener(v -> {
            hideKeyboard();

            if (currentStep == 0) {
                // Exit registration -> back to initial container
                settingsParent.setVisibility(View.GONE);
                initialContainer.setVisibility(View.VISIBLE);
                softResetFields();
                btnBack.setVisibility(View.GONE);
                btnNext.setVisibility(View.GONE);
            }

            else if (currentStep == 1) {
                // Go back to BASIC tab
                currentStep = 0;
                basicBtn.setChecked(true);
                showBasicTab();

                btnNext.setVisibility(View.VISIBLE);
            }

            else if (currentStep == 2) {
                // Go back to SPEED/TORQUE tab
                currentStep = 1;
                advancedBtn.setChecked(true);
                showAdvancedTab();

                btnNext.setVisibility(View.VISIBLE);

                // IMPORTANT FIX → MUST NOT RESET PIN OR PATTERN WHEN GOING BACK
            }

        });



        return view;
    }

    private void disableToggle(RadioButton btn) {
        btn.setClickable(false);
        btn.setFocusable(false);
        btn.setLongClickable(false);

        btn.setOnTouchListener((v, event) -> true); // block touch completely
    }

    // --------------------------
    // UI Helpers
    // --------------------------
    private void showBasicTab() {
        basicContainer.setVisibility(View.VISIBLE);
        advancedContainer.setVisibility(View.GONE);
        securityContainer.setVisibility(View.GONE);

        btnRegister.setVisibility(View.GONE);
        btnNext.setVisibility(View.VISIBLE);
    }
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager)
                requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

        if (imm != null && requireActivity().getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(requireActivity().getCurrentFocus().getWindowToken(), 0);
        }
    }


    private void showAdvancedTab() {
        basicContainer.setVisibility(View.GONE);
        advancedContainer.setVisibility(View.VISIBLE);
        securityContainer.setVisibility(View.GONE);

        btnRegister.setVisibility(View.GONE);   // hide register
        btnNext.setVisibility(View.VISIBLE);    // show next
    }


    private void showSecurityTab() {
        basicContainer.setVisibility(View.GONE);
        advancedContainer.setVisibility(View.GONE);
        securityContainer.setVisibility(View.VISIBLE);

        btnNext.setVisibility(View.GONE);
        btnRegister.setVisibility(View.VISIBLE);
    }


    private void clearEntryFields() {
        etUsername.setText("");
        etAge.setText("");
        seekBarMaxSpeed.setProgress(40);
        seekBarTorque.setProgress(1);
        etPin.setText("");
        etPinConfirm.setText("");
        resetPatterns();
        patternViewRegister.setVisibility(View.GONE);
        patternViewConfirm.setVisibility(View.GONE);
        TVdrawpat.setVisibility(View.GONE);
        TVconfirmpat.setVisibility(View.GONE);


        pinMatched = false;
        isPatternConfirmed = false;
        selectedPattern.clear();
        firstPattern.clear();
        confirmPattern.clear();

//        customKeypad.setVisibility(View.GONE);
    }

    // --------------------------
    // Load / manage rider cards
    // --------------------------
    private void loadUsers() {
        if (ridersContainer == null) return;
        ridersContainer.removeAllViews();

        Set<String> list = new HashSet<>();
        for (Map.Entry<String, ?> e : sharedPreferences.getAll().entrySet()) {
            if (e.getKey().startsWith("user_") && e.getKey().contains("_age")) {
                list.add(e.getKey().replace("user_", "").replace("_age", ""));
            }
        }
        for (String name : list) addRiderCard(name);
    }

    private void addRiderCard(String username) {
        View card = LayoutInflater.from(ctx).inflate(R.layout.rider_card, ridersContainer, false);

        TextView nameText = card.findViewById(R.id.riderName);
        ImageButton deleteBtn = card.findViewById(R.id.deleteBtn);
        nameText.setText(username);

        deleteBtn.setOnClickListener(v -> {
            if (getRegisteredRiderCount() <= 1) {
                showCustomToasterror("At least one rider must remain");
                return;
            }

            View dialogView = LayoutInflater.from(ctx).inflate(R.layout.popupdialog, null);
            AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
            builder.setView(dialogView);
            AlertDialog dialog = builder.create();
            if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

            ((TextView) dialogView.findViewById(R.id.dialogTitle)).setText("Confirm Deletion");
            ((TextView) dialogView.findViewById(R.id.dialogMessage)).setText("Delete rider \"" + username + "\" ?");

            Button yes = dialogView.findViewById(R.id.btnYes);
            Button no = dialogView.findViewById(R.id.btnNo);

            yes.setText("Delete");
            no.setText("Cancel");

            yes.setOnClickListener(b -> {
                dialog.dismiss();
                deleteUser(username);
            });
            no.setOnClickListener(b -> dialog.dismiss());
            dialog.show();
        });

        ridersContainer.addView(card);
    }

    private int getRegisteredRiderCount() {
        Set<String> usernames = new HashSet<>();
        for (Map.Entry<String, ?> entry : sharedPreferences.getAll().entrySet()) {
            if (entry.getKey().startsWith("user_") && entry.getKey().endsWith("_age")) {
                usernames.add(entry.getKey().replace("user_", "").replace("_age", ""));
            }
        }
        return usernames.size();
    }

    private void deleteUser(String username) {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            String[] keys = {
                    "user_" + username + "_age",
                    "user_" + username + "_lucky",
                    "user_" + username + "_maxspeed",
                    "user_" + username + "_torque",
                    "user_" + username + "_pin",
                    "user_" + username + "_salt",
                    "user_" + username + "_pattern"
            };
            for (String key : keys) editor.remove(key);
            editor.apply();
            SoundPlayer.playOnce(ctx, R.raw.deletesound);
            loadUsers();
            showCustomToast("Rider Deleted " + username);
        } catch (Exception e) {
            e.printStackTrace();
            showCustomToasterror("Error deleting " + username);
        }
    }
    private boolean pinMatched = false;

    // --------------------------
    // Save/Register logic (from RegisterActivity)
    // --------------------------
    private void handleRegisterSave() {

        String username = etUsername.getText().toString().trim();
        String age = etAge.getText().toString().trim();
        String maxspeed = String.valueOf(maxSpeedValue);
        String torque = selectedTorqueLevel;

        String pin = etPin.getText().toString().trim();
        String pinConfirm = etPinConfirm.getText().toString().trim();

        btnNext.setVisibility(View.GONE);
        currentStep = 0;

        // -------- BASIC VALIDATION --------
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(age)) {
            playErrorSound();
            showCustomToasterror("Please enter name and age");
            return;
        }

        if (TextUtils.isEmpty(maxspeed) || TextUtils.isEmpty(torque)) {
            playErrorSound();
            showCustomToasterror("Please set speed and torque");
            return;
        }

        // -------- PIN VALIDATION ----------
        if (TextUtils.isEmpty(pin) || TextUtils.isEmpty(pinConfirm)) {
            playErrorSound();
            showCustomToasterror("Please enter PIN");
            return;
        }

        if (!pin.equals(pinConfirm)) {
            playErrorSound();
            etPin.setText("");
            etPinConfirm.setText("");
            showCustomToasterror("PIN does not match");
            return;
        }

        // -------- PATTERN VALIDATION -------
        if (!isPatternConfirmed || selectedPattern.size() < 4) {
            playErrorSound();
            showCustomToasterror("Please draw and confirm the pattern");
            return;
        }

        // -------- SAVE USER ---------------
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();

            editor.putString("user_" + username + "_age", age);
            editor.putString("user_" + username + "_maxspeed", maxspeed);
            editor.putString("user_" + username + "_torque", torque);

            // Save PIN
            String salt = generateSalt();
            String hashedPin = hashWithSalt(pin, salt);

            editor.putString("user_" + username + "_salt", salt);
            editor.putString("user_" + username + "_pin", hashedPin);

            // Save Pattern
            editor.putString("user_" + username + "_pattern",
                    TextUtils.join("", selectedPattern));

            editor.apply();

            SoundPlayer.playOnce(ctx, R.raw.locksound2);
            showCustomToast("Registered Successfully");

            // Go back to initial list
            btnRegister.setVisibility(View.GONE);
            btnBack.setVisibility(View.GONE);
            settingsParent.setVisibility(View.GONE);
            initialContainer.setVisibility(View.VISIBLE);
            softResetFields();
            loadUsers();

        } catch (Exception e) {
            e.printStackTrace();
            playErrorSound();
            showCustomToasterror("Error saving user");
        }
    }
    private void softResetFields() {

        // Basic
        etUsername.setText("");
        etAge.setText("");

        // Advanced
        seekBarMaxSpeed.setProgress(40);
        seekBarTorque.setProgress(1);
        tvMaxspeedValue.setText((20 + 40) + " Kmph");
        tvTorqueLevel.setText("Medium");

        // Security (reset inputs)
        etPin.setText("");
        etPinConfirm.setText("");

        // Reset pattern data
        pinMatched = false;
        isPatternConfirmed = false;
        selectedPattern.clear();
        firstPattern.clear();
        confirmPattern.clear();

        TVdrawpat.setText("Draw Pattern");
        TVdrawpat.setTextColor(Color.parseColor("#9D9D9D"));

        TVconfirmpat.setText("Confirm Pattern");
        TVconfirmpat.setTextColor(Color.parseColor("#9D9D9D"));

        // Pattern visibility restored
        patternViewRegister.setVisibility(View.GONE);
        patternViewConfirm.setVisibility(View.GONE);
        TVdrawpat.setVisibility(View.GONE);
        TVconfirmpat.setVisibility(View.GONE);

        // Re-enable PIN UI
        etPin.setVisibility(View.VISIBLE);
        etPinConfirm.setVisibility(View.VISIBLE);
        tvEnterPin.setVisibility(View.VISIBLE);
        tvConfirmPin.setVisibility(View.VISIBLE);
    }

    // --------------------------
    // Security helpers (pattern + PIN)
    // --------------------------
    private void security() {
        String ageText = etAge.getText().toString().trim();
        String newUsername = etUsername.getText().toString().trim();

        if (ageText.isEmpty() || newUsername.isEmpty()) {
            showCustomToasterror("Please enter all details");
            playErrorSound();
            return;
        }

        try {
            int ageValue = Integer.parseInt(ageText);
            if (ageValue < 18) {
                showCustomToasterror("Invalid rider age, must be 18 or above");
                playErrorSound();
                return;
            }
        } catch (NumberFormatException e) {
            showCustomToasterror("Enter a valid age");
            playErrorSound();
            return;
        }

        boolean exists = false;
        for (String key : sharedPreferences.getAll().keySet()) {
            if (key.startsWith("user_") && key.endsWith("_age")) {
                String existingName = key.substring(5, key.length() - 4);
                if (existingName.equalsIgnoreCase(newUsername)) {
                    exists = true;
                    break;
                }
            }
        }

        if (exists) {
            showCustomToasterror("Rider name already exists");
            playErrorSound();
            return;
        }

        // show PIN inputs to allow user to set PIN
        patternViewRegister.setVisibility(View.GONE);
        patternViewConfirm.setVisibility(View.GONE);
        etPin.setVisibility(View.VISIBLE);
        etPinConfirm.setVisibility(View.VISIBLE);
        tvEnterPin.setVisibility(View.VISIBLE);
        tvConfirmPin.setVisibility(View.VISIBLE);
        TVconfirmpat.setVisibility(View.GONE);
        TVdrawpat.setVisibility(View.GONE);
//        customKeypad.setVisibility(View.VISIBLE);
        resetPatterns();
        etPin.requestFocus();
    }
    private String generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.encodeToString(salt, Base64.NO_WRAP);
    }

    private String hashWithSalt(String input, String saltBase64) {
        try {
            byte[] saltBytes = Base64.decode(saltBase64, Base64.NO_WRAP);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(saltBytes);

            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void setPatternListeners() {
        if (patternViewRegister != null) {
            patternViewRegister.setOnPatternCompleteListener(pattern -> {
                firstPattern.clear();
                firstPattern.addAll(pattern);

                patternViewRegister.clearPattern();

                // Switch to confirm pattern UI
                patternViewRegister.setVisibility(View.GONE);
                TVdrawpat.setVisibility(View.GONE);

                patternViewConfirm.setVisibility(View.VISIBLE);
                TVconfirmpat.setVisibility(View.VISIBLE);
            });

        }

        if (patternViewConfirm != null) {
            patternViewConfirm.setOnPatternCompleteListener(pattern -> {

                confirmPattern.clear();
                confirmPattern.addAll(pattern);

                if (!confirmPattern.equals(firstPattern)) {
                    patternViewConfirm.clearPattern();
                    playErrorSound();
                    showCustomToasterror("Patterns do not match");
                    return;
                }

                // PATTERN CONFIRMED ✔
                isPatternConfirmed = true;
                selectedPattern.clear();
                selectedPattern.addAll(firstPattern);

                // Hide both pattern views
                patternViewConfirm.setVisibility(View.GONE);
                TVconfirmpat.setVisibility(View.GONE);

                // Show Verified tick text
                TVdrawpat.setVisibility(View.VISIBLE);
                TVdrawpat.setText("Verified Successfully, Please save your profile");
                TVdrawpat.setTextColor(getResources().getColor(android.R.color.holo_blue_bright));

                // Show save button
                btnRegister.setVisibility(View.VISIBLE);
            });

        }
    }

    private void resetPatterns() {
        firstPattern.clear();
        confirmPattern.clear();
        selectedPattern.clear();
        isPatternConfirmed = false;
        if (patternViewRegister != null) patternViewRegister.clearPattern();
        if (patternViewConfirm != null) patternViewConfirm.clearPattern();
    }

    // --------------------------
    // Keypad helpers + focus
    // --------------------------
    private void setupFocusListeners() {
        OnFocusChangeListener focusListener = (v, hasFocus) -> {
            if (hasFocus && v instanceof EditText) {
                currentFocusedEditText = (EditText) v;
            }
        };
        etPin.setOnFocusChangeListener(focusListener);
        etPinConfirm.setOnFocusChangeListener(focusListener);
        etAge.setOnFocusChangeListener(focusListener);
        etUsername.setOnFocusChangeListener(focusListener);
    }

    private void disableSoftKeyboard(EditText... editTexts) {
        for (EditText et : editTexts) {
            try { et.setShowSoftInputOnFocus(false); } catch (Exception ignored) {}
        }
    }

    // Called by keypad buttons in included layout (set android:onClick="onKeypadButtonClick")
    public void onKeypadButtonClick(View view) {
        if (!(view instanceof Button) && !(view instanceof ImageButton)) return;
        Object tagObj = view.getTag();
        if (tagObj == null) return;
        String tag = String.valueOf(tagObj);
        EditText focused = currentFocusedEditText;
        if (focused == null) return;

        if ("del".equals(tag)) {
            String text = focused.getText().toString();
            if (!text.isEmpty()) focused.setText(text.substring(0, text.length() - 1));
        } else {
            focused.append(tag);
        }
    }

    // --------------------------
    // Utils: toast, sound, hashing
    // --------------------------
    private void showCustomToast(String message) {
        View layout = LayoutInflater.from(ctx).inflate(R.layout.toast_custom, null);
        ((ImageView) layout.findViewById(R.id.toast_icon)).setImageResource(R.drawable.toastlogo);
        ((TextView) layout.findViewById(R.id.toast_text)).setText(message);
        Toast t = new Toast(ctx);
        t.setDuration(Toast.LENGTH_SHORT);
        t.setView(layout);
        t.show();
    }

    private void showCustomToasterror(String message) {
        View layout = LayoutInflater.from(ctx).inflate(R.layout.custom_toast_errror, null);
        ((ImageView) layout.findViewById(R.id.toast_icon)).setImageResource(R.drawable.toastlogo);
        ((TextView) layout.findViewById(R.id.toast_text)).setText(message);
        Toast t = new Toast(ctx);
        t.setDuration(Toast.LENGTH_SHORT);
        t.setView(layout);
        t.show();
    }

    private void playErrorSound() {
        try {
            MediaPlayer mediaPlayer = MediaPlayer.create(ctx, R.raw.errorsoundtrim);
            if (mediaPlayer != null) {
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                mediaPlayer.start();
            }
        } catch (Exception ignored) {}
    }



    private class AppCompatRadioGroup {

    }
}
