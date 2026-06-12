package com.fanconn.rideOS.menu;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.fanconn.rideOS.FrameUtils;
import com.fanconn.rideOS.InactivityManager;
import com.fanconn.rideOS.MainActivity;
import com.fanconn.rideOS.OtaBranchManager;
import com.fanconn.rideOS.R;
import com.fanconn.rideOS.SerialPortManager;
import com.fanconn.rideOS.UpdateService;
import com.fanconn.rideOS.UptimeManager;
import com.fanconn.rideOS.aosp_mainActivity;
import com.fanconn.rideOS.aosp_ota_update_check;
import com.fanconn.rideOS.mcuupdate.McuOtaExecutor;
import com.fanconn.rideOS.serialApplication;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class OS_UpdatesFragment extends Fragment {
    public SerialPortManager serialPortManager;  // 串口管理类
    private ImageButton otaupdatebtn;
    //private ImageButton mcuotaupdatebtn;
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
    private Context ctx;
    private String version_URL = "http://ota.rivotmotors.com/OTA/sc126/ElectricBike/version.json";
    private Button lteToggle;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_os_updates, container, false);

        // Header UI
        otaupdatebtn = view.findViewById(R.id.otaupdatebtn);
       // mcuotaupdatebtn = view.findViewById(R.id.Fmcuotaupdatebtn);
        arrowImg = view.findViewById(R.id.arrowdown);
        serialPortManager = serialApplication.getSerialPortManager();

        // Expandable section
        layoutReleaseNotes = view.findViewById(R.id.layoutReleaseNotes);
        tvReleasePoints = view.findViewById(R.id.tvReleasePoints);
        arrowImg = view.findViewById(R.id.arrowdown);
        lteToggle = view.findViewById(R.id.ltetoggle);
//        AOSP_OTA_btn = view.findViewById(R.id.AOSP_OTA_btn);
        updateMobileDataUI();
        lteToggle.setOnClickListener(v -> toggleMobileData());
//        AOSP_OTA_btn.setOnClickListener(v -> {
//
//            InactivityManager.getInstance().stopTimer();
//
//            //  Internet check
//            if (!FrameUtils.isNetworkAvailable(requireActivity())) {
//                //Toast.makeText(getContext(), "No internet connection", Toast.LENGTH_SHORT).show();
//                showCustomToast("No internet connection");
//                return;
//            }
//
//            //  Disable button to avoid double clicks
//            AOSP_OTA_btn.setEnabled(false);
//
//            int localVersion = Integer.parseInt(Build.ID); // same as activity
//            String versionUrl = "http://ota.rivotmotors.com/OTA/sc126/ElectricBike/version.json";
//
//            // 3️ Lightweight OTA check
//            aosp_ota_update_check.checkAospUpdate(versionUrl, localVersion,
//                    new aosp_ota_update_check.Callback() {
//
//                        @Override
//                        public void onResult(boolean updateAvailable) {
//                            if (!isAdded()) return;
//
//                            requireActivity().runOnUiThread(() -> {
//                                AOSP_OTA_btn.setEnabled(true);
//
//                                if (updateAvailable) {
//                                    // ✅ OPEN activity only if update exists
//                                    Intent intent = new Intent(requireActivity(), aosp_mainActivity.class);
//                                    startActivity(intent);
//                                } else {
////                                    // ❌ Stay in fragment
////                                    Toast.makeText(
////                                            getContext(),
////                                            "Software is already up to date",
////                                            Toast.LENGTH_SHORT
////                                    ).show();
//                                    showCustomToast("Software is already up to date");
//
//                                }
//                            });
//                        }
//
//                        @Override
//                        public void onError() {
//                            if (!isAdded()) return;
//
//                            requireActivity().runOnUiThread(() -> {
//                                AOSP_OTA_btn.setEnabled(true);
////                                Toast.makeText(
////                                        getContext(),
////                                        "Server error.Please try again.",
////                                        Toast.LENGTH_SHORT
////                                ).show();
//                                showCustomToast("Server error.Please try again");
//
//                            });
//                        }
//                    });
//        });

        // Reset state every time fragment opens
        layoutReleaseNotes.setVisibility(View.GONE);
        isExpanded = false;

        // Other UI
        tvUptime = view.findViewById(R.id.tvUptime);
        ctx = requireContext();

        updateVersionInfo(view);
        fetchServerUpdateInfo(view);
        startUptimeTimer();

        otaupdatebtn.setOnClickListener(v -> {

            InactivityManager.getInstance().stopTimer();

            // 1️ Internet check
            if (!FrameUtils.isNetworkAvailable(requireActivity())) {
//                Toast.makeText(getContext(), "No internet connection", Toast.LENGTH_SHORT).show();
                showCustomToast("No internet connection");

                return;
            }

            // 2️ Disable button (avoid double clicks)
            otaupdatebtn.setEnabled(false);

            // 3️ Check AOSP version FIRST
            int localAospVersion = Integer.parseInt(Build.ID);
            String versionUrl = "http://ota.rivotmotors.com/OTA/sc126/ElectricBike/version.json";

            aosp_ota_update_check.checkAospUpdate(
                    versionUrl,
                    localAospVersion,
                    new aosp_ota_update_check.Callback() {

                        @Override
                        public void onResult(boolean aospUpdateAvailable) {
                            if (!isAdded()) return;

                            requireActivity().runOnUiThread(() -> {

                                //  CASE 1: AOSP update available → STOP HERE
                                if (aospUpdateAvailable) {
                                    otaupdatebtn.setEnabled(true);
                                    InactivityManager.getInstance().stopTimer();

                                    Intent intent = new Intent(requireActivity(), aosp_mainActivity.class);
                                    intent.putExtra(aosp_mainActivity.EXTRA_SILENT_OTA, false);  //  explicit manual mode
                                    startActivity(intent);

                                    return; //  Do NOT check app OTA
                                }

                                // 🟢 CASE 2: AOSP up to date → check APP OTA
                                checkAppUpdate();
                            });
                        }

                        @Override
                        public void onError() {
                            if (!isAdded()) return;

                            requireActivity().runOnUiThread(() -> {
                                otaupdatebtn.setEnabled(true);
//                                Toast.makeText(
//                                        getContext(),
//                                        "Server error. Please try again.",
//                                        Toast.LENGTH_SHORT
//                                ).show();
                                checkAppUpdate();
                                showCustomToast("Server error. Please try again.");
                            });
                        }
                    }
            );
        });



//        mcuotaupdatebtn.setOnClickListener(v -> {
//
//            // 1️⃣ Internet check (Activity required)
//            if (!FrameUtils.isNetworkAvailable(requireActivity())) {
//                showCustomToast("No internet connection");
//                return;
//            }
//
//            new Thread(() -> {
//
//                int serverVersion = fetchServerVersion(version_URL);
//                int localVersion  = getEffectiveLocalMcuVersion();
//
//                Log.d("OTA", "Server version = " + serverVersion);
//                Log.d("OTA", "Local version  = " + localVersion);
//
//                // 2️⃣ FINAL decision (no mutation later)
//                final boolean updateRequired =
//                        serverVersion > 0 &&
//                                serverVersion > localVersion;
//
//                if (!isAdded()) return;
//
//                // 3️⃣ UI thread
//                requireActivity().runOnUiThread(() -> {
//                    if (updateRequired) {
//                        forceStopSelfAndLaunchOther();
//                    } else {
//                        showCustomToast("No update available");
//                    }
//                });
//
//            }).start();
//        });


        layoutReleaseNotes = view.findViewById(R.id.layoutReleaseNotes);
        tvReleasePoints = view.findViewById(R.id.tvReleasePoints);

        layoutReleaseNotes.setVisibility(View.GONE); // default state
        isExpanded = false;
        arrowImg.setOnClickListener(v -> toggleReleaseNotes());


        return view;
    }

    private void checkAppUpdate() {

        otaupdatebtn.setEnabled(true);

        Intent intent =
                new Intent(
                        getContext(),
                        UpdateService.class
                );

        intent.putExtra(
                "manual_check",
                true
        );

        requireContext().startService(intent);
    }

    private void forceStopSelfAndLaunchOther() {

        if (!isAdded()) return; // Fragment safety

        Context context = requireContext();
        String myPkg = context.getPackageName();
        String targetPkg = "com.fanconn.rideOSfw";

        try {
            // 1️⃣ Launch other app FIRST
            Intent intent = context.getPackageManager()
                    .getLaunchIntentForPackage(targetPkg);

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } else {
              //  Log.e("OTA", "Target app not found: " + targetPkg);
                return;
            }

            // 2️⃣ Force-stop THIS app
            Runtime.getRuntime().exec(new String[]{
                    "sh", "-c", "am force-stop " + myPkg
            });

           // Log.i("OTA", "Force-stopped self and launched target app");

        } catch (Exception e) {
          //  Log.e("OTA", "Failed to force stop and launch", e);
        }
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

                String savedBranch =
                        OtaBranchManager.getBranch(requireContext());

                final String branch =
                        savedBranch == null
                                ? UpdateService.BRANCH_PRODUCTION
                                : savedBranch;

                String jsonUrl =
                        UpdateService.BRANCH_RND.equals(branch)
                                ? UpdateService.BETA_UPDATE_URL
                                : UpdateService.PROD_UPDATE_URL;

                URL url = new URL(jsonUrl);

                HttpURLConnection conn =
                        (HttpURLConnection) url.openConnection();

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
                        String label =
                                UpdateService.BRANCH_RND.equals(branch)
                                        ? "Latest beta version "
                                        : "Latest production version ";

                        serverVersionTV.setText(
                                label + latestVersion
                        );
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
    private static final int REQUIRED_TAPS = 10;
    private static final long TAP_TIMEOUT_MS = 1500; // 1.2 sec max gap

    private int buildTapCount = 0;
    private long lastTapTime = 0;
    private static final String TAG = "HiddenMcuOta";

    private void updateVersionInfo(View view) {

      //  Log.d(TAG, "updateVersionInfo() called");

        TextView versionText = view.findViewById(R.id.version);
        TextView installedDateTV = view.findViewById(R.id.installeddate);
        TextView buildNumTV = view.findViewById(R.id.buildnum);
        TextView deviceNameTV = view.findViewById(R.id.devicenameTV);
        //TextView bluetoothTV = view.findViewById(R.id.bluetoothTV);
        TextView firmwareV = view.findViewById(R.id.firmwareV);
        int localVersion =
                McuOtaExecutor.getEffectiveLocalMcuVersion(ctx);
        firmwareV.setText(localVersion+"_GA");

        String versionName = "N/A";
        long updateTime = 0;

        try {
            PackageInfo pInfo = requireContext()
                    .getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0);

            versionName = pInfo.versionName;
            updateTime = pInfo.lastUpdateTime;

//            Log.d(TAG, "App version=" + versionName +
//                    " updateTime=" + updateTime);

        } catch (Exception e) {
           // Log.e(TAG, "Failed to read package info", e);
        }

        installedDateTV.setText(
                new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        .format(new Date(updateTime))
        );

        buildNumTV.setText(Build.DISPLAY+"_GA");
        versionText.setText("rideOS " + versionName);

        //Log.d(TAG, "Build number displayed: GA_" + Build.DISPLAY);

        //  Hidden 7-tap activation
        buildNumTV.setOnClickListener(v -> {
         //   Log.d(TAG, "Build number tapped");
            handleBuildNumberTap();
        });

        String deviceName = Settings.Global.getString(
                requireContext().getContentResolver(), "device_name"
        );

        if (deviceName == null) {
            deviceName = Build.DEVICE;
           // Log.w(TAG, "Device name not found in settings, using Build.DEVICE");
        }

        deviceNameTV.setText(deviceName);
       // Log.d(TAG, "Device name=" + deviceName);

        //String bluetoothName = "Unavailable";
//        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
//
//        if (adapter != null) {
//            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
//                    ContextCompat.checkSelfPermission(
//                            requireContext(),
//                            Manifest.permission.BLUETOOTH_CONNECT
//                    ) == PackageManager.PERMISSION_GRANTED) {
//
//                bluetoothName = adapter.getName();
//                Log.d(TAG, "Bluetooth name=" + bluetoothName);
//            } else {
//                Log.w(TAG, "Bluetooth permission not granted");
//            }
//        } else {
//            Log.w(TAG, "Bluetooth adapter is null");
//        }
//
//        bluetoothTV.setText(bluetoothName);
    }

    private void triggerHiddenMcuOta() {

       // Log.i(TAG, "triggerHiddenMcuOta() started");

        if (!FrameUtils.isNetworkAvailable(requireActivity())) {
           // Log.e(TAG, "No internet connection → aborting MCU OTA");
            showCustomToast("No internet connection");
            return;
        }

       // Log.d(TAG, "Internet available, checking versions");

        new Thread(() -> {

            int serverVersion = fetchServerVersion(version_URL);
            int localVersion  = getEffectiveLocalMcuVersion();

            Log.d(TAG, "MCU Server version=" + serverVersion);
            Log.d(TAG, "MCU Local version=" + localVersion);

            final boolean updateRequired =
                    serverVersion > 0 &&
                            serverVersion > localVersion;

          //  Log.d(TAG, "MCU updateRequired=" + updateRequired);

            if (!isAdded()) {
              // Log.w(TAG, "Fragment not attached, aborting UI update");
                return;
            }

            requireActivity().runOnUiThread(() -> {
                if (updateRequired) {
                   // Log.i(TAG, "MCU update REQUIRED → launching updater");
                    forceStopSelfAndLaunchOther();
                } else {
                  //  Log.i(TAG, "MCU already up-to-date");
                    showCustomToast("No update available");
                }
            });

        }).start();
    }


    // ---------------- CLEANUP ----------------
    private void handleBuildNumberTap() {

        long now = System.currentTimeMillis();

        // Reset if delayed
        if (now - lastTapTime > TAP_TIMEOUT_MS) {
            //Log.w(TAG, "Tap timeout exceeded → resetting tap counter");
            buildTapCount = 0;
        }

        lastTapTime = now;
        buildTapCount++;

        //Log.d(TAG, "Build taps=" + buildTapCount + "/" + REQUIRED_TAPS);

        if (buildTapCount < REQUIRED_TAPS) {
            //showCustomToast("Tap " + (REQUIRED_TAPS - buildTapCount) + " more times");
            return;
        }

        //Activation reached
       // Log.i(TAG, "Hidden MCU OTA mode ACTIVATED");

        buildTapCount = 0;
        lastTapTime = 0;

        //showCustomToast("Update mode enabled");
        triggerHiddenMcuOta();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        uptimeHandler.removeCallbacks(uptimeRunnable);
    }
    private static final String PREFS_OTA = "ota_prefs";
    private static final String KEY_LAST_MCU_VERSION = "last_mcu_version";

    private void saveLastMcuVersion(int version) {
        if (version <= 0) return;

        requireContext()
                .getSharedPreferences(PREFS_OTA, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_LAST_MCU_VERSION, version)
                .apply();
    }

    private int getLastSavedMcuVersion() {
        return requireContext()
                .getSharedPreferences(PREFS_OTA, Context.MODE_PRIVATE)
                .getInt(KEY_LAST_MCU_VERSION, 0);
    }
    private int getEffectiveLocalMcuVersion() {
        int liveVersion = 0;

        try {
            if (serialPortManager != null && serialPortManager.mad != null) {
                liveVersion = serialPortManager.mad.getMcuVersions();
            }
        } catch (Exception e) {
           // Log.e("OTA", "MCU version read failed", e);
        }

        if (liveVersion > 0) {
            saveLastMcuVersion(liveVersion);
           // Log.d("OTA", "LIVE MCU version used: " + liveVersion);
            return liveVersion;
        }

        int storedVersion = getLastSavedMcuVersion();
       // Log.w("OTA", "MCU invalid, fallback to stored version: " + storedVersion);
        return storedVersion;
    }
    public static int fetchServerVersion(String versionUrl) {
        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            URL url = new URL(versionUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
               // Log.e("OTA", "HTTP error: " + connection.getResponseCode());
                return -1;
            }

            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder json = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                json.append(line);
            }

            // Example version.json:
            // { "version": 12, "url": "xxx.bin" }
            JSONObject obj = new JSONObject(json.toString());
            return obj.getInt("version");

        } catch (Exception e) {
           // Log.e("OTA", "Failed to fetch version", e);
            return -1;
        } finally {
            try {
                if (reader != null) reader.close();
            } catch (Exception ignored) {}
            if (connection != null) connection.disconnect();
        }
    }
    private boolean isUpdateRequired(int serverVersion) {

        int localVersion = getEffectiveLocalMcuVersion();

      //  Log.d("OTA", "Server version = " + serverVersion);
       // Log.d("OTA", "Local version  = " + localVersion);
        if (serverVersion <= 0 || localVersion <= 0) {
           // Log.w("OTA", "Invalid version detected, update blocked");
            return false;
        }

        boolean updateRequired = serverVersion > localVersion;

      //  Log.d("OTA", "Update required = " + updateRequired);
        return updateRequired;
    }

    private void showCustomToast(String message) {
        View layout = LayoutInflater.from(ctx).inflate(R.layout.toast_custom, null);
        ((ImageView) layout.findViewById(R.id.toast_icon)).setImageResource(R.drawable.toastlogo);
        ((TextView) layout.findViewById(R.id.toast_text)).setText(message);
        Toast t = new Toast(ctx);
        t.setDuration(Toast.LENGTH_SHORT);
        t.setView(layout);
        t.show();
    }
    private void toggleMobileData() {
        if (!hasSimCard()) {
            showCustomToast("No SIM card detected");
            //Toast.makeText(this, "No SIM card detected", Toast.LENGTH_SHORT).show();
            return;
        }
        setMobileDataEnabled(!getMobileDataState());
        updateMobileDataUI();
    }

    private void updateMobileDataUI() {
        int on = Color.parseColor("#0283FF");
        int off = Color.parseColor("#555555");

        boolean enabled = getMobileDataState();
        lteToggle.setBackgroundTintList(ColorStateList.valueOf(enabled ? on : off));
        lteToggle.setText(enabled ? "Data ON" : "Data OFF");
    }

    private boolean hasSimCard() {
        TelephonyManager tm =
                (TelephonyManager) requireContext().getSystemService(Context.TELEPHONY_SERVICE);
        return tm != null && tm.getSimState() == TelephonyManager.SIM_STATE_READY;
    }


    private boolean getMobileDataState() {
        try {
            TelephonyManager tm =
                    (TelephonyManager) requireContext().getSystemService(Context.TELEPHONY_SERVICE);
            Method m = TelephonyManager.class.getDeclaredMethod("getDataEnabled");
            m.setAccessible(true);
            return (boolean) m.invoke(tm);
        } catch (Exception e) {
            return false;
        }
    }


    private void setMobileDataEnabled(boolean enabled) {
        try {
            TelephonyManager tm =
                    (TelephonyManager) requireContext().getSystemService(Context.TELEPHONY_SERVICE);
            Method m = TelephonyManager.class.getDeclaredMethod("setDataEnabled", boolean.class);
            m.setAccessible(true);
            m.invoke(tm, enabled);
        } catch (Exception e) {
            Toast.makeText(requireContext(),
                    "Requires system privileges", Toast.LENGTH_SHORT).show();
        }
    }



}
