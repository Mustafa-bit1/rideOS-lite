package com.fanconn.rideOS;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
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

    // UI
    private ImageView backbtntpms, tpmsBikeImg;
    private TextView frontPressureView, frontTempView;
    private TextView rearPressureView, rearTempView;
    private TextView bluetoothWarningText;
    private TextView statusText, rearStatus, frontStatus;

    // Receivers
    private BroadcastReceiver tpmsReceiver;

    private final MetadataManager metadata = MetadataManager.get();

    // Sensor states
    private boolean frontLow = false;
    private boolean rearLow = false;
    private boolean frontAvailable = false;
    private boolean rearAvailable = false;

    private long lastFrontUpdate = 0L;
    private long lastRearUpdate  = 0L;

    private int frontPressure = 0;
    private int frontTemp = 0;
    private int rearPressure = 0;
    private int rearTemp = 0;

    // timeout (very large – you already tuned)
    private static final long SENSOR_TIMEOUT_MS = 12 * 60 * 60000;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private final Runnable uiTick = new Runnable() {
        @Override
        public void run() {
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

            // ✔ Safe self-reference
            uiHandler.postDelayed(this, 1000);
        }
    };

    // Bluetooth receiver
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) return;

            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            if (bluetoothWarningText == null) return;

            if (state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_TURNING_OFF) {
                bluetoothWarningText.setVisibility(View.VISIBLE);
                bluetoothWarningText.setText("Device Bluetooth OFF — sensor data may not be updated.");
            } else if (state == BluetoothAdapter.STATE_ON) {
                bluetoothWarningText.setVisibility(View.GONE);

                LocalBroadcastManager.getInstance(context)
                        .sendBroadcast(new Intent(TpmsScanService.ACTION_TPMS_RESTART));

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    LocalBroadcastManager.getInstance(context)
                            .sendBroadcast(new Intent(TpmsScanService.ACTION_TPMS_REQUEST));
                }, 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tpms);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        TextView powered = findViewById(R.id.powered_message);
        String text = "Powered by alerTire";
        SpannableString spannable = new SpannableString(text);
// index of 'T'
        int start = text.indexOf("T");
        int end = start + 1;
        spannable.setSpan(
                new ForegroundColorSpan(Color.parseColor("#FF7A00")),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        powered.setText(spannable);

        bindViews();
        setupBluetoothUI();
        setupBackButton();
        prepareTpmsReceiver();
//        InactivityManager.getInstance().stopTimer();

        LocalBroadcastManager.getInstance(this)
                .sendBroadcast(new Intent(TpmsScanService.ACTION_TPMS_REQUEST));
    }

    private void bindViews() {
        backbtntpms = findViewById(R.id.backbtntpms);
        frontPressureView = findViewById(R.id.frontpressurevalue);
        frontTempView = findViewById(R.id.fronttempvalue);
        rearPressureView = findViewById(R.id.rearpressurevalue);
        rearTempView = findViewById(R.id.reartempvalue);
        bluetoothWarningText = findViewById(R.id.tpmsbluetoothstatus);
        statusText = findViewById(R.id.tpmsStatusText);
        rearStatus = findViewById(R.id.reartpmsIdealStatus);
        frontStatus = findViewById(R.id.fronttpmsIdealStatus);
        tpmsBikeImg = findViewById(R.id.tpmsbikeIMG);
    }

    private void setupBackButton() {
        backbtntpms.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void setupBluetoothUI() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null && adapter.isEnabled()) {
            bluetoothWarningText.setVisibility(View.GONE);
        } else {
            bluetoothWarningText.setVisibility(View.VISIBLE);
            bluetoothWarningText.setText("Device Bluetooth OFF — sensor data may not be updated.");
        }
    }
    /**
     * FINAL receiver: receives FINAL PSI + TEMP packed by service
     */
    private void prepareTpmsReceiver() {
        tpmsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!TpmsScanService.ACTION_TPMS_UPDATE.equals(intent.getAction())) return;

                String mac = intent.getStringExtra(TpmsScanService.EXTRA_MAC);
                int minor = intent.getIntExtra(TpmsScanService.EXTRA_MINOR, -1);
                if (mac == null || minor < 0) return;

                int psi = (minor >> 8) & 0xFF;
                int temp = minor & 0xFF;

                long now = System.currentTimeMillis();

                Log.d(TAG, "TPMS FINAL | " + mac + " | PSI=" + psi + " | TEMP=" + temp);

                if (TextUtils.equals(mac, metadata.getFrontTPMS())) {
                    frontAvailable = true;
                    lastFrontUpdate = now;
                    frontPressure = psi;
                    frontTemp = temp;
                    frontLow = psi < 27;

                    frontPressureView.setText(psi + " psi");
                    frontTempView.setText(temp + "°C");
                }

                if (TextUtils.equals(mac, metadata.getRearTPMS())) {
                    rearAvailable = true;
                    lastRearUpdate = now;
                    rearPressure = psi;
                    rearTemp = temp;
                    rearLow = psi < 27;

                    rearPressureView.setText(psi + " psi");
                    rearTempView.setText(temp + "°C");
                }

                updateUI();
            }
        };

        LocalBroadcastManager.getInstance(this).registerReceiver(
                tpmsReceiver,
                new IntentFilter(TpmsScanService.ACTION_TPMS_UPDATE)
        );
    }

    private void updateUI() {

        if (frontAvailable && rearAvailable) {

            if (frontLow && rearLow) {
                statusText.setText("Both tires critical. Inflate immediately.");
                statusText.setTextColor(Color.YELLOW);
                frontStatus.setText("CRITICAL"); frontStatus.setTextColor(Color.RED);
                rearStatus.setText("CRITICAL");  rearStatus.setTextColor(Color.RED);
                tpmsBikeImg.setImageResource(R.drawable.tpmsrr);

            } else if (frontLow) {
                statusText.setText("Front tire low pressure warning!");
                statusText.setTextColor(Color.YELLOW);
                frontStatus.setText("CRITICAL"); frontStatus.setTextColor(Color.RED);
                rearStatus.setText("IDEAL");    rearStatus.setTextColor(Color.parseColor("#3BFF21"));
                tpmsBikeImg.setImageResource(R.drawable.tpmsgr);

            } else if (rearLow) {
                statusText.setText("Rear tire low pressure warning!");
                statusText.setTextColor(Color.YELLOW);
                rearStatus.setText("CRITICAL"); rearStatus.setTextColor(Color.RED);
                frontStatus.setText("IDEAL");   frontStatus.setTextColor(Color.parseColor("#3BFF21"));
                tpmsBikeImg.setImageResource(R.drawable.tpmsrg);

            } else {
                statusText.setText("Both tires optimal. rideOS keeps you rolling.");
                statusText.setTextColor(Color.WHITE);
                frontStatus.setText("IDEAL"); frontStatus.setTextColor(Color.parseColor("#3BFF21"));
                rearStatus.setText("IDEAL");  rearStatus.setTextColor(Color.parseColor("#3BFF21"));
                tpmsBikeImg.setImageResource(R.drawable.tpmsgg);
            }

        } else if (frontAvailable) {

            statusText.setText(frontLow ? "Front tire low pressure!" : "Front tire optimal.");
            frontStatus.setText(frontLow ? "CRITICAL" : "IDEAL");
            frontStatus.setTextColor(frontLow ? Color.RED : Color.parseColor("#3BFF21"));
            rearStatus.setText("No sensor detected");
            tpmsBikeImg.setImageResource(frontLow ? R.drawable.tpmsrr : R.drawable.tpmsrg);

        } else if (rearAvailable) {

            statusText.setText(rearLow ? "Rear tire low pressure!" : "Rear tire optimal.");
            rearStatus.setText(rearLow ? "CRITICAL" : "IDEAL");
            rearStatus.setTextColor(rearLow ? Color.RED : Color.parseColor("#3BFF21"));
            frontStatus.setText("No sensor detected");
            tpmsBikeImg.setImageResource(rearLow ? R.drawable.tpmsrr : R.drawable.tpmsgr);

        } else {
            statusText.setText("No TPMS sensors detected.");
            statusText.setTextColor(Color.GRAY);
            frontStatus.setText("No sensor detected");
            rearStatus.setText("No sensor detected");
            tpmsBikeImg.setImageResource(R.drawable.tpmsgg);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(bluetoothReceiver,
                new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        uiHandler.post(uiTick);
    }

    @Override
    protected void onStop() {
        super.onStop();
        try { unregisterReceiver(bluetoothReceiver); } catch (Exception ignored) {}
        uiHandler.removeCallbacks(uiTick);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(tpmsReceiver);
        } catch (Exception ignored) {}
        uiHandler.removeCallbacksAndMessages(null);
    }

    float[] startY = new float[1];
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return GestureHelper.handleGesture(this, ev, startY)
                || super.dispatchTouchEvent(ev);
    }
}
