package com.fanconn.rideOS.menu;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.fanconn.rideOS.R;

import java.lang.ref.WeakReference;
import java.util.List;

public class WirelessFragment extends Fragment {

    private static final int PERMISSION_REQUEST_CODE = 101;

    private Button wifiToggle;
    private TextView wifiStatus;
    private ListView wifiNetworksList;

    private WifiManager wifiManager;
    private LinearLayout wifiSectionLayout;
    private ArrayAdapter<String> wifiAdapter;

    private int customOnColor = Color.parseColor("#1C78BE");
    private int customOffColor = Color.parseColor("#555555");

    private Handler handler = new Handler(Looper.getMainLooper());
    private WifiReceiver wifiStateReceiver;

    // --- Lifecycle ---
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_wireless_fragment, container, false);

        wifiToggle = view.findViewById(R.id.wifiToggle);
        wifiStatus = view.findViewById(R.id.wifiStatus);
        wifiNetworksList = view.findViewById(R.id.wifiNetworksList);
        wifiSectionLayout = view.findViewById(R.id.wifiSectionLayout);

        wifiManager = (WifiManager) requireContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        wifiAdapter = new ArrayAdapter<>(requireContext(), R.layout.devices_listitems, R.id.deviceName);
        wifiNetworksList.setAdapter(wifiAdapter);

        if (!hasPermissions()) {
            requestPermissions(getRequiredPermissions(), PERMISSION_REQUEST_CODE);
        } else {
            initUI();
        }

        return view;
    }

    private void initUI() {
        updateButtonStates();
        scanWifi();

        wifiToggle.setOnClickListener(v -> {
            if (wifiManager != null) {
                boolean enabling = !wifiManager.isWifiEnabled();
                wifiToggle.setBackgroundTintList(ColorStateList.valueOf(enabling ? customOnColor : customOffColor));
                wifiStatus.setText(enabling ? "Turning ON..." : "Turning OFF...");
                wifiManager.setWifiEnabled(enabling);
            }
        });

        wifiNetworksList.setOnItemClickListener((parent, view, position, id) -> {
            String ssid = wifiAdapter.getItem(position);
            if (ssid == null) return;
            boolean secured = isWifiSecured(ssid);
            if (secured) showWifiPasswordDialog(ssid);
            else connectToWifi(ssid, null);
        });

        wifiStateReceiver = new WifiReceiver(this);
    }

    // --- Wi-Fi helpers ---
    private boolean isWifiSecured(String ssid) {
        List<ScanResult> results = wifiManager.getScanResults();
        for (ScanResult result : results) {
            if (result.SSID.equals(ssid)) {
                String capabilities = result.capabilities;
                return capabilities.contains("WEP") || capabilities.contains("WPA") || capabilities.contains("WPA2");
            }
        }
        return false;
    }

    private void showWifiPasswordDialog(String ssid) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.wifi_password_dialog, null);

        TextView title = dialogView.findViewById(R.id.dialogTitle);
        EditText passwordInput = dialogView.findViewById(R.id.passwordInput);
        ImageView togglePassword = dialogView.findViewById(R.id.togglePassword);
        Button connectBtn = dialogView.findViewById(R.id.connectBtn);

        title.setText("Connect to " + ssid);

        final boolean[] isPasswordVisible = {false};

        togglePassword.setOnClickListener(v -> {
            if (isPasswordVisible[0]) {
                passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                togglePassword.setImageResource(R.drawable.ic_eye_closed);
                isPasswordVisible[0] = false;
            } else {
                passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                togglePassword.setImageResource(R.drawable.ic_eye_open);
                isPasswordVisible[0] = true;
            }
            passwordInput.setSelection(passwordInput.getText().length());
        });

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        connectBtn.setOnClickListener(v -> {
            String password = passwordInput.getText().toString().trim();
            if (!password.isEmpty()) {
                connectToWifi(ssid, password);
                dialog.dismiss();
            } else {
                Toast.makeText(requireContext(), "Enter password", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void connectToWifi(String ssid, String password) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ssid == null || ssid.isEmpty()) return;

            if (password != null && !password.isEmpty()) saveWifiNetwork(ssid, password);

            WifiNetworkSpecifier.Builder builder = new WifiNetworkSpecifier.Builder()
                    .setSsid(ssid);

            if (password != null && !password.isEmpty())
                builder.setWpa2Passphrase(password);

            WifiNetworkSpecifier specifier = builder.build();

            NetworkRequest request = new NetworkRequest.Builder()
                    .addTransportType(android.net.NetworkCapabilities.TRANSPORT_WIFI)
                    .setNetworkSpecifier(specifier)
                    .build();

            ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    super.onAvailable(network);
                    cm.bindProcessToNetwork(network);
                    handler.post(() -> {
                        Toast.makeText(requireContext(), "Connected to " + ssid, Toast.LENGTH_SHORT).show();
                        updateButtonStates();
                    });
                }

                @Override
                public void onUnavailable() {
                    super.onUnavailable();
                    handler.post(() -> Toast.makeText(requireContext(), "Failed to connect to " + ssid, Toast.LENGTH_SHORT).show());
                }
            };

            Toast.makeText(requireContext(), "Connecting to " + ssid, Toast.LENGTH_SHORT).show();
            cm.requestNetwork(request, networkCallback);

        } else {
            Toast.makeText(requireContext(), "Wi-Fi connection supported only on Android 10+", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveWifiNetwork(String ssid, String password) {
        if (ssid == null || password == null) return;
        SharedPreferences prefs = requireContext().getSharedPreferences("wifi_prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        String queueStr = prefs.getString("wifi_queue", "");
        String[] queue = queueStr.isEmpty() ? new String[]{} : queueStr.split(",");

        String newQueueStr = ssid;
        int count = 1;
        for (String oldSSID : queue) {
            if (!oldSSID.equals(ssid) && count < 3) {
                newQueueStr += "," + oldSSID;
                count++;
            }
        }

        editor.putString("wifi_queue", newQueueStr);
        editor.putString("wifi_" + ssid, password);
        editor.apply();
    }

    // --- Lifecycle overrides ---
    @Override
    public void onResume() {
        super.onResume();
        if (wifiStateReceiver != null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
            filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
            filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            requireContext().registerReceiver(wifiStateReceiver, filter);
        }
        updateButtonStates();
        handler.post(wifiAutoScanRunnable); // start periodic scanning

    }

    @Override
    public void onPause() {
        super.onPause();
        if (wifiStateReceiver != null) try {
            requireContext().unregisterReceiver(wifiStateReceiver);
        } catch (Exception ignored) {}

        handler.removeCallbacks(wifiAutoScanRunnable); // stop scanning
        handler.removeCallbacksAndMessages(null);
    }

    // --- UI Updates ---
    private void updateButtonStates() {
        boolean wifiOn = wifiManager != null && wifiManager.isWifiEnabled();
        wifiToggle.setBackgroundTintList(ColorStateList.valueOf(wifiOn ? customOnColor : customOffColor));

        if (wifiOn) {
            wifiSectionLayout.setVisibility(View.VISIBLE);
        } else {
            wifiSectionLayout.setVisibility(View.GONE);
        }

        String connectedSSID = "Not connected";
        if (wifiOn && wifiManager != null) {
            WifiInfo info = wifiManager.getConnectionInfo();
            if (info != null && info.getNetworkId() != -1) {
                connectedSSID = info.getSSID();
                if (connectedSSID.startsWith("\"") && connectedSSID.endsWith("\""))
                    connectedSSID = connectedSSID.substring(1, connectedSSID.length() - 1);
            }
        }
        wifiStatus.setText("Connected to : " + connectedSSID);
    }

    private void scanWifi() {
        if (wifiManager != null && wifiManager.isWifiEnabled()) wifiManager.startScan();
    }

    // --- Receiver ---
    private static class WifiReceiver extends BroadcastReceiver {
        private final WeakReference<WirelessFragment> fragmentRef;

        WifiReceiver(WirelessFragment fragment) {
            fragmentRef = new WeakReference<>(fragment);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            WirelessFragment fragment = fragmentRef.get();
            if (fragment == null) return;

            String action = intent.getAction();
            if (action == null) return;

            switch (action) {
                case WifiManager.WIFI_STATE_CHANGED_ACTION:
                case WifiManager.NETWORK_STATE_CHANGED_ACTION:
                    fragment.updateButtonStates();
                    if (fragment.wifiManager != null && fragment.wifiManager.isWifiEnabled()) {
                        fragment.scanWifi();
                        fragment.handler.removeCallbacks(fragment.wifiAutoScanRunnable);
                        fragment.handler.post(fragment.wifiAutoScanRunnable);
                    } else {
                        fragment.handler.removeCallbacks(fragment.wifiAutoScanRunnable);
                    }
                    break;
                case WifiManager.SCAN_RESULTS_AVAILABLE_ACTION:
                    List<ScanResult> results = fragment.wifiManager.getScanResults();
                    fragment.wifiAdapter.clear();
                    for (ScanResult result : results) {
                        if (result.SSID != null && !result.SSID.isEmpty())
                            fragment.wifiAdapter.add(result.SSID);
                    }
                    break;
            }
        }
    }

    // --- Permissions ---
    private String[] getRequiredPermissions() {
        return new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE
        };
    }

    private boolean hasPermissions() {
        for (String perm : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(requireContext(), perm) != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults)
                if (result != PackageManager.PERMISSION_GRANTED)
                    allGranted = false;
            if (allGranted) initUI();
            else Toast.makeText(requireContext(), "Permissions are required", Toast.LENGTH_LONG).show();
        }
    }

    private final int WIFI_SCAN_INTERVAL = 5000; // 5 seconds
    private final Runnable wifiAutoScanRunnable = new Runnable() {
        @Override
        public void run() {
            if (wifiManager != null && wifiManager.isWifiEnabled()) {
                wifiManager.startScan();
                handler.postDelayed(this, WIFI_SCAN_INTERVAL);
            }
        }
    };

}
