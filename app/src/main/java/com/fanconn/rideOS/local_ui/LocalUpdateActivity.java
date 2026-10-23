package com.fanconn.rideOS.local_ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.UpdateEngine;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.fanconn.rideOS.R;
import com.fanconn.rideOS.UpdateConfig;
import com.fanconn.rideOS.util.UpdateConfigs;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class LocalUpdateActivity extends Activity {
    private static final String TAG = "LocalUpdateActivity";
    private Button mStart_update;
    private TextView mTextProduct;
    private TextView mtextModulet;
    private TextView mtextVersions;
    private TextView mprogressBarState;
    private TextView  mprogressValue;
    private Spinner mSpinnerConfigs;
    private static final String SP_module = Build.BRAND + "\t" + Build.DEVICE;
    private static final String SP_product = Build.PRODUCT;
    private static final String SP_versions = Build.VERSION.INCREMENTAL;
    private int update_status;
    private ProgressBar mProgressBar;
    private List<UpdateConfig> mConfigs;
    private final UpdateManager mUpdateManager =
            new UpdateManager(new UpdateEngine(), new Handler());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.local_update);

        getWindow().setStatusBarColor(getResources().getColor(R.color.white,null));//设置状态栏颜色
        getWindow().setNavigationBarColor(getResources().getColor(R.color.white,null));//设置导航栏颜色
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        uiResetWidgets();
        loadUpdateConfigs();
        Resources systemResources = Resources.getSystem();
        int drawableId = systemResources.getIdentifier("blank_tile", "drawable", "android");
        Drawable drawable = systemResources.getDrawable(drawableId, null);
    }

    /**
     *返回按钮事件
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (update_status == UpdaterState.RUNNING) {
                    showStopUpdateDialog();
                } else if(update_status == UpdaterState.SLOT_SWITCH_REQUIRED){
                    showSlotSwitchDialog();
                }else {
                    finish();
                }

                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** 返回按钮事件  **/
    //返回
    public void local_get_back(View view) {
        if (update_status == UpdaterState.RUNNING) {
            showStopUpdateDialog();
        } else if(update_status == UpdaterState.SLOT_SWITCH_REQUIRED){
            showSlotSwitchDialog();
        }else {
            finish();
        }
    }

    /**
     * 开始更新 按钮点击事件
     * @param view
     */
    public void onStartUpdateClick(View view) {
        mStart_update.setEnabled(false);
        mStart_update.setBackgroundResource(R.drawable.disabled_button);

        if (update_status != 5 ){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.update_tips));
            builder.setMessage(getString(R.string.really_update));
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // 处理点击“确定”按钮的逻辑
                    File configFile = new File(getFilesDir(), "configs/config.json");
                    if(!configFile.exists()){
                        // configs 目录下没有 config.json 文件的处理
                        mStart_update.setEnabled(true);
                        mStart_update.setBackgroundResource(R.drawable.custom_button);
                        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR );
                        Toast.makeText(LocalUpdateActivity.this, getString(R.string.no_file), Toast.LENGTH_SHORT).show();
                        return;
                    } else {
                        if(isUpdateZipFileExists()) {
                            loadUpdateConfigs();
                            applyUpdate(getSelectedConfig());
                        } else {
                            mStart_update.setEnabled(true);
                            mStart_update.setBackgroundResource(R.drawable.custom_button);
                            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR );
                        }

                    }
                }
            });
            builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // 处理点击“取消”按钮的逻辑
                    mStart_update.setEnabled(true);
                    mStart_update.setBackgroundResource(R.drawable.custom_button);
                    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR );
                }
            });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    mStart_update.setEnabled(true);
                    mStart_update.setBackgroundResource(R.drawable.custom_button);
                }
            });
            builder.setCancelable(false); //禁止用户点击对话框外部取消对话框
            AlertDialog dialog = builder.create();
            dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR );
            dialog.show();
        } else {
            reboot();
        }
    }

    /**
     * 判断指定路径的文件是否存在
     * @return 如果文件存在则返回 true，否则返回 false
     */
    private boolean isUpdateZipFileExists() {
        String url = parseUrl("/data/data/com.fanconn.rideOS/files/configs/config.json");
        //Log.e(TAG, "balkue isUpdateZipFileExists：文件路径：" + url);
        String filePath = url.substring("file://".length()); // 去掉 "file://" 前缀

        try {
            File updateZipFile = new File(filePath);
            if (updateZipFile.exists()) {
                return true;
            } else {
                Log.e(TAG, "balkue isUpdateZipFileExists：" + filePath + " 文件不存在！");
                Toast.makeText(LocalUpdateActivity.this, getString(R.string.not_detected) + filePath +  getString(R.string.update_file), Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (SecurityException e) {
            Log.e(TAG, "balkue isUpdateZipFileExists：安全异常：" + filePath + " 文件无法访问!");
            Toast.makeText(LocalUpdateActivity.this, getString(R.string.security_exception) + filePath +  getString(R.string.cannot_accessed), Toast.LENGTH_SHORT).show();
            return false;
        } catch (Exception e) {
            Log.e(TAG, "balkue isUpdateZipFileExists：未知异常：" + e.getMessage());
            return false;
        }
    }

    /**
     * 解析 config.json 文件内容，并获取 url 字段的值
     *
     * @param filePath config.json 文件路径
     * @return url 字段的值
     */
    private String parseUrl(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            JSONObject json = new JSONObject(sb.toString());
            return json.getString("url");
        } catch (Exception e) {
            // 处理异常
            return null;
        }
    }

    private void applyUpdate(UpdateConfig config) {
        try {
            mUpdateManager.applyUpdate(this, config);
        } catch (UpdaterState.InvalidTransitionException e) {
            Log.e(TAG, "Failed to apply update " + config.getName(), e);
        }
    }

    private UpdateConfig getSelectedConfig() {
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
        } else {
            File configFile = new File(file, "config.json");
            if(!configFile.exists()){
                // configs目录下没有 config.json 文件的处理
                Toast.makeText(this, getString(R.string.no_file), Toast.LENGTH_SHORT).show();
            }
        }
        Log.e(TAG, "balkue loadUpdateConfigs：");
        mConfigs = UpdateConfigs.getUpdateConfigs(this);
        loadConfigsToSpinner(mConfigs);
    }

    private void loadConfigsToSpinner(List<UpdateConfig> configs) {
        String[] spinnerArray = UpdateConfigs.configsToNames(configs);
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                spinnerArray);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout
                .simple_spinner_dropdown_item);
        mSpinnerConfigs.setAdapter(spinnerArrayAdapter);
    }

    /** resets ui */
    private void uiResetWidgets() {
        this.mSpinnerConfigs = findViewById(R.id.spinnerConfigs);
        this.mTextProduct = findViewById(R.id.textProduct);
        this.mProgressBar = findViewById(R.id.progressBar);
        this.mprogressBarState = findViewById(R.id.progressBarState);
        this.mprogressValue = findViewById(R.id.progressValue);
        this.mStart_update = findViewById(R.id.Start_update);
        this.mtextModulet = findViewById(R.id.textModule);
        this.mtextVersions = findViewById(R.id.textVersions);

        mtextModulet.setText(SP_module);
        mTextProduct.setText(SP_product);
        mtextVersions.setText(SP_versions);

        HiddenControl();

        this.mUpdateManager.setOnStateChangeCallback(this::onUpdaterStateChange);
        this.mUpdateManager.setOnProgressUpdateCallback(this::onProgressUpdate);
    }

    /**
     * 显示进度条
     */
    private void DisplayControl() {
        mProgressBar.setVisibility(ProgressBar.VISIBLE);
        mprogressBarState.setVisibility(View.VISIBLE);
        mprogressValue.setVisibility(View.VISIBLE);

    }

    /**
     * 隐藏进度条
     */
    private void HiddenControl() {
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);
        mprogressBarState.setVisibility(View.INVISIBLE);
        mprogressValue.setVisibility(View.INVISIBLE);
        mSpinnerConfigs.setVisibility(View.INVISIBLE);

    }

    @Override
    protected void onDestroy() {
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
     * Invoked when update progress changes.
     * 实时获取升级更新进度
     */
    private void onProgressUpdate(double progress) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 更新进度条在 UI 线程中进行
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mProgressBar.setProgress((int) (100 * progress));
                        // 更新 TextView 显示的文本
                        String progressStr = String.format("%.0f", progress * 100) + "%";
                        mprogressValue.setText(progressStr);
                    }
                });
            }
        }).start();
    }

    /**
     * Invoked when SystemUpdaterSample app state changes.
     * Value of {@code state} will be one of the
     * values from {@link UpdaterState}.
     * 获取升级状态
     */
    private void onUpdaterStateChange(int state) {
        Log.i(TAG, "UpdaterStateChange state="
                + UpdaterState.getStateText(state)
                + "/" + state);
        runOnUiThread(() -> {
            if (state == UpdaterState.IDLE) {
                update_status = state;
                mProgressBar.setProgress(0);
                Log.e(TAG, "balkue UpdaterStateChange state=" + UpdaterState.IDLE);
            } else if (state == UpdaterState.RUNNING) {
                update_status = state;
                mprogressBarState.setText(getString(R.string.ota_update));
                DisplayControl();
                Log.e(TAG, "balkue UpdaterStateChange state=" + UpdaterState.RUNNING);
            } else if (state == UpdaterState.PAUSED) {
                update_status = state;
                DisplayControl();
                Log.e(TAG, "balkue UpdaterStateChange state=" + UpdaterState.PAUSED);
            } else if (state == UpdaterState.ERROR) {
                update_status = state;
                Log.e(TAG, "balkue UpdaterStateChange state=" + UpdaterState.ERROR);
                try {
                    mUpdateManager.resetUpdate(); //重置更新状态
                } catch (UpdaterState.InvalidTransitionException e) {
                    throw new RuntimeException(e);
                }
                HiddenControl();
                Error_Display();
            } else if (state == UpdaterState.SLOT_SWITCH_REQUIRED) {
                Log.e(TAG, "balkue UpdaterStateChange state=" + UpdaterState.SLOT_SWITCH_REQUIRED);
                Log.e(TAG, "balkue UpdaterStateChange 可以切换分区啦" );
                update_status = state;
                mprogressBarState.setText(getString(R.string.ota_switch));
                DisplayControl();
                mUpdateManager.setSwitchSlotOnReboot(); //切换分区
            } else if (state == UpdaterState.REBOOT_REQUIRED) {
                //  uiStateRebootRequired();
                Log.e(TAG, "balkue UpdaterStateChange state=" + UpdaterState.REBOOT_REQUIRED);
                Log.e(TAG, "balkue UpdaterStateChange 重启即可生效" );
                update_status = state;
                mStart_update.setBackgroundResource(R.drawable.custom_button);
                mStart_update.setText(getString(R.string.reboot));
                mStart_update.setEnabled(true);
                HiddenControl();
                reboot();
            }
        });
    }

    /**
     * 更新失败提示
     */
    private void  Error_Display() {
        update_status = 0;
        mStart_update.setEnabled(false);
        mStart_update.setBackgroundResource(R.drawable.disabled_button);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.update_tips);
        builder.setMessage(R.string.error);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mStart_update.setEnabled(true);
                mStart_update.setBackgroundResource(R.drawable.custom_button);
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR );
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                mStart_update.setEnabled(true);
                mStart_update.setBackgroundResource(R.drawable.custom_button);
            }
        });
        builder.setCancelable(false); //禁止用户点击对话框外部取消对话框
        AlertDialog dialog = builder.create();
        dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR );
        dialog.show();
    }

    /**
     * 重启
     */
    private void reboot() {
        mStart_update.setEnabled(false);
        mStart_update.setBackgroundResource(R.drawable.disabled_button);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.update_tips));
        builder.setMessage(getString(R.string.really_reboot));
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 在这里执行重启系统的操作
                try {
                    Runtime.getRuntime().exec("reboot");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mStart_update.setBackgroundResource(R.drawable.custom_button);
                mStart_update.setText(getString(R.string.reboot));
                mStart_update.setEnabled(true);
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR );
                // 取消操作，关闭对话框
                dialog.dismiss();
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                mStart_update.setEnabled(true);
                mStart_update.setBackgroundResource(R.drawable.custom_button);
            }
        });
        builder.setCancelable(false); //禁止用户点击对话框外部取消对话框
        AlertDialog dialog = builder.create();
        dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR );
        dialog.show();
    }

    /**
     * 停止更新
     */
    @Override
    public void onBackPressed() {
        if (update_status == UpdaterState.RUNNING) {
            showStopUpdateDialog();
        } else if(update_status == UpdaterState.SLOT_SWITCH_REQUIRED){
            showSlotSwitchDialog();
        }else {
            super.onBackPressed();
        }

    }

    public void showStopUpdateDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.stop_updating));
        builder.setMessage(getString(R.string.really_stop_update));
        builder.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                cancelRunningUpdate();
                HiddenControl();
                mStart_update.setEnabled(true);
                mStart_update.setBackgroundResource(R.drawable.custom_button);
                update_status = UpdaterState.PAUSED;
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR );
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setCancelable(false); //禁止用户点击对话框外部取消对话框
        AlertDialog dialog = builder.create();
        dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        dialog.show();

    }

    private void cancelRunningUpdate() {
        try {
            mUpdateManager.cancelRunningUpdate();
        } catch (UpdaterState.InvalidTransitionException e) {
            Log.e(TAG, "Failed to cancel running update", e);
        }
        //super.onBackPressed();
    }

    public void showSlotSwitchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.wait));
        builder.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setCancelable(false); //禁止用户点击对话框外部取消对话框
        AlertDialog dialog = builder.create();
        dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        dialog.show();
    }

}
