package com.fanconn.rideOS;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AppCompatActivity;




public class MetadataActivity extends AppCompatActivity {

    private EditText etVin, etcharger, etOwner, etPhone, etController, etMotor, etBms, etSmartKey;
    private Button btnSave;
    private ImageButton menuBackBtn;
    private EditText etModel,etfronttpms,etreartpms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_metadata);

        etVin = findViewById(R.id.etVin);
        etcharger = findViewById(R.id.etCharger);
        etOwner = findViewById(R.id.etOwner);
        etPhone = findViewById(R.id.etPhone);
        etController = findViewById(R.id.etController);
        etMotor = findViewById(R.id.etMotor);
        etBms = findViewById(R.id.etBms);
        etSmartKey = findViewById(R.id.etSmartKey);
        btnSave = findViewById(R.id.btnSave);
        menuBackBtn = findViewById(R.id.menubackbtn);
        etModel = findViewById(R.id.etModel);
        etfronttpms = findViewById(R.id.etFrontMacAddress);
        etreartpms = findViewById(R.id.etRearMacAddress);

        loadExistingData();

        btnSave.setOnClickListener(v -> {

            View dialogView = LayoutInflater.from(this).inflate(R.layout.popupdialog, null);
            AlertDialog dialog = createDialog(dialogView, "Confirm Restart", "Are you sure you want to Restart the device?", "Restart", () -> {
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                if (pm != null) pm.reboot(null);
            });
            dialog.show();
        });

        menuBackBtn.setOnClickListener(v -> {
            if (isKeyboardVisible()) {
                hideKeyboard();
            } else {
                finish();
            }
        });
    }
    private AlertDialog createDialog(View dialogView, String titleText, String messageText, String positiveText, Runnable positiveAction) {
        TextView title = dialogView.findViewById(R.id.dialogTitle);
        TextView message = dialogView.findViewById(R.id.dialogMessage);
        Button btnYes = dialogView.findViewById(R.id.btnYes);
        Button btnNo = dialogView.findViewById(R.id.btnNo);

        title.setText(titleText);
        message.setText(messageText);
        btnYes.setText(positiveText);
        btnNo.setText("Cancel");

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        btnYes.setOnClickListener(v -> {
            MetadataManager.get().saveMetadata(
                    etVin.getText().toString().trim(),
                    etcharger.getText().toString().trim(),
                    etOwner.getText().toString().trim(),
                    etPhone.getText().toString().trim(),
                    etController.getText().toString().trim(),
                    etMotor.getText().toString().trim(),
                    etBms.getText().toString().trim(),
                    etSmartKey.getText().toString().trim(),
                    etModel.getText().toString().trim(),
                    etfronttpms.getText().toString().trim(),
                    etreartpms.getText().toString().trim()

            );
            positiveAction.run();
            dialog.dismiss();
        });
        btnNo.setOnClickListener(v -> dialog.dismiss());

        return dialog;
    }

    private void loadExistingData() {
        MetadataManager mm = MetadataManager.get();

        setIfValid(etVin, mm.getVin());
        setIfValid(etcharger, mm.getCharger());
        setIfValid(etOwner, mm.getOwner());
        setIfValid(etPhone, mm.getPhone());
        setIfValid(etController, mm.getControllerId());
        setIfValid(etMotor, mm.getMotorId());
        setIfValid(etBms, mm.getBmsId());
        setIfValid(etSmartKey, mm.getSmartKeyId());
        setIfValid(etModel, mm.getModel());
        setIfValid(etfronttpms, mm.getFrontTPMS());
        setIfValid(etreartpms, mm.getRearTPMS());

    }

    private void setIfValid(EditText field, String value) {
        if (value != null && !value.isEmpty()
                && !value.startsWith("UNKNOWN")
                && !value.startsWith("NA")) {
            field.setText(value);
        }
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private boolean isKeyboardVisible() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            return imm.isActive(view);
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            hideKeyboard();
        }
        return super.onTouchEvent(event);
    }
}
