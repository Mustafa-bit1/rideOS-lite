package com.fanconn.rideOS.menu;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.Manifest;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.fanconn.rideOS.R;
import com.fanconn.rideOS.UpdateService;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class OS_UpdatesFragment extends Fragment {

    private ImageButton otaupdatebtn;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_os_updates, container, false);
        updateVersionInfo(view);
        fetchServerUpdateInfo(view);

        otaupdatebtn = view.findViewById(R.id.otaupdatebtn);

        otaupdatebtn.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), UpdateService.class);
            intent.putExtra("manual_check", true);
            getContext().startService(intent);
        });

        return view;
    }
    private void fetchServerUpdateInfo(View view) {
        TextView serverVersionTV = view.findViewById(R.id.server_version);
        TextView serverReleaseDateTV = view.findViewById(R.id.server_release_date);

        new Thread(() -> {
            try {
                URL url = new URL("http://ota.rivotmotors.com/OTA/sc126/ElectricBike/rideOS/update.json");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.connect();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder jsonBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonBuilder.append(line);
                }
                reader.close();

                JSONObject jsonObject = new JSONObject(jsonBuilder.toString());
                String latestVersion = jsonObject.getString("latest_version");
                String releaseDate = jsonObject.optString("release_date", "N/A");

                requireActivity().runOnUiThread(() -> {
                    serverVersionTV.setText("App Version " + latestVersion);
                    serverReleaseDateTV.setText("Release Date: " + releaseDate);
                });

            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> {
                    serverVersionTV.setText("Latest Version: Error");
                    serverReleaseDateTV.setText("Release Date: Error");
                });
            }
        }).start();
    }

    private void updateVersionInfo(View view) {
        TextView versionText = view.findViewById(R.id.version);
        TextView insatlleddata = view.findViewById(R.id.installeddate);
        TextView buildnum = view.findViewById(R.id.buildnum);
        TextView deviceNameTV = view.findViewById(R.id.devicenameTV);  // Add this line

        String versionName = "N/A";
        long updateTime = 0;
        try {
            PackageInfo pInfo = requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0);
            versionName = pInfo.versionName;
            updateTime = pInfo.lastUpdateTime;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        String installDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(updateTime));
        insatlleddata.setText(installDate);

        String buildNumber = Build.DISPLAY;
        buildnum.setText(buildNumber);

        String versionInfo = "rideOS " + versionName;
        versionText.setText(versionInfo);

        String deviceName = Settings.Global.getString(requireContext().getContentResolver(), "device_name");
        if (deviceName == null) deviceName = Build.DEVICE;

        deviceNameTV.setText(deviceName);

        TextView bluetoothTV = view.findViewById(R.id.bluetoothTV);

// Default fallback
        String bluetoothName = "Unknown";

// Get Bluetooth Adapter and Name
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                bluetoothName = bluetoothAdapter.getName();
            }
        }

        if (bluetoothName == null || bluetoothName.isEmpty()) {
            bluetoothName = "Unavailable";
        }

        bluetoothTV.setText(bluetoothName);

    }
    private String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.toLowerCase().startsWith(manufacturer.toLowerCase())) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }

    private String capitalize(String s) {
        if (s == null || s.length() == 0) return "";
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

}
