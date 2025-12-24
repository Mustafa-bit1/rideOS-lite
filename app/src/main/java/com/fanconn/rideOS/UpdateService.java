package com.fanconn.rideOS;

import android.app.Dialog;
import android.app.Service;
import android.content.*;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateService extends Service {

    private static final String TAG = "UpdateService";

    private static final String UPDATE_URL =
            "http://ota.rivotmotors.com/OTA/sc126/ElectricBike/rideOS/production_OTA/prod_update.json";

    /* ================= ACTIONS ================= */

    public static final String ACTION_INSTALL_UPDATE =
            "com.fanconn.rideOS.ACTION_INSTALL_UPDATE";

    /* ================= STATE ================= */

    private static volatile long nextAllowedCheckTime = 0;
    private static volatile File downloadedApkFile = null;

    private Handler mainHandler;
    private Dialog updateDialog;

    /* ================= SERVICE ================= */

    @Override
    public void onCreate() {
        super.onCreate();
        mainHandler = new Handler(Looper.getMainLooper());

        LocalBroadcastManager.getInstance(this).registerReceiver(
                installReceiver,
                new IntentFilter(ACTION_INSTALL_UPDATE)
        );

        Log.i(TAG, "UpdateService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        boolean manualCheck =
                intent != null && intent.getBooleanExtra("manual_check", false);

        if (manualCheck || canCheckNow()) {
            Log.i(TAG, "Starting OTA server check | manual=" + manualCheck);
            checkForUpdate(manualCheck);
        } else {
            Log.i(TAG, "OTA check skipped (remind-later active)");
        }

        return START_STICKY;
    }

    /* ================= UPDATE CHECK ================= */

    private boolean canCheckNow() {
        return System.currentTimeMillis() >= nextAllowedCheckTime;
    }

    private void checkForUpdate(boolean manualCheck) {
        new Thread(() -> {
            try {
                Log.i(TAG, "Checking OTA JSON | manual=" + manualCheck);

                HttpURLConnection conn =
                        (HttpURLConnection) new URL(UPDATE_URL).openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.connect();

                BufferedReader br =
                        new BufferedReader(new InputStreamReader(conn.getInputStream()));

                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONObject json = new JSONObject(sb.toString());

                String latestVersion = json.getString("latest_version");
                String apkUrl = json.getString("apk_url");

                String currentVersion =
                        getPackageManager()
                                .getPackageInfo(getPackageName(), 0)
                                .versionName;

                Log.i(TAG, "Current=" + currentVersion +
                        " | Server=" + latestVersion);

                if (!latestVersion.equals(currentVersion)) {
                    Log.i(TAG, "OTA update AVAILABLE");

                    mainHandler.post(() -> showUpdatePopup(apkUrl));

                } else {
                    Log.i(TAG, "No OTA update available");

                    // ✅ SHOW TOAST ONLY FOR MANUAL CHECK
                    if (manualCheck) {
                        mainHandler.post(() ->
                                showCustomToast("Your system is up to date")
                        );
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "OTA check failed", e);

                if (manualCheck) {
                    mainHandler.post(() ->
                            showCustomToast("No update availabe")
                    );
                }
            }
        }).start();
    }

    private void showCustomToast(String message) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
            try {
                LayoutInflater inflater =
                        (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                View layout = inflater.inflate(R.layout.toast_custom, null);

                ImageView icon = layout.findViewById(R.id.toast_icon);
                TextView text = layout.findViewById(R.id.toast_text);

                icon.setImageResource(R.drawable.toastlogo);
                text.setText(message);

                Toast toast = new Toast(getApplicationContext());
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(layout);
                toast.show();

                Log.i(TAG, "Custom toast shown: " + message);

            } catch (Exception e) {
                Log.e(TAG, "Failed to show custom toast", e);
            }
        });
    }

    /* ================= POPUP ================= */

    private void showUpdatePopup(String apkUrl) {
        if (apkUrl == null) return;

        if (updateDialog != null && updateDialog.isShowing()) {
            Log.w(TAG, "OTA popup already showing");
            return;
        }

        Log.i(TAG, "Showing custom OTA popup");

        try {
            updateDialog = new Dialog(this);
            updateDialog.setCancelable(false);
            updateDialog.setCanceledOnTouchOutside(false);

            LayoutInflater inflater =
                    (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(R.layout.popupdialog, null);

            TextView title = view.findViewById(R.id.dialogTitle);
            TextView message = view.findViewById(R.id.dialogMessage);
            Button btnYes = view.findViewById(R.id.btnYes);
            Button btnNo = view.findViewById(R.id.btnNo);

            title.setText("Update Available");
            message.setText("New rideOS update is available.\nDo you want to update now?");

            btnYes.setText("Update Now");
            btnNo.setText("Remind Later");

            // Stop inactivity timer while popup is visible
            Log.i(TAG, "Stopping inactivity timer for OTA popup");

            new Handler(Looper.getMainLooper()).postDelayed(() -> InactivityManager.getInstance().stopTimer(), 5000);

            btnYes.setOnClickListener(v -> {
                Log.i(TAG, "User accepted OTA update");
                updateDialog.dismiss();
                updateDialog = null;
                launchProgressAndDownload(apkUrl);
            });

            btnNo.setOnClickListener(v -> {
                Log.i(TAG, "User selected Remind Me Later");

                //  Schedule NEXT SERVER CHECK (not popup)
                nextAllowedCheckTime =
                        System.currentTimeMillis() + (24 * 60 * 60 * 1000); // 24
//                                               System.currentTimeMillis() + (10* 1000); // 24hh
                Log.i(TAG, "Next OTA check scheduled after 24h");

                if (UIUpdater.directionalGears == 0) {
                    InactivityManager.getInstance().startTimer(1);
                }

                updateDialog.dismiss();
                updateDialog = null;
            });

            updateDialog.setContentView(view);

            Window window = updateDialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawableResource(android.R.color.transparent);
                window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);

                WindowManager.LayoutParams params = window.getAttributes();
                params.width = WindowManager.LayoutParams.WRAP_CONTENT;
                params.height = WindowManager.LayoutParams.WRAP_CONTENT;
                params.gravity = Gravity.CENTER;
                window.setAttributes(params);
            }

            updateDialog.show();

        } catch (Exception e) {
            Log.e(TAG, "Failed to show OTA popup", e);
        }
    }

    /* ================= DOWNLOAD ================= */

    private void launchProgressAndDownload(String apkUrl) {
        Intent i = new Intent(this, UpdateProgressActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);

        downloadApk(apkUrl);
    }

    private void downloadApk(String apkUrl) {
        new Thread(() -> {
            try {
                downloadedApkFile =
                        new File(getExternalFilesDir(null), "update.apk");

                HttpURLConnection conn =
                        (HttpURLConnection) new URL(apkUrl).openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.connect();

                int total = conn.getContentLength();
                Log.i(TAG, "APK size=" + total);

                InputStream is = conn.getInputStream();
                OutputStream os = new FileOutputStream(downloadedApkFile);

                byte[] buf = new byte[4096];
                long sum = 0;
                int lastProgress = 0;

                int len;
                while ((len = is.read(buf)) > 0) {
                    os.write(buf, 0, len);
                    sum += len;

                    int progress =
                            total > 0 ? (int) ((sum * 100) / total)
                                    : Math.min(90, lastProgress + 1);

                    progress = Math.max(0, Math.min(progress, 100));

                    if (progress != lastProgress) {
                        lastProgress = progress;

                        Intent pi = new Intent("com.fanconn.UPDATE_PROGRESS");
                        pi.putExtra("progress", progress);
                        LocalBroadcastManager.getInstance(this).sendBroadcast(pi);

                        Log.d(TAG, "Download progress=" + progress + "%");
                    }
                }

                os.close();
                is.close();

                Intent done = new Intent("com.fanconn.UPDATE_PROGRESS");
                done.putExtra("progress", 100);
                LocalBroadcastManager.getInstance(this).sendBroadcast(done);

                Log.i(TAG, "APK download completed");

            } catch (Exception e) {
                Log.e(TAG, "APK download failed", e);

                Intent fail = new Intent("com.fanconn.UPDATE_PROGRESS");
                fail.putExtra("progress", -1);
                LocalBroadcastManager.getInstance(this).sendBroadcast(fail);
            }
        }).start();
    }

    /* ================= INSTALL ================= */

    private final BroadcastReceiver installReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "ACTION_INSTALL_UPDATE received");

            if (downloadedApkFile != null && downloadedApkFile.exists()) {
                installApkSilently(downloadedApkFile);
            } else {
                Log.e(TAG, "APK missing, cannot install");
            }
        }
    };

    private void installApkSilently(File apk) {
        try {
            String cmd = "pm install -r " + apk.getAbsolutePath();
            Log.i(TAG, "Executing: " + cmd);
            Runtime.getRuntime().exec(cmd).waitFor();
        } catch (Exception e) {
            Log.e(TAG, "Silent install failed", e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(installReceiver);
        Log.i(TAG, "UpdateService destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
