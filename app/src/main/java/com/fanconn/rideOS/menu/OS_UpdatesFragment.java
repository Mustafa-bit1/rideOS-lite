package com.fanconn.rideOS.menu;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.fanconn.rideOS.R;
import com.fanconn.rideOS.UpdateService;
import com.fanconn.rideOS.UptimeManager;

import org.json.JSONArray;
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
    private TextView tvUptime;
    private Handler uptimeHandler = new Handler();
    private Runnable uptimeRunnable;

    // Expandable release notes
    private LinearLayout layoutReleaseNotes;
    private TextView tvReleasePoints;
    private boolean isExpanded = false;

    // Server data
    private String releasePoints = "Loading...";
    private ImageButton arrowImg;

    public static boolean showupdatepopup;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_os_updates, container, false);

        // Header UI
        otaupdatebtn = view.findViewById(R.id.otaupdatebtn);
        arrowImg = view.findViewById(R.id.arrowdown);

        // Expandable section
        layoutReleaseNotes = view.findViewById(R.id.layoutReleaseNotes);
        tvReleasePoints = view.findViewById(R.id.tvReleasePoints);
        arrowImg = view.findViewById(R.id.arrowdown);

        // Reset state every time fragment opens
        layoutReleaseNotes.setVisibility(View.GONE);
        isExpanded = false;

        // Other UI
        tvUptime = view.findViewById(R.id.tvUptime);

        updateVersionInfo(view);
        fetchServerUpdateInfo(view);
        startUptimeTimer();

        otaupdatebtn.setOnClickListener(v -> {
            showupdatepopup = true;
            Intent intent = new Intent(getContext(), UpdateService.class);
            intent.putExtra("manual_check", true);
            requireContext().startService(intent);
        });
        layoutReleaseNotes = view.findViewById(R.id.layoutReleaseNotes);
        tvReleasePoints = view.findViewById(R.id.tvReleasePoints);

        layoutReleaseNotes.setVisibility(View.GONE); // default state
        isExpanded = false;
        arrowImg.setOnClickListener(v -> toggleReleaseNotes());

        return view;
    }

    // ---------------- EXPAND / COLLAPSE ----------------

    private void toggleReleaseNotes() {
        if (isExpanded) {
            layoutReleaseNotes.setVisibility(View.GONE);
            arrowImg.setImageResource(R.drawable.arrowdown); // DOWN
            isExpanded = false;
        } else {
            tvReleasePoints.setText(formattedReleasePoints);
            layoutReleaseNotes.setVisibility(View.VISIBLE);
            arrowImg.setImageResource(R.drawable.uparrow); // UP
            isExpanded = true;
        }
    }


    // ---------------- UPTIME ----------------

    private void startUptimeTimer() {
        uptimeRunnable = new Runnable() {
            @Override
            public void run() {
                if (tvUptime != null) {
                    tvUptime.setText("Uptime: " + UptimeManager.getUptime());
                }
                uptimeHandler.postDelayed(this, 1000);
            }
        };
        uptimeHandler.post(uptimeRunnable);
    }

    // ---------------- SERVER OTA INFO ----------------
    private String formattedReleasePoints = "";

    private void fetchServerUpdateInfo(View view) {

        TextView serverVersionTV = view.findViewById(R.id.server_version);
        TextView serverReleaseDateTV = view.findViewById(R.id.server_release_date);

        new Thread(() -> {
            try {
                URL url = new URL(
                        "http://ota.rivotmotors.com/OTA/sc126/ElectricBike/rideOS/production_OTA/prod_update.json"
                );

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.connect();

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream())
                );

                StringBuilder jsonBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonBuilder.append(line);
                }
                reader.close();

                JSONObject jsonObject = new JSONObject(jsonBuilder.toString());

                String latestVersion = jsonObject.getString("latest_version");
                String releaseDate = jsonObject.optString("release_date", "N/A");

                // ===== BULLET POINT PARSING =====
                JSONArray pointsArray = jsonObject.optJSONArray("release_points");
                StringBuilder bulletBuilder = new StringBuilder();

                if (pointsArray != null) {
                    for (int i = 0; i < pointsArray.length(); i++) {
                        bulletBuilder.append("•  ")
                                .append(pointsArray.getString(i))
                                .append("\n");
                    }
                    formattedReleasePoints = bulletBuilder.toString().trim();
                } else {
                    formattedReleasePoints = "• No release details available";
                }

                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        serverVersionTV.setText("Latest Version " + latestVersion);
                        serverReleaseDateTV.setText("Release Date: " + releaseDate);
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                formattedReleasePoints = "• No update available";

                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        serverVersionTV.setText("Latest Version : Software up-to-date");
                        serverReleaseDateTV.setText("Release Date");
                    });
                }
            }
        }).start();
    }

    // ---------------- DEVICE / BUILD INFO ----------------

    private void updateVersionInfo(View view) {

        TextView versionText = view.findViewById(R.id.version);
        TextView installedDateTV = view.findViewById(R.id.installeddate);
        TextView buildNumTV = view.findViewById(R.id.buildnum);
        TextView deviceNameTV = view.findViewById(R.id.devicenameTV);
        TextView bluetoothTV = view.findViewById(R.id.bluetoothTV);

        String versionName = "N/A";
        long updateTime = 0;

        try {
            PackageInfo pInfo = requireContext()
                    .getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0);
            versionName = pInfo.versionName;
            updateTime = pInfo.lastUpdateTime;
        } catch (Exception ignored) {}

        installedDateTV.setText(
                new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        .format(new Date(updateTime))
        );

        buildNumTV.setText( "GA_"+Build.DISPLAY);
        versionText.setText("rideOS " + versionName);

        String deviceName = Settings.Global.getString(
                requireContext().getContentResolver(), "device_name"
        );
        if (deviceName == null) deviceName = Build.DEVICE;
        deviceNameTV.setText(deviceName);

        String bluetoothName = "Unavailable";
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        if (adapter != null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED) {

                bluetoothName = adapter.getName();
            }
        }

        bluetoothTV.setText(bluetoothName);
    }

    // ---------------- CLEANUP ----------------

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        uptimeHandler.removeCallbacks(uptimeRunnable);
    }
}
