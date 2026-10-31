package com.fanconn.rideOS;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.fanconn.rideOS.authen.LoginActivity;
import com.fanconn.rideOS.databinding.ActivityMainBinding;

/**
 * UI 更新类，负责更新主活动中的 UI 组件
 */
public class UIUpdater {

    private final ActivityMainBinding binding;
    private final Handler handlerTurnSignal;
    private final Context context;
    private static final long DOUBLE_TAP_INTERVAL_MS = 300;
    private long lastSpeedTapTime = 0;
    private int currentVehicleSpeed = 0;
    private final MainActivity activity; // Actual activity for dialogs, UI

    public UIUpdater(ActivityMainBinding binding, Context context) {
        this.binding = binding;
        this.handlerTurnSignal = new Handler();
        this.context = context;

        if (context instanceof MainActivity) {
            this.activity = (MainActivity) context;
        } else {
            throw new IllegalArgumentException("UIUpdater must be initialized with an Activity context");
        }
    }

    /**
     * 更新 UI 组件
     * @param hb1 解析后的 Heartbeat1 对象
     */
    public void updateHeartbeat1UI(Heartbeat1 hb1) {
        // Show the headlight image only when ON
        if (hb1.getHeadlights() == 1) {
            binding.highBeamIV.setVisibility(View.VISIBLE);
            binding.highBeamIV.setImageResource(R.drawable.lamp_on);
        } else {
            binding.highBeamIV.setVisibility(View.INVISIBLE);
        }
        // 更新转向灯状态
        updateTurnSignalImages(hb1.getTurnSignal());
    }

    /**
     * 更新 ImageView 的图片资源
     * @param imageView 需要更新的 ImageView
     * @param resId 图片资源 ID
     */
    private void updateImageView(ImageView imageView, int resId) {
        imageView.setImageResource(resId);
    }

    /**
     * 更新 TextView 的文本内容
     * @param textView 需要更新的 TextView
     * @param resId 字符串资源 ID
     */
    private void updateTextView(TextView textView, int resId) {
        textView.setText(resId);
    }

    /**
     * 更新 TextView 的文本内容
     * @param textView 需要更新的 TextView
     * @param text 文本内容
     */
    private void updateTextView(TextView textView, String text) {
        textView.setText(text);
    }

    /**
     * 获取手势状态对应的文本
     * @param context 上下文环境
     * @param gesture 手势状态
     * @return 对应的手势状态文本
     */
    private String getGestureStatusText(Context context, int gesture) {
        switch (gesture) {
            case 0:
                return context.getString(R.string.nothing);
            case 1:
                return context.getString(R.string.left);
            case 2:
                return context.getString(R.string.right);
            default:
                return "";
        }
    }


    // Update turn signal images based on the current state
    private void updateTurnSignalImages(int turnSignalState) {

        // Remove previously scheduled animation callbacks
        handlerTurnSignal.removeCallbacksAndMessages(null);

        switch (turnSignalState) {
            case 0:
                // Indicators OFF: hide both indicators
                binding.turnLeftIV.setVisibility(View.INVISIBLE);
                binding.turnRightIV.setVisibility(View.INVISIBLE);
                stopTurnSignalAnimation();
                break;

            case 1:
                // Left indicator ON: use left_on and blink without grey images
                binding.turnLeftIV.setVisibility(View.VISIBLE);
                binding.turnRightIV.setVisibility(View.INVISIBLE);

                binding.turnLeftIV.setImageResource(R.drawable.left_on);

                startTurnSignalAnimation(
                        R.drawable.left_on,
                        R.drawable.left_on,
                        binding.turnLeftIV
                );
                break;

            case 2:
                // Right indicator ON: use right_on and blink without grey images
                binding.turnLeftIV.setVisibility(View.INVISIBLE);
                binding.turnRightIV.setVisibility(View.VISIBLE);

                binding.turnRightIV.setImageResource(R.drawable.right_on);

                startTurnSignalAnimation(
                        R.drawable.right_on,
                        R.drawable.right_on,
                        binding.turnRightIV
                );
                break;

            case 3:
                // Hazard lights ON
                binding.turnLeftIV.setVisibility(View.VISIBLE);
                binding.turnRightIV.setVisibility(View.VISIBLE);

                binding.turnLeftIV.setImageResource(R.drawable.left_on);
                binding.turnRightIV.setImageResource(R.drawable.right_on);

                startFaultSignalAnimation();
                break;

            default:
                // Invalid state: hide both indicators
                binding.turnLeftIV.setVisibility(View.INVISIBLE);
                binding.turnRightIV.setVisibility(View.INVISIBLE);
                stopTurnSignalAnimation();
                break;
        }
    }

    // Update turn signal drawable resources
    private void updateTurnSignal(int leftRes, int rightRes, boolean animated) {

        binding.turnLeftIV.setImageResource(leftRes);
        binding.turnRightIV.setImageResource(rightRes);

        if (!animated) {
            stopTurnSignalAnimation();
        }
    }

    // Start left/right indicator blinking animation
    private void startTurnSignalAnimation(
            final int initialRes,
            final int alternateRes,
            final ImageView imageView) {

        imageView.setImageResource(initialRes);

        handlerTurnSignal.postDelayed(new Runnable() {
            private boolean isVisible = true;

            @Override
            public void run() {
                if (imageView.getVisibility() != View.VISIBLE) {
                    return;
                }

                isVisible = !isVisible;
                imageView.setVisibility(isVisible ? View.VISIBLE : View.INVISIBLE);

                handlerTurnSignal.postDelayed(this, 500);
            }
        }, 500);
    }

    // Start hazard light blinking animation
    private void startFaultSignalAnimation() {

        handlerTurnSignal.postDelayed(new Runnable() {
            private boolean isVisible = true;

            @Override
            public void run() {
                isVisible = !isVisible;

                binding.turnLeftIV.setVisibility(
                        isVisible ? View.VISIBLE : View.INVISIBLE
                );
                binding.turnRightIV.setVisibility(
                        isVisible ? View.VISIBLE : View.INVISIBLE
                );

                handlerTurnSignal.postDelayed(this, 500);
            }
        }, 500);
    }
    /**
     * 切换 ImageView 的图片资源
     * @param imageView 需要切换的 ImageView
     * @param res1 图片资源1
     * @param res2 图片资源2
     */
    private void toggleImageResource(ImageView imageView, int res1, int res2) {
        if (imageView.getDrawable().getConstantState().equals(imageView.getContext().getResources().getDrawable(res1).getConstantState())) {
            imageView.setImageResource(res2);
        } else {
            imageView.setImageResource(res1);
        }
    }

    /**
     * 停止更新和动画
     */
    public void stopUpdating() {
        stopChargingUiWatcher();
        stopTurnSignalAnimation();
    }

    /**
     * 停止转向灯动画和定时任务
     */
    private void stopTurnSignalAnimation() {
        handlerTurnSignal.removeCallbacksAndMessages(null);
    }

    /**
     * 更新 UI 组件
     * @param hb2 解析后的 Heartbeat2 对象
     */
    public void updateHeartbeat2UI(Heartbeat2 hb2) {
        // 更新六轴数据
        updateSixAxisData(hb2);
    }

    /**
     * 更新六轴数据
     * @param hb2 解析后的 Heartbeat2 对象
     */
    private void updateSixAxisData(Heartbeat2 hb2) {
//        double xuAcc = hb2.getXuAcc() / 1000000.0;
//        double yuAcc = hb2.getYuAcc() / 1000000.0;
//        double zuAcc = hb2.getZuAcc() / 1000000.0;
//        double xGyro = hb2.getXGyro() / 1000000.0;
//        double yGyro = hb2.getYGyro() / 1000000.0;
//        double zGyro = hb2.getZGyro() / 1000000.0;
//
//        String sixAxisData = String.format("uAcc  ：%.6f  %.6f  %.6f\nuGyro：%.6f  %.6f  %.6f", xuAcc, yuAcc, zuAcc, xGyro, yGyro, zGyro);
//
//        binding.sixAxisDateTV.setText(sixAxisData);
    }

    /**
     * 更新 UI 组件
     * @param hb3 解析后的 Heartbeat3 对象
     */
    public void updateHeartbeat3UI(Heartbeat3 hb3) {
        switch (hb3.getCanId()) {
            case "18FF50E5":
                updateCanData18FF50E5(hb3);
                break;
            case "18904001":
                updateCanData18904001(hb3);
                break;
            case "10F807A8":
                updateCanData10F807A8(hb3);
                break;
            case "10F907A8":
                updateCanData10F907A8(hb3);
                break;
            case "10FA07A8":
                updateCanData10FA07A8(hb3);
                break;
            case "10FB07A8":
                updateCanData10FB07A8(hb3);
                break;
            case "10FC07A8":
                updateCanData10FC07A8(hb3);
                break;
            case "10FD07A8":
                updateCanData10FD07A8(hb3);
                break;
            default:
                // 处理未知的 CAN ID
                break;
        }
    }

    /**
     * 更新 CAN 数据 18904001
     * @param hb3 解析后的 Heartbeat3 对象
     */
    public void updateCanData18904001(Heartbeat3 hb3) {
        // Get battery SOC
        int batteryLevel = hb3.getBatteryLevel() / 10;
Log.d("BATTRY","THIS IS BAT UI"+batteryLevel);
        // Update only for valid SOC values (0–100%)
        if (batteryLevel >= 0 && batteryLevel <= 100) {
            binding.batteryTV.setText("Battery " + batteryLevel + "%");
        }

        activity.runOnUiThread(() -> {
            binding.batteryArc.setBatteryLevel(batteryLevel);
        });
    }
    private int previousGearState = -1;
    public boolean isStand=false;

    public void updateCanData10F807A8(Heartbeat3 hb3) {

        int directionalGears = FrameUtils.extractBits(
                hb3.getBSG1_BYTE1(), 0, 1
        );

        int STAND = FrameUtils.extractBits(
                hb3.getBSG1_BYTE2(), 0, 1
        );

        isStand = (STAND == 1);

        // Send handle unlock command only when gear state changes
        if (directionalGears != previousGearState) {

            if (directionalGears == 1 || directionalGears == 2) {
                SerialPortManager.getInstance()
                        .VCUSendToMCU("FFAA550101000A01");
            }

            previousGearState = directionalGears;
        }

        // Update gear image
        if (isStand) {
            // Stand is open
            binding.gearImg.setImageResource(R.drawable.sidestandicon);
        } else {
            // Stand is closed: show image based on gear
            switch (directionalGears) {
                case 0:
                    binding.gearImg.setImageResource(R.drawable.neutralicon);
                    break;

                case 1:
                    binding.gearImg.setImageResource(R.drawable.forwardicon);
                    break;

                case 2:
                    binding.gearImg.setImageResource(R.drawable.reverseicon);
                    break;
            }
        }
    }

    public void updateCanData18FF50E5(Heartbeat3 hb3) {
        refreshChargingIndicator();
    }

    private static final long CHARGING_UI_REFRESH_MS = 500L;

    private final Handler chargingUiHandler =
            new Handler(Looper.getMainLooper());

    private boolean chargingUiWatcherStarted = false;

    private final Runnable chargingUiWatcher = new Runnable() {
        @Override
        public void run() {
            if (!chargingUiWatcherStarted) {
                return;
            }

            refreshChargingIndicator();

            chargingUiHandler.postDelayed(
                    this,
                    CHARGING_UI_REFRESH_MS
            );
        }
    };

    public void startChargingUiWatcher() {
        if (chargingUiWatcherStarted) {
            return;
        }

        chargingUiWatcherStarted = true;
        chargingUiHandler.removeCallbacks(chargingUiWatcher);
        chargingUiHandler.post(chargingUiWatcher);

        Log.d("UIUpdater", "Charging UI watcher started");
    }

    public void stopChargingUiWatcher() {
        chargingUiWatcherStarted = false;
        chargingUiHandler.removeCallbacks(chargingUiWatcher);

        Log.d("UIUpdater", "Charging UI watcher stopped");
    }

    private void refreshChargingIndicator() {
        ChargingStatusManager manager =
                ChargingStatusManager.getInstance();

        ChargingStatusManager.ChargingState state =
                manager.getChargingState();

        switch (state) {
            case CHARGING:
                binding.chargingImg.setImageResource(
                        R.drawable.chargingimg
                );
                binding.chargingImg.setVisibility(View.VISIBLE);
                break;

            case PLUGGED_IN:
                binding.chargingImg.setImageResource(
                        R.drawable.pluggedin
                );
                binding.chargingImg.setVisibility(View.VISIBLE);
                break;

            case NOT_CHARGING:
            default:
                binding.chargingImg.setVisibility(View.GONE);
                break;
        }
    }
    /**
     * 更新 CAN 数据 10F907A8
     * @param hb3 解析后的 Heartbeat3 对象
     */
    private double Speed_ratio_Value = 1.6;//// 10% high

    public void updateCanData10F907A8(Heartbeat3 hb3) {
//        double speed = (hb3.getBSG2_BYTE78() * 60.0) / 3.3125 * 0.96 / 1000;
        double speed = (int)((hb3.getBSG2_BYTE78() * 60.0 * Speed_ratio_Value) / 3.3125 * 1 / 1000); // 10% high

        // Ignore invalid or noisy speed values.
        if (Double.isNaN(speed)
                || Double.isInfinite(speed)
                || speed < 0
                || speed > 200) {
            return;
        }

        currentVehicleSpeed = (int) speed;
        updateSpeedTrueAnimation((int)speed,currentComsForSpeed*-1);
        binding.speedNumberTV.setText(
                String.valueOf(currentVehicleSpeed)
        );

    }

    private void updateSpeedTrueAnimation(int newSpeed,int currentColour) {
        binding.speedArc.setSpeed(newSpeed,currentColour);
    }
    public void setupSpeedDoubleTap() {
        binding.speedNumberTV.setOnClickListener(v -> {
            long currentTime = SystemClock.elapsedRealtime();

            if (currentTime - lastSpeedTapTime > DOUBLE_TAP_INTERVAL_MS) {
                lastSpeedTapTime = currentTime;
                return;
            }

            // Double-tap detected
            lastSpeedTapTime = 0;

            // Do not open LoginActivity while moving
            if (currentVehicleSpeed > 0) {
                return;
            }
            MainActivity.isLoginActivityLaunching = true;
            Intent intent = new Intent(v.getContext(), LoginActivity.class);
            v.getContext().startActivity(intent);
        });
    }

    /**
     * 更新 CAN 数据 10FA07A8
     * @param hb3 解析后的 Heartbeat3 对象
     */
    private static int currentComsForSpeed;

    private void updateCanData10FA07A8(Heartbeat3 hb3) {

        float current = (float) roundTo1Decimal(
                getFormattedBusCurrent(hb3.getBSG3_BYTE34()) * -1
        );
        currentComsForSpeed=(int)current;
    }

    public static float getFormattedBusCurrent(String byte34Hex) {

        // Step 1: parse HEX string to int
        int raw = Integer.parseInt(byte34Hex, 16);

        // Step 2: signed 16-bit conversion
        if (raw > 0x7FFF) {          // 32767
            raw -= 0x10000;         // 65536
        }

        // Step 3: apply resolution (0.1 A / bit)
        float current = raw * 0.1f;

        // Step 4: return formatted string
        return current;
    }

    private double roundTo1Decimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }


    /**
     * 更新 CAN 数据 10FB07A8
     * @param hb3 解析后的 Heartbeat3 对象
     */
    private void updateCanData10FB07A8(Heartbeat3 hb3) {

    }

    /**
     * 更新 CAN 数据 10FC07A8
     * @param hb3 解析后的 Heartbeat3 对象
     */
    private void updateCanData10FC07A8(Heartbeat3 hb3) {

    }

    /**
     * 更新 CAN 数据 10FD07A8
     * @param hb3 解析后的 Heartbeat3 对象
     */
    private void updateCanData10FD07A8(Heartbeat3 hb3) {

    }

    /**
     * 更新 UI 组件
     * @param mad 解析后的 McuAnswerData 对象
     */
    public void updateMcuAnswerDataUI(McuAnswerData mad) {
        switch (mad.getTaskId()) {
            case "0100":
                updateBleMacChange(mad);
                break;
            case "0200":
                updateBleMac(mad);
                break;
            case "0204":
                updateMcuVersions(mad);
                break;
            default:
                // 处理未知的 CAN ID
                break;
        }
    }

    /**
     * 更新 蓝牙 Mac 地址
     * @param mad 解析后的 McuAnswerData 对象
     */
    private void updateBleMac(McuAnswerData mad) {
        // 蓝牙 Mac 地址
        String formattedMac = FrameUtils.formatMacAddress(mad.getBleMac());
        Log.d("UIUpdater", "updateBleMac succeed " + mad.getTaskId());
//        binding.BluetoothMACInput.setText(formattedMac);
    }

    /**
     * 提示 蓝牙 Mac 地址 修改状态
     * @param mad 解析后的 McuAnswerData 对象
     */
    private void updateBleMacChange(McuAnswerData mad) {
        // 提示 蓝牙 Mac 地址 修改状态
        if(mad.getBleMacChange() == 1){
            // 成功
            Toast.makeText(context, R.string.tips_bluetooth_mac_update_success, Toast.LENGTH_SHORT).show();
        } else {
            // 失败
            Toast.makeText(context, R.string.tips_bluetooth_mac_update_failure, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 提示 蓝牙 Mac 地址 修改状态
     * @param mad 解析后的 McuAnswerData 对象
     */
    private void updateMcuVersions(McuAnswerData mad) {
        // 获取MCU版本
        Log.d("UIUpdater", "updateMacVersions getMcuVersions: " + mad.getMcuVersions());
    }

}
