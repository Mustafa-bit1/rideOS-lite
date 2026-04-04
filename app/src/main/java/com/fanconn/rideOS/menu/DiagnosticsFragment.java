package com.fanconn.rideOS.menu;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.fanconn.rideOS.*;
import com.fanconn.rideOS.authen.LoginActivity;
import com.fanconn.rideOS.mcuupdate.FileDownloader;
import com.fanconn.rideOS.mcuupdate.YMode;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DiagnosticsFragment extends Fragment {
    private AhTrackerManager ahTrackerManager;
    private TextView ipTV;
    private Button resetRecoBtn, resetChargerRegenBtn;
    private Button resetOdoBtn;
    private Button bootbutton, handlelockbutton, handleUNlockbutton,  lockScreen, ToAPU, ToMain, LEDON,In60,In90,out0, LEDOFF, debugbtn,lowtorue,highTorque;
    private ImageButton  rebootbtn, shutdownbtn;
    private Button wifiBtn, wifiOffBtn, Devicesettings;
    private TextView tireDimensionDisplay;
    private ProgressDialog progressDialog;
    private SerialPortManager serialPortManager;
    private YMode yMode;
    private File mFirmwareFile;
    private String version_URL = "http://ota.rivotmotors.com/OTA/sc126/ElectricBike/version.json";
    private String saveDir = "/sdcard";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_diagnostic_fragment, container, false);
        serialPortManager = serialApplication.getSerialPortManager();
        yMode = new YMode(requireContext());

        initViews(view);
        initListeners(view);
        updateDisplay(view);
        updateVersionInfo(view);
        String ip = getDeviceIpAddress();
        ipTV.setText("Board ID: " + ip);

        view.findViewById(R.id.btnmetadata).setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), MetadataActivity.class);
            startActivity(intent);
        });
        ahTrackerManager = AhTrackerManager.getInstance(requireContext());
        InactivityManager.getInstance().stopTimer();

        return view;

    }

    private void initViews(View view) {
        bootbutton = view.findViewById(R.id.bootbutton);
        highTorque = view.findViewById(R.id.hightorque);
        lowtorue = view.findViewById(R.id.Low);
        handlelockbutton = view.findViewById(R.id.handlelockbutton);
        handleUNlockbutton = view.findViewById(R.id.handleUNlockbutton);
        lockScreen = view.findViewById(R.id.lockScreen);
        ToAPU = view.findViewById(R.id.ToAPU);
        ToMain = view.findViewById(R.id.ToMain);
        LEDON = view.findViewById(R.id.LEDON);
        LEDOFF = view.findViewById(R.id.LEDOFF);
        In60 = view.findViewById(R.id.in60);
        In90 = view.findViewById(R.id.in90);
        out0 = view.findViewById(R.id.out0);
        rebootbtn = view.findViewById(R.id.rebootbtn);
        shutdownbtn = view.findViewById(R.id.shutdownbtn);
        wifiBtn = view.findViewById(R.id.wifiBtn);
        wifiOffBtn = view.findViewById(R.id.wifiOffBtn);
        Devicesettings = view.findViewById(R.id.Devicesettings);
        debugbtn = view.findViewById(R.id.debug);
        tireDimensionDisplay = view.findViewById(R.id.tireDimensionDisplay);
        ipTV = view.findViewById(R.id.ipTV);
        resetRecoBtn = view.findViewById(R.id.resetRecoBtn);
        resetChargerRegenBtn = view.findViewById(R.id.resetChargerRegenBtn);
        resetOdoBtn = view.findViewById(R.id.resetOdoBtn);
    }
    private void initListeners(View view) {
        WifiManager wifiManager = (WifiManager) requireContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        view.findViewById(R.id.mcuotaupdatebtn).setOnClickListener(v -> MCUOTA_Onclick(v));

        bootbutton.setOnClickListener(v -> serialPortManager.VCUSendToMCU("FFAA550102000A01"));

//        bootbutton.setOnClickListener(v -> serialPortManager.VCUSendToMCU("FFAA5501030015160000800200000000000000"));
        handlelockbutton.setOnClickListener(v -> serialPortManager.VCUSendToMCU("FFAA550101000A00"));
        handleUNlockbutton.setOnClickListener(v -> serialPortManager.VCUSendToMCU("FFAA550101000A01"));
        view.findViewById(R.id.resetAhvalues).setOnClickListener(v -> {
            // Reset AH counters
            ahTrackerManager.resetAll();

        });
        highTorque.setOnClickListener(v -> {
            serialPortManager.VCUSendToMCU("FFAA550103001510FBA807B8018C00B004B400");//FFAA550103001510FBA807B8018C000000B400 35 - 45 regen(8C00-B400 , phase cur 300A(B004) , line current 110A(B801) ,
            new Handler(Looper.getMainLooper()).postDelayed(
                    () -> serialPortManager.VCUSendToMCU("FFAA550103001510FCA807100E00000000051E"),
                    1200
            );
            new Handler(Looper.getMainLooper()).postDelayed(
                    () -> serialPortManager.VCUSendToMCU("FFAA550103001510F8A8070000000000007387"),
                    1200
            );
        });
        lowtorue.setOnClickListener(v -> {
            serialPortManager.VCUSendToMCU("FFAA550103001510FBA8073C008C00B004B400");//FFAA550103001510FBA807B8018C000000B400 35 - 45 regen(8C00-B400 , phase cur 300A(B004) , line current 15A(3C00) ,
            new Handler(Looper.getMainLooper()).postDelayed(
                    () -> serialPortManager.VCUSendToMCU("FFAA550103001510FCA807100E00000000051E"),
                    1200
            );
            new Handler(Looper.getMainLooper()).postDelayed(
                    () -> serialPortManager.VCUSendToMCU("FFAA550103001510F8A8070000000000007387"),
                    1200
            );
        });

        resetRecoBtn.setOnClickListener(v -> {
            View dialogView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.popupdialog, null);

            AlertDialog dialog = createDialog(
                    dialogView,
                    "Reset recoEngine",
                    "Are you sure you want to reset all recoEngine parameters?",
                    "Reset",
                    () -> {
                        if (UIUpdater.getInstance() != null) {
                            UIUpdater.getInstance().resetAllRecoEngineParameters();
                            showCustomToast("All recoEngine params reset");
                        }
                    }
            );
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.show();
        });

        resetChargerRegenBtn.setOnClickListener(v -> {
            View dialogView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.popupdialog, null);

            AlertDialog dialog = createDialog(
                    dialogView,
                    "Reset AH Stats",
                    "Are you sure you want to reset charger and total regen Ah?",
                    "Reset",
                    () -> {
                        if (UIUpdater.getInstance() != null) {
                            UIUpdater.getInstance().resetChargerAndTotalRegenAh();
                            showCustomToast("AH reset");
                        }
                    }
            );

            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.show();
        });

        resetOdoBtn.setOnClickListener(v -> {
            View dialogView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.popupdialog, null);

            AlertDialog dialog = createDialog(
                    dialogView,
                    "Reset ODO Meter",
                    "Are you sure you want to reset the ODO meter?",
                    "Reset",
                    () -> {
                        if (UIUpdater.getInstance() != null &&
                                UIUpdater.getInstance().tripManager != null) {

                            UIUpdater.getInstance().tripManager.resetOdo();

                            showCustomToast("ODO meter reset");
                        }
                    }
            );

            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.show();
        });
//        highTorque.setOnClickListener(v -> {
//            serialPortManager.VCUSendToMCU("FFAA5501030015189040010000000000000064");//FFAA550103001510FBA807B8018C000000B400 35 - 45 regen(8C00-B400 , phase cur 300A(B004) , line current 110A(B801) ,
//            new Handler(Looper.getMainLooper()).postDelayed(
//                    () -> serialPortManager.VCUSendToMCU("FFAA5501030015189840010000000000000000"),
//                    1000
//            );
//            new Handler(Looper.getMainLooper()).postDelayed(
//                    () -> serialPortManager.VCUSendToMCU("FFAA5501030015189040010000000000000064"),
//                    2000
//            );
//            new Handler(Looper.getMainLooper()).postDelayed(
//                    () -> serialPortManager.VCUSendToMCU("FFAA5501030015189840010000000000000000"),
//                    2500
//            );
//        });

        lockScreen.setOnClickListener(v -> {
            // Start lock
            EVLockService.getInstance().startLockSequence();

            // Pop this fragment (remove from backstack if needed)
            requireActivity().getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

            // Start LoginActivity
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            requireContext().startActivity(intent);
        });


        ToAPU.setOnClickListener(v -> new batteryPackSwitcher().startSwitchingToAPU());
        ToMain.setOnClickListener(v -> new batteryPackSwitcher().startSwitchingToNormalState());

        LEDON.setOnClickListener(v -> serialPortManager.VCUSendToMCU("FFAA550107000A01"));
        LEDOFF.setOnClickListener(v -> serialPortManager.VCUSendToMCU("FFAA550107000A00"));
        In60.setOnClickListener(v -> serialPortManager.VCUSendToMCU("FFAA5501030015160A01800000EA6000000000"));
        In90.setOnClickListener(v -> serialPortManager.VCUSendToMCU("FFAA5501030015160A018000015F9000000000"));
        out0.setOnClickListener(v -> serialPortManager.VCUSendToMCU("FFAA5501030015160C01800000000000000000"));
        wifiBtn.setOnClickListener(v -> {
            if (!wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(true);
                showCustomToast("WiFi ON");
            } else {
                showCustomToast("WiFi already ON");
            }
        });

        wifiOffBtn.setOnClickListener(v -> {
            if (wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(false);
                showCustomToast("WiFi OFF");
            } else {
                showCustomToast("WiFi already OFF");
            }
        });

        Devicesettings.setOnClickListener(v -> showPasswordDialog());
        debugbtn.setOnClickListener(v -> {
            InactivityManager.getInstance().stopTimer();
            startActivity(new Intent(requireContext(), debugPage.class));
        });


        view.findViewById(R.id.btnIncrease).setOnClickListener(v -> {
            UIUpdater.tiredimensions += 0.05;
            updateDisplay(view);
        });
        view.findViewById(R.id.btnDecrease).setOnClickListener(v -> {
            UIUpdater.tiredimensions -= 0.05;
            updateDisplay(view);
        });

        rebootbtn.setOnClickListener(v -> showRebootDialog());
        shutdownbtn.setOnClickListener(v -> showShutdownDialog());
    }//END OF onCreateView

    private String getDeviceIpAddress() {
        ConnectivityManager cm =
                (ConnectivityManager) requireContext()
                        .getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) return "No Network";

        Network network = cm.getActiveNetwork();
        if (network == null) return "Disconnected";

        LinkProperties props = cm.getLinkProperties(network);
        if (props == null) return "No IP";

        for (LinkAddress linkAddress : props.getLinkAddresses()) {
            InetAddress addr = linkAddress.getAddress();
            if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                return addr.getHostAddress();
            }
        }
        return "IP Not Found";
    }

    // ------------------ HELPER: SEND CMD TWICE THEN CALLBACK ------------------
    private void sendCommandTwiceThen(Runnable afterTask) {

        if (serialPortManager == null) {
            Log.e("UART", "SerialPortManager is NULL");
            afterTask.run();
            return;
        }

        Handler handler = new Handler(Looper.getMainLooper());

        // Send 1st time
        serialPortManager.VCUSendToMCU("FFAA550107000A00");

        // Send 2nd time after 1 second
        handler.postDelayed(() -> {

            serialPortManager.VCUSendToMCU("FFAA550107000A00");

            // After another 1 sec → execute final task (reboot/shutdown)
            handler.postDelayed(afterTask, 1000);

        }, 1000);
    }
// ---------------------------------------------------------------------------

    private void showRebootDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.popupdialog, null);

        AlertDialog dialog = createDialog(dialogView,
                "Confirm Restart",
                "Are you sure you want to Restart the device?",
                "Restart",
                () -> sendCommandTwiceThen(() -> {
                    PowerManager pm = (PowerManager) requireContext().getSystemService(Context.POWER_SERVICE);
                    if (pm != null) pm.reboot(null);
                })
        );
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        dialog.show();
    }



    private void showShutdownDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.popupdialog, null);

        AlertDialog dialog = createDialog(dialogView,
                "Confirm Shutdown",
                "Are you sure you want to shut down the device?",
                "Shutdown",
                () -> sendCommandTwiceThen(() -> {
                    try {
                        PowerManager pm = (PowerManager) requireContext().getSystemService(Context.POWER_SERVICE);
                        if (pm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            Method method = PowerManager.class.getMethod(
                                    "shutdown",
                                    boolean.class,
                                    String.class,
                                    boolean.class
                            );
                            method.invoke(pm, false, null, false);
                        } else {
                            pm.reboot("shutdown");
                        }
                    } catch (Exception e) {
                        Log.e("Shutdown", "Shutdown failed", e);
                    }
                })
        );
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        dialog.show();
    }



    // ------------------ COMMON DIALOG CODE ------------------
    private AlertDialog createDialog(View dialogView, String titleText, String messageText, String positiveText, Runnable positiveAction) {

        TextView title = dialogView.findViewById(R.id.dialogTitle);
        TextView message = dialogView.findViewById(R.id.dialogMessage);
        Button btnYes = dialogView.findViewById(R.id.btnYes);
        Button btnNo = dialogView.findViewById(R.id.btnNo);

        title.setText(titleText);
        message.setText(messageText);
        btnYes.setText(positiveText);
        btnNo.setText("Cancel");

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
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




    private void showPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Admin");
        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);
        builder.setPositiveButton("OK", (dialog, which) -> {
            if (input.getText().toString().equals("nx")) {
                Intent intent = new Intent(Settings.ACTION_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } else {
                showCustomToast("Access denied");
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void updateDisplay(View view) {
        tireDimensionDisplay.setText(String.format(Locale.US, "%.3f", UIUpdater.tiredimensions));
    }

    private void updateVersionInfo(View view) {
        TextView versionText = view.findViewById(R.id.rideOS_version);
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
        String buildNumber = Build.DISPLAY;
        String versionInfo = "rideOS " + versionName + "   " + installDate + "\nBuild : " + buildNumber;
        versionText.setText(versionInfo);
    }

    private void showCustomToast(String message) {
        View layout = getLayoutInflater().inflate(R.layout.toast_custom, null);
        ImageView icon = layout.findViewById(R.id.toast_icon);
        TextView text = layout.findViewById(R.id.toast_text);
        icon.setImageResource(R.drawable.toastlogo);
        text.setText(message);
        Toast toast = new Toast(requireContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }

    public void MCUOTA_Onclick(View view) {
        if (FrameUtils.isNetworkAvailable(requireActivity())) {
            new FetchVersionTask().execute(version_URL);
        } else {
            showCustomToast("No network!");
        }
    }

    private class FetchVersionTask extends AsyncTask<String, Void, Integer> {
        @Override
        protected Integer doInBackground(String... urls) {
            try {
                return FileDownloader.fetchVersion(urls[0]);
            } catch (IOException e) {
                return -1;
            }
        }

        @Override
        protected void onPostExecute(Integer mcuOtaVer_new) {
            int mcuOtaVer_local = serialPortManager.mad.getMcuVersions();
            if (mcuOtaVer_new != -1) {
                if (mcuOtaVer_new > mcuOtaVer_local) {
                    showUpdateDialog();
                } else {
                    showCustomToast("You are running on the latest version.");
                }
            } else {
                showCustomToast("Failed to fetch new MCU version!");
            }
        }
    }

    private void showUpdateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Firmware Update Available")
                .setMessage("A new version is available. Do you want to update?")
                .setPositiveButton("Update", (dialog, which) -> startFirmwareDownload())
                .setNegativeButton("Cancel", (dialog, which) -> showCustomToast("Update canceled."))
                .setCancelable(false)
                .show();
    }

    private void startFirmwareDownload() {
        new Thread(() -> {
            try {
                String fileUrl = FileDownloader.fetchDownloadUrl(version_URL);
                FileDownloader.downloadFile(fileUrl, saveDir);
                mFirmwareFile = new File(saveDir, fileUrl.substring(fileUrl.lastIndexOf('/') + 1));
                if (mFirmwareFile.exists()) {
                    requireActivity().runOnUiThread(() -> showProgressDialog());
                    yMode.send(mFirmwareFile, progress -> requireActivity().runOnUiThread(() -> updateProgressDialog(progress)));
                    requireActivity().runOnUiThread(() -> hideProgressDialog());
                } else {
                    showCustomToast("Firmware file does not exist!");
                }
            } catch (IOException | InterruptedException e) {
                showCustomToast("Firmware download failed!");
            }
        }).start();
    }

    private void showProgressDialog() {
        progressDialog = new ProgressDialog(requireContext());
        progressDialog.setTitle("Firmware Update");
        progressDialog.setMessage("Updating firmware...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(100);
        progressDialog.setProgress(0);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void updateProgressDialog(int progress) {
        if (progressDialog != null) progressDialog.setProgress(progress);
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
