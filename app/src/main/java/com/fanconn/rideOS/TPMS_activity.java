package com.fanconn.rideOS;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class TPMS_activity extends AppCompatActivity {
    private static final String TAG = "TPMS-Activity";

    // Views
    private ImageView backbtntpms;
    private TextView frontPressureView, frontTempView, rearPressureView, rearTempView;
    private TextView bluetoothWarningText;
    private TextView statusText, rearStatus, frontStatus;

    // Receiver for TPMS updates
    private BroadcastReceiver tpmsReceiver;

    // Bluetooth state receiver
// Bluetooth state receiver
    // Bluetooth state receiver
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (bluetoothWarningText == null) return;

                if (state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_TURNING_OFF) {
                    bluetoothWarningText.setVisibility(View.VISIBLE);
                    bluetoothWarningText.setText("Device Bluetooth OFF — sensor data may not be updated.");
                } else if (state == BluetoothAdapter.STATE_ON) {
                    bluetoothWarningText.setVisibility(View.GONE);

                    // 🔄 Ask service to restart scanning instead of killing it
                    Log.d(TAG, "Bluetooth ON → sending ACTION_TPMS_RESTART");
                    LocalBroadcastManager.getInstance(context)
                            .sendBroadcast(new Intent(TpmsScanService.ACTION_TPMS_RESTART));

                    // After short delay, request cached values
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        LocalBroadcastManager.getInstance(context)
                                .sendBroadcast(new Intent(TpmsScanService.ACTION_TPMS_REQUEST));
                    }, 1000);
                }
            }
        }
    };

    private boolean frontLow = false;
    private boolean rearLow = false;
    private boolean frontAvailable = false;
    private boolean rearAvailable = false;

    private long lastFrontUpdate = 0L;
    private long lastRearUpdate  = 0L;

    // Sensible default timeout: 2 minutes (adjustable)
    private static final long SENSOR_TIMEOUT_MS = 12*60*60000;

    // UI updater (runs periodically to refresh timeout state)
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final Runnable uiTick = new Runnable() {
        @Override
        public void run() {
            // Recompute availability based on timeout and refresh UI
            long now = System.currentTimeMillis();
            if (frontAvailable && now - lastFrontUpdate > SENSOR_TIMEOUT_MS) {
                frontAvailable = false;
                Log.d(TAG, "Front sensor timed out");
            }
            if (rearAvailable && now - lastRearUpdate > SENSOR_TIMEOUT_MS) {
                rearAvailable = false;
                Log.d(TAG, "Rear sensor timed out");
            }
            updateUI();
            uiHandler.postDelayed(this, 1000); // tick every second (lightweight)
        }
    };

    private final MetadataManager metadata = MetadataManager.get();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tpms);

        // window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // bind views
        backbtntpms = findViewById(R.id.backbtntpms);
        frontPressureView = findViewById(R.id.frontpressurevalue);
        frontTempView = findViewById(R.id.fronttempvalue);
        rearPressureView = findViewById(R.id.rearpressurevalue);
        rearTempView = findViewById(R.id.reartempvalue);
        bluetoothWarningText = findViewById(R.id.tpmsbluetoothstatus);
        statusText = findViewById(R.id.tpmsStatusText);
        rearStatus = findViewById(R.id.reartpmsIdealStatus);
        frontStatus = findViewById(R.id.fronttpmsIdealStatus);

        // back button -> go to main (clearing quicksettings)
        backbtntpms.setOnClickListener(v -> {
            Intent intent = new Intent(TPMS_activity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        // initial bluetooth UI
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothWarningText != null) {
            if (adapter != null && adapter.isEnabled()) {
                bluetoothWarningText.setVisibility(TextView.GONE);
            } else {
                bluetoothWarningText.setVisibility(TextView.VISIBLE);
                bluetoothWarningText.setText("Device Bluetooth OFF — sensor data may not be updated.");
            }
        }

        // Prepare and register TPMS receiver
        prepareTpmsReceiver();
        Log.d(TAG, "onCreate complete. TPMS receiver prepared.");

        // Request cached values from service (so UI shows last known immediately)
        LocalBroadcastManager.getInstance(this)
                .sendBroadcast(new Intent(TpmsScanService.ACTION_TPMS_REQUEST));
    }

    private void prepareTpmsReceiver() {
        tpmsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!TpmsScanService.ACTION_TPMS_UPDATE.equals(intent.getAction())) return;

                try {
                    String mac = intent.getStringExtra(TpmsScanService.EXTRA_MAC);
                    int minor = intent.getIntExtra(TpmsScanService.EXTRA_MINOR, -1);

                    if (mac == null || minor < 0) {
                        Log.w(TAG, "Received invalid TPMS packet: mac=" + mac + " minor=" + minor);
                        return;
                    }

                    // Extract bytes — high byte = pressureRaw, low byte = tempRaw
                    int pressureRaw = (minor >> 8) & 0xFF;
                    int tempRaw = minor & 0xFF;

                    double pressureKpa = pressureRaw * 2.5;
                    double pressurePsi = pressureKpa * 0.145038;
                    int temperature = tempRaw - 40;

                    Log.d(TAG, String.format("TPMS packet | MAC=%s | minor=%d | pRaw=%d | tRaw=%d | psi=%.2f | °C=%d",
                            mac, minor, pressureRaw, tempRaw, pressurePsi, temperature));

                    // forward a raw numeric to telemetry (you may change this)
                    //TelemetryDataManager.getInstance().setTeleTirePressure(minor);

                    // Update sensor that matches MAC. Use TextUtils.equals for null-safety.
                    long now = System.currentTimeMillis();

                    if (!TextUtils.isEmpty(metadata.getFrontTPMS()) && TextUtils.equals(mac, metadata.getFrontTPMS())) {
                        frontLow = (pressurePsi < 20.0);
                        frontAvailable = true;
                        lastFrontUpdate = now;
                        if (frontPressureView != null) frontPressureView.setText(String.format("%.1f psi", pressurePsi));
                        if (frontTempView != null) frontTempView.setText(String.format("%d\u00B0C", temperature));
                        Log.d(TAG, "Front sensor updated");
                    }

                    if (!TextUtils.isEmpty(metadata.getRearTPMS()) && TextUtils.equals(mac, metadata.getRearTPMS())) {
                        rearLow = (pressurePsi < 20.0);
                        rearAvailable = true;
                        lastRearUpdate = now;
                        if (rearPressureView != null) rearPressureView.setText(String.format("%.1f psi", pressurePsi));
                        if (rearTempView != null) rearTempView.setText(String.format("%d\u00B0C", temperature));
                        Log.d(TAG, "Rear sensor updated");
                    }

                    // If MAC matches neither, log and ignore
                    if (!(TextUtils.equals(mac, metadata.getFrontTPMS()) || TextUtils.equals(mac, metadata.getRearTPMS()))) {
                        Log.w(TAG, "TPMS packet from unconfigured MAC: " + mac);
                    }

                    // Immediately update UI after processing
                    updateUI();
                } catch (Exception e) {
                    Log.e(TAG, "Error processing TPMS packet", e);
                }
            }
        };

        // Register with LocalBroadcastManager (service sends updates locally)
        IntentFilter filter = new IntentFilter(TpmsScanService.ACTION_TPMS_UPDATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(tpmsReceiver, filter);
        Log.d(TAG, "TPMS receiver registered with LocalBroadcastManager");
    }

    /**
     * updateUI reads per-sensor availability/low flags and writes UI.
     * This method is safe to call frequently.
     */
    private void updateUI() {
        if (statusText == null || rearStatus == null || frontStatus == null) {
            Log.w(TAG, "updateUI: UI elements missing");
            return;
        }

        // availability already updated by receiver; uiTick also marks timed-out sensors as unavailable

        if (frontAvailable && rearAvailable) {
            // both present
            if (frontLow && rearLow) {
                statusText.setText("Both tires critical. Inflate immediately for longer range and smooth ride");
                statusText.setTextColor(Color.YELLOW);
                frontStatus.setText("CRITICAL"); frontStatus.setTextColor(Color.RED);
                rearStatus.setText("CRITICAL"); rearStatus.setTextColor(Color.RED);

            } else if (frontLow) {
                statusText.setText("Front tire low pressure warning!");
                statusText.setTextColor(Color.YELLOW);
                frontStatus.setText("CRITICAL"); frontStatus.setTextColor(Color.RED);
                rearStatus.setText("IDEAL"); rearStatus.setTextColor(Color.parseColor("#3BFF21"));

            } else if (rearLow) {
                statusText.setText("Rear tire low pressure warning!");
                statusText.setTextColor(Color.YELLOW);
                rearStatus.setText("CRITICAL"); rearStatus.setTextColor(Color.RED);
                frontStatus.setText("IDEAL"); frontStatus.setTextColor(Color.parseColor("#FFC30F"));

            } else {
                statusText.setText("Both tires optimal. rideOS keeps you rolling with confidence.");
                statusText.setTextColor(Color.WHITE);
                frontStatus.setText("IDEAL"); frontStatus.setTextColor(Color.parseColor("#FFC30F"));
                rearStatus.setText("IDEAL"); rearStatus.setTextColor(Color.parseColor("#3BFF21"));
            }

        } else if (frontAvailable) {
            // only front
            if (frontLow) {
                statusText.setText("Front tire low pressure warning!");
                statusText.setTextColor(Color.YELLOW);
                frontStatus.setText("CRITICAL"); frontStatus.setTextColor(Color.RED);
            } else {
                statusText.setText("Front tire optimal. Rear sensor not detected.");
                statusText.setTextColor(Color.WHITE);
                frontStatus.setText("IDEAL"); frontStatus.setTextColor(Color.parseColor("#FFC30F"));
            }
            rearStatus.setText("No sensor detected"); //rearStatus.setTextColor(Color.GRAY);

        } else if (rearAvailable) {
            // only rear
            if (rearLow) {
                statusText.setText("Rear tire low pressure warning!");
                statusText.setTextColor(Color.YELLOW);
                rearStatus.setText("CRITICAL"); rearStatus.setTextColor(Color.RED);
            } else {
                statusText.setText("Rear tire optimal. Front sensor not detected.");
                statusText.setTextColor(Color.WHITE);
                rearStatus.setText("IDEAL"); rearStatus.setTextColor(Color.parseColor("#3BFF21"));
            }
            frontStatus.setText("No sensor detected"); //frontStatus.setTextColor(Color.GRAY);

        } else {
            // none
            statusText.setText("No TPMS sensors detected.");
            statusText.setTextColor(Color.GRAY);
            frontStatus.setText("No sensor detected"); //frontStatus.setTextColor(Color.GRAY);
            rearStatus.setText("No sensor detected"); //rearStatus.setTextColor(Color.GRAY);
        }
    }

    // lifecycle: register system bluetooth receiver and start UI tick in onStart; stop in onStop
    @Override
    protected void onStart() {
        super.onStart();
        // register bluetooth receiver (system-wide)
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothReceiver, filter);

        // start UI tick
        uiHandler.post(uiTick);
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            unregisterReceiver(bluetoothReceiver);
        } catch (Exception e) {
            // ignore if not registered
            Log.w(TAG, "bluetoothReceiver unregister failed: " + e.getMessage());
        }

        // stop UI tick
        uiHandler.removeCallbacks(uiTick);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister local TPMS receiver
        try {
            if (tpmsReceiver != null) {
                LocalBroadcastManager.getInstance(this).unregisterReceiver(tpmsReceiver);
                tpmsReceiver = null;
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to unregister tpmsReceiver: " + e.getMessage());
        }
        // cleanup handler
        uiHandler.removeCallbacksAndMessages(null);
        Log.d(TAG, "onDestroy: cleaned up");
    }

    float[] startY = new float[1];
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (GestureHelper.handleGesture(this, ev, startY)) {
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }
}
