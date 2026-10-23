package com.fanconn.rideOS;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.fanconn.rideOS.databinding.ActivityMainBinding;

/**
 * UI 更新类，负责更新主活动中的 UI 组件
 */
public class UIUpdater {

    private final ActivityMainBinding binding;
    private final Handler handlerTurnSignal;
    private final Context context;

    public UIUpdater(ActivityMainBinding binding, Context context) {
        this.binding = binding;
        this.handlerTurnSignal = new Handler();
        this.context = context;
    }

    /**
     * 更新 UI 组件
     * @param hb1 解析后的 Heartbeat1 对象
     */
    public void updateHeartbeat1UI(Heartbeat1 hb1) {
        // 更新大灯状态
        updateImageView(binding.highBeamIV, hb1.getHeadlights() == 1 ? R.drawable.lamp_on : R.drawable.lamp_off);
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

    /**
     * 更新转向灯图片资源
     * @param turnSignalState 转向灯状态
     */
    private void updateTurnSignalImages(int turnSignalState) {
        // 立即移除之前的任务
        handlerTurnSignal.removeCallbacksAndMessages(null);
        switch (turnSignalState) {
            case 0:
                updateTurnSignal(R.drawable.indicators, R.drawable.indicatorsr, false);
                break;
            case 1:
                updateTurnSignal(R.drawable.left_on, R.drawable.indicatorsr, true);
                startTurnSignalAnimation(R.drawable.indicators, R.drawable.left_on, binding.turnLeftIV);
                break;
            case 2:
                updateTurnSignal(R.drawable.indicators, R.drawable.right_on, true);
                startTurnSignalAnimation(R.drawable.indicatorsr, R.drawable.right_on, binding.turnRightIV);
                break;
            case 3:
                updateTurnSignal(R.drawable.left_on, R.drawable.right_on, true);
                startFaultSignalAnimation();
                break;
            default:
                break;
        }
    }

    /**
     * 更新转向灯图片资源
     * @param leftRes 左转向灯资源
     * @param rightRes 右转向灯资源
     * @param animated 是否需要动画
     */
    private void updateTurnSignal(int leftRes, int rightRes, boolean animated) {
        binding.turnLeftIV.setImageResource(leftRes);
        binding.turnRightIV.setImageResource(rightRes);

        if (!animated) {
            stopTurnSignalAnimation();
        }
    }

    /**
     * 开始转向灯动画
     * @param initialRes 初始资源
     * @param alternateRes 替换资源
     * @param imageView 图片视图
     */
    private void startTurnSignalAnimation(final int initialRes, final int alternateRes, final ImageView imageView) {
        handlerTurnSignal.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (imageView.getDrawable().getConstantState().equals(imageView.getContext().getResources().getDrawable(alternateRes).getConstantState())) {
                    imageView.setImageResource(initialRes);
                } else {
                    imageView.setImageResource(alternateRes);
                }
                handlerTurnSignal.postDelayed(this, 500);
            }
        }, 500);
    }

    /**
     * 开始故障灯动画
     */
    private void startFaultSignalAnimation() {
        handlerTurnSignal.postDelayed(new Runnable() {
            @Override
            public void run() {
                toggleImageResource(binding.turnLeftIV, R.drawable.right, R.drawable.lmp_l);
                toggleImageResource(binding.turnRightIV, R.drawable.left, R.drawable.lmp_r);
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
        // 更新电池电量
        int batteryLevel = hb3.getBatteryLevel() / 10;
        binding.batteryTV.setText(batteryLevel + "%");

    }

    public void updateCanData10F807A8(Heartbeat3 hb3) {
        int directionalGears = FrameUtils.extractBits(hb3.getBSG1_BYTE1(), 0, 1);
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

    /**
     * 更新 CAN 数据 10F907A8
     * @param hb3 解析后的 Heartbeat3 对象
     */
    public void updateCanData10F907A8(Heartbeat3 hb3) {
        // 计算速度
        double speed = (hb3.getBSG2_BYTE78() * 60.0) / 3.3125 * 0.96 / 1000;
        binding.speedNumberTV.setText(String.valueOf((int) speed));
    }

    /**
     * 更新 CAN 数据 10FA07A8
     * @param hb3 解析后的 Heartbeat3 对象
     */
    private void updateCanData10FA07A8(Heartbeat3 hb3) {

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
