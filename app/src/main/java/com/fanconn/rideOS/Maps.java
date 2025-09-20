package com.fanconn.rideOS;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.fanconn.rideOS.mcuupdate.YMode;

import java.io.File;

public class Maps extends AppCompatActivity {
    SerialPortManager serialPortManager = serialApplication.getSerialPortManager();
    private File mFirmwareFile;
    private YMode yMode;
    private ProgressDialog progressDialog;
    private String version_URL = "http://ota.rivotmotors.com/OTA/sc126/ElectricBike/version.json";
    private String saveDir = "/sdcard";
    ImageButton returnbtn;
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otaupdates);
        setFullScreen();
//        yMode = new YMode(this);
//        GestureHelper.enableSwipeGestures(this);
        returnbtn=findViewById(R.id.returnbtn);
        returnbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //InactivityManager.getInstance().startTimer(1);
                finish();
            }
        });
    }
    float[] startY = new float[1]; // Declare this as a field

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (GestureHelper.handleGesture(this, ev, startY)) {
            return true; // handled
        }
        return super.dispatchTouchEvent(ev);
    }

    private void setFullScreen() {
        // 设置窗口标志，启用全屏模式
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // 获取装饰视图
        View decorView = getWindow().getDecorView();

        // 设置系统 UI 选项，隐藏导航栏和状态栏，启用粘性沉浸模式
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
    }

//    public void MCUOTA_Onclick(View view) {
//        if (FrameUtils.isNetworkAvailable(this)) {
//            new FetchVersionTask().execute(version_URL);
//        } else {
//            Log.d("OTA_onClick", "no network ....");
//            Toast.makeText(Maps.this, R.string.tips_mcuota_no_network, Toast.LENGTH_SHORT).show();
//        }
//    }
//
//    private class FetchVersionTask extends AsyncTask<String, Void, Integer> {
//
//    @Override
//    protected Integer doInBackground(String... urls) {
//        try {
//            return FileDownloader.fetchVersion(urls[0]);
//        } catch (IOException e) {
//            e.printStackTrace();
//            return -1;
//        }
//    }
//
//    @Override
//    protected void onPostExecute(Integer mcuOtaVer_new) {
//        int mcuOtaVer_local = serialPortManager.mad.getMcuVersions();
//        Log.d("UIUpdater", "updateMacVersions mcuOtaVer_local: " + mcuOtaVer_local);
//        Log.d("UIUpdater", "updateMacVersions mcuOtaVer_new: " + mcuOtaVer_new);
//        if (mcuOtaVer_new != -1) {
//            if (mcuOtaVer_new > mcuOtaVer_local) {
//                showUpdateDialog();
//            } else {
//                Toast.makeText(Maps.this, R.string.tips_mcuota_latest_version, Toast.LENGTH_SHORT).show();
//                showCustomToast("This is the latest version, do not update!");
//            }
//        } else {
//            Toast.makeText(Maps.this, R.string.tips_mcuota_file_fetch_version, Toast.LENGTH_SHORT).show();
//        }
//    }
//}
//
//    private void showUpdateDialog() {
//        AlertDialog.Builder builder = new AlertDialog.Builder(Maps.this);
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
//                    Toast.makeText(Maps.this, R.string.tips_mcuota_file_unexist, Toast.LENGTH_SHORT).show();
//                }
//            } catch (IOException | InterruptedException e) {
//                Log.e("startFirmwareDownload", "Error downloading or sending firmware", e);
//                Toast.makeText(Maps.this, R.string.tips_mcuota_Error_downloading_or_sending_firmware, Toast.LENGTH_SHORT).show();
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
}