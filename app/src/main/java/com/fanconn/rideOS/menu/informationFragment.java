package com.fanconn.rideOS.menu;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.fanconn.rideOS.MetadataManager;
import com.fanconn.rideOS.R;
import com.fanconn.rideOS.TpmsScanService;

public class informationFragment extends Fragment {
    Button refreshBtn;
    private Toast currentToast;
    private int vinTapCount = 0;
    private long lastTapTime = 0;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_information_fragment, container, false);
        refreshBtn = view.findViewById(R.id.Refreshbtn);
        // Get references to the visible TextViews
        TextView vinView = view.findViewById(R.id.vinnumber);
        TextView ownerName = view.findViewById(R.id.OwnerName);
        TextView nx100Model = view.findViewById(R.id.nx100Model);
        TextView controllerView = view.findViewById(R.id.controllerID);
        TextView motorView = view.findViewById(R.id.MotorID);
        TextView bmsView = view.findViewById(R.id.BMSID);
        TextView smartKeyView = view.findViewById(R.id.smartkeyid);
        TextView rearTPMSID = view.findViewById(R.id.rearTPMSID);
        TextView frontTPMSID = view.findViewById(R.id.frontTPMSID);
        TextView chargerID = view.findViewById(R.id.chargerID);
        // Set text from MetadataManager
        vinView.setText(MetadataManager.get().getVin());

        vinView.setOnClickListener(v -> {

            long now = System.currentTimeMillis();

            if (now - lastTapTime > 2000) {
                vinTapCount = 0;
            }

            lastTapTime = now;
            vinTapCount++;

            if (vinTapCount >= 3) {
                vinTapCount = 0;
                showDiagnosticPasswordDialog();
            }
        });


        ownerName.setText(MetadataManager.get().getOwner());
        nx100Model.setText(MetadataManager.get().getModel());
        controllerView.setText(MetadataManager.get().getControllerId());
        motorView.setText( MetadataManager.get().getMotorId());
        bmsView.setText(MetadataManager.get().getBmsId());
        smartKeyView.setText(MetadataManager.get().getSmartKeyId());
        frontTPMSID.setText(MetadataManager.get().getFrontTPMS());
        rearTPMSID.setText(MetadataManager.get().getRearTPMS());
        chargerID.setText(MetadataManager.get().getCharger());

        String initialVin = MetadataManager.get().getVin();

        if (initialVin != null
                && !initialVin.equals("UNKNOWN")
                && !initialVin.equals("UNKNOWN_VIN")
                && !initialVin.trim().isEmpty()) {
            updateMetadataUI(
                    vinView,
                    ownerName,
                    nx100Model,
                    controllerView,
                    motorView,
                    bmsView,
                    smartKeyView,
                    frontTPMSID,
                    rearTPMSID,
                    chargerID
            );
        }
        refreshBtn.setOnClickListener(v -> {

            // CLICK ANIMATION
            v.animate()
                    .scaleX(0.92f)
                    .scaleY(0.92f)
                    .setDuration(70)
                    .withEndAction(() -> v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(10))
                    .start();

            // INTERNET CHECK
            if (!isInternetAvailable()) {

                showCustomToast("No internet");

                Log.e("INFO_FRAGMENT", "NO INTERNET");

                return;
            }

            String vin = MetadataManager.get().getVin();

            if (vin == null
                    || vin.equals("UNKNOWN")
                    || vin.equals("UNKNOWN_VIN")
                    || vin.trim().isEmpty()) {

                Log.e("INFO_FRAGMENT", "INVALID VIN");

                showCustomToast("Invalid VIN");

                return;
            }

            Log.d("INFO_FRAGMENT", "REFRESHING METADATA");

            MetadataManager.get().syncMetadataFromServer(vin, () -> {

                requireActivity().runOnUiThread(() -> {

                    updateMetadataUI(
                            vinView,
                            ownerName,
                            nx100Model,
                            controllerView,
                            motorView,
                            bmsView,
                            smartKeyView,
                            frontTPMSID,
                            rearTPMSID,
                            chargerID
                    );

                    Log.d("INFO_FRAGMENT",
                            "METADATA UI UPDATED");

                    // RELOAD TPMS IDS
                    LocalBroadcastManager.getInstance(requireContext())
                            .sendBroadcast(
                                    new Intent(
                                            TpmsScanService.ACTION_TPMS_RELOAD_IDS
                                    )
                            );

                    Log.d("INFO_FRAGMENT",
                            "TPMS IDS RELOADED");

                    showCustomToast("Refresh successful");
                });
            });
        });
//        Log.d("META_CHECK", "VIN = " + MetadataManager.get().getVin());
//        Log.d("META_CHECK", "Controller = " + MetadataManager.get().getControllerId());
//        Log.d("META_CHECK", "Motor = " + MetadataManager.get().getMotorId());
//        Log.d("META_CHECK", "BMS = " + MetadataManager.get().getBmsId());
//        Log.d("META_CHECK", "SmartKey = " + MetadataManager.get().getSmartKeyId());
//        Log.d("META_CHECK", "Front tpoms id = " + MetadataManager.get().getFrontTPMS());
//        Log.d("META_CHECK", "rear tpms id = " + MetadataManager.get().getRearTPMS());
//        Log.d("META_CHECK", "owner name = " + MetadataManager.get().getOwner());
//        Log.d("META_CHECK", "phone number = " + MetadataManager.get().getPhone());
//        Log.d("META_CHECK", "phone number = " + MetadataManager.get().getModel());

        return view;
    }


    private void showDiagnosticPasswordDialog() {

        View view = LayoutInflater.from(requireContext())
                .inflate(
                        R.layout.dialog_diagnostic_password,
                        null
                );

        AlertDialog dialog =
                new AlertDialog.Builder(requireContext())
                        .setView(view)
                        .create();

        dialog.getWindow()
                .setBackgroundDrawable(
                        new ColorDrawable(
                                Color.TRANSPARENT
                        )
                );

        EditText passwordInput =
                view.findViewById(
                        R.id.passwordInput
                );

        Button openBtn =
                view.findViewById(
                        R.id.connectBtn
                );

        openBtn.setOnClickListener(v -> {

            String vin =
                    MetadataManager.get()
                            .getVin();

            String expectedPassword = "";

            if (vin != null &&
                    vin.length() >= 4) {

                expectedPassword =
                        vin.substring(
                                vin.length() - 4
                        );
            }

            String entered =
                    passwordInput
                            .getText()
                            .toString()
                            .trim();

            if (entered.equals(expectedPassword)) {

                dialog.dismiss();

                settings activity =
                        (settings) requireActivity();

                activity.openDiagnosticsFragment();

            } else {
                showCustomToast("Wrong password - Access Denied");
            }
        });

        dialog.show();
    }
    private boolean isInternetAvailable() {

        ConnectivityManager cm =
                (ConnectivityManager)
                        requireContext().getSystemService(
                                android.content.Context.CONNECTIVITY_SERVICE);

        if (cm == null) return false;

        Network network = cm.getActiveNetwork();

        if (network == null) return false;

        NetworkCapabilities capabilities =
                cm.getNetworkCapabilities(network);

        return capabilities != null &&
                capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
    private void showCustomToast(String message) {

        // Cancel previous toast if showing
        if (currentToast != null) {
            currentToast.cancel();
        }

        View layout = LayoutInflater.from(requireContext())
                .inflate(R.layout.toast_custom, null);

        ((ImageView) layout.findViewById(R.id.toast_icon))
                .setImageResource(R.drawable.toastlogo);

        ((TextView) layout.findViewById(R.id.toast_text))
                .setText(message);

        currentToast = new Toast(requireContext());

        currentToast.setDuration(Toast.LENGTH_SHORT);

        currentToast.setView(layout);

        currentToast.show();
    }
    private void updateMetadataUI(
            TextView vinView,
            TextView ownerName,
            TextView nx100Model,
            TextView controllerView,
            TextView motorView,
            TextView bmsView,
            TextView smartKeyView,
            TextView frontTPMSID,
            TextView rearTPMSID,
            TextView chargerID)
    {

        vinView.setText(MetadataManager.get().getVin());

        ownerName.setText(MetadataManager.get().getOwner());

        nx100Model.setText(MetadataManager.get().getModel());

        controllerView.setText(
                MetadataManager.get().getControllerId());

        motorView.setText(
                MetadataManager.get().getMotorId());

        bmsView.setText(
                MetadataManager.get().getBmsId());

        smartKeyView.setText(
                MetadataManager.get().getSmartKeyId());

        frontTPMSID.setText(
                MetadataManager.get().getFrontTPMS());

        rearTPMSID.setText(
                MetadataManager.get().getRearTPMS());

        chargerID.setText(
                MetadataManager.get().getCharger());
    }
}
