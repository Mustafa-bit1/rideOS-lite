package com.fanconn.rideOS;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.lang.reflect.Method;

public class debugPage extends AppCompatActivity {

    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable screenToggleRunnable;
    private boolean isRunning = false;

    private ImageButton backbtn;
    private SerialPortManager serialPortManager; // assumed initialized elsewhere
    TextView deviceNameView, buildNumberView, bluetoothNameView, wifiSsidView;

    String expectedDeviceName = "RIVOT-nx100";
    String expectedBuildNumber = "3.1";
    String expectedBluetoothName = "nx100";
    String expectedSSID = "RIVOT-care";
    Button btnStartDebug;
    Button btnStopDebug;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug_page);

        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK |
                        PowerManager.ACQUIRE_CAUSES_WAKEUP |
                        PowerManager.ON_AFTER_RELEASE,
                "MyApp:WakeLockTag"
        );

        setupToggleRunnable();
        serialPortManager = serialApplication.getSerialPortManager();

        btnStartDebug = findViewById(R.id.btnStartDebug);
        btnStopDebug = findViewById(R.id.btnStopDebug);

// Initially: Stop is disabled, Start is enabled
        btnStartDebug.setEnabled(true);
        btnStopDebug.setEnabled(false);

        btnStartDebug.setOnClickListener(v -> {
            startSequence();
            btnStartDebug.setEnabled(false);  // Disable start
            btnStopDebug.setEnabled(true);   // Enable stop
        });

        btnStopDebug.setOnClickListener(v -> {
            stopSequence();
            btnStartDebug.setEnabled(true);  // Enable start
            btnStopDebug.setEnabled(false); // Disable stop
        });

        backbtn = findViewById(R.id.backbtn);
        backbtn.setEnabled(true);

        backbtn.setOnClickListener(v -> {
            if (isRunning) {
//                Toast.makeText(debugPage.this, "Please stop debug first.", Toast.LENGTH_SHORT).show();
                showCustomToast("Debug ON, Please STOP Debug Mode to exit");
            } else {
                finish(); // only exit if debug is not running
            }
        });


        initializeViews();
        checkDeviceInfo();

        ledOnCounter = findViewById(R.id.ledOnCounter);
        handleOpenCounter = findViewById(R.id.handleOpenCounter);
        handleCloseCounter = findViewById(R.id.handleCloseCounter);
        seatLockCounter = findViewById(R.id.seatLockCounter);
        ledOffCounter = findViewById(R.id.ledOffCounter);

        findViewById(R.id.btnResetCounters).setOnClickListener(v -> resetCounters());

    }
    private void resetCounters() {
        ledOnCount = handleOpenCount = handleCloseCount = seatLockCount = ledOffCount = 0;
        updateCounterViews();
    }
    private void updateCounterViews() {
        ledOnCounter.setText("LED On: " + ledOnCount);
        handleOpenCounter.setText("Handle Lock Open: " + handleOpenCount);
        handleCloseCounter.setText("Handle Lock Close: " + handleCloseCount);
        seatLockCounter.setText("Seat Lock Activate: " + seatLockCount);
        ledOffCounter.setText("LED Off: " + ledOffCount);
    }

    private void startSequence() {
        if (!isRunning) {
            isRunning = true;
            backbtn.setEnabled(false); // disable back while running
            btnStartDebug.setEnabled(false);
            btnStopDebug.setEnabled(true);
            handler.post(screenToggleRunnable);
            Log.d("ScreenTest", "Sequence started.");
        }
    }


    private void stopSequence() {
        if (isRunning) {
            isRunning = false;
            handler.removeCallbacks(screenToggleRunnable);
            backbtn.setEnabled(true); // enable back button after stopping
          //  Log.d("ScreenTest", "Sequence stopped.");
        }
    }
    private int ledOnCount = 0, handleOpenCount = 0, handleCloseCount = 0, seatLockCount = 0, ledOffCount = 0;
    private TextView ledOnCounter, handleOpenCounter, handleCloseCounter, seatLockCounter, ledOffCounter;
    private boolean screenJustTurnedOn = false;

    private void setupToggleRunnable() {
        screenToggleRunnable = new Runnable() {
            int commandStep = 0;
            final Runnable self = this;

            @Override
            public void run() {
                if (!isRunning) return;

                // Step 0: Turn ON screen and wait 2s before sending LED ON
                if (commandStep == 0 && !screenJustTurnedOn) {
                    if (!wakeLock.isHeld()) {
                        wakeLock.acquire(2000); // Screen ON
                       // Log.d("ScreenTest", "Screen turned ON");
                    }
                    screenJustTurnedOn = true;
                    handler.postDelayed(this, 2000); // Wait 2s before LED ON
                    return;
                }

                switch (commandStep) {
                    case 0:
                        serialPortManager.VCUSendToMCU("FFAA550107000A01");
                        ledOnCount++;
                        break;
                    case 1:
                        serialPortManager.VCUSendToMCU("FFAA550101000A01");
                        handleOpenCount++;
                        break;
                    case 2:
                        serialPortManager.VCUSendToMCU("FFAA550101000A00");
                        handleCloseCount++;
                        break;
                    case 3:
                        serialPortManager.VCUSendToMCU("FFAA550102000A01");
                        seatLockCount++;
                        break;
                    case 4:
                        serialPortManager.VCUSendToMCU("FFAA550107000A00");
                        ledOffCount++;
                        break;
                }

                updateCounterViews();
               // Log.d("ScreenTest", "Sent command step: " + commandStep);

                commandStep++;
                if (commandStep > 4) {
                    commandStep = 0;
                    screenJustTurnedOn = false; // Reset for next cycle

                    // After LED off, turn off screen and repeat after 3s
                    handler.postDelayed(() -> {
                        try {
                            Method goToSleep = PowerManager.class.getMethod("goToSleep", long.class);
                            goToSleep.invoke(powerManager, SystemClock.uptimeMillis());
                           // Log.d("ScreenTest", "Screen turned OFF");
                        } catch (Exception e) {
                           // Log.e("ScreenTest", "Failed to turn off screen", e);
                        }

                        handler.postDelayed(self, 2000);
                    }, 2000);
                } else {
                    handler.postDelayed(self, 2000); // Continue command sequence
                }
            }
        };

    }

    private void initializeViews() {
        deviceNameView = findViewById(R.id.deviceNameView);
        buildNumberView = findViewById(R.id.buildNumberView);
        bluetoothNameView = findViewById(R.id.bluetoothNameView);
        wifiSsidView = findViewById(R.id.wifiSsidView);
    }

    private void checkDeviceInfo() {
        // 1. Device Name
        String deviceName = Settings.Global.getString(getContentResolver(), "device_name");
        if (deviceName == null) deviceName = Build.DEVICE;

        boolean deviceNameMatch = expectedDeviceName.equals(deviceName);
        deviceNameView.setText("Device Name: " + deviceName + (deviceNameMatch ? " ✅" : " ❌"));

        // 2. Build Number (only displaying, not comparing)
        String buildNumber = Build.DISPLAY;
        buildNumberView.setText("Build Number: " + buildNumber);

        // 3. Bluetooth Name
        String btName = "Unavailable";
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter != null) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                btName = adapter.getName();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        boolean btMatch = expectedBluetoothName.equals(btName);
        bluetoothNameView.setText("Bluetooth Name: " + btName + (btMatch ? " ✅" : " ❌"));

        // 4. Wi-Fi SSID
        String ssid = "Unavailable";
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo != null && wifiInfo.getSSID() != null) {
                    ssid = wifiInfo.getSSID().replace("\"", "");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        boolean ssidMatch = expectedSSID.equals(ssid);
        wifiSsidView.setText("WiFi SSID: " + ssid + (ssidMatch ? " ✅" : " ❌"));
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
}
