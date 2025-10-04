package com.fanconn.rideOS;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateService extends Service {

    private static final String TAG = "UpdateService";
    private static final String UPDATE_URL = "http://ota.rivotmotors.com/OTA/sc126/ElectricBike/rideOS/update.json";
    private final Handler handler = new Handler();
    private final int CHECK_INTERVAL = 24 * 60 * 60 * 1000; // 24 hours
    public static String pendingUpdateApkUrl = null;

    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            checkForUpdate(false); // automatic check
            handler.postDelayed(this, CHECK_INTERVAL);
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "UpdateService started");

        if (intent != null) {
            if (intent.getBooleanExtra("show_prompt_only", false)) {
                if (pendingUpdateApkUrl != null) {
                    Log.d(TAG, "Showing deferred update prompt...");
                    showUpdateDialog(pendingUpdateApkUrl);
                    pendingUpdateApkUrl = null;
                }
                return START_NOT_STICKY;
            }

            if (intent.getBooleanExtra("manual_check", false)) {
                checkForUpdate(true);
                return START_NOT_STICKY;
            }
        }

        handler.post(updateRunnable);
        return START_STICKY;
    }

    private void checkForUpdate(boolean manual) {
        new Thread(() -> {
            try {
                URL url = new URL(UPDATE_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.connect();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder jsonBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonBuilder.append(line);
                }

                JSONObject jsonObject = new JSONObject(jsonBuilder.toString());
                String latestVersion = jsonObject.getString("latest_version");
                String apkUrl = jsonObject.getString("apk_url");

                String currentVersion = getPackageManager()
                        .getPackageInfo(getPackageName(), 0).versionName;

                Log.d(TAG, "Current version: " + currentVersion + ", Latest version: " + latestVersion);

                if (!latestVersion.equals(currentVersion)) {
                    Log.d(TAG, "Update available!");
                    InactivityManager.getInstance().stopTimer();
                    promptUserToUpdate(apkUrl);
                } else {
                    Log.d(TAG, "App is up-to-date.");
                    if (manual) {
                        handler.post(() ->
                                android.widget.Toast.makeText(getApplicationContext(),
                                        "App is up-to-date", android.widget.Toast.LENGTH_SHORT).show()
                        );
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Update check failed: " + e.getMessage(), e);
                if (manual) {
                    handler.post(() ->
                            android.widget.Toast.makeText(getApplicationContext(),
                                    "No update available", android.widget.Toast.LENGTH_SHORT).show()
                    );
                }
            }

        }).start();
    }

    private void promptUserToUpdate(String apkUrl) {
        Log.d(TAG, "Prompting user for update...");

        // Defer if MainActivity not active
        if (!MainActivity.MainisActive) {
            Log.d(TAG, "MainActivity not active. Deferring update prompt.");
            pendingUpdateApkUrl = apkUrl;
            return;
        }

        showUpdateDialog(apkUrl);
    }

    private void showUpdateDialog(String apkUrl) {
        new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(apkUrl).openConnection();
                conn.setRequestMethod("HEAD");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                int responseCode = conn.getResponseCode();

                if (responseCode != 200) {
                    Log.w(TAG, "APK URL no longer valid. Skipping prompt.");
                    pendingUpdateApkUrl = null;
                    return;
                }

                handler.post(() -> {
                    try {
                        Context context = getApplicationContext();
                        LayoutInflater inflater = LayoutInflater.from(context);
                        View dialogView = inflater.inflate(R.layout.popupdialog, null);

                        TextView title = dialogView.findViewById(R.id.dialogTitle);
                        TextView message = dialogView.findViewById(R.id.dialogMessage);
                        Button btnYes = dialogView.findViewById(R.id.btnYes);
                        Button btnNo = dialogView.findViewById(R.id.btnNo);

                        title.setText("Update Available");
                        message.setText("rideOS update available.\nDo you want to update?");
                        btnYes.setText("Update Now");
                        btnNo.setText("Remind me later");

                        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(context)
                                .setView(dialogView)
                                .setCancelable(false)
                                .create();

                        btnYes.setOnClickListener(v -> {
                            Log.d(TAG, "User accepted the update.");
                            Intent progressIntent = new Intent(context, UpdateProgressActivity.class);
                            progressIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(progressIntent);
                            downloadAndInstall(apkUrl);
                            dialog.dismiss();
                        });

                        btnNo.setOnClickListener(v -> {
                            Log.d(TAG, "Remind later selected");
                            handler.postDelayed(() -> promptUserToUpdate(apkUrl),
                                    24 * 60 * 60 * 1000);
                            dialog.dismiss();
                        });

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            dialog.getWindow().setType(android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
                        } else {
                            dialog.getWindow().setType(android.view.WindowManager.LayoutParams.TYPE_PHONE);
                        }

                        dialog.show();
                        Log.d(TAG, "Custom update dialog displayed");

                    } catch (Exception e) {
                        Log.e(TAG, "Failed to show custom update dialog", e);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "APK URL validation failed", e);
            }
        }).start();
    }

    private void downloadAndInstall(String apkUrl) {
        new Thread(() -> {
            try {
                File apkFile = new File(getExternalFilesDir(null), "update.apk");
                HttpURLConnection conn = (HttpURLConnection) new URL(apkUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.connect();

                int totalLength = conn.getContentLength();
                InputStream is = conn.getInputStream();
                OutputStream os = new FileOutputStream(apkFile);

                byte[] buffer = new byte[4096];
                int len;
                int downloaded = 0;

                while ((len = is.read(buffer)) > 0) {
                    os.write(buffer, 0, len);
                    downloaded += len;

                    int progress = (int) (((float) downloaded / totalLength) * 100);

                    Intent intent = new Intent("com.fanconn.UPDATE_PROGRESS");
                    intent.putExtra("progress", progress);
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                }

                os.close();
                is.close();

                Log.d(TAG, "APK downloaded to: " + apkFile.getAbsolutePath());

                Intent finalIntent = new Intent("com.fanconn.UPDATE_PROGRESS");
                finalIntent.putExtra("progress", 100);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(finalIntent);

                installApkSilently(apkFile);

            } catch (Exception e) {
                Log.e(TAG, "Download/install failed", e);
                Intent intent = new Intent("com.fanconn.UPDATE_PROGRESS");
                intent.putExtra("progress", -1);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
            }
        }).start();
    }

    private void installApkSilently(File apkFile) {
        try {
            String cmd = "pm install -r " + apkFile.getAbsolutePath();
            Log.d(TAG, "Executing: " + cmd);
            Process process = Runtime.getRuntime().exec(cmd);
            int result = process.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                Log.d(TAG, "pm output: " + line);
            }

            if (result == 0) {
                Log.d(TAG, "APK installed successfully.");
            } else {
                Log.e(TAG, "Silent install failed with code: " + result);
            }
        } catch (Exception e) {
            Log.e(TAG, "Silent install failed", e);
        }
    }

    private void restartApp() {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(launchIntent);
        }
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
