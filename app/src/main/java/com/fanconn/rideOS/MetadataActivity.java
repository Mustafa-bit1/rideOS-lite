package com.fanconn.rideOS;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MetadataActivity extends AppCompatActivity {

    private EditText etVin;
    private Button btnSave;
    private ImageButton menuBackBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_metadata);

        etVin = findViewById(R.id.etVin);
        btnSave = findViewById(R.id.btnSave);
        menuBackBtn = findViewById(R.id.menubackbtn);

        InactivityManager.getInstance().stopTimer();

        loadExistingData();

        btnSave.setOnClickListener(v -> {

            String vin = etVin.getText().toString().trim();

            if (vin.isEmpty()) {
                Toast.makeText(
                        MetadataActivity.this,
                        "Enter VIN",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            showSaveDialog(vin);
        });

        menuBackBtn.setOnClickListener(v -> finish());
    }

    private void showSaveDialog(String vin) {

        View dialogView =
                LayoutInflater.from(this)
                        .inflate(R.layout.popupdialog, null);

        AlertDialog dialog = createDialog(
                dialogView,
                "Save VIN",
                "Are you sure you want to save this VIN?\n" + vin,
                "Save",
                () -> {

                    MetadataManager.get().saveMetadata(
                            vin,
                            "UNKNOWN_CHARGER",
                            "UNKNOWN_OWNER",
                            "UNKNOWN_PHONE",
                            "UNKNOWN_CONTROLLER",
                            "UNKNOWN_MOTOR",
                            "UNKNOWN_BMS",
                            "UNKNOWN_KEY",
                            "UNKNOWN_MODEL",
                            "UNKNOWN_FTPMS",
                            "UNKNOWN_RTPMS"
                    );
                    Log.d("VIN_SAVE_CHECK",
                            "VIN AFTER SAVE = "
                                    + MetadataManager.get().getVin());
                    Toast.makeText(
                            MetadataActivity.this,
                            "VIN Saved Successfully",
                            Toast.LENGTH_LONG
                    ).show();

                    Intent serviceIntent =
                            new Intent(
                                    MetadataActivity.this,
                                    TelemetryService.class
                            );

                    serviceIntent.putExtra(
                            "VIN_NUMBER",
                            vin
                    );

                    startService(serviceIntent);

                    finish();
                }
        );

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(
                    android.R.color.transparent
            );
        }

        dialog.show();
    }

    private AlertDialog createDialog(
            View dialogView,
            String titleText,
            String messageText,
            String positiveText,
            Runnable positiveAction) {

        TextView title =
                dialogView.findViewById(R.id.dialogTitle);

        TextView message =
                dialogView.findViewById(R.id.dialogMessage);

        Button btnYes =
                dialogView.findViewById(R.id.btnYes);

        Button btnNo =
                dialogView.findViewById(R.id.btnNo);

        title.setText(titleText);
        message.setText(messageText);

        btnYes.setText(positiveText);
        btnNo.setText("Cancel");

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setView(dialogView)
                        .setCancelable(false)
                        .create();

        btnYes.setOnClickListener(v -> {
            positiveAction.run();
            dialog.dismiss();
        });

        btnNo.setOnClickListener(v -> dialog.dismiss());

        return dialog;
    }

    private void loadExistingData() {

        String vin = MetadataManager.get().getVin();

        if (vin != null
                && !vin.isEmpty()
                && !vin.startsWith("UNKNOWN")
                && !vin.startsWith("NA")) {

            etVin.setText(vin);
        }
    }

    private void hideKeyboard() {

        View view = getCurrentFocus();

        if (view != null) {

            InputMethodManager imm =
                    (InputMethodManager)
                            getSystemService(
                                    Context.INPUT_METHOD_SERVICE);

            imm.hideSoftInputFromWindow(
                    view.getWindowToken(),
                    0
            );
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            hideKeyboard();
        }

        return super.onTouchEvent(event);
    }
}


//package com.fanconn.rideOS;
//
//import static androidx.core.content.ContentProviderCompat.requireContext;
//
//import android.app.AlertDialog;
//import android.content.Context;
//import android.content.Intent;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.Looper;
//import android.os.PowerManager;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.MotionEvent;
//import android.view.View;
//import android.view.inputmethod.InputMethodManager;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.ImageButton;
//import android.widget.TextView;
//import android.widget.Toast;
//import androidx.fragment.app.Fragment;
//import androidx.fragment.app.FragmentManager;
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.fanconn.rideOS.network.ApiService;
//import com.fanconn.rideOS.network.RetrofitClient;
//import com.fanconn.rideOS.network.VehicleMetadataResponse;
//
//import retrofit2.Call;
//import retrofit2.Callback;
//import retrofit2.Response;
//
//
//public class MetadataActivity extends AppCompatActivity {
//
//    private EditText etVin, etcharger, etOwner, etPhone, etController, etMotor, etBms, etSmartKey;
//    private Button btnSave;
//    private ImageButton menuBackBtn;
//    private EditText etModel,etfronttpms,etreartpms;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_metadata);
//
//        etVin = findViewById(R.id.etVin);
//        etcharger = findViewById(R.id.etCharger);
//        etOwner = findViewById(R.id.etOwner);
//        etPhone = findViewById(R.id.etPhone);
//        etController = findViewById(R.id.etController);
//        etMotor = findViewById(R.id.etMotor);
//        etBms = findViewById(R.id.etBms);
//        etSmartKey = findViewById(R.id.etSmartKey);
//        btnSave = findViewById(R.id.btnSave);
//        menuBackBtn = findViewById(R.id.menubackbtn);
//        etModel = findViewById(R.id.etModel);
//        etfronttpms = findViewById(R.id.etFrontMacAddress);
//        etreartpms = findViewById(R.id.etRearMacAddress);
//
//        InactivityManager.getInstance().stopTimer();
//
//        loadExistingData();
//
//        btnSave.setOnClickListener(v -> {
//
//            String vin = etVin.getText().toString().trim();
//
//            if (vin.isEmpty()) {
//                Toast.makeText(this,
//                        "Enter VIN",
//                        Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            MetadataManager.get().saveMetadata(
//                    vin,
//                    "UNKNOWN_CHARGER",
//                    "UNKNOWN_OWNER",
//                    "UNKNOWN_PHONE",
//                    "UNKNOWN_CONTROLLER",
//                    "UNKNOWN_MOTOR",
//                    "UNKNOWN_BMS",
//                    "UNKNOWN_KEY",
//                    "UNKNOWN_MODEL",
//                    "UNKNOWN_FTPMS",
//                    "UNKNOWN_RTPMS"
//            );
//
//            Toast.makeText(this,
//                    "VIN Saved",
//                    Toast.LENGTH_LONG).show();
//
//            Intent intent = new Intent(this, TelemetryService.class);
//            intent.putExtra("VIN_NUMBER", vin);
//            startService(intent);
//
//            finish();
//        });
//        menuBackBtn.setOnClickListener(v -> {
//            if (isKeyboardVisible()) {
//                hideKeyboard();
//            } else {
//                finish();
//            }
//        });
//    }
//    private AlertDialog createDialog(View dialogView, String titleText, String messageText, String positiveText, Runnable positiveAction) {
//        TextView title = dialogView.findViewById(R.id.dialogTitle);
//        TextView message = dialogView.findViewById(R.id.dialogMessage);
//        Button btnYes = dialogView.findViewById(R.id.btnYes);
//        Button btnNo = dialogView.findViewById(R.id.btnNo);
//
//        title.setText(titleText);
//        message.setText(messageText);
//        btnYes.setText(positiveText);
//        btnNo.setText("Cancel");
//
//        AlertDialog dialog = new AlertDialog.Builder(this)
//                .setView(dialogView)
//                .setCancelable(false)
//                .create();
//
//        btnYes.setOnClickListener(v -> {
//            MetadataManager.get().saveMetadata(
//                    etVin.getText().toString().trim(),
//                    etcharger.getText().toString().trim(),
//                    etOwner.getText().toString().trim(),
//                    etPhone.getText().toString().trim(),
//                    etController.getText().toString().trim(),
//                    etMotor.getText().toString().trim(),
//                    etBms.getText().toString().trim(),
//                    etSmartKey.getText().toString().trim(),
//                    etModel.getText().toString().trim(),
//                    etfronttpms.getText().toString().trim(),
//                    etreartpms.getText().toString().trim()
//
//            );
//            positiveAction.run();
//            dialog.dismiss();
//        });
//        btnNo.setOnClickListener(v -> dialog.dismiss());
//
//        return dialog;
//    }
//
//    private void loadExistingData() {
//        MetadataManager mm = MetadataManager.get();
//
//        setIfValid(etVin, mm.getVin());
//        setIfValid(etcharger, mm.getCharger());
//        setIfValid(etOwner, mm.getOwner());
//        setIfValid(etPhone, mm.getPhone());
//        setIfValid(etController, mm.getControllerId());
//        setIfValid(etMotor, mm.getMotorId());
//        setIfValid(etBms, mm.getBmsId());
//        setIfValid(etSmartKey, mm.getSmartKeyId());
//        setIfValid(etModel, mm.getModel());
//        setIfValid(etfronttpms, mm.getFrontTPMS());
//        setIfValid(etreartpms, mm.getRearTPMS());
//
//    }
//
//    private void setIfValid(EditText field, String value) {
//        if (value != null && !value.isEmpty()
//                && !value.startsWith("UNKNOWN")
//                && !value.startsWith("NA")) {
//            field.setText(value);
//        }
//    }
//
//    private void hideKeyboard() {
//        View view = getCurrentFocus();
//        if (view != null) {
//            InputMethodManager imm =
//                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
//        }
//    }
//
//    private boolean isKeyboardVisible() {
//        View view = getCurrentFocus();
//        if (view != null) {
//            InputMethodManager imm =
//                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//            return imm.isActive(view);
//        }
//        return false;
//    }
//
//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        if (event.getAction() == MotionEvent.ACTION_DOWN) {
//            hideKeyboard();
//        }
//        return super.onTouchEvent(event);
//    }
//
//
//}
