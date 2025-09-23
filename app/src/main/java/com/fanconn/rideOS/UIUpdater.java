// Refactored version with leak fixes by ChatGPT
// Original logic preserved

package com.fanconn.rideOS;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;

import com.fanconn.rideOS.authen.LoginActivity;
import com.fanconn.rideOS.databinding.ActivityMainBinding;

import java.io.File;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

//=========================
import android.net.Uri;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.fanconn.rideOS.network.ApiService;
import com.fanconn.rideOS.network.RetrofitClient;
import com.fanconn.rideOS.network.VehicleData;
import com.fanconn.rideOS.network.EVWebSocketClient;
import com.fanconn.rideOS.TelemetryService;
import com.fanconn.rideOS.TelemetryDataManager;
import com.google.gson.Gson;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.net.URI;
import java.util.Map;
//=========================

/**
 * UI 更新类，负责更新主活动中的 UI 组件
 */
public class UIUpdater  {

    private final ActivityMainBinding binding;
    private final Handler handlerTurnSignal;
    private final Context context;
    MainActivity mainActivity;
    //    InactivityManager inactivityManager;
    SerialPortManager serialPortManager;
    batteryPackSwitcher BatPackSwitcher;

    //=================telemetry
//    private EVWebSocketClient evClient;
//    private ApiService apiService;
//    private boolean isSending = true;
//    private final Handler telemetryHandler = new Handler(Looper.getMainLooper());
//
//    public static int telechargingstate;
//    public static int telebmslifecycles;
//    public static String telelatlong;
//    public static int teleHandleLockState;
//    public static int teleSeatLockState;
//    public static String teleBMSMosStates;
//    private static float teletirepressure;
//    private static float teletrip;
//    private static String teleRider;
//    public static String telecurrentrider;
//    private static float teletotalAH;
//    private static float telechargerAH;
//    private static float teleregenAH;
//    private static float teledischargeAH;
//    private static int  teleEvPowerState;
//    private static float  teleCurrentConsumption;
//    private static float  teleBatVoltage;
//    private static float  teleRemainingCapacityAH;
//    private static float teleODO;
//    public static float teleSpeed;
//    public static float teleControllerMosTemp;
//    public static float teleMotorTemp;
//    public static float teleBMSMosTemp;
//    public static String teleNTC;
//    private static float teleSOC;
//    private static float teleAPUSOC;

//    int handleLockState = teleHandleLockState;
//    int seatLockState = teleSeatLockState;
//    String bmsMosStates = teleBMSMosStates;
//    String riders = teleRiders;

    //===========================
    private final MainActivity activity; // Actual activity for dialogs, UI
//    private CameraHandler cameraHandler;
    TelemetryDataManager tm;
    public static UIUpdater instance;
    private BroadcastReceiver tpmsReceiver;
    public UIUpdater(ActivityMainBinding binding, Context ctx) {
        this.binding = binding;

        // Ensure the passed context is actually an Activity
        if (ctx instanceof MainActivity) {
            this.activity = (MainActivity) ctx;
        } else {
            throw new IllegalArgumentException("UIUpdater must be initialized with an Activity context");
        }
        instance = this;
        this.context = ctx.getApplicationContext();

        this.handlerTurnSignal = new Handler();
        this.BatPackSwitcher = new batteryPackSwitcher();

        initIndicatorLogic(context);
        initReverseBeep(context);
        serialPortManager = serialApplication.getSerialPortManager();

//        cameraHandler = new CameraHandler(
//                binding.frontCameraView,
//                binding.rearCameraView,
//                binding.frontCameraContainer,
//                binding.rearCameraContainer,
//                ctx
//        );



        this.tm = TelemetryDataManager.getInstance();
        startCanTimeoutChecker();
        registerTpmsReceiver(ctx);
    }

    public static UIUpdater getInstance() {
        return instance;
    }


//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//    }

//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        tripManager = new TripDistanceManager(this);
//        serialPortManager = serialApplication.getSerialPortManager();
//        Log.d("InitCheck", "serialPortManager = " + serialPortManager);
//
//        BatPackSwitcher = new batteryPackSwitcher(serialPortManager);
//    }

    /**
     * 更新 UI 组件
     * @param hb1 解析后的 Heartbeat1 对象
     */
    private int lastTurnSignalState = -1;

    public void updateHeartbeat1UI(Heartbeat1 hb1) {

        int currentSignal = hb1.getTurnSignal();
//        Log.d("fcgvhbjnk","THIS IS INDICOTORSIGNAL FOR NOW "+currentSignal);
        if (currentSignal != lastTurnSignalState) {
            indicators(currentSignal);  // only call if changed
            lastTurnSignalState = currentSignal; // update tracker

        }
        // 更新龙头锁状态
//        updateImageView(binding.BluetoothIV, hb1.getDragonLock() == 1 ? R.drawable.bluetooth : R.drawable.bluetooth_not_connected);
//        updateImageView(binding.frontLockIV, hb1.getDragonLock() == 1 ? R.drawable.unlocked : R.drawable.lock);
//        if (MainActivity.frontLockSW != null) {
//            MainActivity.frontLockSW.setChecked(hb1.getDragonLock() == 1);
//        }
//        if (MainActivity.frontLockSWSTV != null) {
//            MainActivity.frontLockSWSTV.setText(hb1.getDragonLock() == 1 ? "开" : "关");
//        }
//
//        // 更新坐垫锁状态
//        updateImageView(binding.seatCushionLockIV, hb1.getSeatLock() == 1 ? R.drawable.unlocked : R.drawable.lock);
//        if (MainActivity.seatCushionLockSW != null) {
//            MainActivity.seatCushionLockSW.setChecked(hb1.getSeatLock() == 1);
//        }
//        if (MainActivity.seatCushionLockSWSTV != null) {
//            MainActivity.seatCushionLockSWSTV.setText(hb1.getSeatLock() == 1 ? "开" : "关");
//        }
//
//        // 更新侧⽀架传感器磁感状态
//        updateTextView(binding.sideSupportInductionStatusTV, hb1.getSideStand() == 0 ? R.string.magnetic : R.string.non_magnetic);
//
//        // 更新座椅锁传感器磁感状态
//        updateTextView(binding.seatInductionStatusTV, hb1.getSeatLockSensor() == 0 ? R.string.magnetic : R.string.non_magnetic);
//
//        // 更新小电池电压值
//        updateTextView(binding.smallBatteryVoltageTV, String.format("%.2f V", hb1.getBatteryVoltage() / 1000.0));
//
//        // 更新外部温度
//        updateTextView(binding.temperatureTV, String.format("%.1f°", hb1.getExternalTemperature() / 1000.0));
//
//        // 更新手势磁感状态
//        updateTextView(binding.gestureStatusTV, getGestureStatusText(binding.getRoot().getContext(), hb1.getGestureSlide()));
//
//        // 更新大灯状态
        //updateImageView(binding.highBeamIV, hb1.getHeadlights() == 1 ? R.drawable.high_beam : R.drawable.headlamp);
        if(hb1.getHeadlights() == 1)
        {
            binding.highBeamIVon.setVisibility(View.VISIBLE);
            binding.highBeamIVoff.setVisibility(View.INVISIBLE);
        }
        else if (hb1.getHeadlights() == 0)
        {
            binding.highBeamIVon.setVisibility(View.INVISIBLE);
            binding.highBeamIVoff.setVisibility(View.VISIBLE);

//                if (isDriveMode && !isStand) {
//                    binding.highBeamIVoff.setVisibility(View.VISIBLE);
//                } else {
//                    binding.highBeamIVoff.setVisibility(View.INVISIBLE);
//                }
        }
        // 更新转向灯状态



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
//    private void updateTurnSignalImages(int turnSignalState) {
//        // 立即移除之前的任务
//        handlerTurnSignal.removeCallbacksAndMessages(null);
//        switch (turnSignalState) {
//            case 0:
//                updateTurnSignal(R.drawable.lmp_l, R.drawable.lmp_r, false);
//                break;
//            case 1:
//                updateTurnSignal(R.drawable.left, R.drawable.lmp_r, true);
//                startTurnSignalAnimation(R.drawable.lmp_l, R.drawable.left, binding.turnLeftIV);
//                break;
//            case 2:
//                updateTurnSignal(R.drawable.lmp_l, R.drawable.right, true);
//                startTurnSignalAnimation(R.drawable.lmp_r, R.drawable.right, binding.turnRightIV);
//                break;
//            case 3:
//                updateTurnSignal(R.drawable.flicker_2, R.drawable.flicker_1, true);
//                startFaultSignalAnimation();
//                break;
//            default:
//                break;
//        }
//    }
//
//    /**
//     * 更新转向灯图片资源
//     * @param leftRes 左转向灯资源
//     * @param rightRes 右转向灯资源
//     * @param animated 是否需要动画
//     */
//    private void updateTurnSignal(int leftRes, int rightRes, boolean animated) {
//        binding.turnLeftIV.setImageResource(leftRes);
//        binding.turnRightIV.setImageResource(rightRes);
//
//        if (!animated) {
//            stopTurnSignalAnimation();
//        }
//    }
//
//    /**
//     * 开始转向灯动画
//     * @param initialRes 初始资源
//     * @param alternateRes 替换资源
//     * @param imageView 图片视图
//     */
//    private void startTurnSignalAnimation(final int initialRes, final int alternateRes, final ImageView imageView) {
//        handlerTurnSignal.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                if (imageView.getDrawable().getConstantState().equals(imageView.getContext().getResources().getDrawable(alternateRes).getConstantState())) {
//                    imageView.setImageResource(initialRes);
//                } else {
//                    imageView.setImageResource(alternateRes);
//                }
//                handlerTurnSignal.postDelayed(this, 500);
//            }
//        }, 500);
//    }
//
//    /**
//     * 开始故障灯动画
//     */
//    private void startFaultSignalAnimation() {
//        handlerTurnSignal.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                toggleImageResource(binding.turnLeftIV, R.drawable.flicker_2, R.drawable.lmp_l);
//                toggleImageResource(binding.turnRightIV, R.drawable.flicker_1, R.drawable.lmp_r);
//                handlerTurnSignal.postDelayed(this, 500);
//            }
//        }, 500);
//    }
//
//    /**
//     * 切换 ImageView 的图片资源
//     * @param imageView 需要切换的 ImageView
//     * @param res1 图片资源1
//     * @param res2 图片资源2
//     */
//    private voidoggleImageResource(ImageView imageView, int res1, int res2) {
//        if (imageView.getDrawable().getConstantState().equals(imageView.getContext().getResources().getDrawable(res1).getConstantState())) {
//            imageView.setImageResource(res2);
//        } else {
//            imageView.setImageResource(res1);
//        }
//    }
//
//    /**
//     * 停止更新和动画
//     */
//    public void stopUpdating() {
//        stopTurnSignalAnimation();
//    }
//
//    /**
//     * 停止转向灯动画和定时任务
//     */
//    private void stopTurnSignalAnimation() {
//        handlerTurnSignal.removeCallbacksAndMessages(null);
//    }

    /**
     * 更新 UI 组件
     * @param hb2 解析后的 Heartbeat2 对象
     */
//    public void updateHeartbeat2UI(Heartbeat2 hb2) {
//        // 更新六轴数据
//        updateSixAxisData(hb2);
//    }

    /**
     * 更新六轴数据
     * @param hb2 解析后的 Heartbeat2 对象
     */
//    private void updateSixAxisData(Heartbeat2 hb2) {
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
//    }

    /**
     * 更新 UI 组件
     * @param hb3 解析后的 Heartbeat3 对象
     */
    public void updateHeartbeat3UI(Heartbeat3 hb3) {
        switch (hb3.getCanId()) {
            case "18904001":
                updateCanData18904001(hb3);
                break;
            case "18904002":
                updateCanData18904002(hb3);
                break;
            case "18934001":
                updateCanData18934001(hb3);
                break;
            case "18934002":
                updateCanData18934002(hb3);
                break;
            case "18964001":
                updateCanData18964001(hb3);
                break;
            case "18964002":
                updateCanData18964001(hb3);
                break;
            case "160B8001":
                updateCanData160B8001(hb3);
                break;
            case "160D8001":
                updateCanData160D8001(hb3);
                break;
            case "14028001":
                updateCanData14028001(hb3);
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
    private int lowBatteryAlertCount = 0;
    private static final int MAX_LOW_BATTERY_ALERTS = 3;
    private static boolean  Lowbatflag,todefultflag,atSOC10Lowbatflag,atSOC7Lowbatflag = false;
    private static boolean apuflag=false;
    private  boolean StopInactivtyTimerDuringPopUp=true;
    public static boolean isAPUPresent=false;
    private static int UniversalAPUbatteryLevel;
    public static boolean resetParamtersAfterLowbat=false;
    public void updateCanData18904001(Heartbeat3 hb3) {
        // 更新电池电量
        int batteryLevel= hb3.getBatteryLevel() / 10;
        String sBatteryLevel = String.valueOf(batteryLevel);

        float currentconsumption=(hb3.getrealCurrent()-30000/10);
        if(currentconsumption<500 && currentconsumption>-500){
            tm.setTeleCurrentConsumption(currentconsumption);
        }
        tm.setTeleBatVoltage(hb3.getbatvoltage()/10);
        int range= (int) Math.floor(batteryLevel * 2.8);  // Get the whole number part
        if(batteryLevel>=0 && batteryLevel<=100){
            tm.setTeleSOC(batteryLevel);
            binding.batteryTV.setText("Battery "+sBatteryLevel + "%   |  ~ "+range+" km");
        }

        if (!Lowbatflag && batteryLevel == 20  && tm.getTeleChargingState()==0) {

            if(serialPortManager!=null){
                serialPortManager.VCUSendToMCU("FFAA550103001510FBA8073C007800B004A000");//line current 15A, max phase current 300A ,regen 30-40
                new Handler(Looper.getMainLooper()).postDelayed(() -> serialPortManager.VCUSendToMCU("FFAA550103001510F8A8070000000000007387"), 1200);
            }
            // Start alert
            Animation tempalert = AnimationUtils.loadAnimation(context, R.anim.alpha);

            binding.yellowalert.setVisibility(View.VISIBLE);
            binding.LowBatmsg.setVisibility(View.VISIBLE);

            binding.yellowalert.startAnimation(tempalert);
            binding.LowBatmsg.startAnimation(tempalert);

            startBeeping(2);

            Lowbatflag = true;

            // Stop alert visuals after 10 seconds, but DO NOT reset flag yet
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                stopBeeping();
            }, 15_000);
        }

        if (batteryLevel >= 21 && Lowbatflag) {
            resetParamtersAfterLowbat=true;
            Lowbatflag = false;
        }
        if (batteryLevel >= 21 && atSOC10Lowbatflag) {
            resetParamtersAfterLowbat=true;
            atSOC10Lowbatflag = false;
        }
        if (batteryLevel >= 21 && atSOC7Lowbatflag) {
            resetParamtersAfterLowbat=true;
            atSOC7Lowbatflag = false;
        }

//        if (batteryLevel >= 22) {
//            apuflag=false;
//        }

        if(!atSOC10Lowbatflag && batteryLevel==10 && tm.getTeleChargingState()==0){
            atSOC10Lowbatflag=true;
            if(serialPortManager!=null){
                serialPortManager.VCUSendToMCU("FFAA550103001510FBA80728007800B004A000");//line current 10 A
                new Handler(Looper.getMainLooper()).postDelayed(() -> serialPortManager.VCUSendToMCU("FFAA550103001510F8A8070000000000007387"), 1200);
            }
        }

        if(!atSOC7Lowbatflag && batteryLevel==7 && tm.getTeleChargingState()==0){
            atSOC7Lowbatflag=true;
            startBeeping(2);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                stopBeeping();
            }, 20_000);

            if(isAPUPresent && !apuflag){
                StopInactivtyTimerDuringPopUp=false;
                InactivityManager.getInstance().stopTimer();
                showCustomPopup("Alert","Your Battery is Low, Switch to APU for Extra Range");
            } else if  (!isAPUPresent) {
                StopInactivtyTimerDuringPopUp=false;
                showCustomPopupatSOC7("Alert","Battery low,deep discharge may damage your scooter battery.Please charge your scooter");
                serialPortManager.VCUSendToMCU("FFAA550103001510FBA807280078000000A000");//STOP SCOOTER, phase zero
                new Handler(Looper.getMainLooper()).postDelayed(() -> serialPortManager.VCUSendToMCU("FFAA550103001510F8A8070000000000007387"), 1200);
            } else if (apuflag  && UniversalAPUbatteryLevel<=15) {
                StopInactivtyTimerDuringPopUp=false;
                showCustomPopupatSOC7("Alert","Battery low,deep discharge may damage your scooter battery.Please charge your scooter");
                serialPortManager.VCUSendToMCU("FFAA550103001510FBA807280078000000A000");//STOP SCOOTER, phase zero
                new Handler(Looper.getMainLooper()).postDelayed(() -> serialPortManager.VCUSendToMCU("FFAA550103001510F8A8070000000000007387"), 1200);            }
        }

//        if(batteryLevel==5 && !apuflag  && tm.getTeleChargingState()==0){
//            if(serialPortManager!=null){
//                serialPortManager.VCUSendToMCU("FFAA550103001510FBA8070000000088040000");
//                new Handler(Looper.getMainLooper()).postDelayed(() -> serialPortManager.VCUSendToMCU("FFAA550103001510F8A8070000000000007387"), 1200);
//            }
//            apuflag=true;
//            StopInactivtyTimerDuringPopUp=false;
//            InactivityManager.getInstance().stopTimer();
//            showCustomPopup("Alert","Your Battery is Low, Switch to APU for Extra Range");
//        }

        //BatPackSwitcher.startSwitchingToAPU();
//
        try {
            if (batteryLevel>=0 && batteryLevel<=100) { // ✅ Only update if animation is done
                updateBatteryAnimation(batteryLevel);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        //根据电池电量获取对应的图片资源
        //int batteryImageResId = getBatteryImageResource(batteryLevel);
        //binding.batteryIV.setImageResource(batteryImageResId);
    }
    private chargingUI chargingUI;
    public void setChargingUI(chargingUI chargingUI) {
        this.chargingUI = chargingUI;
    }

    public void updateCanData18904002(Heartbeat3 hb3) {

        isAPUPresent=true;
        // 更新电池电量
        int APUbatteryLevel= hb3.getAPUBatteryLevel() / 10;
        UniversalAPUbatteryLevel= hb3.getAPUBatteryLevel() / 10;
        String sAPUBatteryLevel = String.valueOf(APUbatteryLevel);

//        int range= (int) Math.floor(APUbatteryLevel * 2.8);  // Get the whole number part
        if(APUbatteryLevel>=0 && APUbatteryLevel<=100){
            tm.setTeleAPUSOC(APUbatteryLevel);
            binding.APUbatteryTV.setText("APU "+sAPUBatteryLevel + "% ");}//  |  ~ "+range+" km");}

        int batteryImageResId = getBatteryImageResource(APUbatteryLevel);
        binding.apubaticon.setImageResource(batteryImageResId);



        if (APUbatteryLevel >= 20) {
            todefultflag=false;
        }

        if(APUbatteryLevel==10 && !todefultflag){
            todefultflag=true;
            BatPackSwitcher.startSwitchingToNormalState();
        }


        try {
            if (APUbatteryLevel>=0 && APUbatteryLevel<=100) { // ✅ Only update if animation is done
                APUupdateBatteryAnimation(APUbatteryLevel);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        //根据电池电量获取对应的图片资源
        //int batteryImageResId = getBatteryImageResource(batteryLevel);
        //binding.batteryIV.setImageResource(batteryImageResId);
    }
    private boolean AlreadyInChargingUI = false;

    public void updateCanData14028001(Heartbeat3 hb3) {
       //Log.d("ChargingDebug", "RESETING FROM UIUPDATORrrrrrrrrrrrrrrrrrrrrrrrrrrrrrr");
        //---------------------------
        binding.yellowalert.clearAnimation();
        binding.LowBatmsg.clearAnimation();

        binding.yellowalert.setVisibility(View.INVISIBLE);
        binding.LowBatmsg.setVisibility(View.INVISIBLE);
        //----------------------------
        int chargingbit = hb3.getChargingbit();
//        Log.d("ChargingDebug", "Charging bitKKK: " + chargingbit);
        if (chargingUI == null) {
            //Log.d("UIUpdater", "chargingUI is null — cannot reset timeout");
        } else {
            chargingUI.resetChargingTimeout();
        }

        boolean isCharging = chargingbit == 1;
//        if(isCharging)binding.chargingicononmainactivty.setVisibility(View.VISIBLE);
//        if (isCharging && !AlreadyInChargingUI) {
//            AlreadyInChargingUI = true;
//                finish();
////            Intent intent = new Intent(context, chargingUI.class);
////            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
////            context.startActivity(intent);
//        }
//
//        // Only reset the flag when charging has completely stopped.
//        if (!isCharging && AlreadyInChargingUI) {
//            AlreadyInChargingUI = false;
//        }
    }


    public void updateCanData18934001(Heartbeat3 hb3) {
        // 更新电池电量
        int mainChargingMosState= hb3.getMainChargeMosState();
        int mainDisMosState= hb3.getMainDisChargeMosState();
        tm.setTeleRemainingCapacityAH(hb3.getreamainingAH()/1000);
        tm.setTeleBmsLifeCycles(hb3.getBMSlifecycles());
        tm.setTeleBMSMosStates(hb3.getMainChargeMosState()+","+hb3.getMainDisChargeMosState()+","+hb3.getApuChargeMosState()+","+hb3.getApuDisChargeMosState());
    }
    public void updateCanData18934002(Heartbeat3 hb3) {
        // 更新电池电量

        int apuChargingMosState= hb3.getApuChargeMosState();
        int apuDisMosState= hb3.getApuDisChargeMosState();
        int mainChargeMos=hb3.getMainChargeMosState();
       Log.d("dfsgdfh","MOSSSSSSSSSOOOOOSSSFET"+mainChargeMos);
//        Log.d("sadfgd","MOSSSSSSSSSOOOOOSSSFETAPUDISCG"+apuDisMosState);
        if( apuDisMosState==1 && mainChargeMos==0 ){
            binding.RunningOnAPU.setVisibility(View.VISIBLE);
            binding.redalert.setVisibility(View.VISIBLE);
            apuflag=true;
        }
        else{
            binding.RunningOnAPU.setVisibility(View.INVISIBLE);
            binding.redalert.setVisibility(View.INVISIBLE);
            apuflag=false;
        }
    }

    int NTC1,NTC2,NTC3,NTC4,NTC5,NTC6,NTC7,NTC8;
    boolean flag;  MediaPlayer mediaPlayer;
    private Handler beepHandler = new Handler(Looper.getMainLooper());
    private Runnable beepRunnable;
    private boolean isBeeping = false;

    private boolean is45WarningShown = false;
    private boolean is50WarningActive = false;
    private Handler warningHandler = new Handler();

    public void updateCanData18964001(Heartbeat3 hb3) {
        int chargingbit = hb3.getChargingbit();

        // Apply offset
        NTC1 = hb3.getNTC1() - 40;
        NTC2 = hb3.getNTC2() - 40;
        NTC3 = hb3.getNTC3() - 40;
        NTC4 = hb3.getNTC4() - 40;
        NTC5 = hb3.getNTC5() - 40;
        NTC6 = hb3.getNTC6() - 40;
        NTC7 = hb3.getNTC7() - 40;
        NTC8 = hb3.getNTC8() - 40;

        //Log.d("TemperatureCheck", "NTCs = " + NTC1 + "," + NTC2 + "," + NTC3 + "," + NTC4 + "," + NTC5 + "," + NTC6 + "," + NTC7 + "," + NTC8);
        tm.setTeleNTC(NTC1+","+NTC2+","+NTC3+","+NTC4+","+NTC5+","+NTC6+","+NTC8+","+NTC8);
        int[] temps = {NTC1, NTC2, NTC3, NTC4, NTC5, NTC6, NTC7, NTC8};
        boolean anyIs45 = false;
        boolean anyIs50OrAbove = false;
        boolean allBelow45 = true;

        for (int temp : temps) {
            if (temp < 0 || temp > 70) {
                continue;
            }

            if (temp >= 54 && temp < 58)anyIs45 = true;
            if (temp >= 58) anyIs50OrAbove = true;
            if (temp >= 54) allBelow45 = false;
        }

        // ✅ Reset 45-warning ONLY when all temps are below 45
        if (allBelow45 && is45WarningShown) {
            Log.d("TempReset", "All NTCs below 45. Resetting 10s warning flag.");
            is45WarningShown = false;
        }

        // 🔥 Handle 50+ Continuous warning
        if (anyIs50OrAbove) {
            if (!is50WarningActive) {
                Log.d("TempWarning", "Temp >= 50. Starting continuous warning.");
                startTempWarningAnimation();
                highVolumeForWarning();
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    startBeeping(1);
                }, 1300);
                sendWarningToUI("High Battery Temperature Detected");

                is50WarningActive = true;
            }
        } else {
            // ❄️ Stop 50°C continuous warning if previously active
            if (is50WarningActive) {
                Log.d("TempWarning", "Temp dropped below 50. Stopping continuous warning.");
                stopTempWarningAnimation();
                stopBeeping();
                sendWarningToUI("");
                is50WarningActive = false;
            }

            // ✅ Handle 45°C exactly → 10s warning only ONCE
            if (anyIs45 && !is45WarningShown) {
                Log.d("TempWarning", "Temp == 45. Starting 10s warning.");
                startTempWarningAnimation();
                highVolumeForWarning();
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    startBeeping(1);
                }, 1300);
                sendWarningToUI("High Battery Temperature Detected");
                is45WarningShown = true;

                warningHandler.postDelayed(() -> {
                    Log.d("TempWarning", "10s warning done. Stopping animation and beep.");
                    stopTempWarningAnimation();
                    stopBeeping();
                    sendWarningToUI("");
                }, 15_000);
            }
        }
    }

    public void highVolumeForWarning()
    {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        // Get the maximum volume for the media stream
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        // Set the media volume to maximum
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0);
    }

    private void sendWarningToUI(String message) {
        Intent intent = new Intent("TEMP_WARNING_EVENT");
        intent.putExtra("warning_message", message);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private void startBeeping(int code) {
        if (isBeeping) return;

        isBeeping = true;
        beepRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null) {
                    mediaPlayer.release();
                    mediaPlayer = null;
                }

                switch (code) {
                    case 1:
                        mediaPlayer = MediaPlayer.create(context, R.raw.shortbeep);
                        break;
                    case 2:
                        mediaPlayer = MediaPlayer.create(context, R.raw.batlowalert2);
                        break;
                    case 3:
                        mediaPlayer = MediaPlayer.create(context, R.raw.gearshift);
                        break;
                    default:
                        Log.w("Beep", "Unknown beep code: " + code);
                        stopBeeping(); // Or just return; if you want to keep isBeeping true
                        return;
                }

                if (mediaPlayer != null) {
                    mediaPlayer.start();
                    beepHandler.postDelayed(this, 2720); // Repeat every ~2.7 seconds
                } else {
                    Log.e("Beep", "MediaPlayer failed to create for code: " + code);
                    stopBeeping(); // or just return;
                }
            }
        };

        beepHandler.post(beepRunnable);
    }

    private void stopBeeping() {
        isBeeping = false;
        beepHandler.removeCallbacks(beepRunnable);

        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
    private void startTempWarningAnimation() {
        Animation tempalert = AnimationUtils.loadAnimation(context, R.anim.alpha);

        binding.redalert.setVisibility(View.VISIBLE);
        binding.tempalerticon.setVisibility(View.VISIBLE);
        binding.battempmsg.setVisibility(View.VISIBLE);

        binding.redalert.startAnimation(tempalert);
        binding.battempmsg.startAnimation(tempalert);
    }

    private void stopTempWarningAnimation() {
        binding.redalert.clearAnimation();
        binding.battempmsg.clearAnimation();

        binding.redalert.setVisibility(View.INVISIBLE);
        binding.tempalerticon.setVisibility(View.INVISIBLE);
        binding.battempmsg.setVisibility(View.INVISIBLE);
    }



    // Class variables to persist values across calls
    private float previousAh = 0f;
    private float ahByCharger = 0f;
    private float ahByRegen = 0f;
    public static int chargison;
    public void updateCanData160B8001(Heartbeat3 hb3) {
        float currentAh = roundTo3(hb3.getChargeAH() / 1000f);

        if (previousAh == 0f) {
            previousAh = currentAh;
            loadStoredAhValues(); // Load saved values (if any) on first run
            return; // skip first frame to prevent large delta
        }

        float delta = currentAh - previousAh;

        // Allow small charging increments
        if (delta > 0.001f && delta < 5.0f) {
            if (tm.getTeleChargingState()== 1) {
                ahByCharger += delta;
            } else {
                ahByRegen += delta;
            }
        }

        previousAh = currentAh;

        // Round before storing (3 decimal points everywhere)
        float roundedTotalAh = roundTo3(currentAh);
        float roundedChargerAh = roundTo3(ahByCharger);
        float roundedRegenAh = roundTo3(ahByRegen);

        saveAhValues(roundedTotalAh, roundedChargerAh, roundedRegenAh);

        tm.setTeleTotalAH(roundedTotalAh);
        tm.setTeleChargerAH(roundedChargerAh);
        tm.setTeleRegenAH(roundedRegenAh);

//      Log.d("BMS", "TotalAh: " + roundedTotalAh +
//                ", AhByCharger: " + roundedChargerAh +
//                ", AhByRegen: " + roundedRegenAh);
//        Log.d("DEBUG", "delta: " + delta + ", telechargingstate: " + telechargingstate);
    }

    private void saveAhValues(float totalAh, float chargerAh, float regenAh) {
        SharedPreferences prefs = context.getSharedPreferences("ah_data", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat("totalAh", totalAh);
        editor.putFloat("chargerAh", chargerAh);
        editor.putFloat("regenAh", regenAh);
        editor.apply();
    }

    private void loadStoredAhValues() {
        SharedPreferences prefs = context.getSharedPreferences("ah_data", Context.MODE_PRIVATE);
        tm.setTeleTotalAH( prefs.getFloat("totalAh", 0f));
        tm.setTeleChargerAH(prefs.getFloat("chargerAh", 0f));
        tm.setTeleRegenAH(prefs.getFloat("regenAh", 0f));

        ahByCharger = tm.getTeleChargerAH();
        ahByRegen = tm.getTeleRegenAH();
    }

    // ✅ Updated for 3 decimal point dischargeAh
    public void updateCanData160D8001(Heartbeat3 hb3) {
        float rawDischargeAH = hb3.getDisChargeAH() / 1000f;
        float dischargeAH = roundTo3(rawDischargeAH);

        saveDischargeAh(dischargeAH);
        tm.setTeleDischargeAH(dischargeAH);

        //Log.d("BMS", "DischargeAh: " + String.format("%.3f", dischargeAH));
    }

    private void saveDischargeAh(float value) {
        SharedPreferences prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        prefs.edit().putFloat("dischargeAh", value).apply();
    }

    private float loadDischargeAh() {
        SharedPreferences prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        return prefs.getFloat("dischargeAh", 0f);
    }

    // Utility method for 3 decimal point rounding
    private float roundTo3(float value) {
        return Math.round(value * 1000f) / 1000f;
    }

    /**
     * 根据电池电量获取对应的图片资源
     * @param batteryLevel 电池电量
     * @return 图片资源 ID
     */
//    private int getBatteryImageResource(int batteryLevel) {
//        if (batteryLevel < 20) {
//            return R.drawable.battery_level_1;
//        } else if (batteryLevel < 40) {
//            return R.drawable.battery_level_2;
//        } else if (batteryLevel < 60) {
//            return R.drawable.battery_level_3;
//        } else if (batteryLevel < 80) {
//            return R.drawable.battery_level_4;
//        } else {
//            return R.drawable.battery_level_5;
//        }
//    }
    private boolean isStand=false;
    //private boolean isReady=false;

    /**
     * 更新 CAN 数据 10F807A8
     * @param hb3 解析后的 Heartbeat3 对象
     */
    static int directionalGears;
    static boolean isboost,isCruise;

    private int lastGear = -1;
    private int lastStand = -1;
    private MediaPlayer sideStandPlayer;
    private boolean isSideStandSoundPlaying=false;
    private MediaPlayer cruisePlayer;
    private boolean isCruiseSoundPlaying = false;
    private boolean lastCruise = false; // track changes
    public void updateCanData10F807A8(Heartbeat3 hb3) {
        directionalGears = FrameUtils.extractBits(hb3.getBSG1_BYTE1(), 0, 1);
        int speedGears = FrameUtils.extractBits(hb3.getBSG1_BYTE1(), 2, 4);
        //int READY = FrameUtils.extractBits(hb3.getBSG1_BYTE3(), 0, 0);
        int STAND =FrameUtils.extractBits(hb3.getBSG1_BYTE2(),0,1);
        isboost=hb3.getBSG1_BYTE3()==8; //hex value 08
        isCruise=hb3.getBSG1_BYTE1()==201;//hex value of 201=C9
        tm.setTeleMotorTemp(hb3. getBSG1_BYTE7()-40);   //Motot temprature hex to dec then offset 40
        tm.setTeleControllerMosTemp(hb3.getBSG1_BYTE8()-40);// Controller Mos temp offset 40
//        Log.d("dfhgvhb","CRUISE VALUE IS "+hb3.getBSG1_BYTE1());
//        if(isboost)binding.actualboost.setVisibility(View.VISIBLE);
//        else binding.actualboost.setVisibility(View.INVISIBLE);
        //Log.d("sdafsgdf","BOOSTBYTE IS "+isboost);
        int chargingbit = hb3.getChargingbit();
        if((shouldshowchargingicononmainonot && directionalGears!=2) || shouldshowchargingicononmainonot && isStand){

            TelemetryDataManager.getInstance().setTeleChargingState(1);
            binding.rectangleGearsTV.setVisibility(View.INVISIBLE);
            binding.neutralicon.setVisibility(View.INVISIBLE);
//            binding.chargingicononmainactivty.setVisibility(View.VISIBLE);
            binding.chargingredtab.setVisibility(View.VISIBLE);
            if(chargingbit==1){
                binding.chargingredtab.setImageResource(R.drawable.chargingredtab);
            }
            else binding.chargingredtab.setImageResource(R.drawable.yellowpluggedtab);

//            binding.chargingredtab.setVisibility(View.VISIBLE);
        }
        else {

//            binding.chargingicononmainactivty.setVisibility(View.INVISIBLE);
            binding.chargingredtab.setVisibility(View.INVISIBLE);
            if(directionalGears==0 && !isStand){
                binding.neutralicon.setVisibility(View.VISIBLE);
            } else if (directionalGears==1 && !isStand) {
                binding.rectangleGearsTV.setVisibility(View.VISIBLE);
            }
            else if(isStand){
                binding.removestand.setVisibility(View.VISIBLE);
            }
        }

        if (isCruise != lastCruise) {   // react only when state changes
            lastCruise = isCruise;

            if (isCruise) {
                binding.cruise.setVisibility(View.VISIBLE);
                startCruiseSound();
            } else {
                binding.cruise.setVisibility(View.INVISIBLE);
                stopCruiseSound();
            }
        }

        int batteryLevel = hb3.getBatteryLevel() / 10;
        String sBatteryLevel;
        if(batteryLevel>=0 && batteryLevel<=100){ sBatteryLevel = String.valueOf(batteryLevel);}
        else sBatteryLevel="Analysing";

        double speed = (hb3.getBSG2_BYTE78() * 60.0) / 3.3125 * 0.96 / 1000;
        isStand = (STAND == 1);
        boolean standChanged = (STAND != lastStand);
        lastStand = STAND;

// Priority 2: Stand UI (when unplugged and stand is open)
        if (STAND == 1 && (standChanged || LoginActivity.forceStandUIDUEToCharging)) {
            LoginActivity.forceStandUIDUEToCharging=false;
            InactivityManager.getInstance().startTimer(1);
            standui();
            return;
        }

        if(isStand && !isSideStandSoundPlaying){
            startStandSound();
        }
        else{
            if(isSideStandSoundPlaying){
                stopSideStandSound();
            }
        }

// Priority 3: Gear-based UI (only if not charging and stand is closed)

        if (directionalGears != lastGear || standChanged) {
            lastGear = directionalGears;
            if (!isStand) {
                switch (directionalGears) {
                    case 0: // Neutral
                        neutralUI();
                        //mp3name is shiftneutral
                        playGearSound(R.raw.shiftneutral); // 🔊 Play neutral shift sound
                        if(StopInactivtyTimerDuringPopUp){Log.d("sadfgg","********1");InactivityManager.getInstance().startTimer(1);}
                        break;

                    case 1: // Forward
                        powerOnUi();
                   //     cameraHandler.startRearRecording(rearPath);


                        //mp3name is shiftforward
                        playGearSound(R.raw.shiftforward); // 🔊 Play neutral shift sound
                        if(serialPortManager!=null){
                            serialPortManager.VCUSendToMCU("FFAA550101000A01");}
                        // animateSpeedLevelOnce();
                        if(StopInactivtyTimerDuringPopUp){InactivityManager.getInstance().startTimer(2);}
                        break;

                    case 2: // Reverse
                        reverseUI();
                        startBeeping(4);
                        if(StopInactivtyTimerDuringPopUp){InactivityManager.getInstance().startTimer(2);}
                        break;

                    default:
                        stopReverseBeep();
                        binding.neutralicon.setVisibility(View.INVISIBLE);
                        binding.neutral.setVisibility(View.INVISIBLE);
                        break;
                }
            }
        }


//        binding.rectangleGearsTV.setText(direction);
//        if (MainActivity.driveGear != null) {
//            switch (directionalGears) {
//                case 0:
//                    MainActivity.driveGear.setBackgroundColor(Color.TRANSPARENT); // 透明
//                    MainActivity.reverseGear.setBackgroundColor(Color.TRANSPARENT); // 透明
//                    MainActivity.neutralGear.setBackgroundColor(Color.parseColor("#ADD8E6")); // 淡蓝色
//                    break;
//                case 1:
//                    MainActivity.neutralGear.setBackgroundColor(Color.TRANSPARENT); // 透明
//                    MainActivity.reverseGear.setBackgroundColor(Color.TRANSPARENT); // 透明
//                    MainActivity.driveGear.setBackgroundColor(Color.parseColor("#ADD8E6")); // 淡蓝色
//                    break;
//                case 2:
//                    MainActivity.neutralGear.setBackgroundColor(Color.TRANSPARENT); // 透明
//                    MainActivity.driveGear.setBackgroundColor(Color.TRANSPARENT); // 透明
//                    MainActivity.reverseGear.setBackgroundColor(Color.parseColor("#ADD8E6")); // 淡蓝色
//                    break;
//                default:
//                    MainActivity.neutralGear.setBackgroundColor(Color.TRANSPARENT); // 透明
//                    MainActivity.reverseGear.setBackgroundColor(Color.TRANSPARENT); // 透明
//                    MainActivity.driveGear.setBackgroundColor(Color.parseColor("#ADD8E6")); // 淡蓝色
//                    break;
//            }
//        }

        // 更新速度档位
        String speedGear = FrameUtils.getSpeedGear(speedGears);
//        binding.rectangleTV.setText(speedGear);
//        if (MainActivity.speedThreshold1 != null) {
//            switch (speedGears) {
//                case 0:
//                    MainActivity.speedThreshold2.setBackgroundColor(Color.TRANSPARENT); // 透明
//                    MainActivity.speedThreshold3.setBackgroundColor(Color.TRANSPARENT); // 透明
//                    MainActivity.speedThreshold1.setBackgroundColor(Color.parseColor("#ADD8E6")); // 淡蓝色
//                    break;
//                case 1:
//                    MainActivity.speedThreshold1.setBackgroundColor(Color.TRANSPARENT); // 透明
//                    MainActivity.speedThreshold3.setBackgroundColor(Color.TRANSPARENT); // 透明
//                    MainActivity.speedThreshold2.setBackgroundColor(Color.parseColor("#ADD8E6")); // 淡蓝色
//                    break;
//                case 2:
//                    MainActivity.speedThreshold1.setBackgroundColor(Color.TRANSPARENT); // 透明
//                    MainActivity.speedThreshold2.setBackgroundColor(Color.TRANSPARENT); // 透明
//                    MainActivity.speedThreshold3.setBackgroundColor(Color.parseColor("#ADD8E6")); // 淡蓝色
//                    break;
//                default:
//                    MainActivity.speedThreshold1.setBackgroundColor(Color.TRANSPARENT); // 透明
//                    MainActivity.speedThreshold3.setBackgroundColor(Color.TRANSPARENT); // 透明
//                    MainActivity.speedThreshold2.setBackgroundColor(Color.parseColor("#ADD8E6")); // 淡蓝色
//                    break;
//            }
//        }

        // 更新 READY 状态
        // isReady = (READY == 1);
//        binding.readyIV.setVisibility(isReady ? View.VISIBLE : View.INVISIBLE);
    }

    private void startCruiseSound() {
        if (cruisePlayer == null) {
            cruisePlayer = MediaPlayer.create(context, R.raw.ecocitysportsound);
        }
        if (!cruisePlayer.isPlaying()) {
            cruisePlayer.start();
            isCruiseSoundPlaying = true;
        }
    }

    private void stopCruiseSound() {
        if (cruisePlayer != null && cruisePlayer.isPlaying()) {
            cruisePlayer.pause();
            cruisePlayer.seekTo(0);
        }
        isCruiseSoundPlaying = false;
    }


    private void startStandSound() {
        if (sideStandPlayer == null) {   // ✅ create only once
            sideStandPlayer = MediaPlayer.create(context, R.raw.sidestandopensound);
            sideStandPlayer.setLooping(true);
        }
        if (!sideStandPlayer.isPlaying()) {
            sideStandPlayer.start();     // ✅ only start if not already playing
        }
        isSideStandSoundPlaying = true;
    }

    private void stopSideStandSound() {
        if (sideStandPlayer != null) {
            if (sideStandPlayer.isPlaying()) {
                sideStandPlayer.stop();
            }
            sideStandPlayer.release();
            sideStandPlayer = null;
        }
        isSideStandSoundPlaying = false;
    }


    private MediaPlayer gearshiftmediaPlayer;

    private void playGearSound(int resId) {
        if (context == null) {
            Log.e("GearSound", "Context is null, cannot play sound");
            return;
        }

        try {
            if (gearshiftmediaPlayer != null) {
                gearshiftmediaPlayer.release();
                gearshiftmediaPlayer = null;
            }

            gearshiftmediaPlayer = MediaPlayer.create(context, resId);
            if (gearshiftmediaPlayer == null) {
                Log.e("GearSound", "MediaPlayer creation failed for resId: " + resId);
                return;
            }

            gearshiftmediaPlayer.setOnCompletionListener(mp -> {
                try {
                    mp.release();
                } catch (Exception e) {
                    Log.e("GearSound", "Error releasing MediaPlayer: " + e.getMessage());
                }
                gearshiftmediaPlayer = null;
            });

            gearshiftmediaPlayer.start();

        } catch (Exception e) {
            Log.e("GearSound", "Exception while playing gear sound: " + e.getMessage(), e);
        }
    }

    private void animateSpeedLevelOnce() {

        final Handler handler = new Handler();
        final int[] level = {0};
        final boolean[] increasing = {true};

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                int mappedSpeed = (level[0] / 10) * 10; // Use multiples of 10 (0,10,...100)

                if (MainActivity.speedImageMap.containsKey(mappedSpeed)) {
                    binding.speedImageView.setImageResource(MainActivity.speedImageMap.get(mappedSpeed));
                }

                if (increasing[0]) {
                    level[0] += 10;
                    if (level[0] >= 100) {
                        increasing[0] = false;
                    }
                } else {
                    level[0] -= 10;
                    if (level[0] < 0) {
                        return;
                    }
                }

                handler.postDelayed(this, 50); // Control animation speed
            }
        }, 50);
    }

//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        inactivityManager.resetTimer();  // Reset inactivity timer on any touch
//        return super.onTouchEvent(event);
//    }
//    @Override
//    public boolean dispatchTouchEvent(MotionEvent ev) {
//        if (inactivityManager != null) {
//            inactivityManager.resetTimer();
//        }
//        return super.dispatchTouchEvent(ev);
//    }

    //    public void ChargingUI(String SOC)
//    {
//        binding.tripreco.setVisibility(View.INVISIBLE);
//        binding.neutralicon.setVisibility((View.INVISIBLE));
//        binding.neutral.setVisibility(View.INVISIBLE);
//        binding.batterryBar.setVisibility(View.VISIBLE);
//        binding.batteryTV.setVisibility(View.VISIBLE);
//        binding.network.setVisibility(View.VISIBLE);
//        binding.wifi.setVisibility(View.VISIBLE);
//        binding.timeTextView.setVisibility(View.VISIBLE);
//        binding.BluetoothIV.setVisibility(View.VISIBLE);
//        binding.batteryImageView.setVisibility(View.VISIBLE);
//        binding.picklocationIV.setVisibility(View.INVISIBLE);//visible
//        binding.chargingSOC.setVisibility(View.VISIBLE);
//        //binding.percenticon.setVisibility(View.VISIBLE);
//        binding.chargingSOC.setText(SOC+"%");
//        binding.chargingbike.setVisibility(View.VISIBLE);
//
//        binding.reverseui.setVisibility(View.INVISIBLE);
//        binding.avgSpeed.setVisibility(View.INVISIBLE);
//        binding.tripTextView.setVisibility(View.INVISIBLE);
//        binding.tripresetbtn.setVisibility(View.INVISIBLE);
//        binding.picklocationIV.setVisibility(View.INVISIBLE);//visible
//        binding.speedImageView.setVisibility(View.INVISIBLE);
//        binding.speedNumberTV.setVisibility(View.INVISIBLE);
//        binding.speedTV.setVisibility(View.INVISIBLE);
//
//        binding.neutralicon.setVisibility((View.INVISIBLE));
//        binding.neutral.setVisibility(View.INVISIBLE);
//        binding.brk.setVisibility(View.INVISIBLE);
//        binding.picklocationIV.setVisibility(View.INVISIBLE);
//        binding.removestand.setVisibility(View.INVISIBLE);
//        binding.ecoimage.setVisibility(View.GONE);
//        binding.sports.setVisibility((View.GONE));
//        binding.boost.setVisibility(View.GONE);
//        binding.imageView4.setVisibility(View.INVISIBLE);
//        binding.imageView5.setVisibility(View.INVISIBLE);
//        binding.mapbtn.setVisibility(View.INVISIBLE);
//        binding.menubtn.setVisibility(View.INVISIBLE);
//        binding.enginestartIV.setVisibility(View.INVISIBLE);
//        binding.chargingicon.setVisibility(View.VISIBLE);
//        binding.neutralicon.setVisibility(View.INVISIBLE);
//        binding.arrow3.setVisibility(View.INVISIBLE);
//        binding.rectangleGearsTV.setVisibility((View.INVISIBLE));
//        binding.removestand.setVisibility(View.INVISIBLE);
//        binding.highBeamIVon.setVisibility(View.INVISIBLE);
//        binding.stand.setVisibility(View.INVISIBLE);
//        binding.ecoimage.setVisibility(View.GONE);
//        binding.sports.setVisibility((View.GONE));
//        binding.boost.setVisibility(View.GONE);
//        binding.brk.setVisibility(View.INVISIBLE);
//        binding.startButton.setVisibility(View.INVISIBLE);
//        binding.turnLeftIV.setVisibility(View.INVISIBLE);
//        binding.turnRightIV.setVisibility(View.INVISIBLE);
//        binding.highBeamIVoff.setVisibility(View.INVISIBLE);
//    }
//    private Handler animationHandler = new Handler(Looper.getMainLooper());
//    private Runnable fadeInRunnable;
    public void neutralUI() {
//        runOnUiThread(() -> {
//            if (!isDriveMode && binding.brk.getAnimation() == null) {
//                Animation rotate = AnimationUtils.loadAnimation(binding.getRoot().getContext(), R.anim.rotate);
//                Animation fade = AnimationUtils.loadAnimation(binding.getRoot().getContext(), R.anim.fade);
//
//                rotate.setAnimationListener(new Animation.AnimationListener() {
//                    @Override
//                    public void onAnimationStart(Animation animation) {
//                        binding.startButton.setVisibility(View.INVISIBLE);
//                    }
//
//                    @Override
//                    public void onAnimationEnd(Animation animation) {
//                        if (!isDriveMode) {
//                            binding.brk.startAnimation(rotate);
//                        }
//                    }
//
//                    @Override
//                    public void onAnimationRepeat(Animation animation) {
//                    }
//                });
//
//                binding.brk.startAnimation(rotate);
//
//                // Cancel any previous runnable
//                if (fadeInRunnable != null) animationHandler.removeCallbacks(fadeInRunnable);
//
//                // Delay fade animation
//                fadeInRunnable = () -> {
//                    if (!isDriveMode && chargingFlag) {
//                        binding.startButton.setVisibility(View.VISIBLE);
//                        binding.startButton.startAnimation(fade);
//                    }
//                };
//                animationHandler.postDelayed(fadeInRunnable, 1500);
//
//            } else if (isDriveMode) {
//                // Cancel any pending fade animations
//                if (fadeInRunnable != null) animationHandler.removeCallbacks(fadeInRunnable);
//
//                // Immediately clear animations and hide UI
//                binding.brk.clearAnimation();
//                binding.startButton.clearAnimation();
//                binding.startButton.setVisibility(View.INVISIBLE);
//            }
//        });
//        if (fadeInRunnable != null) animationHandler.removeCallbacks(fadeInRunnable);
//        binding.brk.clearAnimation();
//        binding.startButton.clearAnimation();
        stopReverseBeep();
        binding.neutralicon.setVisibility(View.VISIBLE);
        binding.brk.setVisibility(View.VISIBLE);
        binding.startButton.setVisibility(View.VISIBLE);
        binding.reverseui.setVisibility(View.INVISIBLE);
        binding.avgSpeed.setVisibility(View.INVISIBLE);
        binding.tripTextView.setVisibility(View.INVISIBLE);
        binding.navleft.setVisibility(View.INVISIBLE);
        binding.navright.setVisibility(View.INVISIBLE);
        binding.tripresetbtn.setVisibility(View.INVISIBLE);
        binding.arrow3.setVisibility(View.INVISIBLE);
//        binding.tripreco.setVisibility(View.VISIBLE);

        binding.neutralicon.setImageResource(R.drawable.neutralicon); // Replace with your drawable
        binding.neutral.setVisibility(View.VISIBLE);
        binding.batterryBar.setVisibility(View.VISIBLE);
        binding.batteryTV.setVisibility(View.VISIBLE);
        binding.network.setVisibility(View.VISIBLE);
        binding.wifi.setVisibility(View.VISIBLE);
        binding.timeTextView.setVisibility(View.VISIBLE);
        binding.BluetoothIV.setVisibility(View.VISIBLE);
        binding.batteryImageView.setVisibility(View.VISIBLE);
        binding.APUbatteryImageView.setVisibility(View.VISIBLE);
        binding.picklocationIV.setVisibility(View.VISIBLE);//visible
        binding.speedImageView.setVisibility(View.INVISIBLE);
        binding.speedNumberTV.setVisibility(View.INVISIBLE);
        binding.speedTV.setVisibility(View.INVISIBLE);
        binding.turnLeftIV.setVisibility(View.VISIBLE);
//        binding.highBeamIVoff.setVisibility(View.VISIBLE);
        binding.turnRightIV.setVisibility(View.VISIBLE);
        // binding.leftOn.setVisibility(View.INVISIBLE);
        //  binding.rightOn.setVisibility(View.INVISIBLE);
        //binding.readyIV.setVisibility(View.INVISIBLE);
        // binding.highBeamIVon.setVisibility(View.INVISIBLE);
        binding.enginestartIV.setVisibility(View.INVISIBLE);
        binding.rectangleGearsTV.setVisibility((View.INVISIBLE));
        binding.sports.setVisibility((View.INVISIBLE));
        binding.stand.setVisibility((View.INVISIBLE));
        binding.chargingSOC.setVisibility(View.INVISIBLE);
        //binding.percenticon.setVisibility(View.INVISIBLE);
        binding.chargingbike.setVisibility(View.INVISIBLE);
        binding.mapbtn.setVisibility(View.VISIBLE);
        binding.menubtn.setVisibility(View.VISIBLE);
        binding.chargingicon.setVisibility(View.INVISIBLE);
//        binding.brk.setVisibility(View.INVISIBLE);
//        binding.startButton.setVisibility(View.INVISIBLE);
        binding.apubaticon.setVisibility(View.VISIBLE);
        binding.APUbatteryTV.setVisibility(View.VISIBLE);
        binding.removestand.setVisibility(View.INVISIBLE);
//        binding.chargingicononmainactivty.setVisibility(View.VISIBLE);
    }

    public void powerOnUi()
    {
        // Stop animations immediately
//        if (fadeInRunnable != null) animationHandler.removeCallbacks(fadeInRunnable);
//        binding.brk.clearAnimation();

        stopReverseBeep();
        binding.avgSpeed.setVisibility(View.VISIBLE);
        binding.tripTextView.setVisibility(View.VISIBLE);
        binding.navleft.setVisibility(View.VISIBLE);
        binding.navright.setVisibility(View.VISIBLE);
        binding.tripresetbtn.setVisibility(View.VISIBLE);
//        binding.tripreco.setVisibility(View.INVISIBLE);
        binding.startButton.clearAnimation();
        binding.startButton.setVisibility(View.INVISIBLE);
        binding.picklocationIV.setVisibility(View.INVISIBLE);
        binding.removestand.setVisibility(View.INVISIBLE);
        binding.enginestartIV.setVisibility(View.VISIBLE);
        binding.speedNumberTV.setVisibility(View.VISIBLE);
        binding.speedTV.setVisibility(View.VISIBLE);
        binding.batterryBar.setVisibility(View.VISIBLE);
        binding.batteryTV.setVisibility(View.VISIBLE);
        binding.network.setVisibility(View.VISIBLE);
        binding.wifi.setVisibility(View.VISIBLE);
        binding.timeTextView.setVisibility(View.VISIBLE);
        binding. BluetoothIV.setVisibility(View.VISIBLE);
        binding.batteryImageView.setVisibility(View.VISIBLE);
        binding.APUbatteryImageView.setVisibility(View.VISIBLE);
        if(!shouldshowchargingicononmainonot){
            binding.rectangleGearsTV.setVisibility((View.VISIBLE));
        }
        binding.speedImageView.setVisibility(View.VISIBLE);
        binding.neutralicon.setVisibility((View.INVISIBLE));
        binding.neutral.setVisibility(View.INVISIBLE);
        binding.brk.setVisibility(View.INVISIBLE);
//        binding.tripreco.setVisibility(View.VISIBLE);
        binding.reverseui.setVisibility(View.INVISIBLE);
        binding.arrow3.setVisibility(View.INVISIBLE);
        binding.stand.setVisibility((View.INVISIBLE));
        binding.chargingSOC.setVisibility(View.INVISIBLE);
        // binding.percenticon.setVisibility(View.INVISIBLE);
        binding.chargingbike.setVisibility(View.INVISIBLE);
        binding.mapbtn.setVisibility(View.VISIBLE);
        binding.menubtn.setVisibility(View.VISIBLE);
        binding.chargingicon.setVisibility(View.INVISIBLE);
        binding.apubaticon.setVisibility(View.VISIBLE);
        binding.APUbatteryTV.setVisibility(View.VISIBLE);
        binding.turnLeftIV.setVisibility(View.VISIBLE);
        binding.turnRightIV.setVisibility(View.VISIBLE);
//        binding.highBeamIVoff.setVisibility(View.VISIBLE);
        binding.removestand.setVisibility(View.INVISIBLE);
//        binding.chargingicononmainactivty.setVisibility(View.VISIBLE);
    }

    public void reverseUI()
    {

        // Stop animations immediately
//        if (fadeInRunnable != null) animationHandler.removeCallbacks(fadeInRunnable);
//        binding.brk.clearAnimation();
//        binding.startButton.clearAnimation();
        startReverseBeep();
        binding.startButton.setVisibility(View.INVISIBLE);
        binding.avgSpeed.setVisibility(View.VISIBLE);
        binding.tripTextView.setVisibility(View.INVISIBLE);
        binding.tripresetbtn.setVisibility(View.INVISIBLE);
//        binding.tripreco.setVisibility(View.VISIBLE);
        //if(isReady)binding.readyIV.setVisibility(View.INVISIBLE);
        binding.speedNumberTV.setVisibility(View.INVISIBLE);
        binding.rectangleGearsTV.setVisibility((View.INVISIBLE));
        binding.speedImageView.setVisibility(View.INVISIBLE);
        binding.speedTV.setVisibility(View.INVISIBLE);
        // binding.turnLeftIV.setVisibility(View.INVISIBLE);
        //  binding.turnRightIV.setVisibility(View.INVISIBLE);
        // binding.leftOn.setVisibility(View.INVISIBLE);
        // binding.rightOn.setVisibility(View.INVISIBLE);
//        binding.highBeamIVon.setVisibility(View.INVISIBLE);
        binding.enginestartIV.setVisibility(View.INVISIBLE);
        //  binding.highBeamIVoff.setVisibility(View.INVISIBLE);
        binding.batterryBar.setVisibility(View.INVISIBLE);
        binding.batteryTV.setVisibility(View.INVISIBLE);
        binding.network.setVisibility(View.INVISIBLE);
        binding.wifi.setVisibility(View.INVISIBLE);
        binding.timeTextView.setVisibility(View.INVISIBLE);
        binding.BluetoothIV.setVisibility(View.INVISIBLE);
        binding.batteryImageView.setVisibility(View.INVISIBLE);
        binding.APUbatteryImageView.setVisibility(View.INVISIBLE);

        binding.neutralicon.setVisibility((View.INVISIBLE));
        binding.neutral.setVisibility(View.INVISIBLE);
        binding.brk.setVisibility(View.INVISIBLE);
        binding.picklocationIV.setVisibility(View.INVISIBLE);
        binding.removestand.setVisibility(View.INVISIBLE);
        binding.ecoimage.setVisibility(View.GONE);
        binding.sports.setVisibility((View.GONE));
        binding.boost.setVisibility(View.GONE);
        binding.removestand.setVisibility(View.INVISIBLE);

        //reverse visisble ui
        binding.reverseui.setVisibility(View.VISIBLE);
        binding.arrow3.setVisibility(View.VISIBLE);
        binding.stand.setVisibility((View.INVISIBLE));
        binding.chargingSOC.setVisibility(View.INVISIBLE);
        // binding.percenticon.setVisibility(View.INVISIBLE);
        binding.chargingbike.setVisibility(View.INVISIBLE);
        binding.mapbtn.setVisibility(View.VISIBLE);
        binding.menubtn.setVisibility(View.VISIBLE);
        binding.chargingicon.setVisibility(View.INVISIBLE);
        binding.apubaticon.setVisibility(View.INVISIBLE);
        binding.APUbatteryTV.setVisibility(View.INVISIBLE);
        binding.turnLeftIV.setVisibility(View.VISIBLE);
        binding.turnRightIV.setVisibility(View.VISIBLE);
//        binding.highBeamIVoff.setVisibility(View.VISIBLE);
//        binding.chargingicononmainactivty.setVisibility(View.INVISIBLE);
    }
    public static boolean shouldshowchargingicononmainonot;
    public void standui() {
        if(isStand){
            // Stop animations immediately
//            if (fadeInRunnable != null) animationHandler.removeCallbacks(fadeInRunnable);
//            binding.brk.clearAnimation();
//            binding.startButton.clearAnimation();
            binding.startButton.setVisibility(View.INVISIBLE);}
        stopReverseBeep();
        binding.batterryBar.setVisibility(View.VISIBLE);
        binding.avgSpeed.setVisibility(View.INVISIBLE);
        binding.tripTextView.setVisibility(View.INVISIBLE);
//        binding.tripreco.setVisibility(View.INVISIBLE);
        binding.navleft.setVisibility(View.INVISIBLE);
        binding.navright.setVisibility(View.INVISIBLE);
        binding.tripresetbtn.setVisibility(View.INVISIBLE);
        binding. batteryTV.setVisibility(View.VISIBLE);
        binding.network.setVisibility(View.VISIBLE);
        binding. wifi.setVisibility(View.VISIBLE);
        binding.timeTextView.setVisibility(View.VISIBLE);
        binding.BluetoothIV.setVisibility(View.VISIBLE);
        binding.batteryImageView.setVisibility(View.VISIBLE);
        binding.APUbatteryImageView.setVisibility(View.VISIBLE);
        binding.brk.setVisibility(View.INVISIBLE);

        binding.reverseui.setVisibility(View.INVISIBLE);
        binding.arrow3.setVisibility(View.INVISIBLE);

        binding.removestand.setVisibility((!shouldshowchargingicononmainonot  && isStand   ? View.VISIBLE : View.INVISIBLE));//visible


        binding.stand.setVisibility((isStand ? View.VISIBLE : View.INVISIBLE)); //visisble
        binding.picklocationIV.setVisibility((isStand ? View.VISIBLE : View.INVISIBLE));//visible
        binding.speedNumberTV.setVisibility((isStand ? View.INVISIBLE : View.VISIBLE));
        binding.speedTV.setVisibility((isStand ? View.INVISIBLE : View.VISIBLE));
//                boost.setVisibility((isStand?View.INVISIBLE:View.INVISIBLE));
//                ecoimage.setVisibility((isStand?View.INVISIBLE:View.VISIBLE));
        binding.speedImageView.setVisibility(View.INVISIBLE);
        binding.ecoimage.setVisibility(View.GONE);
        binding.sports.setVisibility((View.GONE));
        binding.boost.setVisibility(View.GONE);
        //  binding.highBeamIVon.setVisibility((isStand ? View.INVISIBLE : View.VISIBLE));
        binding.enginestartIV.setVisibility((isStand ? View.INVISIBLE : View.VISIBLE));
        //  binding.highBeamIVoff.setVisibility((isStand ? View.INVISIBLE : View.VISIBLE));
        binding.neutralicon.setVisibility((View.INVISIBLE));
        binding.neutral.setVisibility(View.INVISIBLE);
        binding.chargingbike.setVisibility(View.INVISIBLE);
        binding.mapbtn.setVisibility(View.VISIBLE);
        binding.menubtn.setVisibility(View.VISIBLE);
        binding.chargingicon.setVisibility(View.INVISIBLE);
        binding.chargingSOC.setVisibility(View.INVISIBLE);
        // binding.percenticon.setVisibility(View.INVISIBLE);
        binding.chargingbike.setVisibility(View.INVISIBLE);
//        binding.highBeamIVoff.setVisibility(View.INVISIBLE);
//        binding.turnLeftIV.setVisibility(View.INVISIBLE);
//        binding.turnRightIV.setVisibility(View.INVISIBLE);
        binding.rectangleGearsTV.setVisibility(View.INVISIBLE);
        binding.apubaticon.setVisibility(View.VISIBLE);
        binding.APUbatteryTV.setVisibility(View.VISIBLE);
    }

    // --- Fields ---
    private int currentState = -1;
    private int lastActiveState = -1;
    private boolean blinkVisible = true;
    private boolean isBlinking = false;
    private boolean isSoundPlaying = false;

    private final Handler blinkHandler = new Handler(Looper.getMainLooper());
    private final Handler stopDelayHandler = new Handler(Looper.getMainLooper());
    private final Handler soundHandler = new Handler(Looper.getMainLooper());

    private final int BLINK_INTERVAL_MS = 500;
    private final int SOUND_INTERVAL_MS = 1000;
    private final int ZERO_HOLD_DURATION_MS = 1500;

    private Runnable blinkRunnable;
    private Runnable stopBlinkingRunnable;
    private Runnable soundRunnable;

    private SoundPool soundPool;
    private int soundId;
    private boolean soundLoaded = false;
    public static boolean ignorefirstblink;


    public void initIndicatorLogic(Context context) {
        soundPool = new SoundPool.Builder().setMaxStreams(1).build();
        soundId = soundPool.load(context, R.raw.turnsound, 1);
        soundPool.setOnLoadCompleteListener((sp, id, status) -> {
            soundLoaded = (status == 0);
        });

        blinkRunnable = () -> {
            blinkVisible = !blinkVisible;
            switch (currentState) {
                case 1:
                    binding.leftOn.setVisibility(blinkVisible ? View.VISIBLE : View.INVISIBLE);
                    binding.rightOn.setVisibility(View.INVISIBLE);
                    //startRearCameraPreview();
                    break;
                case 2:
                    binding.rightOn.setVisibility(blinkVisible ? View.VISIBLE : View.INVISIBLE);
                    binding.leftOn.setVisibility(View.INVISIBLE);
                    //startRearCameraPreview();
                    break;
                case 3:
                    binding.leftOn.setVisibility(blinkVisible ? View.VISIBLE : View.INVISIBLE);
                    binding.rightOn.setVisibility(blinkVisible ? View.VISIBLE : View.INVISIBLE);
                    break;
            }

            if (isBlinking) {
                blinkHandler.postDelayed(blinkRunnable, BLINK_INTERVAL_MS);
            }
        };

        stopBlinkingRunnable = () -> {
            stopBlinking();
            //stopAllCameraPreviews();
            // Log.d("Indicator", "⛔ Stopped due to sustained 0");
        };

        soundRunnable = () -> {
            if (isBlinking && soundLoaded && directionalGears != 2  && !ignorefirstblink) {
                soundPool.play(soundId, 1, 1, 0, 0, 1);
                soundHandler.postDelayed(soundRunnable, SOUND_INTERVAL_MS);
            } else {
                isSoundPlaying = false; // Stop loop if in reverse
            }
        };
    }

    public void indicators(int newSignal) {
        Log.d("Indicator", "➡ New signal: " + newSignal);

        if (isBlinking && currentState == 3 && (newSignal == 1 || newSignal == 2)) {
            Log.d("Indicator", "⚠️ Ignored glitch signal " + newSignal + " while hazard (3) is active");
            return;
        }

        if (newSignal == 0) {
            stopDelayHandler.removeCallbacks(stopBlinkingRunnable);
            stopDelayHandler.postDelayed(stopBlinkingRunnable, ZERO_HOLD_DURATION_MS);
           // stopAllCameraPreviews();
            return;
        }

        stopDelayHandler.removeCallbacks(stopBlinkingRunnable);

        currentState = newSignal;
        lastActiveState = newSignal;
        startBlinking();
    }

    private void startBlinking() {
        InactivityManager.getInstance().stopTimer();
        blinkHandler.removeCallbacks(blinkRunnable);
        blinkVisible = true;
        isBlinking = true;

        // Show ON instantly
        switch (currentState) {
            case 1:
                binding.leftOn.setVisibility(View.VISIBLE);
                binding.rightOn.setVisibility(View.INVISIBLE);
                break;
            case 2:
                binding.rightOn.setVisibility(View.VISIBLE);
                binding.leftOn.setVisibility(View.INVISIBLE);
                break;
            case 3:
                binding.leftOn.setVisibility(View.VISIBLE);
                binding.rightOn.setVisibility(View.VISIBLE);
                break;
        }

        blinkHandler.postDelayed(blinkRunnable, BLINK_INTERVAL_MS);

        // ✅ Only start sound if not in reverse and not already playing
        if (directionalGears != 2 && !isSoundPlaying && soundLoaded) {
            isSoundPlaying = true;
            soundHandler.post(soundRunnable);
        }
    }

    private void stopBlinking() {
        if(directionalGears==0){InactivityManager.getInstance().startTimer(1);}
        isBlinking = false;
        isSoundPlaying = false;

        blinkHandler.removeCallbacks(blinkRunnable);
        soundHandler.removeCallbacks(soundRunnable);
        stopDelayHandler.removeCallbacks(stopBlinkingRunnable);

        binding.leftOn.setVisibility(View.INVISIBLE);
        binding.rightOn.setVisibility(View.INVISIBLE);
    }


//    private void playIndicatorSound() {
//        if (indicatorPlayer != null) {
//            indicatorPlayer.release();
//        }
//
//        indicatorPlayer = MediaPlayer.create(context, R.raw.turnsound);
//        if (indicatorPlayer != null) {
//            indicatorPlayer.setOnCompletionListener(mp -> {
//                mp.release();
//                indicatorPlayer = null;
//            });
//            indicatorPlayer.start();
//        }
//    }



    /**
     * 更新 CAN 数据 10F907A8
     * @param hb3 解析后的 Heartbeat3 对象
     */
    private int lastBatteryLevel = -1; // Store last battery level to avoid redundant updates
    private int lastAPUBatteryLevel = -1; // Store last battery level to avoid redundant updates

    private int lastSpeed = -1; // To avoid redundant updates
    TripDistanceManager tripManager;
    TextView tripTextView;


    public void setTripManager(TripDistanceManager tripManager) {
        this.tripManager = tripManager;
    }
    public static double tiredimensions=1.4825;
    private static double rpm;
    private String previousMode = "";
    private MediaPlayer modeChangePlayer;
    private long lastCanUpdateTime = 0;
    private static final long CAN_TIMEOUT_MS = 5000; // 5 seconds
    private boolean canPopupShown = false;
    private Handler canTimeoutHandler = new Handler(Looper.getMainLooper());

    // Call this in Activity.onCreate or UIUpdater constructor
    private boolean controllerErrorSoundPlayed = false;
    private MediaPlayer controllerErrorPlayer;
    public static boolean islock;
    private void startCanTimeoutChecker() {
        canTimeoutHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();

                // If timeout happened and popup not yet shown
                if ((now - lastCanUpdateTime) > CAN_TIMEOUT_MS && !canPopupShown  && islock) {
                    canPopupShown = true;

                    if (!activity.isFinishing() && !activity.isDestroyed()) {

//                        if (serialPortManager != null) {
//                            serialPortManager.getHb3().setBSG2_BYTE78(0);
//                        }
//                        binding.speedNumberTV.setText("0");
//                        updateSpeedAnimation(0);
//                        binding.ecoimage.setVisibility(View.GONE);
//                        binding.sports.setVisibility(View.GONE);
//                        binding.boost.setVisibility(View.GONE);

                         //🔊 Play sound only once per error event
                        if (!controllerErrorSoundPlayed) {
                            controllerErrorPlayer = MediaPlayer.create(activity, R.raw.controllererror);
                            controllerErrorPlayer.setOnCompletionListener(mp -> {
                                mp.release();
                                controllerErrorPlayer = null;
                            });
                            controllerErrorPlayer.start();
                            controllerErrorSoundPlayed = true; // Prevent replay until reset
                        }

                         //🚨 Show the alert
                        new AlertDialog.Builder(activity)
                                .setTitle("Error Alert")
                                .setMessage("Controller switched off abruptly, or a CAN line issue occurred.")
                                .setPositiveButton("OK", (dialog, which) -> {
                                    // Reset speed data
                                    if (serialPortManager != null) {
                                        serialPortManager.getHb3().setBSG2_BYTE78(0);
                                    }
                                    binding.speedNumberTV.setText("0");
                                    updateSpeedAnimation(0);
                                    binding.ecoimage.setVisibility(View.GONE);
                                    binding.sports.setVisibility(View.GONE);
                                    binding.boost.setVisibility(View.GONE);

                                    // 🔄 Allow sound to play again for next error
                                    controllerErrorSoundPlayed = false;

                                    dialog.dismiss();
                                })
                                .setCancelable(false)
                                .show();
                    }
                }

                // Keep checking every second
                canTimeoutHandler.postDelayed(this, 1000);
            }
        }, 1000);
    }
    private boolean resetboost=false;
    public void updateCanData10F907A8(Heartbeat3 hb3) {

        long now = System.currentTimeMillis();
        lastCanUpdateTime = now;
        canPopupShown = false;

        double speed = (hb3.getBSG2_BYTE78() * 60.0 * tiredimensions) / 3.3125 * 1 / 1000;//1.4825
        rpm =hb3.getBSG2_BYTE78();
        if (tripManager == null) return;
        if(hb3.getBSG2_BYTE78()<4700){tripManager.updateTrip(hb3.getBSG2_BYTE78());}
//        Log.d("sadfdgf","THIS IS TRIPBUG    "+hb3.getBSG2_BYTE78());
        double tripKm = tripManager.getTripDistance();
        double odoKm = tripManager.getOdoDistance();
        tm.setTeleTrip((float) roundTo2Decimal(tripKm));
        tm.setTeleODO((float) roundTo2Decimal(odoKm));
//        Log.d("dafsgd","ODO is "+odoKm);
//        Log.d("dafsgd","ODOTRIP is "+tripKm);

        if (activity != null) {
            activity.refreshDistance();
        }
        InactivityManager.getInstance().updateSpeed(speed);

        if(speed < 150){
            try {
                tm.setTeleSpeed((int)speed);
                binding.speedNumberTV.setText(String.valueOf((int) speed));
                updateSpeedAnimation((int)speed); // animate smoothly to new value
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        String currentMode = ""; // new
        int boostEcoSpeed=((int)speed);

        if (directionalGears != 2 && !isboost) {
            resetboost=true;
//            Log.d("dsfgd","SOOOOOOboost=true");
            binding.actualboost.setVisibility(View.INVISIBLE);

            if (boostEcoSpeed <= 45 && boostEcoSpeed != 0) {
                currentMode = "eco";
                if(!isStand)binding.ecoimage.setVisibility(View.VISIBLE);
                binding.sports.setVisibility(View.GONE);
                binding.boost.setVisibility(View.GONE);
            } else if (boostEcoSpeed > 45 && boostEcoSpeed <= 75) {
                currentMode = "city";
                binding.ecoimage.setVisibility(View.GONE);
                if(!isStand)binding.sports.setVisibility(View.VISIBLE);
                binding.boost.setVisibility(View.GONE);
            } else if (boostEcoSpeed > 75 && boostEcoSpeed < 150) {
                currentMode = "sport";
                binding.ecoimage.setVisibility(View.GONE);
                binding.sports.setVisibility(View.GONE);
                if(!isStand)binding.boost.setVisibility(View.VISIBLE);
            } else {
                currentMode = "";
                binding.ecoimage.setVisibility(View.GONE);
                binding.sports.setVisibility(View.GONE);
                binding.boost.setVisibility(View.GONE);
            }

            // ✅ Play sound only on upward mode transition
            if (
                    (previousMode.equals("eco") && currentMode.equals("city")) ||
                            (previousMode.equals("city") && currentMode.equals("sport"))
            ) {
                playModeChangeSound(); // play sound
            }

            // ✅ Update previousMode
            if (!currentMode.equals("")) {
                previousMode = currentMode;
            }

        } else if (directionalGears != 2 && resetboost && boostEcoSpeed > 0) {
            binding.actualboost.setVisibility(View.VISIBLE);
            binding.ecoimage.setVisibility(View.GONE);
            binding.sports.setVisibility(View.GONE);
            binding.boost.setVisibility(View.GONE);
            playModeChangeSound();
//            Log.d("dsfgd","SOOOOOOboost=false");
            resetboost=false;
        }
    }

    private double roundTo2Decimal(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
    private void playModeChangeSound() {
        if (modeChangePlayer != null) {
            modeChangePlayer.release();
        }
        modeChangePlayer = MediaPlayer.create(context, R.raw.ecocitysportsound);
        modeChangePlayer.start();
    }

    public void updateSpeedAnimation(int speed) {
        int mappedSpeed = (speed > 100) ? 101 : Math.max(0, speed); // If speed > 100, use 101 (speed_100)

        if (mappedSpeed == lastSpeed) return; // Avoid redundant updates

        if (MainActivity.speedImageMap.containsKey(mappedSpeed)) {
            binding.speedImageView.setImageResource(MainActivity.speedImageMap.get(mappedSpeed));
        }

        lastSpeed = mappedSpeed;
    }


    public void updateBatteryAnimation(int batteryLevel) {
        // Clamp battery level between 0-100 and round to the nearest even number
        int mappedBattery = (batteryLevel > 100) ? 100 : Math.max(0, (batteryLevel / 2) * 2);

        if (mappedBattery == lastBatteryLevel) return; // Skip if battery level hasn't changed

        if (MainActivity.batteryImageMap.containsKey(mappedBattery)) {
            binding.batteryImageView.setImageResource(MainActivity.batteryImageMap.get(mappedBattery)); // Update ImageView
        }

        lastBatteryLevel = mappedBattery;
    }

    public void APUupdateBatteryAnimation(int batteryLevel) {
        // Clamp battery level between 0-100 and round to the nearest even number
        int mappedAPUBattery = (batteryLevel > 100) ? 100 : Math.max(0, (batteryLevel / 2) * 2);

        if (mappedAPUBattery == lastAPUBatteryLevel) return; // Skip if battery level hasn't changed

        if (MainActivity.APUbatteryImageMap.containsKey(mappedAPUBattery)) {
            binding.APUbatteryImageView.setImageResource(MainActivity.APUbatteryImageMap.get(mappedAPUBattery)); // Update ImageView
        }

        lastAPUBatteryLevel = mappedAPUBattery;
    }

    private int getBatteryImageResource(int batteryLevel) {
        if (batteryLevel < 25) {
            return R.drawable.battery_level_1;
        } else if (batteryLevel < 60) {
            return R.drawable.battery_level_2;
        } else {
            return R.drawable.battery_level_3;
        }
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
                //updateBleMac(mad);
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
//    private void updateBleMac(McuAnswerData mad) {
//        // 蓝牙 Mac 地址
//        String formattedMac = FrameUtils.formatMacAddress(mad.getBleMac());
//        Log.d("UIUpdater", "updateBleMac succeed " + mad.getTaskId());
//        binding.BluetoothMACInput.setText(formattedMac);
//    }

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

    public void showCustomPopup(String title, String message) {
        // Inflate the custom layout
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.popupdialog, null);

        // Create the dialog with the custom view
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(dialogView);

        AlertDialog alertDialog = builder.create();
        alertDialog.setCancelable(false); // User can't dismiss with back button or outside touch

        // Set dynamic content
        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        TextView dialogMessage = dialogView.findViewById(R.id.dialogMessage);
        dialogTitle.setText(title);
        dialogMessage.setText(message);

        // Handle button actions
        Button btnYes = dialogView.findViewById(R.id.btnYes);
        Button btnNo = dialogView.findViewById(R.id.btnNo);

        btnYes.setOnClickListener(v -> {
            StopInactivtyTimerDuringPopUp=true;
            batteryPackSwitcher switcher = new batteryPackSwitcher(binding.textstatus);
            switcher.startSwitchingToAPU();
            apuflag=true;
//            BatPackSwitcher.startSwitchingToAPU();
            alertDialog.dismiss();
        });

        btnNo.setOnClickListener(v -> {
            StopInactivtyTimerDuringPopUp=true;
            Log.d("CustomPopup", "USER DECLINE TO SWITCH TO APU");
            alertDialog.dismiss();
            serialPortManager.VCUSendToMCU("FFAA550103001510FBA80728007800B004A000");//line current 10 A
            new Handler(Looper.getMainLooper()).postDelayed(() -> serialPortManager.VCUSendToMCU("FFAA550103001510F8A8070000000000007387"), 1200);
        });

        alertDialog.show();
    }

    public void showCustomPopupatSOC7(String title, String message) {
        // Inflate the custom layout
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.stopopup, null);

        // Create the dialog with the custom view
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(dialogView);

        AlertDialog alertDialog = builder.create();
        alertDialog.setCancelable(false); // User can't dismiss with back button or outside touch

        // Set dynamic content
        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        TextView dialogMessage = dialogView.findViewById(R.id.dialogMessage);
        dialogTitle.setText(title);
        dialogMessage.setText(message);

        // Handle button actions
        Button btnYes = dialogView.findViewById(R.id.btnOK);

        btnYes.setOnClickListener(v -> {
            StopInactivtyTimerDuringPopUp=true;
            alertDialog.dismiss();
        });

        alertDialog.show();
    }

    public void onDoubleTapAction() {
        // Your logic when user double-taps screen
        if(rpm==0){


            stopReverseBeep();
            stopBlinking();
//            EVLockService.getInstance().startLockSequence();
            serialPortManager = serialApplication.getSerialPortManager();
            if(serialPortManager!=null){
                serialPortManager.VCUSendToMCU("FFAA550101000A00");


                new Handler().postDelayed(() -> {
                    serialPortManager.VCUSendToMCU("FFAA550101000A00");

//                    Intent i = new Intent(context, CameraService.class);//off rec
//                    i.setAction("STOP_REC");
//                    context.startService(i);

                }, 3000);

                new Handler().postDelayed(() -> {
                    serialPortManager.VCUSendToMCU("FFAA550107000A00");
                }, 8000);

            }

            LoginActivity.markScreenJustOff();
            //Log.d("InactivityHandler", "LoginActivity launched over MainActivity.");
            try {
                PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                Method goToSleep = PowerManager.class.getMethod("goToSleep", long.class);
                goToSleep.invoke(powerManager, SystemClock.uptimeMillis());
//                InactivityManager.getInstance().startTimer(2);
            } catch (Exception e) {
                e.printStackTrace();
            }

            Context context = MainActivity.instance;
            if (context != null) {
                Intent intent = new Intent(context, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                context.startActivity(intent);
            }

        }
        //Log.d("UIUpdater", "Double-tap triggered inUIUPDSTOR");
    }
//    @Override
//    public void onBackPressed() {
//    }

    private final Handler reverseBeepHandler = new Handler(Looper.getMainLooper());
    private Runnable reverseBeepRunnable;

    private SoundPool reverseBeepPool;
    private int reverseBeepSoundId = -1;
    private boolean reverseBeepLoaded = false;
    private boolean isReverseBeeping = false;

    public void initReverseBeep(Context context) {
        if (reverseBeepPool != null) {
            reverseBeepPool.release();
            reverseBeepPool = null;
        }

        reverseBeepPool = new SoundPool.Builder().setMaxStreams(1).build();
        reverseBeepSoundId = reverseBeepPool.load(context, R.raw.backingsound, 1);

        reverseBeepPool.setOnLoadCompleteListener((sp, id, status) -> {
            reverseBeepLoaded = (status == 0);
            if (reverseBeepLoaded && isReverseBeeping) {
                // Start beeping immediately if start was requested before load finished
                reverseBeepHandler.post(reverseBeepRunnable);
            }
        });

        reverseBeepRunnable = () -> {
            if (reverseBeepPool != null && isReverseBeeping && reverseBeepLoaded) {
                reverseBeepPool.play(reverseBeepSoundId, 1f, 1f, 0, 0, 1f);
                reverseBeepHandler.postDelayed(reverseBeepRunnable, 1000);
            }
        };
    }

    public void startReverseBeep() {
        if (reverseBeepPool == null) {
            Log.w("ReverseBeep", "startReverseBeep() called before initReverseBeep()");
            return;
        }

        if (!isReverseBeeping) {
            isReverseBeeping = true;
            if (reverseBeepLoaded) {
                reverseBeepHandler.post(reverseBeepRunnable);
            } else {
                Log.d("ReverseBeep", "Sound not yet loaded, will start after load completes");
            }
        }
    }

    public void stopReverseBeep() {
        isReverseBeeping = false;
        reverseBeepHandler.removeCallbacks(reverseBeepRunnable);
    }

    public void releaseReverseBeep() {
        stopReverseBeep();
        if (reverseBeepPool != null) {
            reverseBeepPool.release();
            reverseBeepPool = null;
        }
        reverseBeepLoaded = false;
    }



    //==============TELEMETRY ==============================================

//    public void startTelemetry(String VINNUMBER) {
//        evClient = new EVWebSocketClient(VINNUMBER, URI.create("ws://192.168.1.122:8080/ws/ev"));
////        evClient = new EVWebSocketClient(VINNUMBER, URI.create("ws://103.174.148.224:8080/ws/ev"));//grafana server
//
//        evClient.connect();
//
//        apiService = RetrofitClient.getClient().create(ApiService.class);
//        isSending = true;
//        //Log.d("Telemetry", "✅ isSending set to true");
//
//        sendDataContinuously(VINNUMBER);
//
//        new Thread(() -> {
//            while (evClient == null || !evClient.isOpen()) {
//                try {
//                    Thread.sleep(500);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            if (serialPortManager != null) {
//                serialPortManager.sendContinuousSensorData();
//            } else {
//                Log.e("Telemetry", "serialPortManager is null!");
//            }
//        }).start();
//    }
//    public static boolean shdtelemetrywhenunlocked;
//    public int sendRealtimeDataOnceEvenAfterLocking=0;
//    private void sendDataContinuously(String vin) {
//        telemetryHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                if (isSending) {
//                    telelatlong=getLatLong(context);
//                    Log.d("dsfgf","ENTERSENDDATCONTINOUSLY "+telelatlong);
//                    // Send updated real-time vehicle data with dynamic fields
//                    sendVehicleData(
//                            context,                      // ← you must pass or define `context`
//                            teleODO,                      // Odometer value
//                            telechargingstate,            // Charging state
//                            teleHandleLockState,          // Handle lock
//                            teleSeatLockState,            // Seat lock
//                            teleBMSMosStates,             // BMS Mosfet states
//                            telecurrentrider,                    // Riders info
//                            telebmslifecycles,
//                            telelatlong
//                    );
//                    if(shdtelemetrywhenunlocked){
//                        sendRealTimeData(vin);
//                        sendRealtimeDataOnceEvenAfterLocking=0;
//                    }
//                    else{
//                        while(sendRealtimeDataOnceEvenAfterLocking==0){
//                            sendRealTimeData(vin);
//                            sendRealtimeDataOnceEvenAfterLocking++;
//                        }
//                    }
//                    telemetryHandler.postDelayed(this, 1000);
//                }
//            }
//        }, 1000);
//    }
//
//
//    public void sendVehicleData(Context context, float odo,  int chargingState,
//                                int handleLockState, int seatLockState, String bmsMosStates, String riders,int bmslifecycles,String lat_long) {
//
//        MetadataManager metadata = MetadataManager.get();
//
//        String vinNumber    = metadata.getVin();
//        String ownerName    = metadata.getOwner();
//        String phoneNumber  = metadata.getPhone();
//        String controllerID = metadata.getControllerId();
//        String motorID      = metadata.getMotorId();
//        String bmsID        = metadata.getBmsId();
//        String chargerID    = metadata.getCharger();
//        String smartKeyID   = metadata.getSmartKeyId();
//        String model        = metadata.getModel(); // ✅ added model
//
//        String rideOSversion = "rideOS N/A";
//        try {
//            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
//            rideOSversion = "rideOS " + pInfo.versionName;
//        } catch (PackageManager.NameNotFoundException e) {
//            e.printStackTrace();
//        }
//
//        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
//
//        if (vinNumber.contains("UNKNOWN")) {
//            //Log.d("VehicleData", "⛔ VIN is UNKNOWN — skipping send.");
//            return;
//        }
//
//        SharedPreferences prefs = context.getSharedPreferences("last_uploaded_metadata", Context.MODE_PRIVATE);
//
//        // ✅ Check if any value changed since last send (including model)
//        boolean anyChanged =
//                !ownerName.equals(prefs.getString("owner", "")) ||
//                        !phoneNumber.equals(prefs.getString("phone", "")) ||
//                        !controllerID.equals(prefs.getString("controller", "")) ||
//                        !motorID.equals(prefs.getString("motor", "")) ||
//                        !bmsID.equals(prefs.getString("bms", "")) ||
//                        !chargerID.equals(prefs.getString("charger", "")) ||
//                        !smartKeyID.equals(prefs.getString("smartKey", "")) ||
//                        !model.equals(prefs.getString("model", "")); // ✅ include model in comparison
//
//        VehicleData data;
//
//        if (anyChanged) {
//           // Log.d("VehicleData", "🆕 Identity changed — sending full payload");
//
//            data = new VehicleData(
//                    vinNumber, model, ownerName, phoneNumber, controllerID,
//                    motorID, bmsID, chargerID, rideOSversion, smartKeyID,
//                    odo, chargingState, handleLockState, seatLockState,
//                    bmsMosStates, riders,bmslifecycles,lat_long, time
//            );
//
//            // ✅ Save all identifiers including model
//            prefs.edit()
//                    .putString("owner", ownerName)
//                    .putString("phone", phoneNumber)
//                    .putString("controller", controllerID)
//                    .putString("motor", motorID)
//                    .putString("bms", bmsID)
//                    .putString("charger", chargerID)
//                    .putString("smartKey", smartKeyID)
//                    .putString("model", model)
//                    .apply();
//        } else {
//            //Log.d("VehicleData", "✅ No identity change — sending minimal payload");
//
//            data = new VehicleData(
//                    vinNumber, null, null, null, null,
//                    null, null, null, rideOSversion, null,
//                    odo,  chargingState, handleLockState, seatLockState,
//                    bmsMosStates, riders,bmslifecycles,lat_long,time
//            );
//        }
//
//        // ✅ Log the payload for debugging
//        try {
//            Gson gson = new Gson();
//           // Log.d("VehicleData", "📦 Payload: " + gson.toJson(data));
//        } catch (Exception e) {
//            //Log.e("VehicleData", "❌ Failed to log payload", e);
//        }
//
//        // ✅ Send to server via Retrofit
//        Call<ResponseBody> call = apiService.saveVehicleData(data);
//        call.enqueue(new Callback<ResponseBody>() {
//            @Override
//            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
//                if (response.isSuccessful()) {
//                    try {
//                        String body = response.body() != null ? response.body().string() : "No response";
//                       // Log.d("VehicleData", "✅ Server: " + body);
//                    } catch (Exception e) {
//                        //Log.e("VehicleData", "❌ Error reading response", e);
//                    }
//                } else {
//                   // Log.e("VehicleData", "❌ Server error: " + response.code());
//                }
//            }
//
//            @Override
//            public void onFailure(Call<ResponseBody> call, Throwable t) {
//                //Log.e("VehicleData", "🚨 Network error: " + t.getMessage());
//            }
//        });
//    }
//
//    private void sendRealTimeData(String vin) {
//        RealTimeData data = new RealTimeData();
//
//        data.vinNumber = vin;
//        data.speed_kmph = teleSpeed;
//        data.controllerMosTemp = teleControllerMosTemp;
//        data.motorTemp = teleMotorTemp;
//        data.bmsMosTemp = teleBMSMosTemp;
//        data.ntc = teleNTC;
//        data.currentconsumption = roundTo3Decimal(teleCurrentConsumption);
//        data.batvoltage = roundTo1Decimal(teleBatVoltage);
//        data.remainingCapacity_AH = roundTo3Decimal(teleRemainingCapacityAH);
//        data.inAH = roundTo3Decimal(teletotalAH);
//        data.inAH_by_charger = roundTo3Decimal(telechargerAH);
//        data.inAH_by_regen = roundTo3Decimal(teleregenAH);
//        data.outAH = roundTo3Decimal(teledischargeAH);
//        data.ev_power_state = shdtelemetrywhenunlocked == true ? "ON" : "OFF";  // example mapping
//        data.currentRider = shdtelemetrywhenunlocked == true ? telecurrentrider : "Scooter Locked";; //!= null ? telecurrentrider : teleRider;
//        data.tirepressure = teletirepressure;
//        data.soc = teleSOC;
//        data.apuSoc = teleAPUSOC;
//        data.lat_long = telelatlong;
//        data.time = java.time.OffsetDateTime.now().toString();
//
//        apiService.saveRealTimeData(data).enqueue(new Callback<ResponseBody>() {
//            @Override
//            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
//                if (response.isSuccessful()) {
//                   // Log.d("RealTimeData", "✅ Realtime data sent successfully");
//                } else {
//                    //Log.e("RealTimeData", "❌ Failed to send realtime data: " + response.code());
//                }
//            }
//            @Override
//            public void onFailure(Call<ResponseBody> call, Throwable t) {
//                //Log.e("RealTimeData", "🚨 Network error sending realtime data: " + t.getMessage());
//            }
//        });
//    }
//    private double roundTo3Decimal(double value) {
//        return Math.round(value * 1000.0) / 1000.0;
//    }
//
//    private double roundTo1Decimal(double value) {
//        return Math.round(value * 10.0) / 10.0;
//    }
//    @SuppressLint("MissingPermission")
//    private String getLatLong(Context context) {
//        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
//        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//
//        if (location != null) {
//            double latitude = location.getLatitude();
//            double longitude = location.getLongitude();
//            return latitude + ", " + longitude; // Return as "lat, long"
//        } else {
//            return "Location not available";
//        }
//    }
//
//    private boolean isUnknown(String value) {
//        if (value == null) return true;
//
//        String normalized = value.trim().toUpperCase();
//
//        return normalized.startsWith("UNKNOWN");
//    }


//    private void sendVehicleDataIfValid() {
//        SharedPreferences sharedPrefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
//
//        String vin = sharedPrefs.getString("vin", "UNKNOWN_VIN");
//        if (vin.contains("UNKNOWN")) return;  // Skip if VIN not initialized
//
//        String model = sharedPrefs.getString("model", "UNKNOWN_MODEL");
//        String ownerName = sharedPrefs.getString("ownerName", "UNKNOWN_OWNER");
//        String phoneNumber = sharedPrefs.getString("phoneNumber", "UNKNOWN_PHONE");
//        String controllerID = sharedPrefs.getString("controllerID", "UNKNOWN_CONTROLLERID");
//        String motorID = sharedPrefs.getString("motorID", "UNKNOWN_MOTORID");
//        String bmsID = sharedPrefs.getString("bmsID", "UNKNOWN_BMSID");
//        String chargerID = sharedPrefs.getString("chargerID", "UNKNOWN_CHARGERID");
//        String smartKeyID = sharedPrefs.getString("smartKeyID", "UNKNOWN_SMARTKEYID");
//
//        String rideOSversion = "rideOS3.1"; // or fetch from system
//        float odo = teleODO;
//        float tripkm = teletrip;
//        int chargingState = telechargingstate;
//        int handleLockState = teleHandleLockState;
//        int seatLockState = teleSeatLockState;
//        String bmsMosStates = teleBMSMosStates;
//        String riders = teleRider;
//
//        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
//
//        VehicleData vehicleData = new VehicleData(
//                vin,
//                model,
//                ownerName,
//                phoneNumber,
//                controllerID,
//                motorID,
//                bmsID,
//                chargerID,
//                rideOSversion,
//                smartKeyID,
//                odo,
//                tripkm,
//                chargingState,
//                handleLockState,
//                seatLockState,
//                bmsMosStates,
//                riders,
//                currentTime
//        );
//
//        // Skip sending identity fields if all are initialized
//        if (!model.contains("UNKNOWN") &&
//                !ownerName.contains("UNKNOWN") &&
//                !phoneNumber.contains("UNKNOWN") &&
//                !controllerID.contains("UNKNOWN") &&
//                !motorID.contains("UNKNOWN") &&
//                !bmsID.contains("UNKNOWN") &&
//                !chargerID.contains("UNKNOWN") &&
//                !smartKeyID.contains("UNKNOWN")) {
//
//            // If all identity fields are initialized, clear them before sending
//            vehicleData = new VehicleData(
//                    vin,
//                    null, null, null, null, null, null, null,  // clear identity fields
//                    rideOSversion,
//                    null,
//                    odo, tripkm, chargingState, handleLockState, seatLockState,
//                    bmsMosStates, riders, currentTime
//            );
//        }
//
//        Call<ResponseBody> call = apiService.saveVehicleData(data);
//        call.enqueue(new Callback<ResponseBody>() {
//            @Override
//            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
//                if (response.isSuccessful()) {
//                    try {
//                        String body = response.body() != null ? response.body().string() : "No response";
//                        Log.d("VehicleData", "✅ Data sent: " + body);
//                    } catch (Exception e) {
//                        Log.e("VehicleData", "Error reading body", e);
//                    }
//                } else {
//                    Log.e("VehicleData", "❌ Failed: " + response.code());
//                }
//            }
//
//            @Override
//            public void onFailure(Call<ResponseBody> call, Throwable t) {
//                Log.e("VehicleData", "🚨 Error: " + t.getMessage());
//            }
//        });



    //=========================================================================================

    public void release() {
        Log.d("dfsg","REALSE CALLED");
        handlerTurnSignal.removeCallbacksAndMessages(null);
        beepHandler.removeCallbacksAndMessages(null);
        warningHandler.removeCallbacksAndMessages(null);
        blinkHandler.removeCallbacksAndMessages(null);
        stopDelayHandler.removeCallbacksAndMessages(null);
        soundHandler.removeCallbacksAndMessages(null);
        reverseBeepHandler.removeCallbacksAndMessages(null);

        stopBeeping();
        stopReverseBeep();

        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }

        if (reverseBeepPool != null) {
            reverseBeepPool.release();
            reverseBeepPool = null;
        }

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        if (gearshiftmediaPlayer != null) {
            gearshiftmediaPlayer.release();
            gearshiftmediaPlayer = null;
        }

        if (modeChangePlayer != null) {
            modeChangePlayer.release();
            modeChangePlayer = null;
        }
        serialPortManager = null;

    }
    private boolean frontWarning = false;
    private boolean rearWarning = false;
    private static final double MIN_PRESSURE_PSI = 20.0;
    private static final int MAX_TEMP_C = 50;
    private MetadataManager metadata = MetadataManager.get();
    private Integer frontMinor = null;
    private Integer rearMinor = null;

    private void registerTpmsReceiver(Context ctx) {
        tpmsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (TpmsScanService.ACTION_TPMS_UPDATE.equals(intent.getAction())) {
                    String mac = intent.getStringExtra(TpmsScanService.EXTRA_MAC);
                    int minor = intent.getIntExtra(TpmsScanService.EXTRA_MINOR, -1);

                    if (minor != -1 && mac != null) {
                        // Extract raw bytes
                        int pressureRaw = (minor >> 8) & 0xFF;
                        int tempRaw = minor & 0xFF;

                        // Convert raw -> kPa -> psi (same conversion used in TPMS_activity)
                        double pressureKpa = pressureRaw * 2.5;
                        double pressurePsiDouble = pressureKpa * 0.145038;
                        int pressurePsi = (int) Math.round(pressurePsiDouble); // integer PSI for display/encoding
                        int temperature = tempRaw - 40;

                        // Safe debug log (shows both decimal and rounded value)
//                        Log.d("TPMS-Decode", String.format(
//                                "Minor=%d | PressureRaw=%d | TempRaw=%d | Pressure=%.1f psi (rounded=%d) | Temp=%d°C",
//                                minor, pressureRaw, tempRaw, pressurePsiDouble, pressurePsi, temperature));

                        // Update correct tire and warn flags
                        if (metadata.getFrontTPMS() != null && metadata.getFrontTPMS().equals(mac)) {
                            frontMinor = minor;
                            frontWarning = (pressurePsi < MIN_PRESSURE_PSI || temperature > MAX_TEMP_C);
                            //Log.d("UIUpdater", String.format("FRONT -> %d psi, %d °C", pressurePsi, temperature));

                        } else if (metadata.getRearTPMS() != null && metadata.getRearTPMS().equals(mac)) {
                            rearMinor = minor;
                            rearWarning = (pressurePsi < MIN_PRESSURE_PSI || temperature > MAX_TEMP_C);
                            //Log.d("UIUpdater", String.format("REAR -> %d psi, %d °C", pressurePsi, temperature));
                        }

                        // Build encoded values from the stored minors (use converted PSI, not raw)
                        int fPress = 0, fTemp = 0, rPress = 0, rTemp = 0;

                        if (frontMinor != null) {
                            int raw = (frontMinor >> 8) & 0xFF;
                            double pKpa = raw * 2.5;
                            int psi = (int) Math.round(pKpa * 0.145038);
                            fPress = psi;
                            fTemp = (frontMinor & 0xFF) - 40;
                        }

                        if (rearMinor != null) {
                            int raw = (rearMinor >> 8) & 0xFF;
                            double pKpa = raw * 2.5;
                            int psi = (int) Math.round(pKpa * 0.145038);
                            rPress = psi;
                            rTemp = (rearMinor & 0xFF) - 40;
                        }

                        // Concatenate as 02-digit fields (frontP, frontT, rearP, rearT)
                        @SuppressLint("DefaultLocale")
                        String teleTripString = String.format("%02d%02d%02d%02d",
                                fPress, fTemp, rPress, rTemp);

                        try {
                            float teleTripValue = Float.parseFloat(teleTripString);
                            TelemetryDataManager.getInstance().setTeleTirePressure(teleTripValue);
                            //Log.d("UIUpdater", "TeleTirePressure updated = " + teleTripString);
                        } catch (NumberFormatException e) {
                            Log.e("UIUpdater", "Error parsing TeleTirePressure", e);
                        }

                        // Show/hide warning icon
                        if (frontWarning || rearWarning) {
                            binding.tpmsicon.setVisibility(View.VISIBLE);
                        } else {
                            binding.tpmsicon.setVisibility(View.INVISIBLE);
                        }
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter(TpmsScanService.ACTION_TPMS_UPDATE);
        LocalBroadcastManager.getInstance(ctx).registerReceiver(tpmsReceiver, filter);
        Log.d("TPMS-UI", "registerTpmsReceiver: TPMS receiver registered ");
    }

    public void cleanup(Context ctx) {
        if (tpmsReceiver != null) {
            LocalBroadcastManager.getInstance(ctx).unregisterReceiver(tpmsReceiver);
            tpmsReceiver = null;
        }
    }

}