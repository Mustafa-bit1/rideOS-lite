package com.fanconn.rideOS.authen;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fanconn.rideOS.MainActivity;
import com.fanconn.rideOS.R;

public class LoginActivity extends AppCompatActivity {

    private static final String CORRECT_PIN = "2222";
    private static final String TAG = "LoginActivity";

    private EditText etCredential;

    // Stores the actual PIN entered by the user.
    private final StringBuilder actualPin = new StringBuilder();

    private boolean isValidating = false;
    private boolean isInputLocked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etCredential = findViewById(R.id.etCredential);

        // The PIN is entered using the on-screen keypad.
        // Display dots instead of the actual PIN.
        etCredential.setShowSoftInputOnFocus(false);
        etCredential.setText("");

        // Prevent the EditText from triggering validation based on
        // the number of displayed masking characters.
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
                // PIN validation is handled by onKeypadButtonClick().
            }
        });
    }

    /**
     * Handles PIN keypad buttons declared using android:onClick.
     */
    public void onKeypadButtonClick(View view) {

        if (isInputLocked || isValidating) {
            return;
        }

        // Play the existing keypad sound if this method is available
        // in your project. See the note below if it does not compile.

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

        // Display only masking dots.
        StringBuilder masked = new StringBuilder();

        for (int i = 0; i < actualPin.length(); i++) {
            masked.append("•");
        }

        etCredential.setText(masked.toString());

        // Validate when exactly four digits have been entered.
        if (actualPin.length() == 4) {
            validatePin();
        }
    }

    private void validatePin() {

        if (isValidating) {
            return;
        }

        isValidating = true;

        String enteredPin = actualPin.toString();

        if (CORRECT_PIN.equals(enteredPin)) {

            launchMainActivity();

        } else {

            showCustomToasterror("Invalid PIN");

            // Clear the actual PIN and the displayed masking dots.
            actualPin.setLength(0);
            etCredential.setText("");
        }

        isValidating = false;
    }
    private void launchMainActivity() {
        loginSuccessful = true;
        stopScreenOffTimer();

        Intent intent = new Intent(
                LoginActivity.this,
                MainActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
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

    /**
     * Prevent the previous PIN from remaining when LoginActivity
     * becomes visible again.
     */
    @Override
    protected void onResume() {
        super.onResume();

        isInputLocked = false;
        isValidating = false;
        actualPin.setLength(0);

        if (etCredential != null) {
            etCredential.setText("");
        }

        if (!loginSuccessful) {
            startScreenOffTimer();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopScreenOffTimer();
    }

    @Override
    protected void onDestroy() {
        stopScreenOffTimer();
        super.onDestroy();
    }



    private static final long SCREEN_OFF_DELAY_MS = 10_000L;

    private final Handler screenOffHandler =
            new Handler(Looper.getMainLooper());

    private boolean loginSuccessful = false;

    private final Runnable screenOffRunnable = () -> {
        if (isFinishing() || isDestroyed() || loginSuccessful) {
            return;
        }

        PowerManager powerManager =
                (PowerManager) getSystemService(POWER_SERVICE);

        if (powerManager == null || !powerManager.isInteractive()) {
            return;
        }

        try {
            java.lang.reflect.Method method =
                    PowerManager.class.getMethod(
                            "goToSleep", long.class
                    );

            method.invoke(
                    powerManager,
                    SystemClock.uptimeMillis()
            );

            Log.d(TAG, "Screen off requested after 10 seconds");

        } catch (Exception e) {
            Log.e(TAG, "Unable to turn screen off", e);
        }
    };

    private void startScreenOffTimer() {
        screenOffHandler.removeCallbacks(screenOffRunnable);
        screenOffHandler.postDelayed(
                screenOffRunnable,
                SCREEN_OFF_DELAY_MS
        );
    }

    private void stopScreenOffTimer() {
        screenOffHandler.removeCallbacks(screenOffRunnable);
    }
}
