package com.fanconn.rideOS;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.fanconn.rideOS.menu.BrightnessManager;
import com.fanconn.rideOS.menu.settings;

import java.lang.reflect.Method;
import java.util.Set;

public class QuickSetting extends AppCompatActivity {

    private GestureDetector gestureDetector;
    private BrightnessManager brightnessManager;
    private BluetoothAdapter bluetoothAdapter;
    private final Handler handler = new Handler(Looper.getMainLooper());
    ImageButton tpmsBtn,allSetbtn;

    private static final int REQUEST_BLUETOOTH_CONNECT = 1001;

    private Button lteToggle;
    private TextView lteStatusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_quick_setting);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        gestureDetector = new GestureDetector(this, new QuickGestureListener());

        // Request WRITE_SETTINGS permission
        if (!Settings.System.canWrite(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

        tpmsBtn = findViewById(R.id.tpmsbtn);
        allSetbtn = findViewById(R.id.allSetbtn);


        tpmsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(QuickSetting.this, TPMS_activity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });

        allSetbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(QuickSetting.this,settings.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });



        // Init Brightness
        brightnessManager = new BrightnessManager(this);
        SeekBar brightnessSlider = findViewById(R.id.brightnessSlider);
        TextView autoBrightnessText = findViewById(R.id.autoBrightnessText);
        brightnessManager.applyToSeekBar(brightnessSlider, autoBrightnessText);

        // Volume
        SeekBar volumeSlider = findViewById(R.id.volumeSlider);
        ImageButton muteButton = findViewById(R.id.muteButton);
        brightnessManager.applyToVolumeSlider(volumeSlider, muteButton);

        // WiFi
        Button wifiToggle = findViewById(R.id.wifiToggle);
        TextView wifiStatusText = findViewById(R.id.wifiStatusText);
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        updateWifiUI(wifiManager, wifiToggle, wifiStatusText);
        wifiToggle.setOnClickListener(v -> {
            boolean enabled = wifiManager.isWifiEnabled();
            wifiManager.setWifiEnabled(!enabled);
            handler.postDelayed(() -> updateWifiUI(wifiManager, wifiToggle, wifiStatusText), 1500);
        });

        // Bluetooth
        bluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        Button bluetoothToggle = findViewById(R.id.bluetoothtoggle);
        TextView bluetoothStatusText = findViewById(R.id.bluetoothStatusText);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_CONNECT);
        } else {
            updateBluetoothUI(bluetoothToggle, bluetoothStatusText);
        }

        bluetoothToggle.setOnClickListener(v -> {
            if (bluetoothAdapter == null) {
                bluetoothStatusText.setText("Not supported");
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Bluetooth permission denied", Toast.LENGTH_SHORT).show();
                return;
            }

            if (bluetoothAdapter.isEnabled()) {
                // Show warning dialog before turning off
                createDialog(
                        "Warning",
                        "RIVOT smart key feature and tire pressure monitoring system may not work,  Are you sure you want to turn OFF the Bluetooth?",
                        "Turn Off",
                        () -> {
                            bluetoothAdapter.disable();
                            bluetoothToggle.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#555555")));
                            bluetoothStatusText.setText("");
                        }
                ).show();
            } else {
                bluetoothAdapter.enable();
                bluetoothToggle.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1C78BE")));
                bluetoothStatusText.setText("Turning on...");
                handler.postDelayed(() -> updateBluetoothUI(bluetoothToggle, bluetoothStatusText), 3000);
            }
        });


        // LTE / Mobile Data
        lteToggle = findViewById(R.id.ltetoggle);
        lteStatusText = findViewById(R.id.ltetoggleStatusText);
        updateMobileDataUI();

        lteToggle.setOnClickListener(v -> handleLteToggle());
    }

    // ---- WiFi ----
    private void updateWifiUI(WifiManager wifiManager, Button button, TextView statusText) {
        int customOnColor = Color.parseColor("#1C78BE");
        int customOffColor = Color.parseColor("#555555");
        if (wifiManager.isWifiEnabled()) {
            button.setBackgroundTintList(ColorStateList.valueOf(customOnColor));
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            String ssid = wifiInfo.getSSID();
            if (ssid != null && !ssid.equals("<unknown ssid>") && wifiInfo.getNetworkId() != -1) {
                statusText.setText(ssid.replace("\"", ""));
            } else {
                statusText.setText("Not connected");
            }
        } else {
            button.setBackgroundTintList(ColorStateList.valueOf(customOffColor));
            statusText.setText("");
        }
    }

    // ---- Bluetooth ----
    private void updateBluetoothUI(Button button, TextView statusText) {
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1C78BE")));
            String deviceName = getConnectedBluetoothDeviceName();
            statusText.setText(deviceName.isEmpty() ? "Not connected" : deviceName);
        } else {
            button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#555555")));
            statusText.setText("");
        }
    }

    private String getConnectedBluetoothDeviceName() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) return "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return "";
        }
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : bondedDevices) {
            int state = bluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET);
            if (state == BluetoothProfile.STATE_CONNECTED) {
                return device.getName();
            }
        }
        return "";
    }

    // ---- Mobile Data (LTE) ----
    private boolean hasSimCard() {
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (tm == null) return false;
        return tm.getSimState() == TelephonyManager.SIM_STATE_READY;
    }

    private void handleLteToggle() {
        if (!hasSimCard()) {
            Toast.makeText(this, "No SIM card detected", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean newState = !getMobileDataState();
        setMobileDataEnabled(newState);
        updateMobileDataUI();
    }

    private void updateMobileDataUI() {
        int customOnColor = Color.parseColor("#1C78BE");
        int customOffColor = Color.parseColor("#555555");

        boolean enabled = getMobileDataState();

        lteToggle.setEnabled(hasSimCard());
        lteToggle.setBackgroundTintList(ColorStateList.valueOf(enabled ? customOnColor : customOffColor));
        lteStatusText.setText(enabled ? "Data On" : "Data Off");
    }

    // ---- Reflection to control mobile data ----
    private boolean getMobileDataState() {
        try {
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            Method getDataEnabled = TelephonyManager.class.getDeclaredMethod("getDataEnabled");
            getDataEnabled.setAccessible(true);
            return (boolean) getDataEnabled.invoke(tm);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void setMobileDataEnabled(boolean enabled) {
        try {
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            Method setDataEnabled = TelephonyManager.class.getDeclaredMethod("setDataEnabled", boolean.class);
            setDataEnabled.setAccessible(true);
            setDataEnabled.invoke(tm, enabled);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Cannot toggle mobile data. Requires system privileges.", Toast.LENGTH_SHORT).show();
        }
    }

    // ---- Permissions ----
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_CONNECT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Button bluetoothToggle = findViewById(R.id.bluetoothtoggle);
                TextView bluetoothStatusText = findViewById(R.id.bluetoothStatusText);
                updateBluetoothUI(bluetoothToggle, bluetoothStatusText);
            } else {
                Toast.makeText(this, "Bluetooth permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ---- Gestures ----
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private class QuickGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float diffY = e2.getY() - e1.getY();
            if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffY < 0) {
                    finish();
                    return true;
                }
            }
            return false;
        }
    }

    private AlertDialog createDialog(String titleText, String messageText, String positiveText, Runnable positiveAction) {
        View dialogView = getLayoutInflater().inflate(R.layout.popupdialog, null);
        TextView title = dialogView.findViewById(R.id.dialogTitle);
        TextView message = dialogView.findViewById(R.id.dialogMessage);
        Button btnYes = dialogView.findViewById(R.id.btnYes);
        Button btnNo = dialogView.findViewById(R.id.btnNo);

        title.setText(titleText);
        message.setText(messageText);
        btnYes.setText(positiveText);
        btnNo.setText("Cancel");

        AlertDialog dialog = new AlertDialog.Builder(this)
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

}