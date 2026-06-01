package com.fanconn.rideOS.menu;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.fanconn.rideOS.MetadataManager;
import com.fanconn.rideOS.R;

public class informationFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_information_fragment, container, false);

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
        ownerName.setText(MetadataManager.get().getOwner());
        nx100Model.setText(MetadataManager.get().getModel());
        controllerView.setText(MetadataManager.get().getControllerId());
        motorView.setText( MetadataManager.get().getMotorId());
        bmsView.setText(MetadataManager.get().getBmsId());
        smartKeyView.setText(MetadataManager.get().getSmartKeyId());
        frontTPMSID.setText(MetadataManager.get().getFrontTPMS());
        rearTPMSID.setText(MetadataManager.get().getRearTPMS());
        chargerID.setText(MetadataManager.get().getCharger());

        String vin = MetadataManager.get().getVin();

        if (vin != null
                && !vin.equals("UNKNOWN")
                && !vin.equals("UNKNOWN_VIN")
                && !vin.trim().isEmpty()) {
            Log.d("FRAGMENT_VIN",
                    "VIN BEFORE API = "
                            + vin);
            MetadataManager.get().syncMetadataFromServer(vin, () -> {

                requireActivity().runOnUiThread(() -> {

                    vinView.setText(MetadataManager.get().getVin());
                    controllerView.setText(MetadataManager.get().getControllerId());
                    motorView.setText(MetadataManager.get().getMotorId());
                    bmsView.setText(MetadataManager.get().getBmsId());
                    smartKeyView.setText(MetadataManager.get().getSmartKeyId());
                    chargerID.setText(MetadataManager.get().getCharger());

                    Log.d("META_CHECK",
                            "Controller = "
                                    + MetadataManager.get().getControllerId());
                });
            });
        }
        Log.d("META_CHECK", "VIN = " + MetadataManager.get().getVin());
        Log.d("META_CHECK", "Controller = " + MetadataManager.get().getControllerId());
        Log.d("META_CHECK", "Motor = " + MetadataManager.get().getMotorId());
        Log.d("META_CHECK", "BMS = " + MetadataManager.get().getBmsId());
        Log.d("META_CHECK", "SmartKey = " + MetadataManager.get().getSmartKeyId());
        Log.d("META_CHECK", "Front tpoms id = " + MetadataManager.get().getFrontTPMS());
        Log.d("META_CHECK", "rear tpms id = " + MetadataManager.get().getRearTPMS());
        Log.d("META_CHECK", "owner name = " + MetadataManager.get().getOwner());
        Log.d("META_CHECK", "phone number = " + MetadataManager.get().getPhone());
        Log.d("META_CHECK", "phone number = " + MetadataManager.get().getModel());

        return view;
    }
}
