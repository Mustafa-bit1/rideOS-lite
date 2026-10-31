
package com.fanconn.rideOS.authen;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fanconn.rideOS.ChargingStatusManager;
import com.fanconn.rideOS.MainActivity;
import com.fanconn.rideOS.R;
import com.fanconn.rideOS.SerialPortManager;
import com.fanconn.rideOS.scooterPowerManager;

public class LoginActivity extends AppCompatActivity {

    private static final String CORRECT_PIN = "2468";
    private static final String TAG = "LoginActivity";
    private boolean isLoginScreenActive = false;
    private static final int REQUIRED_SETTINGS_TAPS = 5;
    private static final long MAX_TAP_INTERVAL_MS = 1000;
    private static final long SCREEN_OFF_DELAY_MS = 10_000L;

    private EditText etCredential;
    private Button launchSettingsButton;

    private final StringBuilder actualPin = new StringBuilder();
    private final Handler screenOffHandler =
            new Handler(Looper.getMainLooper());

    private int settingsTapCount = 0;
    private long lastSettingsTapTime = 0;

    private boolean isValidating = false;
    private boolean isInputLocked = false;
    private boolean loginSuccessful = false;

    private long lastActivityTime = 0;

    private final Runnable screenOffRunnable = this::handleScreenOff;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        chargingStatus = findViewById(R.id.chargingStatus);
        chargingStatus.setVisibility(View.GONE);

        launchSettingsButton = findViewById(R.id.launchSettings);
        launchSettingsButton.setOnClickListener(v -> {
            long currentTime = SystemClock.elapsedRealtime();

            if (lastSettingsTapTime == 0
                    || currentTime - lastSettingsTapTime > MAX_TAP_INTERVAL_MS) {
                settingsTapCount = 0;
            }

            settingsTapCount++;
            lastSettingsTapTime = currentTime;

            Log.d(TAG, "Settings tap count=" + settingsTapCount);

            if (settingsTapCount >= REQUIRED_SETTINGS_TAPS) {
                settingsTapCount = 0;
                lastSettingsTapTime = 0;

                try {
                    startActivity(new Intent(Settings.ACTION_SETTINGS));
                } catch (Exception e) {
                    Log.e(TAG, "Unable to open Android Settings", e);
                }
            }
        });

        etCredential = findViewById(R.id.etCredential);
        etCredential.setShowSoftInputOnFocus(false);
        etCredential.setText("");

        etCredential.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Validation is handled by the keypad method.
            }
        });

        Log.d(TAG, "LoginActivity created");
    }


    private void handleScreenOff() {
        // Screen-off is allowed only while LoginActivity is active.
        if (loginSuccessful
                || !isLoginScreenActive
                || isFinishing()
                || isDestroyed()) {
            stopScreenOffTimer();
            return;
        }

        long idleTime =
                SystemClock.elapsedRealtime() - lastActivityTime;

        if (idleTime < SCREEN_OFF_DELAY_MS) {
            screenOffHandler.postDelayed(
                    screenOffRunnable,
                    SCREEN_OFF_DELAY_MS - idleTime
            );
            return;
        }

        PowerManager powerManager =
                (PowerManager) getSystemService(POWER_SERVICE);

        if (powerManager == null || !powerManager.isInteractive()) {
            return;
        }

        // Final check immediately before requesting screen-off.
        if (loginSuccessful
                || !isLoginScreenActive
                || isFinishing()
                || isDestroyed()) {
            return;
        }

        try {
            java.lang.reflect.Method method =
                    PowerManager.class.getMethod(
                            "goToSleep",
                            long.class
                    );

            method.invoke(
                    powerManager,
                    SystemClock.uptimeMillis()
            );

            Log.d(TAG, "Login idle timeout: screen-off requested");

        } catch (Exception e) {
            Log.e(TAG, "Unable to turn screen off", e);
        }
    }

    private void startScreenOffTimer() {
        stopScreenOffTimer();

        if (loginSuccessful
                || !isLoginScreenActive
                || isFinishing()
                || isDestroyed()) {
            return;
        }

        lastActivityTime = SystemClock.elapsedRealtime();

        screenOffHandler.postDelayed(
                screenOffRunnable,
                SCREEN_OFF_DELAY_MS
        );

        Log.d(TAG, "Login screen timer started: 10 seconds");
    }

    private void stopScreenOffTimer() {
        screenOffHandler.removeCallbacks(screenOffRunnable);
    }


    @Override
    public void onUserInteraction() {
        super.onUserInteraction();

        if (isLoginScreenActive && !loginSuccessful) {
            startScreenOffTimer();
        }
    }

    public void onKeypadButtonClick(View view) {
        if (isInputLocked || isValidating) {
            return;
        }

        Object tagObject = view.getTag();

        if (tagObject == null) {
            Log.e(TAG, "Keypad button has no android:tag");
            return;
        }

        String tag = tagObject.toString();

        if ("del".equals(tag)) {
            if (actualPin.length() > 0) {
                actualPin.deleteCharAt(actualPin.length() - 1);
            }
        } else if (tag.matches("[0-9]") && actualPin.length() < 4) {
            actualPin.append(tag);
        }

        StringBuilder masked = new StringBuilder();
        for (int i = 0; i < actualPin.length(); i++) {
            masked.append("•");
        }

        etCredential.setText(masked.toString());

        if (actualPin.length() == 4) {
            validatePin();
        }
    }

    private void validatePin() {
        if (isValidating) {
            return;
        }

        isValidating = true;

        if (CORRECT_PIN.equals(actualPin.toString())) {
            launchMainActivity();
        } else {
            showCustomToasterror("Invalid PIN");
            actualPin.setLength(0);
            etCredential.setText("");
        }

        isValidating = false;
    }

    private void launchMainActivity() {
        loginSuccessful = true;
        isLoginScreenActive = false;

        stopScreenOffTimer();

        Log.d(TAG, "Login successful: screen-off timer stopped");

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        startActivity(intent);
        finish();
    }

    private void showCustomToasterror(String message) {
        LayoutInflater inflater = getLayoutInflater();

        View layout = inflater.inflate(
                R.layout.custom_toast_errror,
                null
        );

        ImageView icon = layout.findViewById(R.id.toast_icon);
        TextView text = layout.findViewById(R.id.toast_text);

        icon.setImageResource(R.drawable.toastlogo);
        text.setText(message);

        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        scooterPowerManager.lock(
                command -> SerialPortManager.getInstance()
                        .VCUSendToMCU(command)
        );

        isInputLocked = false;
        isValidating = false;
        actualPin.setLength(0);

        if (etCredential != null) {
            etCredential.setText("");
        }

        if (!loginSuccessful && !isFinishing()) {
            isLoginScreenActive = true;
            startScreenOffTimer();

            chargingStatusHandler.removeCallbacks(chargingStatusRunnable);
            chargingStatusHandler.post(chargingStatusRunnable);

        } else {
            isLoginScreenActive = false;
            stopScreenOffTimer();
            chargingStatusHandler.removeCallbacks(chargingStatusRunnable);

            if (chargingStatus != null) {
                chargingStatus.setVisibility(View.GONE);
            }
        }
        Log.d(TAG, "onResume: loginSuccessful=" + loginSuccessful
                + ", isLoginScreenActive=" + isLoginScreenActive);
    }

    @Override
    protected void onPause() {
        isLoginScreenActive = false;
        stopScreenOffTimer();
        chargingStatusHandler.removeCallbacks(chargingStatusRunnable);

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        isLoginScreenActive = false;
        stopScreenOffTimer();
        chargingStatusHandler.removeCallbacks(chargingStatusRunnable);

        super.onDestroy();
    }

    private TextView chargingStatus;

    private final Handler chargingStatusHandler =
            new Handler(Looper.getMainLooper());

    private final Runnable chargingStatusRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isLoginScreenActive || loginSuccessful
                    || isFinishing() || isDestroyed()) {
                return;
            }

            updateChargingStatus();

            // Check the charging state once per second.
            chargingStatusHandler.postDelayed(this, 1000);
        }
    };


    private void updateChargingStatus() {

        ChargingStatusManager.ChargingState state =
                ChargingStatusManager.getInstance().getChargingState();

        switch (state) {

            case CHARGING:
                chargingStatus.setText("Charging");
                chargingStatus.setTextColor(Color.rgb(0, 200, 83));
                chargingStatus.setVisibility(View.VISIBLE);
                break;

            case PLUGGED_IN:
                chargingStatus.setText("Plugged In");
                chargingStatus.setTextColor(Color.rgb(255, 193, 7));
                chargingStatus.setVisibility(View.VISIBLE);
                break;

            case NOT_CHARGING:
            default:
                chargingStatus.setVisibility(View.GONE);
                break;
        }
    }
}
