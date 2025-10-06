//package com.fanconn.rideOS;
//
//import android.app.AlertDialog;
//import android.app.ProgressDialog;
//import android.content.Context;
//import android.content.DialogInterface;
//import android.content.Intent;
//import android.content.pm.PackageInfo;
//import android.content.pm.PackageManager;
//import android.net.wifi.WifiManager;
//import android.os.AsyncTask;
//import android.os.Build;
//import android.os.Bundle;
//import android.os.PowerManager;
//import android.provider.settings;
//import android.text.InputType;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.WindowManager;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.ImageButton;
//import android.widget.ImageView;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.activity.EdgeToEdge;
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.fanconn.rideOS.mcuupdate.FileDownloader;
//import com.fanconn.rideOS.mcuupdate.YMode;
//
//import java.io.File;
//import java.io.IOException;
//import java.lang.reflect.Method;
//import java.text.SimpleDateFormat;
//import java.util.Date;
//import java.util.Locale;
//
//public class menu extends AppCompatActivity {
//    public Button bootbutton, handlelockbutton, handleUNlockbutton, riderprofile,lockScreen,ToAPU,ToMain,LEDON,LEDOFF,debugbtn;
//    ImageButton returnbtn,rebootbtn,shutdownbtn;
//    SerialPortManager serialPortManager;
//    private MainActivity mainActivity;
//    public menu(){}
//    public menu(MainActivity mainActivity) {
//        this.mainActivity = mainActivity;
//    }
////    private boolean isSequenceActive = false;
////    private final int MAX_RETRIES = 20;
////    private int retryCount1 = 0;
////    private int retryCount2 = 0;
//private Button wifiBtn, wifiOffBtn,Devicesettings;
//    private WifiManager wifiManager;
//    private File mFirmwareFile;
//    private YMode yMode;
//    private ProgressDialog progressDialog;
//    private String version_URL = "http://ota.rivotmotors.com/OTA/sc126/ElectricBike/version.json";
//    private String saveDir = "/sdcard";
//    private TextView tireDimensionDisplay;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_menu);
//        setFullScreen();
//        serialPortManager = serialApplication.getSerialPortManager();
//        batteryPackSwitcher BatPackSwitcher = new batteryPackSwitcher();
//        yMode = new YMode(this);
//        returnbtn=findViewById(R.id.returnbtn);
//        returnbtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
////                InactivityManager.getInstance().startTimer(1);
//                finish();
//            }
//        });
//
//        riderprofile = findViewById(R.id.riderprofile);
//        bootbutton = findViewById(R.id.bootbutton);
//        lockScreen = findViewById(R.id.lockScreen);
//        ToAPU=findViewById(R.id.ToAPU);
//        ToMain=findViewById(R.id.ToMain);
//        LEDON=findViewById(R.id.LEDON);
//        LEDOFF=findViewById(R.id.LEDOFF);
//        rebootbtn=findViewById(R.id.rebootbtn);
//        shutdownbtn=findViewById(R.id.shutdownbtn);
//        debugbtn=findViewById(R.id.debug);
//
//
//        bootbutton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                serialPortManager.VCUSendToMCU("FFAA550102000A01");
//                //serialPortManager.VCUSendToMCU("FFAA55010300150400FF800001020304050607");
//            }
//        });
//        handlelockbutton = findViewById(R.id.handlelockbutton);
//        handlelockbutton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                serialPortManager.VCUSendToMCU("FFAA550101000A00");
//            }
//        });
//        handleUNlockbutton = findViewById(R.id.handleUNlockbutton);
//        handleUNlockbutton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                serialPortManager.VCUSendToMCU("FFAA550101000A01");
//            }
//        });
//
//        riderprofile.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(menu.this, RiderProfile.class);
//                InactivityManager.getInstance().stopTimer();
//                startActivity(intent);
//            }
//        });
//
//        lockScreen.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent("com.ev.LOCK");
//                sendBroadcast(intent); // This finishes MainActivity
//                finish(); // This finishes SettingsActivity
//            }
//        });
//        ToAPU.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                BatPackSwitcher.startSwitchingToAPU();
////                //serialPortManager.VCUSendToMCU("FFAA5501030015162A02800101020304050607");
//            }
//        });
//        ToMain.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                BatPackSwitcher.startSwitchingToNormalState();
////                //serialPortManager.VCUSendToMCU("FFAA5501030015162A02800101020304050607");
//            }
//        });
//
//        LEDON.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                serialPortManager.VCUSendToMCU("FFAA550107000A01");
//            }
//        });
//        LEDOFF.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                serialPortManager.VCUSendToMCU("FFAA550107000A00");
//            }
//        });
//        wifiBtn = findViewById(R.id.wifiBtn);
//        wifiOffBtn = findViewById(R.id.wifiOffBtn);
//
//        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
//
//        wifiBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (!wifiManager.isWifiEnabled()) {
//                    wifiManager.setWifiEnabled(true);
////                    Toast.makeText(menu.this, "WiFi ON", Toast.LENGTH_SHORT).show();
//                    showCustomToast("WiFi ON");
//                } else {
////                    Toast.makeText(menu.this, "WiFi already ON", Toast.LENGTH_SHORT).show();
//                    showCustomToast("WiFi already ON");
//                }
//            }
//        });
//
//        wifiOffBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (wifiManager.isWifiEnabled()) {
//                    wifiManager.setWifiEnabled(false);
////                    Toast.makeText(menu.this, "WiFi OFF", Toast.LENGTH_SHORT).show();
//                    showCustomToast("WiFi OFF");
//                } else {
////                    Toast.makeText(menu.this, "WiFi already OFF", Toast.LENGTH_SHORT).show();
//                    showCustomToast("WiFi already OFF");
//                }
//            }
//        });
//        Devicesettings = findViewById(R.id.Devicesettings);
//
//        Devicesettings.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                showPasswordDialog();
//
//            }
//        });
//        debugbtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(menu.this, debugPage.class);
//                startActivity(intent);
//            }
//        });
//
//        rebootbtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // Inflate your existing custom dialog layout
//                LayoutInflater inflater = LayoutInflater.from(menu.this);
//                View dialogView = inflater.inflate(R.layout.popupdialog, null);
//
//                // Initialize views inside the dialog
//                TextView title = dialogView.findViewById(R.id.dialogTitle);
//                TextView message = dialogView.findViewById(R.id.dialogMessage);
//                Button btnYes = dialogView.findViewById(R.id.btnYes);
//                Button btnNo = dialogView.findViewById(R.id.btnNo);
//
//                // Customize content
//                title.setText("Confirm Restart");
//                message.setText("Are you sure you want to Restart the device?");
//                btnYes.setText("Restart");
//                btnNo.setText("Cancel");
//
//                // Create and show the dialog
//                AlertDialog dialog = new AlertDialog.Builder(menu.this)
//                        .setView(dialogView)
//                        .setCancelable(false)
//                        .create();
//
//                // Handle Reboot
//                btnYes.setOnClickListener(view -> {
//                    try {
//                        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
//                        if (powerManager != null) {
//                            powerManager.reboot(null); // reboot normally
//                        }
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                        Toast.makeText(menu.this, "Reboot failed", Toast.LENGTH_SHORT).show();
//                    }
//                    dialog.dismiss();
//                });
//
//                // Cancel button
//                btnNo.setOnClickListener(view -> dialog.dismiss());
//
//                dialog.show();
//            }
//        });
//
//
//
//        shutdownbtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // Inflate your custom dialog layout
//                LayoutInflater inflater = LayoutInflater.from(menu.this);
//                View dialogView = inflater.inflate(R.layout.popupdialog, null);
//
//                // Access views in layout
//                TextView title = dialogView.findViewById(R.id.dialogTitle);
//                TextView message = dialogView.findViewById(R.id.dialogMessage);
//                Button btnYes = dialogView.findViewById(R.id.btnYes);
//                Button btnNo = dialogView.findViewById(R.id.btnNo);
//
//                // Customize text
//                title.setText("Confirm Shutdown");
//                message.setText("Are you sure you want to shut down the device?");
//                btnYes.setText("Shutdown");
//                btnNo.setText("Cancel");
//
//                // Create the dialog
//                AlertDialog dialog = new AlertDialog.Builder(menu.this)
//                        .setView(dialogView)
//                        .setCancelable(false)
//                        .create();
//
//                // Shutdown logic
//                btnYes.setOnClickListener(view -> {
//                    try {
//                        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
//                        if (powerManager == null) {
//                            Log.e("Shutdown", "PowerManager is null");
//                            Toast.makeText(menu.this, "Shutdown not supported", Toast.LENGTH_SHORT).show();
//                            dialog.dismiss();
//                            return;
//                        }
//
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                            try {
//                                Method shutdownMethod = PowerManager.class.getMethod(
//                                        "shutdown", boolean.class, String.class, boolean.class);
//                                shutdownMethod.invoke(powerManager, false, null, false);
//                                Log.d("Shutdown", "Shutdown triggered via reflection");
//                            } catch (NoSuchMethodException e) {
//                                Log.e("Shutdown", "shutdown() method not found", e);
//                                Toast.makeText(menu.this, "Shutdown method not supported", Toast.LENGTH_SHORT).show();
//                            } catch (Exception e) {
//                                Log.e("Shutdown", "Error invoking shutdown()", e);
//                                Toast.makeText(menu.this, "Shutdown failed", Toast.LENGTH_SHORT).show();
//                            }
//                        } else {
//                            try {
//                                powerManager.reboot("shutdown");
//                                Log.d("Shutdown", "Fallback reboot(\"shutdown\") triggered");
//                            } catch (Exception e) {
//                                Log.e("Shutdown", "Fallback reboot failed", e);
//                                Toast.makeText(menu.this, "Shutdown fallback failed", Toast.LENGTH_SHORT).show();
//                            }
//                        }
//
//                    } catch (Exception e) {
//                        Log.e("Shutdown", "Unexpected error", e);
//                        Toast.makeText(menu.this, "Unexpected shutdown error", Toast.LENGTH_SHORT).show();
//                    }
//                    dialog.dismiss();
//                });
//
//                btnNo.setOnClickListener(view -> dialog.dismiss());
//
//                dialog.show();
//            }
//        });
//
//
//
//        Button btnIncrease = findViewById(R.id.btnIncrease);
//        Button btnDecrease = findViewById(R.id.btnDecrease);
//        tireDimensionDisplay = findViewById(R.id.tireDimensionDisplay);
//
//        updateDisplay();
//
//        btnIncrease.setOnClickListener(v -> {
//            UIUpdater.tiredimensions += 0.05;
//            updateDisplay();
//        });
//
//        btnDecrease.setOnClickListener(v -> {
//            UIUpdater.tiredimensions -= 0.05;
//            updateDisplay();
//        });
//
//        TextView versionText = findViewById(R.id.rideOS_version);
//
//        // 1. Get App Version and Install/Update Date
//        String versionName = "N/A";
//        long updateTime = 0;
//        try {
//            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
//            versionName = pInfo.versionName;
//            updateTime = pInfo.lastUpdateTime; // or use pInfo.firstInstallTime
//        } catch (PackageManager.NameNotFoundException e) {
//            e.printStackTrace();
//        }
//
//        String installDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(updateTime));
//
//        // 2. Get Device Build Number
//        String buildNumber = Build.DISPLAY; // or Build.FINGERPRINT or Build.ID
//
//        // 3. Set Text Dynamically
//        String versionInfo = "rideOS " + versionName + "   " + installDate + "\nBuild : " + buildNumber;
//        versionText.setText(versionInfo);
//
//    }
//    private void showPasswordDialog() {
//        AlertDialog.Builder builder = new AlertDialog.Builder(menu.this);
//        builder.setTitle("Admin");
//
//        final EditText input = new EditText(menu.this);
//        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
//        builder.setView(input);
//
//        builder.setPositiveButton("OK", (dialog, which) -> {
//            String enteredPassword = input.getText().toString();
//            if (enteredPassword.equals("nx")) {
//                Intent intent = new Intent(settings.ACTION_SETTINGS);
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Optional but safe
//                startActivity(intent);
//            } else {
////                Toast.makeText(menu.this, "Incorrect password", Toast.LENGTH_SHORT).show();
//                showCustomToast("Access denied");
//            }
//        });
//
//        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
//
//        builder.show();
//    }
//
//    private void updateDisplay() {
//        tireDimensionDisplay.setText(String.format(Locale.US, "%.3f", UIUpdater.tiredimensions));
//    }
////    public void startSwitchingToAPU() {
////        isSequenceActive = true;
////        retryCount1 = 0;
////        retryCount2 = 0;
////        apuDisON();
////    }
////
////    // ------------------ STEP 1 -------------------
////    private void apuDisON() {
////        if (!isSequenceActive) return;
////
////        serialPortManager.VCUSendToMCU("FFAA5501030015162C02800101020304050607");
////        Log.d("CommandSequence", "APU Dis  On cmd sent");
////
////        new Handler(Looper.getMainLooper()).postDelayed(() -> {
////            Heartbeat3 hb3 = serialPortManager.getHb3();
////            Log.d("CommandSequence", "APU DIs MOsState "+hb3.getApuDisChargeMosState());
////
////            boolean commandSuccess = (hb3 != null && hb3.getApuDisChargeMosState() == 1);
////
////            if (!commandSuccess && retryCount2 < MAX_RETRIES) {
////                retryCount2++;
////                Log.d("CommandSequence", "Retrying command 1, attempt " + retryCount2);
////                apuDisON(); // retry
////            } else if (commandSuccess) {
////                Log.i("APU", "Switched to APU successfully");
////                mainChargeOFF();
////            } else {
////                Log.e("CommandSequence", "Command 1 failed after max retries");
////                isSequenceActive = false;
////            }
////        }, 800); // adjust delay as needed
////    }
////
////    private void mainChargeOFF() {
////        if (!isSequenceActive) return;
////
////        serialPortManager.VCUSendToMCU("FFAA5501030015162A03800001020304050607");
////        Log.d("CommandSequence", "Sent command 2");
////
////        new Handler(Looper.getMainLooper()).postDelayed(() -> {
////            Heartbeat3 hb3 = serialPortManager.getHb3();
////            Log.d("CommandSequence", "MainChargeState"+hb3.getMainChargeMosState());
////            boolean commandSuccess = (hb3 != null && hb3.getMainChargeMosState() == 0);
////
////            if (!commandSuccess && retryCount1 < MAX_RETRIES) {
////                retryCount1++;
////                Log.d("CommandSequence", "Retrying command 2, attempt " + retryCount1);
////                mainChargeOFF(); // retry
////            } else if (commandSuccess) {
////                Log.i("CommandSequence", "Swicthed TO APU succeeded");
////                isSequenceActive = false;
////            } else {
////                Log.e("CommandSequence", "Command 2 failed after max retries");
////                isSequenceActive = false;
////            }
////        }, 800); // adjust delay as needed
////    }
////
////    // ------------------ STEP 2 -------------------
////
////
////    // ------------------ STEP 3 -------------------
////    public void startSwitchingToNormalState() {
////        isSequenceActive = true;
////        retryCount1 = 0;
////        retryCount2 = 0;
////        apuDisOFF();
////    }
////
////    private void apuDisOFF() {
////        if (!isSequenceActive) return;
////
////        serialPortManager.VCUSendToMCU("FFAA5501030015162C02800001020304050607");
////        Log.d("CommandSequence", "ApU DIS OFF  command Sent ");
////
////        new Handler(Looper.getMainLooper()).postDelayed(() -> {
////            Heartbeat3 hb3 = serialPortManager.getHb3();
////            Log.d("CommandSequence", "APU Dis State "+hb3.getApuDisChargeMosState());
////
////            boolean commandSuccess = (hb3 != null && hb3.getApuDisChargeMosState() == 0);
////
////            if (!commandSuccess && retryCount1 < MAX_RETRIES) {
////                retryCount1++;
////                Log.d("CommandSequence", "Retrying command 1, attempt " + retryCount1);
////                apuDisOFF(); // retry
////            } else if (commandSuccess) {
////                Log.i("CommandSequence", "Command 1 succeeded");
////                mainChargeON(); // proceed to next
////            } else {
////                Log.e("CommandSequence", "Command 1 failed after max retries");
////                isSequenceActive = false;
////            }
////        }, 800); // adjust delay as needed
////    }
////
////    private void mainChargeON() {
////        if (!isSequenceActive) return;
////
////        serialPortManager.VCUSendToMCU("FFAA5501030015162A03800101020304050607");
////        Log.d("CommandSequence", "main CHarge ON cmd sent");
////
////        new Handler(Looper.getMainLooper()).postDelayed(() -> {
////            Heartbeat3 hb3 = serialPortManager.getHb3();
////            Log.d("CommandSequence", "MainChargeState"+hb3.getMainChargeMosState());
////
////            boolean commandSuccess = (hb3 != null && hb3.getMainChargeMosState() == 1);
////
////            if (!commandSuccess && retryCount2 < MAX_RETRIES) {
////                retryCount2++;
////                Log.d("CommandSequence", "Retrying command 2, attempt " + retryCount2);
////                mainChargeON(); // retry
////            } else if (commandSuccess) {
////                Log.i("APU", "Switched to Main successfully");
////                isSequenceActive = false;
////            } else {
////                Log.e("CommandSequence", "Command 2 failed after max retries");
////                isSequenceActive = false;
////            }
////        }, 800); // adjust delay as needed
////    }
//
//    public void MCUOTA_Onclick(View view) {
//        if (FrameUtils.isNetworkAvailable(this)) {
//            new menu.FetchVersionTask().execute(version_URL);
//        } else {
//            Log.d("OTA_onClick", "no network ....");
//            //Toast.makeText(menu.this, R.string.tips_mcuota_no_network, Toast.LENGTH_SHORT).show();
//            showCustomToast("no network!");
//        }
//    }
//
//    private class FetchVersionTask extends AsyncTask<String, Void, Integer> {
//
//        @Override
//        protected Integer doInBackground(String... urls) {
//            try {
//                return FileDownloader.fetchVersion(urls[0]);
//            } catch (IOException e) {
//                e.printStackTrace();
//                return -1;
//            }
//        }
//
//        @Override
//        protected void onPostExecute(Integer mcuOtaVer_new) {
//            int mcuOtaVer_local = serialPortManager.mad.getMcuVersions();
//            Log.d("UIUpdater", "updateMacVersions mcuOtaVer_local: " + mcuOtaVer_local);
//            Log.d("UIUpdater", "updateMacVersions mcuOtaVer_new: " + mcuOtaVer_new);
//            if (mcuOtaVer_new != -1) {
//                if (mcuOtaVer_new > mcuOtaVer_local) {
//                    showUpdateDialog();
//                } else {
////                    Toast.makeText(menu.this, R.string.tips_mcuota_latest_version, Toast.LENGTH_SHORT).show();
//                    showCustomToast("This is the latest version, do not update!");
//                }
//            } else {
////                Toast.makeText(menu.this, R.string.tips_mcuota_file_fetch_version, Toast.LENGTH_SHORT).show();
//                showCustomToast("Failed to fetch new MCU version!");
//            }
//        }
//    }
//
//    private void showUpdateDialog() {
//        AlertDialog.Builder builder = new AlertDialog.Builder(menu.this);
//        builder.setTitle(getString(R.string.tips_mcuota_update))
//                .setMessage(getString(R.string.tips_mcuota_update_content))
//                .setIcon(android.R.drawable.ic_dialog_alert)
//                .setPositiveButton(getString(R.string.tips_mcuota_confirm), (dialog, which) -> startFirmwareDownload())
//                .setNegativeButton(getString(R.string.tips_mcuota_cancel), (dialog, which) -> showCustomToast("Cancel update!"))
//                .setCancelable(false);
//        AlertDialog dialog = builder.create();
//        dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        dialog.getWindow().getDecorView().setSystemUiVisibility(
//                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                        | View.SYSTEM_UI_FLAG_FULLSCREEN
//                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//        );
//        dialog.show();
//    }
//
//    private void startFirmwareDownload() {
//        new Thread(() -> {
//            String fileUrl;
//            try {
//                fileUrl = FileDownloader.fetchDownloadUrl(version_URL);
//                // 下载文件到本地
//                FileDownloader.downloadFile(fileUrl, saveDir);
//                mFirmwareFile = new File(saveDir, fileUrl.substring(fileUrl.lastIndexOf('/') + 1));
//                if (mFirmwareFile.exists()) {
//                    runOnUiThread(() -> showProgressDialog());
//                    yMode.send(mFirmwareFile, progress -> runOnUiThread(() -> updateProgressDialog(progress)));
//                    runOnUiThread(() -> hideProgressDialog());
//                } else {
////                    Toast.makeText(menu.this, R.string.tips_mcuota_file_unexist, Toast.LENGTH_SHORT).show();
//                    showCustomToast("Firmware file does not exist!");
//                }
//            } catch (IOException | InterruptedException e) {
//                Log.e("startFirmwareDownload", "Error downloading or sending firmware", e);
////                Toast.makeText(menu.this, R.string.tips_mcuota_Error_downloading_or_sending_firmware, Toast.LENGTH_SHORT).show();
//                showCustomToast("Error downloading or sending firmware!");
//            }
//        }).start();
//    }
//
//    // 显示进度条对话框并设置初始进度
//    private void showProgressDialog() {
//        progressDialog = new ProgressDialog(this);
//        progressDialog.setTitle("Firmware Update");
//        progressDialog.setMessage("Updating firmware, please wait...");
//        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//        progressDialog.setMax(100);
//        progressDialog.setProgress(0);
//        progressDialog.setCancelable(false);
//        progressDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        progressDialog.getWindow().getDecorView().setSystemUiVisibility(
//                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                        | View.SYSTEM_UI_FLAG_FULLSCREEN
//                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//        );
//        progressDialog.show();
//    }
//
//    // 更新进度条进度
//    private void updateProgressDialog(int progress) {
//        if (progressDialog != null) {
//            progressDialog.setProgress(progress);
//        }
//    }
//
//    // 隐藏进度条对话框
//    private void hideProgressDialog() {
//        if (progressDialog != null && progressDialog.isShowing()) {
//            progressDialog.dismiss();
//
//        }
//    }
//
//    private void setFullScreen() {
//        // 设置窗口标志，启用全屏模式
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
//
//        // 获取装饰视图
//        View decorView = getWindow().getDecorView();
//
//        // 设置系统 UI 选项，隐藏导航栏和状态栏，启用粘性沉浸模式
//        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
//        decorView.setSystemUiVisibility(uiOptions);
//    }
//
//    private void showCustomToast(String message) {
//        LayoutInflater inflater = getLayoutInflater();
//        View layout = inflater.inflate(R.layout.toast_custom, null);
//        ImageView icon = layout.findViewById(R.id.toast_icon);
//        TextView text = layout.findViewById(R.id.toast_text);
//        icon.setImageResource(R.drawable.toastlogo);
//        text.setText(message);
//        Toast toast = new Toast(getApplicationContext());
//        toast.setDuration(Toast.LENGTH_SHORT);
//        toast.setView(layout);
//        toast.show();
//    }
//}