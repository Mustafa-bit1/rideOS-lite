package com.fanconn.rideOS.authen;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.fanconn.rideOS.R;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class RegisterActivity extends AppCompatActivity {

    EditText etUsername, etAge, etPin, etPinConfirm;
    TextView tvMaxspeedValue;
    SeekBar seekBarMaxSpeed;
    int maxSpeedValue = 60;  // Default to 60
    SeekBar seekBarTorque;
    TextView tvTorqueLevel;
    String selectedTorqueLevel = "Level 1"; // Default

    TextView tvEnterPin, tvConfirmPin, TVconfirmpat, TVdrawpat;
    RadioGroup radioGroup;
    PatternViewRegister patternView, patternViewConfirm;
    Button btnRegister, btnLogin;
    ImageButton securitybtn;

    List<Integer> firstPattern = new ArrayList<>();
    List<Integer> confirmPattern = new ArrayList<>();
    List<Integer> selectedPattern = new ArrayList<>();

    boolean isPatternConfirmed = false;
    SharedPreferences sharedPreferences;

    private EditText currentFocusedEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        initializeViews();
        initializeEncryptedSharedPreferences();
        setPatternListeners();
        setButtonListeners();
        security();

        disableSoftKeyboard(etPin, etPinConfirm, etAge);
        setupFocusListeners();
        setFocusChangeListeners();

        etPin.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable editable) {
                if (editable.length() == 4) {
                    etPinConfirm.requestFocus();
                }
            }
        });

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
                        showCustomToasterror("PINs do not match. Please try again.");
                    } else {
                        patternView.setVisibility(View.VISIBLE);
                        TVdrawpat.setVisibility(View.VISIBLE);
                        patternViewConfirm.setVisibility(View.GONE);
                        etPin.setVisibility(View.GONE);
                        etPinConfirm.setVisibility(View.GONE);
                        tvEnterPin.setVisibility(View.GONE);
                        tvConfirmPin.setVisibility(View.GONE);
                        TVconfirmpat.setVisibility(View.GONE);
                        findViewById(R.id.customKeypad).setVisibility(View.GONE);
                        resetPatterns();
                    }
                }
            }
        });
    }

    private void initializeViews() {
        etUsername = findViewById(R.id.etUsername);
        etAge = findViewById(R.id.etAge);
        tvMaxspeedValue = findViewById(R.id.tvMaxspeedValue);
        seekBarMaxSpeed = findViewById(R.id.seekBarMaxSpeed);
        seekBarMaxSpeed.setMax(80); // 20 to 100
        seekBarMaxSpeed.setProgress(40); // default 60
        maxSpeedValue = 20 + seekBarMaxSpeed.getProgress();
        tvMaxspeedValue.setText("Max Speed: " + maxSpeedValue);

        etPin = findViewById(R.id.etPin);
        etPinConfirm = findViewById(R.id.etPinConfirm);
        btnRegister = findViewById(R.id.btnRegister);
        btnLogin = findViewById(R.id.btnLogin);
        securitybtn = findViewById(R.id.securitybtn);
        radioGroup = findViewById(R.id.radioGroup);
        patternView = findViewById(R.id.patternViewRegister);
        patternViewConfirm = findViewById(R.id.patternViewConfirm);
        tvEnterPin = findViewById(R.id.tvEnterPin);
        tvConfirmPin = findViewById(R.id.tvConfirmPin);
        TVconfirmpat = findViewById(R.id.TVconfirmpat);
        TVdrawpat = findViewById(R.id.TVdrawpat);

        patternView.setVisibility(View.GONE);
        patternViewConfirm.setVisibility(View.GONE);
        etPin.setVisibility(View.GONE);
        etPinConfirm.setVisibility(View.GONE);
        tvEnterPin.setVisibility(View.GONE);
        tvConfirmPin.setVisibility(View.GONE);
        TVconfirmpat.setVisibility(View.GONE);
        TVdrawpat.setVisibility(View.GONE);
        findViewById(R.id.customKeypad).setVisibility(View.GONE);

        seekBarTorque = findViewById(R.id.seekBarTorque);
        tvTorqueLevel = findViewById(R.id.tvTorqueLevel);

        seekBarTorque.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                switch (progress) {
                    case 0: selectedTorqueLevel = "Easy"; break;
                    case 1: selectedTorqueLevel = "Medium"; break;
                    case 2: selectedTorqueLevel = "Rush"; break;
                    case 3: selectedTorqueLevel = "Insane"; break;
                }
                tvTorqueLevel.setText(selectedTorqueLevel);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

    }

    private void disableSoftKeyboard(EditText... editTexts) {
        for (EditText et : editTexts) {
            et.setShowSoftInputOnFocus(false);
        }
    }

    private void initializeEncryptedSharedPreferences() {
        try {
            sharedPreferences = EncryptedSharedPreferences.create(
                    "user_data",
                    MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
                    getApplicationContext(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            e.printStackTrace();
            playErrorSound();
            showCustomToasterror("Error initializing secure storage");
        }
    }

    public void security() {
        securitybtn.setOnClickListener(v -> {
            patternView.setVisibility(View.GONE);
            patternViewConfirm.setVisibility(View.GONE);
            etPin.setVisibility(View.VISIBLE);
            etPinConfirm.setVisibility(View.VISIBLE);
            tvEnterPin.setVisibility(View.VISIBLE);
            tvConfirmPin.setVisibility(View.VISIBLE);
            TVconfirmpat.setVisibility(View.GONE);
            TVdrawpat.setVisibility(View.GONE);
            findViewById(R.id.customKeypad).setVisibility(View.VISIBLE);
            resetPatterns();
            etPin.requestFocus();
        });
    }

    private void setPatternListeners() {
        patternView.setOnPatternCompleteListener(pattern -> {
            firstPattern.clear();
            firstPattern.addAll(pattern);
            patternView.setVisibility(View.GONE);
            TVdrawpat.setVisibility(View.GONE);
            patternViewConfirm.setVisibility(View.VISIBLE);
            TVconfirmpat.setVisibility(View.VISIBLE);
            showCustomToast("Now confirm your pattern");
        });

        patternViewConfirm.setOnPatternCompleteListener(pattern -> {
            confirmPattern.clear();
            confirmPattern.addAll(pattern);

            if (!firstPattern.equals(confirmPattern)) {
                playErrorSound();
                showCustomToasterror("Patterns do not match. Try again.");
                resetPatterns();
                patternView.postDelayed(() -> {
                    patternView.setVisibility(View.VISIBLE);
                    TVdrawpat.setVisibility(View.VISIBLE);
                    patternViewConfirm.setVisibility(View.GONE);
                    TVconfirmpat.setVisibility(View.GONE);
                    patternView.invalidate();
                    patternViewConfirm.invalidate();
                }, 100);
            } else {
                selectedPattern.clear();
                selectedPattern.addAll(confirmPattern);
                isPatternConfirmed = true;
                showCustomToast("Pattern confirmed. Click Register to proceed.");
                patternView.setVisibility(View.GONE);
                patternViewConfirm.setVisibility(View.GONE);
                TVdrawpat.setVisibility(View.GONE);
                TVconfirmpat.setVisibility(View.GONE);
            }
        });
    }

    private void setButtonListeners() {
        btnRegister.setOnClickListener(v -> handleRegister());
        btnLogin.setOnClickListener(v -> finish());
    }

    private void handleRegister() {
        String username = etUsername.getText().toString().trim();
        String age = etAge.getText().toString().trim();
        String maxspeed = String.valueOf(maxSpeedValue);
        String torque = selectedTorqueLevel;
        String pin = etPin.getText().toString().trim();
        String pinConfirm = etPinConfirm.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(age) ||
                TextUtils.isEmpty(maxspeed) || TextUtils.isEmpty(torque) ||
                TextUtils.isEmpty(pin) || TextUtils.isEmpty(pinConfirm)) {
            playErrorSound();
            showCustomToasterror("Please fill all fields");
            return;
        }

        if (!pin.equals(pinConfirm)) {
            etPin.setText(""); etPinConfirm.setText("");
            playErrorSound();
            showCustomToasterror("PINs do not match. Please try again.");
            return;
        }

        if (!isPatternConfirmed || selectedPattern.size() < 4) {
            showCustomToast("Setup your pattern before registering.");
            return;
        }

        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("user_" + username + "_age", age);
            editor.putString("user_" + username + "_lucky", maxspeed);
            editor.putString("user_" + username + "_torque", torque);

            String salt = generateSalt();
            String hashedPin = hashWithSalt(pin, salt);
            editor.putString("user_" + username + "_salt", salt);
            editor.putString("user_" + username + "_pin", hashedPin);

            String patternStr = TextUtils.join("", selectedPattern);
            editor.putString("user_" + username + "_pattern", patternStr);

            editor.apply();
            showCustomToast("Registered Successfully");
            Intent resultIntent = new Intent();
            resultIntent.putExtra("newRider", username);
            setResult(RESULT_OK, resultIntent);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
            playErrorSound();
            showCustomToasterror("Error saving user data");
        }
    }

    private void resetPatterns() {
        firstPattern.clear();
        confirmPattern.clear();
        selectedPattern.clear();
        isPatternConfirmed = false;
        if (patternView != null) patternView.clearPattern();
        if (patternViewConfirm != null) patternViewConfirm.clearPattern();
    }

    private void setupFocusListeners() {
        OnFocusChangeListener focusListener = (v, hasFocus) -> {
            if (hasFocus && v instanceof EditText) {
                currentFocusedEditText = (EditText) v;
            }
        };

        etPin.setOnFocusChangeListener(focusListener);
        etPinConfirm.setOnFocusChangeListener(focusListener);
        etAge.setOnFocusChangeListener(focusListener);
    }

    private void setFocusChangeListeners() {
        seekBarMaxSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                maxSpeedValue = 20 + progress;
                tvMaxspeedValue.setText(maxSpeedValue+" Kmph");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    public void onKeypadButtonClick(View view) {
        if (!(view instanceof Button) && !(view instanceof ImageButton)) return;
        String tag = (String) view.getTag();
        if (tag == null) return;

        EditText focused = currentFocusedEditText;
        if (focused == null) return;

        if (tag.equals("del")) {
            String text = focused.getText().toString();
            if (!text.isEmpty()) focused.setText(text.substring(0, text.length() - 1));
        } else {
            focused.append(tag);
        }
    }

    private void showCustomToast(String message) {
        View layout = LayoutInflater.from(this).inflate(R.layout.toast_custom, null);
        ((ImageView) layout.findViewById(R.id.toast_icon)).setImageResource(R.drawable.toastlogo);
        ((TextView) layout.findViewById(R.id.toast_text)).setText(message);
        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }

    private void showCustomToasterror(String message) {
        View layout = LayoutInflater.from(this).inflate(R.layout.custom_toast_errror, null);
        ((ImageView) layout.findViewById(R.id.toast_icon)).setImageResource(R.drawable.toastlogo);
        ((TextView) layout.findViewById(R.id.toast_text)).setText(message);
        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }

    private void playErrorSound() {
        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.errorsoundtrim);
        if (mediaPlayer != null) {
            mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            mediaPlayer.start();
        }
    }

    private String generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.encodeToString(salt, Base64.NO_WRAP);
    }

    private String hashWithSalt(String input, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
