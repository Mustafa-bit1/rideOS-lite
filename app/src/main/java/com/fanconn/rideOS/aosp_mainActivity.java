package com.fanconn.rideOS;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UpdateEngine;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.fanconn.rideOS.local_ui.SelectActivity;
import com.fanconn.rideOS.util.UpdateConfigs;
import com.fanconn.rideOS.util.http;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class aosp_mainActivity extends AppCompatActivity {

    private static final String TAG = "AOSPActivity";
    private String mPrivatePath;
    private ProgressBar mProgressBar_main;
    private ProgressBar mprogressBar_round;
    private TextView mTextProduct_main;
    private TextView mtextModule_main;
    private TextView mtextVersions_main;
    private TextView mtextNewVersion;
    private TextView mupdate_title;
    private TextView mupdate_content;
    private TextView mprogressBarState_main;
    private TextView mprogressBar_round_txt;
    private TextView mprogressValue_main;
    private Button mCheck_update;
    //private static final String version_URL = "http://fanconn.com/OTA/sc126/version.json";   //后台更新信息值
    private static  String FOTAFILE ;
    private ScrollView mscroll;
    private static final String SP_module = Build.DEVICE;   //设备模组
    private static final String SP_product = Build.PRODUCT;    //设备产品名称
    private static final String SP_versions = Build.VERSION.INCREMENTAL;  //设备软件版本
    private static final int SP_version_verification = Integer.parseInt(Build.ID);   //设备软件版本校验值
    //private static final int SP_version_verification = Integer.parseInt("100");   //设备软件版本校验值
    private static final String version_URL = "http://ota.rivotmotors.com/OTA/sc126/ElectricBike/version.json" ;
    String json_buf = "";   //接收version.json数据返回值
    String version = "";     //接收version.json解析的版本号
    String url = "";     //接收version.json解析的网址
    String tips = "";     //接收version.json解析的更新内容
    String author = "";   //接收version.json解析的作者

    private Intent mLocal_update_select_activity;
    private List<com.fanconn.rideOS.UpdateConfig> mConfigs;
    private  int Check_update_status = 0;
    public static final int Button_Check = 0;
    public static final int Button_ERROR = 1;
    public static final int Button_reboot = 5;
    public static final int Button_update = 6;
    public static final String EXTRA_SILENT_OTA = "silent_ota";

    private Spinner mSpinnerConfigs;
    private final com.fanconn.rideOS.UpdateManager mUpdateManager =
            new com.fanconn.rideOS.UpdateManager(new UpdateEngine(), new Handler());
    //文件权限
    private static final int MEDIA_RECORDER_REQUEST = 0;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE" };
    private ImageButton aosp_OTA_back_btn;
    // ===== WATCHDOG =====
    private static final long OTA_WATCHDOG_TIMEOUT = 20 * 60 * 1000; // 20 minutes
    private Handler otaWatchdogHandler = new Handler();
    private Runnable otaWatchdogRunnable;
    private int lastProgressPercent = -1;
    private boolean otaInProgress = false;
    private void startOtaWatchdog() {
        stopOtaWatchdog(); // safety

        otaInProgress = true;

        otaWatchdogRunnable = () -> {
            Log.e(TAG, "OTA WATCHDOG TIMEOUT! No progress for 10 minutes");

            runOnUiThread(() -> {
                handleOtaStuck();
            });
        };

        otaWatchdogHandler.postDelayed(otaWatchdogRunnable, OTA_WATCHDOG_TIMEOUT);
    }
    private void resetOtaWatchdog() {
        if (!otaInProgress) return;

        otaWatchdogHandler.removeCallbacks(otaWatchdogRunnable);
        otaWatchdogHandler.postDelayed(otaWatchdogRunnable, OTA_WATCHDOG_TIMEOUT);

        Log.d(TAG, "OTA watchdog reset");
    }

    private void stopOtaWatchdog() {
        otaInProgress = false;
        otaWatchdogHandler.removeCallbacksAndMessages(null);
    }

    private void handleOtaStuck() {
        Log.e(TAG, "Handling OTA stuck state");

        stopOtaWatchdog();

        try {
            if (Check_update_status == com.fanconn.rideOS.UpdaterState.RUNNING) {
                // safest during apply
                mUpdateManager.suspend();
                Log.e(TAG, "OTA paused due to watchdog");
            }

            // force OTA failure path (ONLY ONCE)
            mUpdateManager.resetUpdate();
            Log.e(TAG, "OTA reset due to watchdog");

        } catch (Exception e) {
            // This includes "No ongoing update to cancel" → SAFE
            Log.w(TAG, "Watchdog stop/reset skipped (safe)", e);
        }

        // 🔴 USE EXISTING OTA ERROR UI (same as ERROR state)
        runOnUiThread(() -> {
            Log.e(TAG, "Watchdog triggered OTA failure UI");

            HiddenControl();
            HiddenControlRound();
            Error_Display();   // ✅ reuse same popup & behavior
        });
    }


    private boolean isSilentStart = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aosp_mainactivty);

        getWindow().setStatusBarColor(getResources().getColor(R.color.white,null));
        getWindow().setNavigationBarColor(getResources().getColor(R.color.white,null));
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR |
                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        );
        InactivityManager.getInstance().stopTimer();
        bindSelectActivity();
        loadUpdateConfigs();

        aosp_OTA_back_btn = findViewById(R.id.aosp_OTA_back_btn);
        aosp_OTA_back_btn.setOnClickListener(v -> finish());

        // Permissions
        if (isAllPermissionGranted()) {
            initData();
        } else {
            requestNecessaryPermissions();
        }

        // 🔥 Read silent flag FIRST
        isSilentStart = getIntent().getBooleanExtra(EXTRA_SILENT_OTA, false);

        if (isSilentStart) {
            // 🤖 MQTT FLOW → silent OTA
            Log.i(TAG, "🔥 Silent OTA mode triggered from MQTT");

            new Thread(() -> {
                if (isNetworkAvailable(aosp_mainActivity.this)) {
                    Detection_update(version_URL);   // auto StartUpdate()
                } else {
                    Log.e(TAG, "❌ Silent OTA failed: No internet");
                    finish();
                }
            }).start();

        } else {
            // 🧑 USER FLOW → manual OTA
            handleCheckUpdateAction();  // UI-based flow only
        }
    }


    /**
     * 创建菜单
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        stopOtaWatchdog();

        if (rebootHandler != null && rebootRunnable != null) {
            rebootHandler.removeCallbacks(rebootRunnable);
        }

        this.mUpdateManager.setOnProgressUpdateCallback(null);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Binding to UpdateEngine invokes onStatusUpdate callback,
        // persisted updater state has to be loaded and prepared beforehand.

        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        this.mUpdateManager.bind();
    }

    @Override
    protected void onPause() {
        this.mUpdateManager.unbind();
        super.onPause();
    }

    /**
     * 菜单点击事件
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_item1) {
            startActivity(mLocal_update_select_activity);  //跳转到本地更新活动界面
            return true;
        } else if (id == R.id.menu_item2) {
            // 处理菜单项2被点击的情况
            //Toast.makeText(aosp_mainActivity.this,getString(R.string.click), Toast.LENGTH_LONG).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 数据初始化
     */
    private void initData() {

        mPrivatePath = getFilesDir().toString();
    }

    /**
     * 绑定活动界面函数
     */
    private void bindSelectActivity() {
        this.mSpinnerConfigs = findViewById(R.id.spinnerConfigs_main);
        mLocal_update_select_activity = new Intent(this, SelectActivity.class);
        this.mCheck_update = findViewById(R.id.Check_update);
        this.mscroll = findViewById(R.id.scroll);
        this.mTextProduct_main = findViewById(R.id.textProduct_main);
        this.mprogressBarState_main = findViewById(R.id.progressBarState_main);
        this.mprogressBar_round_txt = findViewById(R.id.progressBar_round_txt);
        this.mprogressValue_main = findViewById(R.id.progressValue_main);
        this.mProgressBar_main = findViewById(R.id.progressBar_main);
        this.mprogressBar_round = findViewById(R.id.progressBar_round);
        this.mtextModule_main = findViewById(R.id.textModule_main);
        this.mtextVersions_main = findViewById(R.id.textVersions_main);
        this.mtextNewVersion = findViewById(R.id.textNewVersion);
        this.mupdate_title = findViewById(R.id.update_title);
        this.mupdate_content = findViewById(R.id.update_content);

        mtextModule_main.setText(SP_module);
        mTextProduct_main.setText(SP_product);
        mtextVersions_main.setText(SP_versions);

        this.mUpdateManager.setOnStateChangeCallback(this::onUpdaterStateChange);
        this.mUpdateManager.setOnProgressUpdateCallback(this::onProgressUpdate);

    }

    /**
     * Invoked when SystemUpdaterSample app state changes.
     * Value of {@code state} will be one of the
     * values from {@link com.fanconn.rideOS.UpdaterState}.
     * 获取升级状态
     */
    private void onUpdaterStateChange(int state) {
        Log.i(TAG, "UpdaterStateChange state="
                + com.fanconn.rideOS.UpdaterState.getStateText(state)
                + "/" + state);
        runOnUiThread(() -> {
            if (state == com.fanconn.rideOS.UpdaterState.IDLE) {
                Log.e(TAG, "balkue UpdaterStateChange state=" + com.fanconn.rideOS.UpdaterState.IDLE);
                Check_update_status = state;
                mProgressBar_main.setProgress(0);
            } else if (state == com.fanconn.rideOS.UpdaterState.RUNNING) {
                Log.e(TAG, "balkue UpdaterStateChange state=" + com.fanconn.rideOS.UpdaterState.RUNNING);
                Check_update_status = state;
                //mprogressBarState_main.setText("OTA升级中，请勿退出当前界面(1/2)...");
                //DisplayControl();
            } else if (state == com.fanconn.rideOS.UpdaterState.PAUSED) {
                Log.e(TAG, "balkue UpdaterStateChange state=" + com.fanconn.rideOS.UpdaterState.PAUSED);
                Check_update_status = state;
                DisplayControl();
            } else if (state == com.fanconn.rideOS.UpdaterState.ERROR) {
                Log.e(TAG, "balkue UpdaterStateChange state=" + com.fanconn.rideOS.UpdaterState.ERROR);
                Check_update_status = state;
                try {
                    mUpdateManager.resetUpdate(); //重置更新状态
                } catch (com.fanconn.rideOS.UpdaterState.InvalidTransitionException e) {
                    throw new RuntimeException(e);
                }
                Log.e(TAG, "Watchdog stop failed in Normal mechanism");
                HiddenControl();
                HiddenControlRound();
                Error_Display();
            } else if (state == com.fanconn.rideOS.UpdaterState.SLOT_SWITCH_REQUIRED) {
                Log.e(TAG, "balkue UpdaterStateChange state=" + com.fanconn.rideOS.UpdaterState.SLOT_SWITCH_REQUIRED);
                Log.e(TAG, "balkue UpdaterStateChange 可以切换分区啦" );
                Check_update_status = state;
                mprogressBarState_main.setText(getString(R.string.ota_switch));
                DisplayControl();
                mUpdateManager.setSwitchSlotOnReboot(); //切换分区
            } else if (state == com.fanconn.rideOS.UpdaterState.REBOOT_REQUIRED) {
                //  uiStateRebootRequired();
                Log.e(TAG, "balkue UpdaterStateChange state=" + com.fanconn.rideOS.UpdaterState.REBOOT_REQUIRED);
                Log.e(TAG, "balkue UpdaterStateChange 重启即可生效" );
                Check_update_status = state;
                mCheck_update.setBackgroundResource(R.drawable.custom_button_aosp);
                mCheck_update.setText(getString(R.string.reboot));
                mtextNewVersion.setText(getString(R.string.update_completed));
                mCheck_update.setEnabled(true);
                HiddenControl();
                stopOtaWatchdog();

                mprogressBar_round.setVisibility(ProgressBar.GONE);
                mprogressBar_round_txt.setVisibility(View.GONE);

                mprogressBarState_main.setVisibility(View.VISIBLE);
                mprogressBarState_main.setTextColor(Color.parseColor("#01FF58"));
                mprogressBarState_main.setText("Software update successful");

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    reboot();
                }, 3000);

            }
        });
    }

    /**
     * 显示进度条
     */

    private void DisplayControl() {
        mProgressBar_main.setVisibility(ProgressBar.VISIBLE);
        aosp_OTA_back_btn.setVisibility(View.INVISIBLE);
        mprogressBarState_main.setVisibility(View.VISIBLE);
        mprogressValue_main.setVisibility(View.VISIBLE);
        mupdate_title.setVisibility(View.INVISIBLE);
        mupdate_content.setVisibility(View.INVISIBLE);
        mscroll.setVisibility(View.INVISIBLE);
        resetOtaWatchdog();
    }

    /**
     * 隐藏进度条和更新内容
     */
    private void HiddenControl() {
        mProgressBar_main.setVisibility(ProgressBar.INVISIBLE);
        mprogressBarState_main.setVisibility(View.INVISIBLE);
        mprogressValue_main.setVisibility(View.INVISIBLE);
        mSpinnerConfigs.setVisibility(View.INVISIBLE);
        mupdate_title.setVisibility(View.INVISIBLE);
        mupdate_content.setVisibility(View.INVISIBLE);
        mscroll.setVisibility(View.INVISIBLE);

    }

    /**
     * 显示圆形进度条
     */
    private void DisplayControlRound() {
        mprogressBar_round.setVisibility(ProgressBar.VISIBLE);
        mtextNewVersion.setText(getString(R.string.estimated_time));
        aosp_OTA_back_btn.setVisibility(View.INVISIBLE);
        mprogressBar_round_txt.setVisibility(View.VISIBLE);
        aosp_OTA_back_btn.setVisibility(View.INVISIBLE);
        mscroll.setVisibility(View.INVISIBLE);
        mupdate_title.setVisibility(View.INVISIBLE);
        mupdate_content.setVisibility(View.INVISIBLE);
    }
    /**
     * 隐藏圆形进度条和更新内容
     */
    private void HiddenControlRound() {
        mprogressBar_round.setVisibility(ProgressBar.GONE);
        mprogressBar_round_txt.setVisibility(View.GONE);
        mSpinnerConfigs.setVisibility(View.INVISIBLE);
    }

    /**
     * Invoked when update progress changes.
     * 实时获取升级更新进度
     */
    private void onProgressUpdate(double progress) {
        int percent = (int) (progress * 100);

        runOnUiThread(() -> {
            mProgressBar_main.setProgress(percent);
            mprogressValue_main.setText(percent + "%");

            //  WATCHDOG RESET LOGIC
            if (percent != lastProgressPercent) {
                lastProgressPercent = percent;
                resetOtaWatchdog(); // progress moved → reset timer
            }

            if (percent == 1 && Check_update_status == 2) {
                HiddenControlRound();
                mprogressBarState_main.setText(getString(R.string.ota_update));
                DisplayControl();
            }
        });
    }


    /**
     * 申请访问文件权限
     */
    private boolean isAllPermissionGranted() {
        for (String permission : PERMISSIONS_STORAGE) {
            if (!(checkSelfPermission(permission) ==
                    PackageManager.PERMISSION_GRANTED)) {
                return false;
            }
        }
        return true;
    }
    private void requestNecessaryPermissions() {
        requestPermissions(
                PERMISSIONS_STORAGE,
                MEDIA_RECORDER_REQUEST);
    }

    /**
     * 检查更新 按钮点击事件
     * @param view
     */
    public void onCheckUpdateClick(View view) {
        handleCheckUpdateAction();
    }
    private void handleCheckUpdateAction() {

        switch (Check_update_status) {

            case Button_Check:
                if (isNetworkAvailable(aosp_mainActivity.this)) {
                    Log.i(TAG, "开始download流程");
                    Detection_update(version_URL);
                } else {
                    showCustomToast("no network!");
//                    Toast.makeText(this, R.string.no_network, Toast.LENGTH_SHORT).show();
                }
                break;

            case Button_update:
                StartUpdate();
                break;

            case Button_reboot:
                reboot();
                break;

            default:
                Log.d(TAG, "No matching update state");
        }
    }
    private void showCustomToast(String message) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.toast_custom, null);
        ImageView icon = layout.findViewById(R.id.toast_icon);
        TextView text = layout.findViewById(R.id.toast_text);
        icon.setImageResource(R.drawable.toastlogo);
        text.setText(message);
        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }
    /**
     * 检测网络
     * @param activity
     * @return
     */
    public boolean isNetworkAvailable(Activity activity) {
        Context context = activity.getApplicationContext();
        // 获取手机所有连接管理对象（包括对wi-fi,net等连接的管理）
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) {
            return false;
        } else {
            // 获取NetworkInfo对象
            NetworkInfo[] networkInfo = connectivityManager.getAllNetworkInfo();

            if (networkInfo != null && networkInfo.length > 0) {
                for (int i = 0; i < networkInfo.length; i++) {
                    Log.d(TAG, i + "===状态===" + networkInfo[i].getState());
                    Log.d(TAG, i + "===类型===" + networkInfo[i].getTypeName());
                    // 判断当前网络状态是否为连接状态
                    if (networkInfo[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        Log.i(TAG, "No Internet");
        return false;
    }

    /**
     *下载config.json
     */
    private void downloadJson(String mPath) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "开始");
                FOTAFILE = url + "/config.json";
                int i = downloadFile(getFilesDir().toString() + "/configs/", "config.json", FOTAFILE);
                Log.d(TAG, "i =" + i);
            }
        }).start();
    }

    /**
     *检测是否有新版本
     */
//    private void Detection_update(String mPath) {
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                json_buf = http.doGet(mPath);
//                Log.e(TAG, "version_URL: " + json_buf);
//                try {
//                    string_getURL(json_buf, "ElectricBike");
//
////                    string_getURL(json_buf, Build.PRODUCT);
//                    //string_getURL(json_buf, "Alipay");
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//                if (Integer.parseInt(version) > SP_version_verification){
//                    Check_update_status = 6;
//                    downloadJson(url);
//                    StartUpdate();
//                    Log.e(TAG, "Detection_update: 需要更新 远端版本:" + Integer.parseInt(version) + "  本地版本:" + SP_version_verification );
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            mtextNewVersion.setText(getString(R.string.find_new_versions));
//                            mupdate_content.setText(tips);
//                            mupdate_title.setVisibility(View.VISIBLE); //显示
//                            mtextNewVersion.setVisibility(View.VISIBLE); //显示
//                            mupdate_content.setVisibility(View.VISIBLE); //显示
//
//                        }
//                    });
//                } else {
//                    Log.e(TAG, "Detection_update: 不用更新 远端版本" + Integer.parseInt(version) + "  本地版本:" + SP_version_verification );
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Toast.makeText(aosp_mainActivity.this,R.string.latest_version, Toast.LENGTH_SHORT).show();
//                            mtextNewVersion.setText(getString(R.string.latest_version));
//                            mtextNewVersion.setVisibility(View.VISIBLE); //显示
//                        }
//                    });
//                }
//
//            }
//        }).start();
//    }

    private void Detection_update(String mPath) {
        new Thread(() -> {

            json_buf = http.doGet(mPath);
            Log.e(TAG, "version_URL: " + json_buf);

            //  ABSOLUTE MUST
            if (json_buf == null || json_buf.isEmpty()) {
                Log.e(TAG, "OTA check failed: json_buf is null");

                runOnUiThread(() -> showCustomDialog(
                        aosp_mainActivity.this,
                        "Server Error",
                        "Unable to reach update server.\nPlease try again or contact RIVOT-care.",
                        "OK",
                        null,
                        () -> {
                            finish();
                        },
                        null
                ));
                return; //  STOP THREAD
            }

            try {
                string_getURL(json_buf, "ElectricBike");
            } catch (Exception e) {
                Log.e(TAG, "JSON parse failed", e);
                return;
            }

            if (Integer.parseInt(version) > SP_version_verification) {
                Check_update_status = Button_update;
                downloadJson(url);
                StartUpdate();
            } else {
                runOnUiThread(() -> {
//                    Toast.makeText(
//                            aosp_mainActivity.this,
//                            R.string.latest_version,
//                            Toast.LENGTH_SHORT
//                    ).show();
                    showCustomToast("Software up-to-date, no update required");
                    finish();

                });
            }

        }).start();
    }


    public int downloadFile(String dirName, String fileName, String urlStr) {
        OutputStream output = null;
        InputStream input = null;
        try {
            //将字符串形式的path,转换成一个url
            Log.d(TAG, "downloadFile start");
            java.net.URL url = new URL(urlStr);
            //得到url之后，将要开始连接网络，以为是连接网络的具体代码
            //首先，实例化一个HTTP连接对象conn
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            //定义请求方式为GET，其中GET的大小写不要搞错了。
            conn.setRequestMethod("GET");
            //定义请求时间，在ANDROID中最好是不好超过10秒。否则将被系统回收。
            conn.setConnectTimeout(20 * 1000);
            //请求成功之后，服务器会返回一个响应码。如果是GET方式请求，服务器返回的响应码是200，post请求服务器返回的响应码是206（貌似）。
            Log.d(TAG, "conn.getResponseCode() =" + conn.getResponseCode());
            if (conn.getResponseCode() == 200) {
                //返回码为真
                //从服务器传递过来数据，是一个输入的动作。定义一个输入流，获取从服务器返回的数据
                input = conn.getInputStream();
                File file = createFile(dirName + fileName);
                Log.d(TAG, "file =" + file);
                if (file != null && file.exists()) {
                    Log.d(TAG, "read file");
                    output = new FileOutputStream(file);
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = input.read(buffer)) > 0) {
                        output.write(buffer, 0, length);
                    }
                }
            }
            if (output != null) {
                output.flush();
                input.close();
            }
            System.out.println("success");
            // Check_update_status = 6;
            //StartUpdate();
            //mHandler.sendEmptyMessage(MSG_DOWNLOAD_SUCCESS);
            return 0;
        } catch (MalformedURLException e) {
            Log.d(TAG, "MalformedURLException downloadFile exception");
            //mHandler.sendEmptyMessage(MSG_DOWNLOAD_FAIL);
            e.printStackTrace();
        } catch (Exception e) {
            Log.d(TAG, "downloadFile exception1");
            //mHandler.sendEmptyMessage(MSG_DOWNLOAD_FAIL);
            e.printStackTrace();
        } finally {
            try {
                output.close();
            } catch (IOException e) {
                Log.d(TAG, "downloadFile exception1");
                // mHandler.sendEmptyMessage(MSG_DOWNLOAD_FAIL);
                e.printStackTrace();
            }
        }
        return 1;
    }

    public File createFile(String fileName) {
        File file = new File(fileName);
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

    /**
     * 下载更新
     */
    private void StartUpdate() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mCheck_update.setVisibility(View.INVISIBLE);

                // ✅ SILENT OTA FLOW (MQTT TRIGGER)
                if (isSilentStart) {
                    if (Check_update_status != 5) {
                        SerialPortManager.getInstance().VCUSendToMCU("FFAA550107000A00");
                        DisplayControlRound();
                        loadUpdateConfigs();
                        startOtaWatchdog();
                        applyUpdate(getSelectedConfig());   // 🚀 direct OTA start
                    } else {
                        reboot();
                    }
                    return; // 🔥 important: prevents popup logic
                }

                // ✅ NORMAL FLOW (AOSP BUTTON)
                if (Check_update_status != 5) {
                    showCustomDialog(
                            aosp_mainActivity.this,
                            "Update Available",
                            "New software version available, please update.\n" +
                                    "Park the scooter in an area with strong internet connectivity.\n" +
                                    "Estimated download time: 4G data: ~16 min | Wi-Fi: ~11 min",
                            "Update",
                            "Cancel",
                            () -> {
                                SerialPortManager.getInstance().VCUSendToMCU("FFAA550107000A00");
                                DisplayControlRound();
                                loadUpdateConfigs();
                                startOtaWatchdog();
                                applyUpdate(getSelectedConfig());
                            },
                            () -> {
                                finish();
                            }
                    );
                } else {
                    reboot();
                }
            }
        });
    }

    private static final int REBOOT_COUNTDOWN_SECONDS = 5;
    private Handler rebootHandler = new Handler();
    private int rebootSecondsLeft;
    private Runnable rebootRunnable;

    private void reboot() {
        mprogressBarState_main.setTextColor(Color.parseColor("#FFFFFF"));
        // Hide update button
        mCheck_update.setVisibility(View.INVISIBLE);
        aosp_OTA_back_btn.setVisibility(View.INVISIBLE);

        // Make sure progress text is visible
        mprogressBarState_main.setVisibility(View.VISIBLE);

        startRebootCountdown(REBOOT_COUNTDOWN_SECONDS);
    }
    private void startRebootCountdown(int seconds) {

        rebootSecondsLeft = seconds;

        // Safety: stop any previous countdown
        if (rebootRunnable != null) {
            rebootHandler.removeCallbacks(rebootRunnable);
        }

        rebootRunnable = new Runnable() {
            @Override
            public void run() {

                if (rebootSecondsLeft > 0) {

                    // Update UI text
                    mprogressBarState_main.setText(
                            "Rebooting system in " + rebootSecondsLeft + "…"
                    );

                    rebootSecondsLeft--;
                    rebootHandler.postDelayed(this, 1000);

                } else {

                    // Final text (optional)
                    mprogressBarState_main.setText("Rebooting now…");

                    // Small delay for UX
                    rebootHandler.postDelayed(() -> {
                        try {
                            Runtime.getRuntime().exec("reboot");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }, 500);
                }
            }
        };

        rebootHandler.post(rebootRunnable);
    }

    private void showCustomDialog(
            Context ctx,
            String title,
            String message,
            String yesText,
            String noText,
            Runnable yesAction,
            Runnable noAction
    ) {
        View dialogView = LayoutInflater.from(ctx).inflate(R.layout.popupdialog, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setView(dialogView);
        builder.setCancelable(false);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        TextView dialogMessage = dialogView.findViewById(R.id.dialogMessage);
        Button btnYes = dialogView.findViewById(R.id.btnYes);
        Button btnNo = dialogView.findViewById(R.id.btnNo);

        // Set title & message
        dialogTitle.setText(title);
        dialogMessage.setText(message);

        // YES button (always shown)
        btnYes.setText(yesText);
        btnYes.setVisibility(View.VISIBLE);
        btnYes.setOnClickListener(v -> {
            dialog.dismiss();
            if (yesAction != null) yesAction.run();
        });

        // NO button (optional)
        if (noText == null || noText.trim().isEmpty()) {
            btnNo.setVisibility(View.GONE); //  Hide completely
        } else {
            btnNo.setVisibility(View.VISIBLE);
            btnNo.setText(noText);
            btnNo.setOnClickListener(v -> {
                dialog.dismiss();
                if (noAction != null) noAction.run();
            });
        }

        dialog.show();
    }

    private void applyUpdate(com.fanconn.rideOS.UpdateConfig config) {
        try {
            mUpdateManager.applyUpdate(this, config);
        } catch (com.fanconn.rideOS.UpdaterState.InvalidTransitionException e) {
            Log.e(TAG, "Failed to apply update " + config.getName(), e);
        }
    }

    private com.fanconn.rideOS.UpdateConfig getSelectedConfig() {
        return mConfigs.get(mSpinnerConfigs.getSelectedItemPosition());
    }

    /**
     * loads json configurations from configs dir that is defined in {@link UpdateConfigs}.
     */
    private void loadUpdateConfigs() {
        File file = new File(Paths.get(getFilesDir().toString(),"configs").toString());
        if(!file.exists()){
            try {
                Files.createDirectory(Paths.get(getFilesDir().toString(),"configs/"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Log.e(TAG, "balkue loadUpdateConfigs：");
        mConfigs = UpdateConfigs.getUpdateConfigs(this);
        loadConfigsToSpinner(mConfigs);
    }

    private void loadConfigsToSpinner(List<com.fanconn.rideOS.UpdateConfig> configs) {
        String[] spinnerArray = UpdateConfigs.configsToNames(configs);
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                spinnerArray);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout
                .simple_spinner_dropdown_item);
        mSpinnerConfigs.setAdapter(spinnerArrayAdapter);
    }

    /**
     * 重启
     */


    /**
     * 更新失败提示
     */
//    private void  Error_Display() {
//        Check_update_status = 0;
//        mCheck_update.setEnabled(false);
//        mCheck_update.setBackgroundResource(R.drawable.disabled_button);
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle(R.string.update_tips);
//        builder.setMessage(R.string.error);
//        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                mCheck_update.setText(R.string.check_update);
//                mCheck_update.setEnabled(true);
//                mCheck_update.setBackgroundResource(R.drawable.custom_button);
//                aosp_OTA_back_btn.setVisibility(View.VISIBLE);
//
//            }
//        });
//        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
//            @Override
//            public void onCancel(DialogInterface dialog) {
//                mCheck_update.setText(R.string.check_update);
//                mCheck_update.setEnabled(true);
//                mCheck_update.setBackgroundResource(R.drawable.custom_button);
//                aosp_OTA_back_btn.setVisibility(View.VISIBLE);
//
//            }
//        });
//        builder.setCancelable(false); //禁止用户点击对话框外部取消对话框
//        AlertDialog dialog = builder.create();
//        dialog.show();
//    }
    private void Error_Display() {
        Check_update_status = 0;

//        mCheck_update.setEnabled(false);
//        mCheck_update.setBackgroundResource(R.drawable.disabled_button);
        mCheck_update.setVisibility(View.INVISIBLE);

        showCustomDialog(
                this,
                getString(R.string.update_failed),   // title
                getString(R.string.error),         // message
                getString(R.string.ok),            // YES text
                null,                              // NO button hidden
                () -> {
                    // ✅ SAME logic as before (OK click)
//                    mCheck_update.setText(R.string.check_update);
//                    mCheck_update.setEnabled(true);
//                    mCheck_update.setBackgroundResource(R.drawable.custom_button);
                    aosp_OTA_back_btn.setVisibility(View.VISIBLE);
                    finish();
                },
                null
        );
    }

    /**
     *
     * @param JsonBuf 需要解析的json 字符串
     * @param platform 设备平台名称
     * @return
     */
    public String string_getURL(String JsonBuf, String platform) {
        if (JsonBuf == null) {
            Log.e(TAG, "JsonBuf is null");
            return null;
        }

        JSONObject jo = JSON.parseObject(JsonBuf);

        if (jo == null || !jo.containsKey(platform)) {
            Log.e(TAG, "Invalid JSON or missing platform: " + platform);
            return null;
        }

        JSONObject platformObj = jo.getJSONObject(platform);

        version = platformObj.getString("version");
        url = platformObj.getString("url");
        tips = platformObj.getString("tips");
        author = platformObj.getString("author");

        return url;
    }



}
