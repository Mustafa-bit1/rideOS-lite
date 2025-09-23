package com.fanconn.rideOS.menu;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.fanconn.rideOS.R;

import java.util.List;
import java.util.Set;

public class WirelessFragment extends Fragment {

    private static final int PERMISSION_REQUEST_CODE = 101;

    private Button bluetoothToggle, wifiToggle;
    private TextView bluetoothStatus, wifiStatus;
    private ListView bluetoothDevicesList, wifiNetworksList;

    private BluetoothAdapter bluetoothAdapter;
    private WifiManager wifiManager;

    private ArrayAdapter<String> btAdapter, wifiAdapter;

    private int customOnColor = Color.parseColor("#1C78BE");
    private int customOffColor = Color.parseColor("#555555");

    private BroadcastReceiver btStateReceiver;
    private BroadcastReceiver wifiStateReceiver;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_wireless_fragment, container, false);

        bluetoothToggle = view.findViewById(R.id.bluetoothToggle);
        wifiToggle = view.findViewById(R.id.wifiToggle);
        bluetoothStatus = view.findViewById(R.id.bluetoothStatus);
        wifiStatus = view.findViewById(R.id.wifiStatus);
        bluetoothDevicesList = view.findViewById(R.id.bluetoothDevicesList);
        wifiNetworksList = view.findViewById(R.id.wifiNetworksList);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        wifiManager = (WifiManager) requireContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        btAdapter = new ArrayAdapter<>(requireContext(), R.layout.devices_listitems, R.id.deviceName);
        bluetoothDevicesList.setAdapter(btAdapter);

        wifiAdapter = new ArrayAdapter<>(requireContext(), R.layout.devices_listitems, R.id.deviceName);
        wifiNetworksList.setAdapter(wifiAdapter);

        if (!hasPermissions()) {
            requestPermissions(getRequiredPermissions(), PERMISSION_REQUEST_CODE);
        } else {
            updateButtonStates();
            scanBluetooth();
            scanWifi();
        }

        // Bluetooth toggle
        bluetoothToggle.setOnClickListener(v -> {
            if (bluetoothAdapter == null) return;

            if (bluetoothAdapter.isEnabled()) {
                showBluetoothOffDialog();
            } else {
                bluetoothAdapter.enable();
            }
        });

        // Wi-Fi toggle
        wifiToggle.setOnClickListener(v -> {
            if (wifiManager != null) {
                boolean success = wifiManager.setWifiEnabled(!wifiManager.isWifiEnabled());
                if (success) updateButtonStates();
            }
        });

        setupBluetoothReceiver();
        setupWifiReceiver();

        return view;
    }

    /*** Bluetooth Receiver ***/
    private void setupBluetoothReceiver() {
        btStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                    updateButtonStates();
                    if (bluetoothAdapter.isEnabled()) scanBluetooth();
                }
            }
        };
    }

    /*** Wi-Fi Receiver ***/
    private void setupWifiReceiver() {
        wifiStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action == null) return;

                switch (action) {
                    case WifiManager.WIFI_STATE_CHANGED_ACTION:
                    case WifiManager.NETWORK_STATE_CHANGED_ACTION:
                        updateButtonStates();
                        if (wifiManager != null && wifiManager.isWifiEnabled()) {
                            scanWifi(); // refresh available networks
                        }
                        break;
                    case WifiManager.SCAN_RESULTS_AVAILABLE_ACTION:
                        List<ScanResult> results = wifiManager.getScanResults();
                        wifiAdapter.clear();
                        for (ScanResult result : results) {
                            if (result.SSID != null && !result.SSID.isEmpty()) {
                                wifiAdapter.add(result.SSID);
                            }
                        }
                        break;
                }
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        if (btStateReceiver != null) {
            requireContext().registerReceiver(btStateReceiver,
                    new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        }

        if (wifiStateReceiver != null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
            filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
            filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            requireContext().registerReceiver(wifiStateReceiver, filter);
        }

        updateButtonStates();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (btStateReceiver != null) {
            try { requireContext().unregisterReceiver(btStateReceiver); } catch (IllegalArgumentException ignored) {}
        }
        if (wifiStateReceiver != null) {
            try { requireContext().unregisterReceiver(wifiStateReceiver); } catch (IllegalArgumentException ignored) {}
        }
    }

    /*** Bluetooth OFF confirmation ***/
    private void showBluetoothOffDialog() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.popupdialog, null);

        TextView title = dialogView.findViewById(R.id.dialogTitle);
        TextView message = dialogView.findViewById(R.id.dialogMessage);
        Button btnYes = dialogView.findViewById(R.id.btnYes);
        Button btnNo = dialogView.findViewById(R.id.btnNo);

        title.setText("Turn off Bluetooth?");
        message.setText("This will disable the RIVOT smart key feature and tire pressure monitoring system. Are you sure you want to turn OFF the Bluetooth?");
        btnYes.setText("Turn OFF Bluetooth");
        btnNo.setText("Cancel");

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        btnYes.setOnClickListener(v -> {
            bluetoothAdapter.disable();
            dialog.dismiss();
        });
        btnNo.setOnClickListener(v -> dialog.dismiss());

        if (isAdded()) dialog.show();
    }

    /*** Update UI button states ***/
    private void updateButtonStates() {
        boolean btOn = bluetoothAdapter != null && bluetoothAdapter.isEnabled();
        bluetoothToggle.setBackgroundTintList(ColorStateList.valueOf(btOn ? customOnColor : customOffColor));
        bluetoothStatus.setText(btOn ? "Bluetooth: Enabled" : "Bluetooth OFF - Smart Key Feature Inactivated");

        boolean wifiOn = wifiManager != null && wifiManager.isWifiEnabled();
        wifiToggle.setBackgroundTintList(ColorStateList.valueOf(wifiOn ? customOnColor : customOffColor));

        String connectedSSID = "Not connected";
        if (wifiOn && wifiManager != null) {
            WifiInfo info = wifiManager.getConnectionInfo();
            if (info != null && info.getNetworkId() != -1) {
                connectedSSID = info.getSSID();
                if (connectedSSID.startsWith("\"") && connectedSSID.endsWith("\"")) {
                    connectedSSID = connectedSSID.substring(1, connectedSSID.length() - 1);
                }
            }
        }
        wifiStatus.setText("Connected to : " + connectedSSID);
    }

    /*** Scan paired Bluetooth devices ***/
    private void scanBluetooth() {
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            btAdapter.clear();
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            for (BluetoothDevice device : pairedDevices) {
                String name = device.getName();
                if (name == null || name.isEmpty()) name = "Unnamed Device";
                btAdapter.add(name + " - " + device.getAddress());
            }
        }
    }

    /*** Trigger Wi-Fi scan ***/
    private void scanWifi() {
        if (wifiManager != null && wifiManager.isWifiEnabled()) {
            wifiManager.startScan(); // results handled in SCAN_RESULTS_AVAILABLE_ACTION
        }
    }

    /*** Permissions ***/
    private String[] getRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE
            };
        } else {
            return new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE
            };
        }
    }

    private boolean hasPermissions() {
        for (String perm : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(requireContext(), perm) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                updateButtonStates();
                scanBluetooth();
                scanWifi();
            } else {
                Toast.makeText(requireContext(), "Permissions are required", Toast.LENGTH_LONG).show();
            }
        }
    }
}
