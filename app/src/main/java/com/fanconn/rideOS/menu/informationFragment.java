package com.fanconn.rideOS.menu;

import android.os.Bundle;
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
        TextView controllerView = view.findViewById(R.id.controllerID);
        TextView motorView = view.findViewById(R.id.MotorID);
        TextView bmsView = view.findViewById(R.id.BMSID);
        TextView smartKeyView = view.findViewById(R.id.smartkeyid);
        TextView chargerID = view.findViewById(R.id.chargerID);

        // Set text from MetadataManager
        vinView.setText(MetadataManager.get().getVin());
        controllerView.setText(MetadataManager.get().getControllerId());
        motorView.setText( MetadataManager.get().getMotorId());
        bmsView.setText(MetadataManager.get().getBmsId());
        smartKeyView.setText(MetadataManager.get().getSmartKeyId());
        chargerID.setText(MetadataManager.get().getCharger());

        return view;
    }
}
