package com.fanconn.rideOS.local_ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.fanconn.rideOS.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class SelectActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "SelectActivity";
    private Intent mIntent;
    private Button mLocal_update;
    private Button mUFD_local_up;
    private Button mUFD_local_down;
    private static final int FIRST_MOUNT_PATH = 1;
    private static final int SECOND_MOUNT_PATH = 2;
    private String mPrivatePath;
    private StorageManager mStorageManager;
    private BroadcastReceiver mBroadcastReceiver;
    private List<StorageVolume> mVolumeList;
    private static final int MEDIA_RECORDER_REQUEST = 0;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE" };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.local_update_select);

        getWindow().setStatusBarColor(getResources().getColor(R.color.white,null));//设置状态栏颜色
        getWindow().setNavigationBarColor(getResources().getColor(R.color.white,null));//设置导航栏颜色
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        if (isAllPermissionGranted()) {
            initData();
        } else {
            requestNecessaryPermissions();
        }
    }

    /**
     *返回按钮事件
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     *返回按钮事件
     */
    public void select_get_back(View view) {
        finish();
    }

    /**
     * 按钮点击事件
     * @param view The view that was clicked.
     */
    @Override
    public void onClick(View view) {
        int id = view.getId();

        if (id == R.id.Local_update) {
            startActivity(mIntent);

        } else if (id == R.id.UFD_local_down) {
            moveJsonAndStartActivity(getStoragePath(SECOND_MOUNT_PATH));

        } else if (id == R.id.UFD_local_up) {
            moveJsonAndStartActivity(getStoragePath(FIRST_MOUNT_PATH));
        }
    }

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
     * 初始化数据
     */
    private void initData() {
        initView();

        mPrivatePath = getFilesDir().toString(); //获取当前应用程序的内部存储目录路径：/data/data/com.fanconn.balkue_sc126_ota
        Log.e(TAG, "initData: " + mPrivatePath );

        mStorageManager = (StorageManager) getSystemService(Activity.STORAGE_SERVICE);

        mIntent = new Intent();
        mIntent.setClass(SelectActivity.this, LocalUpdateActivity.class);

        registerBroadcast();

        initialFile();

        updateList();
    }

    /**
     * 绑定按钮控件和点击事件
     */
    private void initView() {
        mLocal_update = findViewById(R.id.Local_update);
        mUFD_local_up = findViewById(R.id.UFD_local_up);
        mUFD_local_down = findViewById(R.id.UFD_local_down);

        mLocal_update.setOnClickListener(this);
        mUFD_local_up.setOnClickListener(this);
        mUFD_local_down.setOnClickListener(this);
    }

    private void registerBroadcast() {
        mBroadcastReceiver = new BrSDPath();
        IntentFilter filter = new IntentFilter();
        filter.addDataScheme("file");
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);

        registerReceiver(mBroadcastReceiver, filter);
    }

    class BrSDPath extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateList();
        }
    }

    private void updateList() {
        mVolumeList = mStorageManager.getStorageVolumes();
        for (StorageVolume volume : mVolumeList) {
            String des = volume.getDescription(getApplicationContext());
            Log.i(TAG, "des = " + des);
            Log.i(TAG, "volume.getState() = " + volume.getState());
            Log.i(TAG, "volume.getUuid() = " + volume.getUuid());
            Log.i(TAG, "volume.toString() = " + volume.toString());
            Log.i(TAG, "volume.isEmulated() = " + volume.isEmulated());
            Log.i(TAG, "volume.isPrimary() = " + volume.isPrimary());
            Log.i(TAG, "volume.isRemovable() = " + volume.isRemovable());
        }
        switch (mVolumeList.size()) {
            case 1:
                mUFD_local_down.setVisibility(View.INVISIBLE);
                mUFD_local_up.setVisibility(View.INVISIBLE);
                break;
            case 2:
                mUFD_local_down.setVisibility(View.INVISIBLE);
                mUFD_local_up.setText(mVolumeList.get(1).getDescription(getApplicationContext()));
                mUFD_local_up.setVisibility(View.VISIBLE);
                break;
            case 3:
                mUFD_local_down.setText(mVolumeList.get(2).getDescription(getApplicationContext()));
                mUFD_local_up.setText(mVolumeList.get(1).getDescription(getApplicationContext()));
                mUFD_local_down.setVisibility(View.VISIBLE);
                mUFD_local_up.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    private void initialFile() {
        File file = new File(Paths.get(mPrivatePath, "configs").toString());
        if (!file.exists()) {
            try {
                Files.createDirectory(Paths.get(mPrivatePath, "configs"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
//        deleteJson();
    }

    private void moveJsonAndStartActivity(String path) {
        Log.i(TAG, "Path : " + path);
        if (isJsonUpdateFilesExist(path)) {
            deleteJson();
            File jsonFile = new File(path + "/config.json");
            File file = new File(mPrivatePath + "/configs/config.json");
            copyJsonToDataPath(jsonFile.getAbsolutePath(), file);
            writeUrlToJson(file, "file://" + path + "/update.zip");
            mIntent.putExtra("path", path);
            startActivity(mIntent);
        } else {
            Toast.makeText(SelectActivity.this, R.string.json_update_not_exist, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isJsonUpdateFilesExist(String path) {
        File updateFile = new File(path + "/update.zip");
        File jsonFile = new File(path + "/config.json");
        if (!updateFile.exists() || !jsonFile.exists()) {
            return false;
        }
        return true;
    }

    private void deleteJson() {
        File file = new File(Paths.get(mPrivatePath, "configs").toString());
        File[] fileList = file.listFiles();
        for (File temp : fileList) {
            if (temp.getName().endsWith("json")) {
                temp.delete();
            }
        }
    }

    private boolean copyJsonToDataPath(String source, File target) {
        if (target == null) {
            Log.e(TAG, "target parameter error");
            return false;
        }

        if (source == null || "".equals(source)) {
            Log.e(TAG, "source parameter error");
            return false;
        }

        Path path = Paths.get(source);
        Log.e(TAG, "balkue path："+ path);
        try {
            Files.copy(path, new FileOutputStream(target));
            Log.i(TAG, "copyJsonToDataPath OK");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public void writeUrlToJson(File f, String newUrl) {
        String json = null;
        JSONObject o = null;

        try {
            json = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (null == json) {
            return;
        }

        try {
            o = new JSONObject(json);
//            o.put("url", newUrl);
            json = json.replace(o.getString("url"), newUrl);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
//        Log.d(TAG, "" + new String(bytes, StandardCharsets.UTF_8));

        try {
            Files.deleteIfExists(f.toPath());
            Files.write(f.toPath(), bytes, StandardOpenOption.CREATE_NEW);
            Log.i(TAG, "writeUrlToJson OK");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getStoragePath(int i) {
        try {
            // Method getVolumePathsMethod = StorageManager.class.getMethod("getVolumePaths", null);
            Method getVolumePathsMethod = StorageManager.class.getMethod("getVolumePaths", (Class<?>[]) null);

            //String[] paths = (String[]) getVolumePathsMethod.invoke(mStorageManager, null);
            String[] paths = (String[]) getVolumePathsMethod.invoke(mStorageManager, (Object[]) null);

            Log.i(TAG, paths[i]);
            return paths[i];
        } catch (Exception e) {
            Log.e(TAG, "getStoragePath() failed", e);
        }
        return null;
    }

}
