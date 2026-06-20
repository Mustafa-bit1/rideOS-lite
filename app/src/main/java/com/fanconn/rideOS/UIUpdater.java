
package com.fanconn.rideOS;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.L;
import com.fanconn.rideOS.authen.LoginActivity;
import com.fanconn.rideOS.databinding.ActivityMainBinding;

import java.lang.reflect.Method;
import java.util.Locale;

//=========================

import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
//=========================

/**
 * UI 更新类，负责更新主活动中的 UI 组件
 */
public class UIUpdater  {

    private final ActivityMainBinding binding;
    private View recoDot1, recoDot2, recoDot3;

    private final Handler handlerTurnSignal;
    private final Context context;
    MainActivity mainActivity;
    //    InactivityManager inactivityManager;
    SerialPortManager serialPortManager;
    batteryPackSwitcher BatPackSwitcher;
    private final MainActivity activity; // Actual activity for dialogs, UI
//    private CameraHandler cameraHandler;
    TelemetryDataManager tm;

    public static UIUpdater instance;
    private BroadcastReceiver tpmsReceiver;
    AhTrackerManager ahTrackerManager;
    private boolean touchBlocked = false;

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
        handler = new Handler(Looper.getMainLooper());

        reverseArrowAnimation = AnimationUtils.loadAnimation(ctx, R.anim.reverse_arrow_blink_down);

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
        long now = System.currentTimeMillis();
        lastCanUpdateTime = now;

        startCanTimeoutChecker();
        registerTpmsReceiver(ctx);
        ahTrackerManager = AhTrackerManager.getInstance(context);
        updateCanData18904001(serialPortManager.getHb3());

// Show RECO UI
        binding.recoContainer.setVisibility(View.VISIBLE);
        binding.recoNavLeft.setVisibility(View.VISIBLE);
        binding.recoNavRight.setVisibility(View.VISIBLE);

// Initial render
        updateRecoKm();
        updateTransportModeWarning();
    }

    public static UIUpdater getInstance() {
        return instance;
    }
    private int recoMode = 0;
    private static final float RECO_KM_PER_AH = 2.5f;
    public void toggleRecoMode() {
        recoMode++;
        if (recoMode > 2) recoMode = 0;
        updateRecoKm();
    }
    public void updateRecoKm() {

        float regenAh = 0f;
        String label = "";

        if (recoMode == 0) {
            // Trip
            regenAh = getTripAhByRegen();
            label = "Trip reco";

        } else if (recoMode == 1) {
            // Total
            regenAh = getLastTripAh();
            label = "Last trip reco";

        } else if (recoMode == 2) {
            // Last Trip
            regenAh = getTotalAhByRegen();
            label = "Total reco";
        }

        float km = regenAh * RECO_KM_PER_AH;

        // Lambda-safe finals
        final float finalKm = km;
        final String finalLabel = label;

        runOnUiThreadSafe(() -> {
            setRecoKmText(finalKm);
            binding.recoLabelText.setText(finalLabel);
            updateRecoDots();
        });
    }
    public void resetAllRecoEngineParameters() {

        // RAM values
        tripAhByRegen = 0f;
        ahByRegen = 0f;
        ahByCharger = 0f;
        previousAh = 0f;

        SharedPreferences prefs =
                context.getSharedPreferences("ah_data", Context.MODE_PRIVATE);

        prefs.edit()
                .putFloat("totalAh", 0f)
                .putFloat("chargerAh", 0f)
                .putFloat("regenAh", 0f)
                .putFloat("lastTripAh", 0f)
                .apply();

        tm.setTeleTotalAH(0f);
        tm.setTeleChargerAH(0f);
        tm.setTeleRegenAH(0f);

        Log.d("RECO_RESET", "All reco engine params reset");

        updateRecoKm();
    }
    public void resetChargerAndTotalRegenAh() {

        // Reset only totals
        ahByCharger = 0f;
        ahByRegen = 0f;

        SharedPreferences prefs =
                context.getSharedPreferences("ah_data", Context.MODE_PRIVATE);

        prefs.edit()
                .putFloat("chargerAh", 0f)
                .putFloat("regenAh", 0f)
                .apply();

        // Update telemetry
        tm.setTeleChargerAH(0f);
        tm.setTeleRegenAH(0f);

        //Log.d("AH_RESET", "✅ Charger Ah + Total Regen Ah reset");

        updateRecoKm();
    }
    private static final String PREF_RECO = "reco_data";
    private static final String KEY_LAST_TRIP_AH = "last_trip_ah";
    public void onTripReset() {
        float currentTripAh = getTripAhByRegen();

        SharedPreferences prefs = context.getSharedPreferences(PREF_RECO, Context.MODE_PRIVATE);
        prefs.edit().putFloat(KEY_LAST_TRIP_AH, currentTripAh).apply();

        resetTripAhByRegen();
    }
    private void setRecoKmText(float km) {

        String valueText =
                String.format("+%.1f km", km);
        SpannableString spannable = new SpannableString(valueText);

        int kmStart = valueText.indexOf("km");
        if (kmStart != -1) {
            spannable.setSpan(
                    new RelativeSizeSpan(0.6f),
                    kmStart,
                    valueText.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
        binding.recoKmText.setText(spannable);
    }

    private void updateRecoDots() {

        // Reset all dots
        binding.recoDot1.setBackgroundResource(R.drawable.dot_unselected);
        binding.recoDot2.setBackgroundResource(R.drawable.dot_unselected);
        binding.recoDot3.setBackgroundResource(R.drawable.dot_unselected);

        // Activate current
        switch (recoMode) {
            case 0:
                binding.recoDot1.setBackgroundResource(R.drawable.dot_selected);
                break;

            case 1:
                binding.recoDot2.setBackgroundResource(R.drawable.dot_selected);
                break;

            case 2:
                binding.recoDot3.setBackgroundResource(R.drawable.dot_selected);
                break;
        }
    }

    private float getLastTripAh() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_RECO, Context.MODE_PRIVATE);
        return prefs.getFloat(KEY_LAST_TRIP_AH, 0f);
    }
    private void runOnUiThreadSafe(Runnable action) {
        if (activity == null || activity.isFinishing()) return;

        if (Looper.myLooper() == Looper.getMainLooper()) {
            action.run();
        } else {
            activity.runOnUiThread(action);
        }
    }

    public float getTripRecoKm() {
        return getTripAhByRegen() * RECO_KM_PER_AH;
    }

    public float getLastTripRecoKm() {
        return getLastTripAh() * RECO_KM_PER_AH;
    }
    public float getTotalRecoKm() {
        return getTotalAhByRegen() * RECO_KM_PER_AH;
    }
    public String getrecoEngineParams() {
        return getLastTripRecoKm()+","+getTripRecoKm()+","+getTotalRecoKm();
    }
    private int lastTurnSignalState = -1;
    private boolean reset_Auto_lock_inHighbeam_flag;
    public void updateHeartbeat1UI(Heartbeat1 hb1) {

        int currentSignal = hb1.getTurnSignal();
        if (currentSignal != lastTurnSignalState) {
            indicators(currentSignal);  // only call if changed
            lastTurnSignalState = currentSignal; // update tracker
        }
        if(hb1.getHeadlights() == 1) {
            binding.highBeamIVon.setVisibility(View.VISIBLE);
            binding.highBeamIVoff.setVisibility(View.INVISIBLE);
            if(reset_Auto_lock_inHighbeam_flag){
                reset_Auto_lock_inHighbeam_flag=false;
                InactivityManager.getInstance().onUserInteraction();
            }
        }
        else if (hb1.getHeadlights() == 0)
        {
            reset_Auto_lock_inHighbeam_flag=true;
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
    public void updateHeartbeat2UI(Heartbeat2 hb2) {
        // 更新六轴数据
        updateSixAxisData(hb2);
    }

    private void updateSixAxisData(Heartbeat2 hb2) {
        double xuAcc = hb2.getXuAcc() / 1000000.0;
        double yuAcc = hb2.getYuAcc() / 1000000.0;
        double zuAcc = hb2.getZuAcc() / 1000000.0;
        double xGyro = hb2.getXGyro() / 1000000.0;
        double yGyro = hb2.getYGyro() / 1000000.0;
        double zGyro = hb2.getZGyro() / 1000000.0;

        String sixAxisData = String.format("uAcc  ：%.6f  %.6f  %.6f\n\n\n\nuGyro：%.6f  %.6f  %.6f", xuAcc, yuAcc, zuAcc, xGyro, yGyro, zGyro);
        //Log.d("asdfg","THISISAXISDATA \n\n"+sixAxisData);
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
            case "18914001":
                updateCanData18914001(hb3);
                break;
            case "18904002":
                updateCanData18904002(hb3);
                break;
            case "18934001":
                updateCanData18934001(hb3);
                break;
            case "18944001":
                updateCanData18944001(hb3);
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
            case "18954001":
                updateCanData18954001(hb3);
                break;
            case "18FF50E5":
                updateCanData18FF50E5(hb3);
                break;
            case "1806E5F4":
                updateCanData1806E5F4(hb3);
                break;
            case "160B8001":
                updateCanData160B8001(hb3);
                break;
            case "160D8001":
                updateCanData160D8001(hb3);
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
    public static boolean apuflag=false;
    private  boolean StopInactivtyTimerDuringPopUp=true;
    public static boolean isAPUPresent=false;
    private static boolean Dynamic80SOCRegencurrent=true;

    private static int UniversalAPUbatteryLevel;
//    public static boolean resetParamtersAfterLowbat=false;
    private double RANGE_km_PER_SOC =1.7;//This value in refrence to km per AH is RANGE_km_PER_SOC=km_per_AH/1.6666666
    private float lastKnownBattery = 0;
    private boolean isBatteryIntroRunning = false;
    private boolean canDataInitialized = false;
    public void updateCanData18904001(Heartbeat3 hb3) {
        // 更新电池电量
        float batteryLevel= hb3.getBatteryLevel();
       /// Log.d("asf","SOCOFTODAY IS "+batteryLevel);
//        int batteryLevel = get10LessSOC(batteryLevel1);//BMS SOC BUT 10 LESS

//        int batteryLevel= hb3.getBatteryLevel() / 10; //BMS SOC AND NO 10 LESS
//        int batteryLevel = (int) ahTrackerManager.getSOC();//AH BASED SOC , TO MAKE AH BASED OR 10 LESS GO TO ahTrackerManager class

//        Log.d("SOC10LESS", "Actual SOC = " + batteryLevel1);
//        Log.d("SOC10LESS", "Display SOC = " + batteryLevel);
       // Log.d("adsfg","THISISAHSOC="+batteryLevel);

//        tm.setTeleRemainingCapacityAH(ahTrackerManager.getRemainingAh());
        if(hb3.getreamainingAH()/1000<500){// ignore noise values
            tm.setTeleRemainingCapacityAH(hb3.getreamainingAH()/1000);
            tm.setBMSTeleRemainingCapacityAH(hb3.getreamainingAH()/1000);// Actual remaining AH directly taking from BMS
        }

//        int batteryLevel = Math.max(0, Math.min(100, (int) ahTrackerManager.getSOC()));

        String sBatteryLevel = String.valueOf(batteryLevel);

//        if(((hb3.getrealCurrent()-30000)/10)<500 && ((hb3.getrealCurrent()-30000)/10)>-500){
//            tm.setTeleBMSCurrentValue((hb3.getrealCurrent()-30000)/10);
//            //Log.d("sadfsgg","CURRWNTCSMALL VALUES IS "+(hb3.getrealCurrent()-30000)/10);
//        }

        float current = (hb3.getrealCurrent() - 30000) / 10;

        if (current > -500 && current < 500) {
            //tm.setTeleCurrentConsumption(current);
           // Log.d("CURR", "Valid current: " + current);
        } else {
           // Log.d("CURR", "Invalid current rejected: " + current);
        }

        tm.setTeleBatVoltage(hb3.getbatvoltage()/10);
        float range= (float) (batteryLevel * RANGE_km_PER_SOC);
        if(batteryLevel>=0 && batteryLevel<=100){
            tm.setTeleSOC(batteryLevel);
            tm.setBMSTeleSOC(hb3.getNoLess10BMSBatteryLevel() /10);
            tm.setTeleRange(range);
            binding.batteryTV.setText(
                    String.format(
                            Locale.US,
                            "Battery %.1f%%   |  ~ %.1f km",
                            batteryLevel,
                            range
                    )
            );        }

        if (!Lowbatflag && (batteryLevel == 20 || batteryLevel ==19 ||  batteryLevel==18 ||  batteryLevel==17)  && tm.getTeleChargingState()==0) {

            if(serialPortManager!=null){
              //  Log.d("SOCWHATACTOION","ENTER 20");
//                serialPortManager.VCUSendToMCU("FFAA550103001510FBA8073C00F000B0044001");//line current 15A, max phase current 300A ,regen 60(F000)-80(4001)
                serialPortManager.VCUSendToMCU("FFAA550103001510FBA8072800F000B0044001");//line current 10A, max phase current 300A ,regen 60(F000)-80(4001)


                new Handler(Looper.getMainLooper()).postDelayed(() -> serialPortManager.VCUSendToMCU("FFAA550103001510FBA8072800F000B0044001"), 800);
//                new Handler(Looper.getMainLooper()).postDelayed(() -> serialPortManager.VCUSendToMCU("FFAA550103001510F8A8070000000000007387"), 1200);
            }
            // Start alert
            Animation tempalert = AnimationUtils.loadAnimation(context, R.anim.alpha);

            binding.yellowalert.setVisibility(View.VISIBLE);
            binding.LowBatmsg.setVisibility(View.VISIBLE);

            binding.yellowalert.startAnimation(tempalert);
            binding.LowBatmsg.startAnimation(tempalert);

            startBeeping(2);

            Lowbatflag = true;
            binding.transportModeYellowWarning.setVisibility(View.GONE);
            // Stop alert visuals after 10 seconds, but DO NOT reset flag yet
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                stopBeeping();
            }, 15_000);
        }

        if (batteryLevel >= 21 && batteryLevel<95 && Lowbatflag) {
//            resetParamtersAfterLowbat=true;
            Lowbatflag = false;
            updateTransportModeWarning();
            if(serialPortManager!=null){
                serialPortManager.VCUSendToMCU("FFAA550103001510FBA807B801F000B0044001"); //max all phase 300, line 100 , regen 60(F000)-80(4001)
                new Handler(Looper.getMainLooper()).postDelayed(() -> serialPortManager.VCUSendToMCU("FFAA550103001510F8A807000001020E007387"), 1200);
            }
            binding.yellowalert.clearAnimation();
            binding.LowBatmsg.clearAnimation();

            binding.yellowalert.setVisibility(View.INVISIBLE);
            binding.LowBatmsg.setVisibility(View.INVISIBLE);
        }
        if (batteryLevel >= 6 && atSOC10Lowbatflag) {
//            resetParamtersAfterLowbat=true;
            atSOC10Lowbatflag = false;
        }
        if (batteryLevel >= 2 && atSOC7Lowbatflag) {
//            resetParamtersAfterLowbat=true;
            atSOC7Lowbatflag = false;
        }

//        if (batteryLevel >= 22) {
//            apuflag=false;
//        }

//        if(!atSOC10Lowbatflag && batteryLevel==10 && tm.getTeleChargingState()==0){
//            atSOC10Lowbatflag=true;
//            if(serialPortManager!=null){
//                Log.d("asdfdg","ENTER 10");
//                serialPortManager.VCUSendToMCU("FFAA550103001510FBA80728007800B004A000");//line current 10 A
//                new Handler(Looper.getMainLooper()).postDelayed(() -> serialPortManager.VCUSendToMCU("FFAA550103001510F8A8070000000000007387"), 1200);
//            }
//        }

        if(!atSOC10Lowbatflag && (batteryLevel==5 || batteryLevel==4 || batteryLevel==3) && tm.getTeleChargingState()==0){
            atSOC10Lowbatflag=true;
            startBeeping(2);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                stopBeeping();
            }, 20_000);

            if(isAPUPresent && !apuflag){
                StopInactivtyTimerDuringPopUp=false;
              //  Log.d("SOCWHATACTOION","ENTER 10 an ask for APU");
                InactivityManager.getInstance().stopTimer();
                showCustomPopup("Alert","Your Battery is Low, Switch to APU for Extra Range");
            } else if  (!isAPUPresent) {
               // Log.d("SOCWHATACTOION","ENTER 10 AND DO 10AMPS");
                StopInactivtyTimerDuringPopUp=false;
                showCustomPopupatSOC7("Alert","Battery too low, deep discharge may damage your scooter battery, Please charge your scooter");//regen 60(F000)-80(4001)
                serialPortManager.VCUSendToMCU("FFAA550103001510FBA8072800F000B0044001");//line current 10A(2800), phase current max, regen currents as 40A(A000) to make 6 (1800) and 50A(C800) to make 8 (2000)
                new Handler(Looper.getMainLooper()).postDelayed(() -> serialPortManager.VCUSendToMCU("FFAA550103001510F8A807000001020E007387"), 1200);
            } else if (apuflag  && UniversalAPUbatteryLevel<=15) {
                StopInactivtyTimerDuringPopUp=false;
         //       Log.d("SOCWHATACTOION","ENTER 10 AND DO 0000AMPS");

                showCustomPopupatSOC7("Alert","Battery too low, deep discharge may damage your scooter battery, Please charge your scooter");
                serialPortManager.VCUSendToMCU("FFAA550103001510FBA8072800F00000004001");//STOP SCOOTER, phase zero
                new Handler(Looper.getMainLooper()).postDelayed(() -> serialPortManager.VCUSendToMCU("FFAA550103001510F8A807000001020E007387"), 1200);
            }
        }

//        if((batteryLevel>=65 && batteryLevel<=95) && Dynamic80SOCRegencurrent) {
//            Dynamic80SOCRegencurrent=false;
//            serialPortManager.VCUSendToMCU("FFAA550103001510FBA807C800F000B0044001");//regen 60(F000)-80(4001),,35 - 45 regen(8C00-B400)40A(A000) 50A(C800) , phase cur 300A(B004) , line current 110A(B801)or 50amps C800 ,
//            new Handler(Looper.getMainLooper()).postDelayed(() -> serialPortManager.VCUSendToMCU("FFAA550103001510FBA807C800F000B0044001"), 500);
//        }
//        if(batteryLevel>=96) {
//            Dynamic80SOCRegencurrent=true;
//        }

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
        if (batteryLevel > 0) {
            canDataInitialized = true;
        }
        if(canDataInitialized && !atSOC7Lowbatflag && (batteryLevel==0) && !apuflag  && tm.getTeleChargingState()==0){
            atSOC7Lowbatflag=true;
            if(serialPortManager!=null){
                serialPortManager.VCUSendToMCU("FFAA550103001510FBA8072800F00000004001");//phase 0 , line 10AMps=2800 ,,regen 60(F000)-80(4001),, regen 40A - 50A(A000-C800)
                new Handler(Looper.getMainLooper()).postDelayed(() ->  serialPortManager.VCUSendToMCU("FFAA550103001510FBA8072800F00000004001"), 1000);//phase 0 , line 10AMps=2800 ,,regen 60(F000)-80(4001),, regen 40A - 50A(A000-C800)
                new Handler(Looper.getMainLooper()).postDelayed(() -> serialPortManager.VCUSendToMCU("FFAA550103001510F8A8070000000000007387"), 1500);
            }
            InactivityManager.getInstance().stopTimer();

            //------------
            StopInactivtyTimerDuringPopUp=false;
          //  Log.d("SOCWHATACTOION","ENTER 5 AND DO 0000AMPS");

            showCustomPopupatSOC7("Alert","Battery too low, deep discharge may damage your scooter battery, Please charge your scooter");
        }
        //BatPackSwitcher.startSwitchingToAPU();
//
//        try {
//            if (batteryLevel>=0 && batteryLevel<=100  && MainActivity.MainisActive) { // Only update if animation is done
//                updateBatteryAnimation(batteryLevel);
//            }
//        } catch (NumberFormatException e) {
//            e.printStackTrace();
//        }
        lastKnownBattery = batteryLevel;

        if (!isBatteryIntroRunning) {
            activity.runOnUiThread(() -> {
                if (!isBatteryIntroRunning) {
                    binding.batteryArc.setBatteryLevel(batteryLevel);
                }
            });
        }
//        try {
//            if (batteryLevel>=0 && batteryLevel<=100 && MainActivity.batteryAnimationCompleted && MainActivity.MainisActive) { // ✅ Only update if animation is done
//                updateBatteryAnimation(batteryLevel);
//            }
//        } catch (NumberFormatException e) {
//            e.printStackTrace();
//        }
        //根据电池电量获取对应的图片资源
        //int batteryImageResId = getBatteryImageResource(batteryLevel);
        //binding.batteryIV.setImageResource(batteryImageResId);
    }
    public float getLastKnownBattery() {
        return lastKnownBattery;
    }

    public void setBatteryIntroRunning(boolean value) {
        isBatteryIntroRunning = value;
    }

    public boolean isBatteryIntroRunning() {
        return isBatteryIntroRunning;
    }
    private int get10LessSOC(int actualSOC) {

        int minActual = 10;
        int maxActual = 100;

        if (actualSOC <= minActual) {
            return 0;
        }

        if (actualSOC >= maxActual) {
            return 100;
        }

        return (actualSOC - minActual) * 100 / (maxActual - minActual);
    }
    public void updateCanData18914001(Heartbeat3 hb3) {
        tm.setteleIndividualCellVoltages(hb3.getIndiCellVol());
    }
    private chargingUI chargingUI;
    public void setChargingUI(chargingUI chargingUI) {
        this.chargingUI = chargingUI;
    }
    private boolean isApuConnected = false;
    private long lastApuFrameTime = 0;
    private static final long APU_TIMEOUT_MS = 20000; // 20 seconds of silence = disconnected
    // Use a single Handler for UI thread safe timing
    private final Handler apuHandler = new Handler(Looper.getMainLooper());
    private boolean apuWatcherRunning = false;


    public void updateCanData18904002(Heartbeat3 hb3) {// when CAN data of apu is detected on CAN line

        lastApuFrameTime = System.currentTimeMillis();

        // Detect APU connection transition
        if (!isApuConnected && !apuflag) {
            isApuConnected = true;
           // Log.d("APU", "APU Connected");
            InactivityManager.getInstance().stopTimer();
            showCustomPopupAPUdetected("Assistance", "APU detected. Do you want to switch to APU?");
        }
        tm.setTeleAPUBatVoltage(hb3.getAPUbatvoltage()/10);
        // Ensure the disconnect watcher is running only once
        if (!apuWatcherRunning) {
            startApuDisconnectWatcher();
        }
        isAPUPresent=true;
        // 更新电池电量
        int APUbatteryLevel= hb3.getAPUBatteryLevel() / 10;
        UniversalAPUbatteryLevel= hb3.getAPUBatteryLevel() / 10;
        String sAPUBatteryLevel = String.valueOf(APUbatteryLevel);

//        int range= (int) Math.floor(APUbatteryLevel * 2.8);  // Get the whole number part
        if(APUbatteryLevel>=0 && APUbatteryLevel<=100){
            tm.setTeleAPUSOC(APUbatteryLevel);
            //binding.APUbatteryTV.setVisibility(View.VISIBLE);
           //binding.apubaticon.setVisibility(View.VISIBLE);
            binding.APUbatteryImageView.setVisibility(View.VISIBLE);
            binding.APUbatteryTV.setText("APU "+sAPUBatteryLevel + "% ");
        }//  |  ~ "+range+" km");}

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
            if (APUbatteryLevel>=0 && APUbatteryLevel<=100) {
                APUupdateBatteryAnimation(APUbatteryLevel);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        //根据电池电量获取对应的图片资源
        //int batteryImageResId = getBatteryImageResource(batteryLevel);
        //binding.batteryIV.setImageResource(batteryImageResId);
    }
    private void startApuDisconnectWatcher() {
        apuWatcherRunning = true;

        apuHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();

                // Check if APU is disconnected
                if (isApuConnected && (now - lastApuFrameTime > APU_TIMEOUT_MS)) {
                    isApuConnected = false;
                //    Log.d("APU", "APU Disconnected");

                    Heartbeat3 hb3 = serialPortManager.getHb3();
                    boolean mainChargeAlreadyON =
                            hb3 != null && hb3.getMainChargeMosState() == 1;
                    if (!mainChargeAlreadyON) {
                        if (apuPopupDialog != null &&
                                apuPopupDialog.isShowing()) {
                            restoreMotorState();
                            apuPopupDialog.dismiss();
                            apuPopupDialog = null;
                        }
                        InactivityManager.getInstance().stopTimer();
                        showAPUDetachedPopup();
                        Log.d("sadfsgdg","APUDETAHC POPUP");
                    }

                    binding.APUbatteryTV.setVisibility(View.GONE);
                    binding.apubaticon.setVisibility(View.GONE);
                    binding.APUbatteryImageView.setVisibility(View.INVISIBLE);
                    isAPUPresent = false;

                    if(serialPortManager!=null){
                        serialPortManager.getHb3().setApuChargeMosState(0);
                        serialPortManager.getHb3().setApuDisChargeMosState(0);

                        tm.setTeleAPUSOC(0);
                        tm.setTeleAPUBatVoltage(0);


                        //NTC VALUES
                        serialPortManager.getHb3().setNTC5(0);
                        serialPortManager.getHb3().setNTC6(0);
                        serialPortManager.getHb3().setNTC7(0);
                        serialPortManager.getHb3().setNTC8(0);

                        serialPortManager.getHb3().setAPUbatvoltage(0); //voltage
                        serialPortManager.getHb3().setAPURealCurrent(0);//Real Time Current
                        serialPortManager.getHb3().setAPUBatteryLevel(0); //APUSOC
                    }

                    // 🔹 Dismiss popup if showing
//                    if (apuPopupDialog != null && apuPopupDialog.isShowing()) {
//                        apuPopupDialog.dismiss();
//                        apuPopupDialog = null;
//                       // Log.d("APU", "Popup dismissed on disconnect");
//                    }
                }

                // Continue checking
                if (apuWatcherRunning) {
                    apuHandler.postDelayed(this, 1000);
                }
            }
        }, 1000);
    }

    public void updateCanData18FF50E5(Heartbeat3 hb3) {
       //Log.d("ChargingDebug", "RESETING FROM UIUPDATORrrrrrrrrrrrrrrrrrrrrrrrrrrrrrr");
        TelemetryDataManager.getInstance().setTeleChargingStateError(hb3.getChargingErrorbit());

        //---------------------------
        binding.yellowalert.clearAnimation();
        binding.LowBatmsg.clearAnimation();

        binding.yellowalert.setVisibility(View.INVISIBLE);
        binding.LowBatmsg.setVisibility(View.INVISIBLE);
        //----------------------------
        float chargingbit = hb3.getChargingbit();
        if(chargingbit<200){
            tm.setTeleChargingCurrentValue(chargingbit);
        }
//        Log.d("ChargingDebug", "Charging bitKKK: " + chargingbit);
        if (chargingUI == null) {
            //Log.d("UIUpdater", "chargingUI is null — cannot reset timeout");
        } else {
            chargingUI.resetChargingTimeout();
        }
    }

    public void updateCanData18934001(Heartbeat3 hb3) {
        // 更新电池电量
        int mainChargingMosState= hb3.getMainChargeMosState();
        int mainDisMosState= hb3.getMainDisChargeMosState();
//        tm.setTeleRemainingCapacityAH(hb3.getreamainingAH()/1000);

        int apuChargingMosState= hb3.getApuChargeMosState();
        int apuDisMosState= hb3.getApuDisChargeMosState();
        int mainChargeMos=hb3.getMainChargeMosState();
        // Log.d("dfsgdfh","MOSSSSSSSSSOOOOOSSSFET"+mainChargeMos);
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

        tm.setTeleBmsLifeCycles(hb3.getBMSlifecycles());
        tm.setTeleBMSMosStates(hb3.getMainChargeMosState()+","+hb3.getMainDisChargeMosState()+","+hb3.getApuChargeMosState()+","+hb3.getApuDisChargeMosState());
    }
    public void updateCanData18944001(Heartbeat3 hb3) {
        int BMS_Mos_Temp=hb3.getBMS_MOS_Temp();
        tm.setTeleBMSMosTemp(BMS_Mos_Temp);
    }
    public void updateCanData18934002(Heartbeat3 hb3) {
        // 更新电池电量

//        int apuChargingMosState= hb3.getApuChargeMosState();
//        int apuDisMosState= hb3.getApuDisChargeMosState();
//        int mainChargeMos=hb3.getMainChargeMosState();
//      // Log.d("dfsgdfh","MOSSSSSSSSSOOOOOSSSFET"+mainChargeMos);
////        Log.d("sadfgd","MOSSSSSSSSSOOOOOSSSFETAPUDISCG"+apuDisMosState);
//        if( apuDisMosState==1 && mainChargeMos==0 ){
//            binding.RunningOnAPU.setVisibility(View.VISIBLE);
//            binding.redalert.setVisibility(View.VISIBLE);
//            apuflag=true;
//        }
//        else{
//            binding.RunningOnAPU.setVisibility(View.INVISIBLE);
//            binding.redalert.setVisibility(View.INVISIBLE);
//            apuflag=false;
//        }
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
        if(NTC5 > 0 || NTC6 > 0 || NTC7 > 0 || NTC8 > 0){
            tm.setTeleNTC(NTC1+","+NTC2+","+NTC3+","+NTC4+","+NTC5+","+NTC6+","+NTC8+","+NTC8);
        }
        else tm.setTeleNTC(NTC1+","+NTC2+","+NTC3+","+NTC4);
        int[] temps = {NTC1, NTC2, NTC3, NTC4, NTC5, NTC6, NTC7, NTC8};
        boolean anyIs45 = false;
        boolean anyIs50OrAbove = false;
        boolean allBelow45 = true;

        for (int temp : temps) {
            if (temp < 0 || temp > 150) {
                continue;
            }

            if (temp >= 80 && temp < 86)anyIs45 = true;
            if (temp >= 86) anyIs50OrAbove = true;
            if (temp >= 80) allBelow45 = false;
        }

        //  Reset 45-warning ONLY when all temps are below 45
        if (allBelow45 && is45WarningShown) {
            //Log.d("TempReset", "All NTCs below 45. Resetting 10s warning flag.");
            is45WarningShown = false;
        }

        //  Handle 50+ Continuous warning
        if (anyIs50OrAbove) {
            if (!is50WarningActive) {
             //   Log.d("TempWarning", "Temp >= 50. Starting continuous warning.");
                startTempWarningAnimation();
                highVolumeForWarning();
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    startBeeping(1);
                }, 1300);
                sendWarningToUI("High Battery Temperature Detected");

                is50WarningActive = true;
            }
        } else {
            //  Stop 50°C continuous warning if previously active
            if (is50WarningActive) {
               // Log.d("TempWarning", "Temp dropped below 50. Stopping continuous warning.");
                stopTempWarningAnimation();
                stopBeeping();
                sendWarningToUI("");
                is50WarningActive = false;
            }

            //  Handle 45°C exactly → 10s warning only ONCE
            if (anyIs45 && !is45WarningShown) {
              //  Log.d("TempWarning", "Temp == 45. Starting 10s warning.");
                startTempWarningAnimation();
                highVolumeForWarning();
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    startBeeping(1);
                }, 1300);
                sendWarningToUI("High Battery Temperature Detected");
                is45WarningShown = true;

                warningHandler.postDelayed(() -> {
               //     Log.d("TempWarning", "10s warning done. Stopping animation and beep.");
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

    public void startBeeping(int code) {
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
                    case 4:
                        mediaPlayer = MediaPlayer.create(context, R.raw.batlowalert2);
                        break;
                    case 5:
                        mediaPlayer = MediaPlayer.create(context, R.raw.low_tire_speech);
                        break;
                    default:
                      //  Log.w("Beep", "Unknown beep code: " + code);
                        stopBeeping(); // Or just return; if you want to keep isBeeping true
                        return;
                }

                if (mediaPlayer != null) {
                    mediaPlayer.start();
                    beepHandler.postDelayed(this, 2720); // Repeat every ~2.7 seconds
                } else {
                  //  Log.e("Beep", "MediaPlayer failed to create for code: " + code);
                    stopBeeping(); // or just return;
                }
            }
        };
        beepHandler.post(beepRunnable);
    }

    public void stopBeeping() {
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
    private boolean isTempWarningActive = false;
    private void startTempWarningAnimation() {
        Animation tempalert = AnimationUtils.loadAnimation(context, R.anim.alpha);
        isTempWarningActive = true;
        binding.redalert.setVisibility(View.VISIBLE);
        //binding.tempalerticon.setVisibility(View.VISIBLE);
        binding.battempmsg.setVisibility(View.VISIBLE);

        binding.redalert.startAnimation(tempalert);
        binding.battempmsg.startAnimation(tempalert);
        binding.transportModeYellowWarning.setVisibility(View.GONE);

    }

    private void stopTempWarningAnimation() {
        isTempWarningActive = false;
        binding.redalert.clearAnimation();
        binding.battempmsg.clearAnimation();

        binding.redalert.setVisibility(View.INVISIBLE);
       // binding.tempalerticon.setVisibility(View.GONE);
        binding.battempmsg.setVisibility(View.INVISIBLE);
        updateTransportModeWarning();
    }

    public void startKeylostRedAlertAnimation() {
        try {
            if (binding.redalert != null) {
                Animation keylost = AnimationUtils.loadAnimation(context, R.anim.alpha);
                binding.redalert.setVisibility(View.VISIBLE);
                binding.KeylostMessage.setVisibility(View.VISIBLE);
                binding.redalert.startAnimation(keylost);
                binding.KeylostMessage.startAnimation(keylost);
                startBeeping(4);
            }
        } catch (Exception e) {
            //Log.e("UIUpdater", "SMARTKEY Error starting red alert animation", e);
        }
    }

    public void stopKeylostRedAlertAnimation() {
        try {
            if (binding.redalert != null) {
                binding.redalert.setVisibility(View.GONE);
                binding.KeylostMessage.setVisibility(View.GONE);
                binding.redalert.clearAnimation();
                binding.KeylostMessage.clearAnimation();
                stopBeeping();
            }
        } catch (Exception e) {
           // Log.e("UIUpdater", "SMARTKEY Error stopping red alert animation", e);
        }
    }
    private float previousAh = 0f;

    private float ahByCharger = 0f;     // Total (stored)
    private float ahByRegen = 0f;        // Total (stored)
    private float tripAhByRegen = 0f;    // Trip only (RAM)
    public static int chargison;
    // =====================
// CHARGE + REGEN AH
// =====================
    public void updateCanData160B8001(Heartbeat3 hb3) {
        updateCanData18904001(hb3);

//        float currentAh = roundTo3(hb3.getChargeAH() / 1000f);//dont rounf off
        float currentAh = hb3.getChargeAH() / 1000f;
       // Log.d("TROBLESHOOTINGCOLOUMB", "IN_AH = " + currentAh);

        ahTrackerManager.updateChargingAh(currentAh);
//        Log.d("TROBLESHOOTINGCOLOUMB", "SOC = " + ahTrackerManager.getSOC());
//        Log.d("TROBLESHOOTINGCOLOUMB", "RemainingAh = " + ahTrackerManager.getRemainingAh());
//        Log.d("TROBLESHOOTINGCOLOUMB", "previousAh = " + previousAh);

        // First frame protection
        if (previousAh == 0f) {
//            Log.d("AHA", "previousAh EQUAL = 0 ENTERED= " + previousAh);
            previousAh = currentAh;
            loadStoredAhValues(); // load persisted totals
            return;
        }

        float delta = currentAh - previousAh;

        // Valid increment range protection
        if (delta > 0 && delta < 20.0f) {
            if (tm.getTeleChargingState() == 1) {
                ahByCharger += delta;
            } else if ((tm.getTeleChargingState() == 0 || tm.getTeleChargingCurrentValue()==0) && rpm>20) {
                ahByRegen += delta;        // total regen
                tripAhByRegen += delta;    // trip regen
            }
            else {
                //Do nothing
            }
        }

        previousAh = currentAh;

        // Round values
        float roundedTotalAh = roundTo3(currentAh);
        float roundedChargerAh = roundTo3(ahByCharger);
        float roundedRegenAh = roundTo3(ahByRegen);

        // Persist only total values
        saveAhValues(roundedTotalAh, roundedChargerAh, roundedRegenAh);

        // Telemetry update
        tm.setTeleTotalAH(roundedTotalAh);
        tm.setTeleChargerAH(roundedChargerAh);
        tm.setTeleRegenAH(roundedRegenAh);

//        Log.d("AH_REGEN",
//                "TotalRegen=" + roundedRegenAh +
//                        ", TripRegen=" + getTripAhByRegen());

//        Log.d("CAN", "TOTALREGENAH      " + getTotalAhByRegen());
//        Log.d("CAN", "TOTALREGENAH_TRIP " + getTripAhByRegen());

        activity.runOnUiThread(this::updateRecoKm);
    }

    // =====================
// SHARED PREFS
// =====================
    private void saveAhValues(float totalAh, float chargerAh, float regenAh) {
        SharedPreferences prefs = context.getSharedPreferences("ah_data", Context.MODE_PRIVATE);
        prefs.edit()
                .putFloat("totalAh", totalAh)
                .putFloat("chargerAh", chargerAh)
                .putFloat("regenAh", regenAh)
                .apply();
    }

    private void loadStoredAhValues() {
        SharedPreferences prefs = context.getSharedPreferences("ah_data", Context.MODE_PRIVATE);

        tm.setTeleTotalAH(prefs.getFloat("totalAh", 0f));
        tm.setTeleChargerAH(prefs.getFloat("chargerAh", 0f));
        tm.setTeleRegenAH(prefs.getFloat("regenAh", 0f));

        ahByCharger = tm.getTeleChargerAH();
        ahByRegen = tm.getTeleRegenAH();

        tripAhByRegen = 0f; // fresh trip
    }

    // =====================
// DISCHARGE AH
// =====================
    public void updateCanData160D8001(Heartbeat3 hb3) {
        updateCanData18904001(hb3);

        float rawDischargeAH = hb3.getDisChargeAH() / 1000f;
//        Log.d("AH", "DIS_AH = " + rawDischargeAH);

        ahTrackerManager.updateDischargingAh(rawDischargeAH);
//        Log.d("AH", "SOC@DIS = " + ahTrackerManager.getSOC());

        float dischargeAH = roundTo3(rawDischargeAH);

        saveDischargeAh(dischargeAH);
        tm.setTeleDischargeAH(dischargeAH);
    }

    private void saveDischargeAh(float value) {
        SharedPreferences prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        prefs.edit().putFloat("dischargeAh", value).apply();
    }

    private float loadDischargeAh() {
        SharedPreferences prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        return prefs.getFloat("dischargeAh", 0f);
    }

    // =====================
// GETTERS
// =====================
    public float getTotalAhByRegen() {
        return roundTo3(ahByRegen);
    }

    public float getTripAhByRegen() {
        return roundTo3(tripAhByRegen);
    }

    // =====================
// RESET (call on LOCK)
// =====================
    public void resetTripAhByRegen() {
        tripAhByRegen = 0f;
//        Log.d("AH_REGEN", "Trip regen Ah reset");

        // Always refresh reco UI after reset
        updateRecoKm();
    }


    // =====================
// UTIL
// =====================
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
    public boolean isStand=false;
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
    private boolean startReasySound = false; // track changes
    private boolean isAlreadyInMainActivity() {
        return activity != null && activity instanceof MainActivity;
    }
    private Animation reverseArrowAnimation;
    public static boolean isChargingUIVisible = false;
    public void updateCanData10F807A8(Heartbeat3 hb3) {
        directionalGears = FrameUtils.extractBits(hb3.getBSG1_BYTE1(), 0, 1);
        int speedGears = FrameUtils.extractBits(hb3.getBSG1_BYTE1(), 2, 4);
        int READY = FrameUtils.extractBits(hb3.getBSG1_BYTE3(), 0, 0);
        //Log.d("CAN", "🟢 ISREDVALUE ENTERED ISREADY======"+READY);

        boolean isReady = (READY == 1);
//        Log.d("nolockbug","VALUE REDAY IS ----------"+isReady);

        int STAND =FrameUtils.extractBits(hb3.getBSG1_BYTE2(),0,1);
        isboost=hb3.getBSG1_BYTE3()==8; //hex value 08
        isCruise=hb3.getBSG1_BYTE1()==201;//hex value of 201=C9
        tm.setTeleMotorTemp(hb3. getBSG1_BYTE7()-40);   //Motor temprature hex to dec then offset 40
        tm.setTeleControllerMosTemp(hb3.getBSG1_BYTE8()-40);// Controller Mos temp offset 40
//        Log.d("dfhgvhb","CRUISE VALUE IS "+hb3.getBSG1_BYTE1());
//        if(isboost)binding.actualboost.setVisibility(View.VISIBLE);
//        else binding.actualboost.setVisibility(View.INVISIBLE);
        //Log.d("sdafsgdf","BOOSTBYTE IS "+isboost);
        float chargingbit = hb3.getChargingbit();
        //Log.d("sdfdg","setChargingErrorbit   "+hb3.getChargingErrorbit());
        if((shouldshowchargingicononmainonot && directionalGears!=2) || shouldshowchargingicononmainonot && isStand){

            binding.rectangleGearsTV.setVisibility(View.INVISIBLE);
            binding.neutralicon.setVisibility(View.INVISIBLE);
//            binding.chargingicononmainactivty.setVisibility(View.VISIBLE);
            binding.chargingredtab.setVisibility(View.VISIBLE);
            if(chargingbit>0){
                binding.chargingredtab.setImageResource(R.drawable.chargingredtab);
                TelemetryDataManager.getInstance().setTeleChargingState(1);
                UIUpdater.isChargingUIVisible = true;
            }
            else {
                binding.chargingredtab.setImageResource(R.drawable.yellowpluggedtab);
                TelemetryDataManager.getInstance().setTeleChargingState(2);
            }

//            binding.chargingredtab.setVisibility(View.VISIBLE);
        }
        else {
            UIUpdater.isChargingUIVisible = false;
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

        int batteryLevel = (int)hb3.getBatteryLevel();
//        float batteryLevel = ahTrackerManager.getSOC();
        String sBatteryLevel;
        if(batteryLevel>=0 && batteryLevel<=100){ sBatteryLevel = String.valueOf(batteryLevel);}
        else sBatteryLevel="Analysing";

//        double speed = (hb3.getBSG2_BYTE78() * 60.0) / 3.3125 * 0.96 / 1000;
        isStand = (STAND == 1);
        boolean standChanged = (STAND != lastStand);
        lastStand = STAND;

// Priority 2: Stand UI (when unplugged and stand is open)
        if (STAND == 1 && (standChanged || LoginActivity.forceStandUIDUEToCharging)) {
            LoginActivity.forceStandUIDUEToCharging=false;
            if (!isChargingUIVisible)
            {
                InactivityManager.getInstance().startTimer(1);
                Log.d("TOUCHTIMER", " Timeout TOUCHTIMER ENTER Side STAND");
            }

            if(serialPortManager!=null){
                serialPortManager.VCUSendToMCU("FFAA550101000A01");
            }
            standui();
            return;
        }

//        if(isStand && !isSideStandSoundPlaying){
//            startStandSound();
//        }
//        else{
//            if(isSideStandSoundPlaying){
//                stopSideStandSound();
//            }
//        }

// Priority 3: Gear-based UI (only if not charging and stand is closed)
        switch (directionalGears) {
            case 0: // Neutral
                MainActivity.currentgear='N';
                break;
            case 1:
                MainActivity.currentgear='F';
                break;
            case 2:
                MainActivity.currentgear='R';
                break;
            default:
                MainActivity.currentgear='O';
                break;
        }

        if (directionalGears != lastGear || standChanged) {
            lastGear = directionalGears;

            binding.arrow3.clearAnimation();
            binding.arrow3.setVisibility(View.GONE);

            if (!isStand) {
                switch (directionalGears) {
                    case 0: // Neutral
                        neutralUI();
                        //mp3name is shiftneutral
                        playGearSound(R.raw.shiftneutral); // 🔊 Play neutral shift sound
                        if(StopInactivtyTimerDuringPopUp){InactivityManager.getInstance().startTimer(1);}
                        break;

                    case 1: // Forward
                        powerOnUi();
                        activity.startSpeedIntroFromUpdater();
                        //if(StopInactivtyTimerDuringPopUp){Log.d("sadfgg","********1");InactivityManager.getInstance().startTimer(1);}
                        InactivityManager.getInstance().stopTimer();

//                        if(MainActivity.MainisActive)
//                        {
//                            goToMainActivityClearStack();
//                        }
                        if (!MainActivity.isVisible && !MainActivity.isrideOSOTA) {
                            //Log.d("UIUPDATER", "TAKEMEHOME2222");

                            Intent intent = new Intent(activity, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                            activity.startActivity(intent);
                            activity.overridePendingTransition(R.anim.zoom_in_enter, R.anim.zoom_out_exit);

                        }

                        if (activity != null) {
                            (activity).closeRecoIfOpen();
                            (activity).closeQuickSettingsIfOpen();
                        }

                        //     cameraHandler.startRearRecording(rearPath);

                        //mp3name is shiftforward
                        //playGearSound(R.raw.shiftforward);
                        if(serialPortManager!=null){
                            serialPortManager.VCUSendToMCU("FFAA550101000A01");}
                        // animateSpeedLevelOnce();
                       // if(StopInactivtyTimerDuringPopUp){InactivityManager.getInstance().startTimer(2);}
                        break;

                    case 2: // Reverse
                        reverseUI();
                        //
                        // startBeeping(4);
                        if(StopInactivtyTimerDuringPopUp){InactivityManager.getInstance().startTimer(2);}
                        break;

                    default:
                        InactivityManager.getInstance().startTimer(1);
                        stopReverseBeep();
                        binding.neutralicon.setVisibility(View.INVISIBLE);
                        binding.neutral.setVisibility(View.INVISIBLE);
                        break;
                }
            }
        }
    }



    public void goToMainActivityClearStack() {
        if (activity == null) return;

        Intent intent = new Intent(activity, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_NEW_TASK);

        activity.startActivity(intent);
    }


    private MediaPlayer ReadybeepPlayer;

    public void ReadystartBeeping() {
        if (ReadybeepPlayer == null) {
            ReadybeepPlayer = MediaPlayer.create(context, R.raw.readysound);
            ReadybeepPlayer.setLooping(true); // continuous
            ReadybeepPlayer.start();
        }
    }

    public void ReadystopBeeping() {
        if (ReadybeepPlayer != null) {
            ReadybeepPlayer.stop();
            ReadybeepPlayer.release();
            ReadybeepPlayer = null;
        }
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

    public void stopSideStandSound() {
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
//            Log.e("GearSound", "Context is null, cannot play sound");
            return;
        }

        try {
            if (gearshiftmediaPlayer != null) {
                gearshiftmediaPlayer.release();
                gearshiftmediaPlayer = null;
            }

            gearshiftmediaPlayer = MediaPlayer.create(context, resId);
            if (gearshiftmediaPlayer == null) {
//                Log.e("GearSound", "MediaPlayer creation failed for resId: " + resId);
                return;
            }

            gearshiftmediaPlayer.setOnCompletionListener(mp -> {
                try {
                    mp.release();
                } catch (Exception e) {
//                    Log.e("GearSound", "Error releasing MediaPlayer: " + e.getMessage());
                }
                gearshiftmediaPlayer = null;
            });

            gearshiftmediaPlayer.start();

        } catch (Exception e) {
//            Log.e("GearSound", "Exception while playing gear sound: " + e.getMessage(), e);
        }
    }

//    private void animateSpeedLevelOnce() {
//
//        final Handler handler = new Handler();
//        final int[] level = {0};
//        final boolean[] increasing = {true};
//
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                int mappedSpeed = (level[0] / 10) * 10; // Use multiples of 10 (0,10,...100)
//
//                if (MainActivity.speedImageMap.containsKey(mappedSpeed)) {
//                    binding.speedImageView.setImageResource(MainActivity.speedImageMap.get(mappedSpeed));
//                }
//
//                if (increasing[0]) {
//                    level[0] += 10;
//                    if (level[0] >= 100) {
//                        increasing[0] = false;
//                    }
//                } else {
//                    level[0] -= 10;
//                    if (level[0] < 0) {
//                        return;
//                    }
//                }
//
//                handler.postDelayed(this, 50); // Control animation speed
//            }
//        }, 50);
//    }

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


    public void neutralUI() {
        stopReverseBeep();
        binding.ForwardInstruction.setVisibility(View.VISIBLE);
        binding.tpmsicon.setVisibility(View.VISIBLE);

        binding.neutralicon.setVisibility(View.VISIBLE);
//        binding.brk.setVisibility(View.VISIBLE);
//        binding.startButton.setVisibility(View.VISIBLE);
        binding.reverseui.setVisibility(View.INVISIBLE);
//        binding.avgSpeed.setVisibility(View.INVISIBLE);
        binding.tripTextView.setVisibility(View.INVISIBLE);
        binding.navleft.setVisibility(View.INVISIBLE);
        binding.navright.setVisibility(View.INVISIBLE);
        binding.tripresetbtn.setVisibility(View.INVISIBLE);
        binding.arrow3.setVisibility(View.INVISIBLE);
//        binding.tripreco.setVisibility(View.VISIBLE);

        binding.neutralicon.setImageResource(R.drawable.neutralicon); // Replace with your drawable
        binding.neutral.setVisibility(View.VISIBLE);
//        binding.batterryBar.setVisibility(View.VISIBLE);
        binding.batteryArc.setVisibility(View.VISIBLE);
        binding.batteryTV.setVisibility(View.VISIBLE);
        binding.network.setVisibility(View.VISIBLE);
        binding.wifi.setVisibility(View.VISIBLE);
        binding.timeTextView.setVisibility(View.VISIBLE);
//        binding.BluetoothIV.setVisibility(View.VISIBLE);
       // binding.batteryImageView.setVisibility(View.VISIBLE);
        binding.speedImageView.setVisibility(View.INVISIBLE);
        binding.speedNumberTV.setVisibility(View.INVISIBLE);
        binding.speedTV.setVisibility(View.INVISIBLE);
//        binding.turnLeftIV.setVisibility(View.INVISIBLE);
//        binding.highBeamIVoff.setVisibility(View.VISIBLE);
//        binding.turnRightIV.setVisibility(View.INVISIBLE);
//        binding.highBeamIVoff.setVisibility(View.INVISIBLE);

        // binding.leftOn.setVisibility(View.INVISIBLE);
        //  binding.rightOn.setVisibility(View.INVISIBLE);
        //binding.readyIV.setVisibility(View.INVISIBLE);
        // binding.highBeamIVon.setVisibility(View.INVISIBLE);
        binding.enginestartIV.setVisibility(View.INVISIBLE);
        binding.speedArc.setVisibility(View.INVISIBLE);
        binding.rectangleGearsTV.setVisibility((View.INVISIBLE));
        binding.sports.setVisibility((View.INVISIBLE));
        binding.stand.setVisibility((View.INVISIBLE));
        binding.chargingSOC.setVisibility(View.INVISIBLE);
        //binding.percenticon.setVisibility(View.INVISIBLE);
       // binding.chargingbike.setVisibility(View.INVISIBLE);
        binding.mapbtn.setVisibility(View.VISIBLE);
        binding.menubtn.setVisibility(View.VISIBLE);
        binding.chargingicon.setVisibility(View.INVISIBLE);
//        binding.brk.setVisibility(View.INVISIBLE);
//        binding.startButton.setVisibility(View.INVISIBLE);
        if(isAPUPresent){
           //binding.apubaticon.setVisibility(View.VISIBLE);
            //binding.APUbatteryTV.setVisibility(View.VISIBLE);
            binding.APUbatteryImageView.setVisibility(View.VISIBLE);
        }
        binding.removestand.setVisibility(View.INVISIBLE);
//        binding.chargingicononmainactivty.setVisibility(View.VISIBLE);
    }

    public void powerOnUi()
    {
        stopReverseBeep();
        binding.tpmsicon.setVisibility(View.VISIBLE);
        binding.ForwardInstruction.setVisibility(View.GONE);
//        binding.avgSpeed.setVisibility(View.VISIBLE);
        binding.tripTextView.setVisibility(View.VISIBLE);
        binding.navleft.setVisibility(View.VISIBLE);
        binding.navright.setVisibility(View.VISIBLE);
        binding.tripresetbtn.setVisibility(View.VISIBLE);
//        binding.tripreco.setVisibility(View.INVISIBLE);
        binding.startButton.clearAnimation();
        binding.startButton.setVisibility(View.INVISIBLE);
        binding.removestand.setVisibility(View.INVISIBLE);
        binding.enginestartIV.setVisibility(View.VISIBLE);
        binding.speedArc.setVisibility(View.VISIBLE);
        binding.speedNumberTV.setVisibility(View.VISIBLE);
        binding.speedTV.setVisibility(View.VISIBLE);
//        binding.batterryBar.setVisibility(View.VISIBLE);
        binding.batteryArc.setVisibility(View.VISIBLE);

        binding.batteryTV.setVisibility(View.VISIBLE);
        binding.network.setVisibility(View.VISIBLE);
        binding.wifi.setVisibility(View.VISIBLE);
        binding.timeTextView.setVisibility(View.VISIBLE);
//        binding. BluetoothIV.setVisibility(View.VISIBLE);
        //binding.batteryImageView.setVisibility(View.VISIBLE);
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
       // binding.chargingbike.setVisibility(View.INVISIBLE);
        binding.mapbtn.setVisibility(View.VISIBLE);
        binding.menubtn.setVisibility(View.VISIBLE);
        binding.chargingicon.setVisibility(View.INVISIBLE);
        if(isAPUPresent){
            //binding.apubaticon.setVisibility(View.VISIBLE);
            //binding.APUbatteryTV.setVisibility(View.VISIBLE);
            binding.APUbatteryImageView.setVisibility(View.VISIBLE);
        }
//        binding.turnLeftIV.setVisibility(View.VISIBLE);
//        binding.turnRightIV.setVisibility(View.VISIBLE);
       // binding.highBeamIVoff.setVisibility(View.VISIBLE);

//        binding.highBeamIVoff.setVisibility(View.VISIBLE);
        binding.removestand.setVisibility(View.INVISIBLE);
//        binding.chargingicononmainactivty.setVisibility(View.VISIBLE);
    }

    public void reverseUI()
    {

        startReverseBeep();
        binding.tpmsicon.setVisibility(View.INVISIBLE);
        binding.ForwardInstruction.setVisibility(View.GONE);
        binding.startButton.setVisibility(View.INVISIBLE);
//        binding.avgSpeed.setVisibility(View.VISIBLE);
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
        binding.speedArc.setVisibility(View.INVISIBLE);

        //  binding.highBeamIVoff.setVisibility(View.INVISIBLE);
//        binding.batterryBar.setVisibility(View.INVISIBLE);
        binding.batteryTV.setVisibility(View.INVISIBLE);
        binding.network.setVisibility(View.INVISIBLE);
        binding.wifi.setVisibility(View.INVISIBLE);
        binding.timeTextView.setVisibility(View.INVISIBLE);
//        binding.BluetoothIV.setVisibility(View.INVISIBLE);
        binding.batteryImageView.setVisibility(View.INVISIBLE);
        binding.batteryArc.setVisibility(View.INVISIBLE);
        binding.APUbatteryImageView.setVisibility(View.INVISIBLE);

        binding.neutralicon.setVisibility((View.INVISIBLE));
        binding.neutral.setVisibility(View.INVISIBLE);
        binding.brk.setVisibility(View.INVISIBLE);
        binding.removestand.setVisibility(View.INVISIBLE);
        binding.regentab.setVisibility(View.GONE);
        binding.ecoimage.setVisibility(View.GONE);
        binding.sports.setVisibility((View.GONE));
        binding.boost.setVisibility(View.GONE);
        binding.removestand.setVisibility(View.INVISIBLE);

        //reverse visisble ui
        binding.reverseui.setVisibility(View.VISIBLE);
        binding.arrow3.setVisibility(View.VISIBLE);
        binding.arrow3.startAnimation(reverseArrowAnimation);
        binding.stand.setVisibility((View.INVISIBLE));
        binding.chargingSOC.setVisibility(View.INVISIBLE);
        // binding.percenticon.setVisibility(View.INVISIBLE);
      //  binding.chargingbike.setVisibility(View.INVISIBLE);
        binding.mapbtn.setVisibility(View.VISIBLE);
        binding.menubtn.setVisibility(View.VISIBLE);
        binding.chargingicon.setVisibility(View.INVISIBLE);
        binding.apubaticon.setVisibility(View.GONE);
        binding.APUbatteryTV.setVisibility(View.GONE);
//        binding.turnLeftIV.setVisibility(View.VISIBLE);
//        binding.turnRightIV.setVisibility(View.VISIBLE);
        //binding.highBeamIVoff.setVisibility(View.VISIBLE);

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
        binding.ForwardInstruction.setVisibility(View.GONE);
        binding.tpmsicon.setVisibility(View.VISIBLE);

//        binding.batterryBar.setVisibility(View.VISIBLE);
        binding.batteryArc.setVisibility(View.VISIBLE);

//        binding.avgSpeed.setVisibility(View.INVISIBLE);
        binding.tripTextView.setVisibility(View.INVISIBLE);
//        binding.tripreco.setVisibility(View.INVISIBLE);
        binding.navleft.setVisibility(View.INVISIBLE);
        binding.navright.setVisibility(View.INVISIBLE);
        binding.tripresetbtn.setVisibility(View.INVISIBLE);
        binding. batteryTV.setVisibility(View.VISIBLE);
        binding.network.setVisibility(View.VISIBLE);
        binding. wifi.setVisibility(View.VISIBLE);
        binding.timeTextView.setVisibility(View.VISIBLE);
//        binding.BluetoothIV.setVisibility(View.VISIBLE);
        binding.batteryImageView.setVisibility(View.VISIBLE);
        binding.brk.setVisibility(View.INVISIBLE);

        binding.reverseui.setVisibility(View.INVISIBLE);
        binding.arrow3.clearAnimation();
        binding.arrow3.setVisibility(View.INVISIBLE);

        binding.removestand.setVisibility((!shouldshowchargingicononmainonot  && isStand   ? View.VISIBLE : View.INVISIBLE));//visible


        binding.stand.setVisibility((isStand ? View.VISIBLE : View.INVISIBLE)); //visisble
        binding.speedNumberTV.setVisibility((isStand ? View.INVISIBLE : View.VISIBLE));
        binding.speedTV.setVisibility((isStand ? View.INVISIBLE : View.VISIBLE));
//                boost.setVisibility((isStand?View.INVISIBLE:View.INVISIBLE));
//                ecoimage.setVisibility((isStand?View.INVISIBLE:View.VISIBLE));
        binding.speedImageView.setVisibility(View.INVISIBLE);
        binding.regentab.setVisibility(View.GONE);
        binding.ecoimage.setVisibility(View.GONE);
        binding.sports.setVisibility((View.GONE));
        binding.boost.setVisibility(View.GONE);
        //  binding.highBeamIVon.setVisibility((isStand ? View.INVISIBLE : View.VISIBLE));
        binding.enginestartIV.setVisibility((isStand ? View.INVISIBLE : View.VISIBLE));
        binding.speedArc.setVisibility((isStand ? View.INVISIBLE : View.VISIBLE));
        //  binding.highBeamIVoff.setVisibility((isStand ? View.INVISIBLE : View.VISIBLE));
        binding.neutralicon.setVisibility((View.INVISIBLE));
        binding.neutral.setVisibility(View.INVISIBLE);
       // binding.chargingbike.setVisibility(View.INVISIBLE);
        binding.mapbtn.setVisibility(View.VISIBLE);
        binding.menubtn.setVisibility(View.VISIBLE);
        binding.chargingicon.setVisibility(View.INVISIBLE);
        binding.chargingSOC.setVisibility(View.INVISIBLE);
        // binding.percenticon.setVisibility(View.INVISIBLE);
      //  binding.chargingbike.setVisibility(View.INVISIBLE);
//        binding.highBeamIVoff.setVisibility(View.INVISIBLE);
//        binding.turnLeftIV.setVisibility(View.INVISIBLE);
//        binding.turnRightIV.setVisibility(View.INVISIBLE);
        binding.rectangleGearsTV.setVisibility(View.INVISIBLE);
        if(isAPUPresent){
            //binding.apubaticon.setVisibility(View.VISIBLE);
            //binding.APUbatteryTV.setVisibility(View.VISIBLE);
            binding.APUbatteryImageView.setVisibility(View.VISIBLE);
        }
    }

    // --- Fields ---
    private int currentState = -1;
    private int lastActiveState = -1;
    private boolean blinkVisible = true;
    private boolean isBlinking = false;
    private boolean isSoundPlaying = false;

    private final Handler blinkHandler = new Handler(Looper.getMainLooper());
    private final Handler stopDelayHandler = new Handler(Looper.getMainLooper());
//    private final Handler soundHandler = new Handler(Looper.getMainLooper());

    private final int BLINK_INTERVAL_MS = 400;
//    private final int SOUND_INTERVAL_MS = 1000;
    private final int ZERO_HOLD_DURATION_MS = 2500;

    private Runnable blinkRunnable;
    private Runnable stopBlinkingRunnable;
//    private Runnable soundRunnable;
private int streamId = 0;
    private SoundPool soundPool;
    private int soundId;
    private boolean soundLoaded = false;
    public static boolean ignorefirstblink;
    public void initIndicatorLogic(Context context) {
        soundPool = new SoundPool.Builder().setMaxStreams(1).build();
        soundId = soundPool.load(context, R.raw.indicatorsound_cleaned, 1);
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

//        soundRunnable = () -> {
//            if (isBlinking && soundLoaded && directionalGears != 2  && !ignorefirstblink) {
//                soundPool.play(soundId, 1, 1, 0, 0, 1);
//                soundHandler.postDelayed(soundRunnable, SOUND_INTERVAL_MS);
//            } else {
//                isSoundPlaying = false; // Stop loop if in reverse
//            }
//        };
    }

    public void indicators(int newSignal) {
//        Log.d("Indicator", "➡ New signal: " + newSignal);

        if (isBlinking && currentState == 3 && (newSignal == 1 || newSignal == 2)) {
//            Log.d("Indicator", "⚠️ Ignored glitch signal " + newSignal + " while hazard (3) is active");
            return;
        }

        if (newSignal == 0) {
            stopDelayHandler.removeCallbacks(stopBlinkingRunnable);
            stopDelayHandler.postDelayed(stopBlinkingRunnable, ZERO_HOLD_DURATION_MS);
           // stopAllCameraPreviews();
            return;
        }

        stopDelayHandler.removeCallbacks(stopBlinkingRunnable);

        Log.d("fcgvhbjnk","THIS IS INDICOTORSIGNAL FOR NOW 3-----"+newSignal);

// LEFT/RIGHT -> HAZARD
        if ((currentState == 1 || currentState == 2) && newSignal == 3) {
            stopIndicatorSound();
        }

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

        if (currentState != 3 && directionalGears != 2 && !isSoundPlaying && soundLoaded && !ignorefirstblink) {
            isSoundPlaying = true;
            streamId = soundPool.play(soundId, 1, 1, 1, -1, 1);
        }
    }

    public void stopBlinking() {
        if(directionalGears==0){InactivityManager.getInstance().startTimer(1);}
        isBlinking = false;
        isSoundPlaying = false;

        blinkHandler.removeCallbacks(blinkRunnable);
        if (streamId != 0) {
            soundPool.stop(streamId);
            streamId = 0;
        }
        stopDelayHandler.removeCallbacks(stopBlinkingRunnable);

        binding.leftOn.setVisibility(View.INVISIBLE);
        binding.rightOn.setVisibility(View.INVISIBLE);
    }
    private void stopIndicatorSound() {
        isSoundPlaying = false;

        if (streamId != 0) {
            soundPool.stop(streamId);
            streamId = 0;
        }
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
    public  TripDistanceManager tripManager;
    TextView tripTextView;


    public void setTripManager(TripDistanceManager tripManager) {
        this.tripManager = tripManager;
    }
    public static double tiredimensions=1.46;//1.4825;
    private static double rpm;
    private String previousMode = "";
    private MediaPlayer modeChangePlayer;
    public long lastCanUpdateTime = 0;
    private static final long CAN_TIMEOUT_MS = 8000; // 5 seconds
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

                if ((now - lastCanUpdateTime) > CAN_TIMEOUT_MS && !canPopupShown && islock) {
                    canPopupShown = true;

                    if (!activity.isFinishing() && !activity.isDestroyed()) {

                        //  Immediately reset state when popup appears
                        if (serialPortManager != null) {
                            serialPortManager.getHb3().setBSG2_BYTE78(0);
                        }
                        updateRideLock(0);
                        updateSpeedTrueAnimation(0,0);
                        InactivityManager.getInstance().startTimer(1);
                        UIUpdater.directionalGears=0;
                        binding.speedNumberTV.setText("0");
                        tm.setTeleSpeed(0);
                        tm.setTeleControllerAbruptOff(1);
                        MainActivity.currentgear='O';
                        rpm = 0;
                        ReadystopBeeping();
                        startReasySound = false;
                        stopReverseBeep();
                        updateSpeedAnimation(0);
                        binding.regentab.setVisibility(View.GONE);
                        binding.ecoimage.setVisibility(View.GONE);
                        binding.sports.setVisibility(View.GONE);
                        binding.boost.setVisibility(View.GONE);

                        // 🔊 Play error sound only once
//                        if (!controllerErrorSoundPlayed) {
//                            controllerErrorPlayer = MediaPlayer.create(activity, R.raw.controllererror);
//                            controllerErrorPlayer.setOnCompletionListener(mp -> {
//                                mp.release();
//                                controllerErrorPlayer = null;
//                            });
//                            controllerErrorPlayer.start();
//                            controllerErrorSoundPlayed = true;
//                        }
//
//                        // 🚨 Show popup
//                        new AlertDialog.Builder(activity)
//                                .setTitle("Error Alert")
//                                .setMessage("Controller switched off abruptly, or a CAN line issue occurred.")
//                                .setPositiveButton("OK", (dialog, which) -> {
//                                    // Just close the dialog now
//                                    InactivityManager.getInstance().startTimer(1);
//                                    dialog.dismiss();
//                                })
//                                .setCancelable(false)
//                                .show();
                                Log.d("saghfkjaf","THIS IS CONTOL OFF ON NOTLOCK");

                    }
                } else if ((now - lastCanUpdateTime) > CAN_TIMEOUT_MS && !canPopupShown) {
                    Log.d("saghfkjaf","THIS IS CONTOL OFF ON LOCK");
                    canPopupShown = true;
                    MainActivity.currentgear='O';
                }
                // Keep checking
                canTimeoutHandler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    private boolean resetboost=false;
    private boolean wasMoving = false;
    private double Speed_ratio_Value = 1.5536444;//// 10% high
//    private double Speed_ratio_Value = 1.48331442;//// 5% high
//    private double Speed_ratio_Value = 1.412404;//// 0% high


    public void updateCanData10F907A8(Heartbeat3 hb3) {

        long now = System.currentTimeMillis();
        lastCanUpdateTime = now;
        canPopupShown = false;
        tm.setTeleControllerAbruptOff(0);
//        double speed = (hb3.getBSG2_BYTE78() * 60.0 * tiredimensions) / 3.3125 * 1 / 1000;//1.4825
        int speed = (int)((hb3.getBSG2_BYTE78() * 60.0 * Speed_ratio_Value) / 3.3125 * 1 / 1000); // 10% high
        updateRideLock(speed);
//        double speed = (hb3.getBSG2_BYTE78() * 60.0 * 1.48331442) / 3.3125 * 1 / 1000;//5 % high
//        double speed = (hb3.getBSG2_BYTE78() * 60.0 * 1.412404) / 3.3125 * 1 / 1000;// 0% high

        rpm =hb3.getBSG2_BYTE78();
        if (tripManager == null) return;
        if(hb3.getBSG2_BYTE78()<5000){tripManager.updateTrip(hb3.getBSG2_BYTE78());}
//        Log.d("sadfdgf","THIS IS TRIPBUG    "+hb3.getBSG2_BYTE78());
        double tripKm = tripManager.getTripDistance();
        double odoKm = tripManager.getOdoDistance();
        tm.setTeleTrip((float) roundTo2Decimal(tripKm));
        tm.setTeleODO((float) roundTo2Decimal(odoKm));

        if (activity != null) {
            activity.refreshDistance();
        }
        InactivityManager.getInstance().updateSpeed(speed);

        if(speed < 150){
            try {
                tm.setTeleSpeed((int)speed);
                binding.speedNumberTV.setText(String.valueOf((int) speed));
                updateSpeedTrueAnimation((int)speed,currentComsForSpeed*-1);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            boolean shouldPlayReadySound =
                    directionalGears == 1 && speed == 0 && !isStand ;

            if (shouldPlayReadySound) {
                if (!startReasySound) {
                    startReasySound = true;
                    ReadystartBeeping();
                }
            } else {
                if (startReasySound) {
                    startReasySound = false;
                    ReadystopBeeping();
                    if (!MainActivity.isVisible && !MainActivity.isrideOSOTA) {

                        Intent intent = new Intent(activity, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                        activity.startActivity(intent);
                        activity.overridePendingTransition(R.anim.zoom_in_enter, R.anim.zoom_out_exit);

                    }
                }
            }

            boolean isMoving = directionalGears == 1 && speed >= 1;

            // Ride started transition
            if (isMoving && !wasMoving) {
                wasMoving = true;
                // stop ready beep
                if (startReasySound) {
                    startReasySound = false;
                    ReadystopBeeping();
                }
                if (activity != null) {
                    (activity).closeRecoIfOpen();
                    (activity).closeQuickSettingsIfOpen();
                }
            }

            // Vehicle stopped again
            if (!isMoving) {
                wasMoving = false;
            }
        }

        String currentMode = ""; // new
        int boostEcoSpeed=currentComsForSpeed*-1;

        if (directionalGears != 2 && !isboost) {
            resetboost=true;
//            Log.d("dsfgd","SOOOOOOboost=true");
            binding.actualboost.setVisibility(View.INVISIBLE);

            if (boostEcoSpeed <= 30 && boostEcoSpeed > 0) {
                currentMode = "eco";
                if(!isStand)binding.ecoimage.setVisibility(View.VISIBLE);
                binding.sports.setVisibility(View.GONE);
                binding.boost.setVisibility(View.GONE);
                binding.regentab.setVisibility(View.GONE);
            } else if (boostEcoSpeed > 30 && boostEcoSpeed <= 70) {
                currentMode = "city";
                binding.ecoimage.setVisibility(View.GONE);
                binding.regentab.setVisibility(View.GONE);
                if(!isStand)binding.sports.setVisibility(View.VISIBLE);
                binding.boost.setVisibility(View.GONE);
            } else if (boostEcoSpeed > 70 && boostEcoSpeed < 110) {
                currentMode = "sport";
                binding.ecoimage.setVisibility(View.GONE);
                binding.sports.setVisibility(View.GONE);
                binding.regentab.setVisibility(View.GONE);
                if(!isStand)binding.boost.setVisibility(View.VISIBLE);
            }
            else if (boostEcoSpeed < 0) {
                currentMode = "regen";
                binding.ecoimage.setVisibility(View.GONE);
                binding.sports.setVisibility(View.GONE);
                binding.boost.setVisibility(View.GONE);
                if(!isStand)binding.regentab.setVisibility(View.VISIBLE);
            }else {
                currentMode = "";
                binding.ecoimage.setVisibility(View.GONE);
                binding.sports.setVisibility(View.GONE);
                binding.boost.setVisibility(View.GONE);
                binding.regentab.setVisibility(View.GONE);
            }

            //  Play sound only on upward mode transition
//            if (
//                    (previousMode.equals("eco") && currentMode.equals("city")) ||
//                            (previousMode.equals("city") && currentMode.equals("sport"))
//            ) {
//                playModeChangeSound(); // play sound
//            }

            // Update previousMode
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


    public void updateBatteryAnimation(int batteryLevel) {
       // Log.d("BAT", "CAlledanimation");

        if (binding == null || binding.batteryImageView == null) {
          // Log.d("BAT", "UI not ready yet — skipping");
            return;
        }

        // Clamp + map to even number
        int mappedBattery = (batteryLevel > 100)
                ? 100
                : Math.max(0, (batteryLevel / 2) * 2);

//        Log.d("BAT", "RAW Level = " + batteryLevel);
//        Log.d("BAT", "Mapped Level = " + mappedBattery);
//        Log.d("BAT", "Last Level = " + lastBatteryLevel);

        // Skip duplicate values
        if (mappedBattery == lastBatteryLevel) {
            //Log.d("BAT", "SKIPPED (no change)");
            return;
        }

        // Check map
        if (MainActivity.batteryImageMap == null) {
         //   Log.e("BAT", "batteryImageMap is NULL");
            return;
        }

        if (!MainActivity.batteryImageMap.containsKey(mappedBattery)) {
          //  Log.e("BAT", "Map missing key = " + mappedBattery);
            return;
        }

        // Apply image
        int imageRes = MainActivity.batteryImageMap.get(mappedBattery);
        binding.batteryImageView.setImageResource(imageRes);

       // Log.d("BAT", "IMAGE UPDATED to key " + mappedBattery);

        lastBatteryLevel = mappedBattery;
//        MainActivity.ReloggedIn=false;
    }

//    public void updateBatteryAnimation(int batteryLevel) {
//        Log.d("BAT", "CAlledanimation");
//
//        if (binding == null || binding.batteryImageView == null) {
//            Log.d("BAT", "UI not ready yet — skipping");
//            return;
//        }
//
//        // Clamp + map to even number
//        int mappedBattery = (batteryLevel > 100)
//                ? 100
//                : Math.max(0, (batteryLevel / 2) * 2);
//
//        Log.d("BAT", "---- updateBatteryAnimation ----");
//        Log.d("BAT", "RAW Level = " + batteryLevel);
//        Log.d("BAT", "Mapped Level = " + mappedBattery);
//        Log.d("BAT", "Last Level = " + lastBatteryLevel);
//
//        // Skip duplicate values
//        if (mappedBattery == lastBatteryLevel && !MainActivity.ReloggedIn) {
//            Log.d("BAT", "SKIPPED (no change)");
//            return;
//        }
//
//        // Check map
//        if (MainActivity.batteryImageMap == null) {
//            Log.e("BAT", "batteryImageMap is NULL");
//            return;
//        }
//
//        if (!MainActivity.batteryImageMap.containsKey(mappedBattery)) {
//           Log.e("BAT", "Map missing key = " + mappedBattery);
//            return;
//        }
//
//        // Apply image
//        int imageRes = MainActivity.batteryImageMap.get(mappedBattery);
//        binding.batteryImageView.setImageResource(imageRes);
//
//        Log.d("BAT", "IMAGE UPDATED to key " + mappedBattery);
//
//        lastBatteryLevel = mappedBattery;
//        MainActivity.ReloggedIn=false;
//    }

    public void setzerobatImage(){
        binding.batteryImageView.setImageResource(R.drawable.bat_0);
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
    private static int currentComsForSpeed;
    private void updateCanData10FA07A8(Heartbeat3 hb3) {

        float current = (float) roundTo1Decimal(
                getFormattedBusCurrent(hb3.getBSG3_BYTE34()) * -1
        );
        tm.setTeleCurrentConsumption(current);

        currentComsForSpeed=(int)current;
        if(current <= 1){
            binding.RegenCurrentValue.setVisibility(View.INVISIBLE);
            binding.CurrentConsumptionValue.setText(current + "A");
            updateRegenAnimation(0);   // reset animation
            //setSpeedColor(false);
        } else {
            binding.RegenCurrentValue.setVisibility(View.VISIBLE);
            binding.RegenCurrentValue.setText(current + "A");
            updateRegenAnimation(current);  //  animation trigger
            //setSpeedColor(true);
        }
        //Log.d("fdsadg", "THIS IS REGEN CURRENT = " + current);
    }


    private void setSpeedColor(boolean isRegen) {
        if (isRegen) {
            // Green text
            binding.speedNumberTV.setTextColor(
                    ContextCompat.getColor(context, R.color.speed_regen)
            );

            // Glow effect
            binding.speedNumberTV.setShadowLayer(
                    12f,   // blur radius
                    0f,    // x offset
                    0f,    // y offset
                    ContextCompat.getColor(context, R.color.speed_regen_glow)
            );

        } else {
            // Normal text
            binding.speedNumberTV.setTextColor(
                    ContextCompat.getColor(context, R.color.speed_normal)
            );

            // Remove glow
            binding.speedNumberTV.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT);
        }
    }


    public void updateSpeedAnimation(int speed) {
        int mappedSpeed = (speed > 100) ? 101 : Math.max(0, speed); // If speed > 100, use 101 (speed_100)

        if (mappedSpeed == lastSpeed) return; // Avoid redundant updates

        if (MainActivity.speedImageMap.containsKey(mappedSpeed)) {
            binding.speedImageView.setImageResource(MainActivity.speedImageMap.get(mappedSpeed));
        }

        lastSpeed = mappedSpeed;
    }
    private ValueAnimator speedAnimator;
    private void updateSpeedTrueAnimation(int newSpeed,int currentColour) {
        binding.speedArc.setSpeed(newSpeed,currentColour);
    }
    private int lastRegen = -1;

    public void updateRegenAnimation(float current) {

        // Clamp current between 0 and 50
        int regenLevel = (int) Math.round(current);
        int mappedRegen = Math.min(50, Math.max(0, regenLevel));  // 0–50 only

        if (mappedRegen == lastRegen) return; // avoid redundant redraw

        if (MainActivity.regenImageMap.containsKey(mappedRegen)) {
            binding.regenImageView.setImageResource(
                    MainActivity.regenImageMap.get(mappedRegen)
            );
        }

        lastRegen = mappedRegen;
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
    private void updateCanData18954001(Heartbeat3 hb3) {
       // tm.setcell1TO3(hb3.getcell1TO3());
    }
    private void updateCanData1806E5F4(Heartbeat3 hb3) {
         tm.setMax_voltage_value(hb3.getTotal_max_voltage_limit());
        tm.setAlowable_charging_current_value(hb3.getAllowable_Charging_currentValue());
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
//        Log.d("UIUpdater", "updateMacVersions getMcuVersions: " + mad.getMcuVersions());
    }

    public void showCustomPopup(String title, String message) {

        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.apupopupdialog, null);

        Dialog dialog = new Dialog(activity);
        dialog.setContentView(dialogView);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        // Window styling (rounded + center)
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);

            WindowManager.LayoutParams params = window.getAttributes();
            params.width = 700;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            params.gravity = Gravity.CENTER;
            window.setAttributes(params);
        }

        // Set dynamic content
        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        TextView dialogMessage = dialogView.findViewById(R.id.dialogMessage);
        dialogTitle.setText(title);
        dialogMessage.setText(message);

        Button btnYes = dialogView.findViewById(R.id.btnYes);
        Button btnNo = dialogView.findViewById(R.id.btnNo);

        btnYes.setOnClickListener(v -> {

            btnYes.setVisibility(View.GONE);
            btnNo.setVisibility(View.GONE);

            ProgressBar progressBar =
                    dialogView.findViewById(R.id.progressBar);

            progressBar.setVisibility(View.VISIBLE);

            final BatteryPackSwitchHelper[] helper = new BatteryPackSwitchHelper[1];

            helper[0] = new BatteryPackSwitchHelper(
                            new SwitchCallback() {

                                @Override
                                public void onProgress(String message) {

                                    dialogMessage.setText(message);
                                }

                                @Override
                                public void onSuccess(String message) {

                                    progressBar.setVisibility(View.GONE);

                                    dialogMessage.setText(message);

                                    btnYes.setText("OK");

                                    btnYes.setVisibility(View.VISIBLE);

                                    btnYes.setOnClickListener(v1 -> {
                                            InactivityManager.getInstance().startTimer(1);

                                        restoreMotorState();
                                        handler.postDelayed(() -> {
                                            if (serialPortManager != null) {
                                                serialPortManager.VCUSendToMCU("FFAA550103001510FBA8073C007800B0047800");
                                            }
                                        }, 1000);
                                        handler.postDelayed(() -> {
                                            if (serialPortManager != null) {
                                                serialPortManager.VCUSendToMCU("FFAA550103001510FBA8073C007800B0047800");
                                            }
                                        }, 2000);
                                        apuPopupDialog.dismiss();
                                        apuPopupDialog = null;
                                    });
                                }

                                @Override
                                public void onFailure(String message) {

                                    progressBar.setVisibility(View.GONE);

                                    dialogMessage.setText(message);

                                    btnYes.setText("REVERT BACK");

                                    btnYes.setVisibility(View.VISIBLE);

                                    btnYes.setOnClickListener(v2 -> {

                                        progressBar.setVisibility(View.VISIBLE);

                                        btnYes.setVisibility(View.GONE);

                                        helper[0].startRevertToMainBattery();
                                    });
                                }
                            });

            helper[0].startSwitchToAPU();
        });
        btnNo.setOnClickListener(v -> {
            StopInactivtyTimerDuringPopUp = true;
//            Log.d("CustomPopup", "USER DECLINE TO SWITCH TO APU");
            if (directionalGears == 0) {
                InactivityManager.getInstance().startTimer(1);
            }
            dialog.dismiss();
            serialPortManager.VCUSendToMCU("FFAA550103001510FBA80728007800B004A000");//line current 10 A
            new Handler(Looper.getMainLooper()).postDelayed(
                    () -> serialPortManager.VCUSendToMCU("FFAA550103001510F8A807000001020E007387"),
                    1200
            );
        });

        dialog.show();
    }

    private Dialog apuPopupDialog = null;
    private final Handler handler;

    public void showCustomPopupAPUdetected(String title, String message) {

        // Prevent multiple popups
        if (apuPopupDialog != null && apuPopupDialog.isShowing()) {
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.apupopupdialog, null);

        apuPopupDialog = new Dialog(activity);
        apuPopupDialog.setContentView(dialogView);
        apuPopupDialog.setCancelable(false);
        apuPopupDialog.setCanceledOnTouchOutside(false);

        // Window styling (rounded + center)
        Window window = apuPopupDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);

            WindowManager.LayoutParams params = window.getAttributes();
            params.width = 700;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            params.gravity = Gravity.CENTER;
            window.setAttributes(params);
        }

        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        TextView dialogMessage = dialogView.findViewById(R.id.dialogMessage);
        dialogTitle.setText(title);
        dialogMessage.setText(message);

        Button btnYes = dialogView.findViewById(R.id.btnYes);
        Button btnNo = dialogView.findViewById(R.id.btnNo);

        btnYes.setOnClickListener(v -> {

            btnYes.setVisibility(View.GONE);
            btnNo.setVisibility(View.GONE);

            ProgressBar progressBar =
                    dialogView.findViewById(R.id.progressBar);

            progressBar.setVisibility(View.VISIBLE);

            final BatteryPackSwitchHelper[] helper = new BatteryPackSwitchHelper[1];

            helper[0] =
                    new BatteryPackSwitchHelper(
                            new SwitchCallback() {

                                @Override
                                public void onProgress(String message) {

                                    dialogMessage.setText(message);
                                }

                                @Override
                                public void onSuccess(String message) {

                                    progressBar.setVisibility(View.GONE);

                                    dialogMessage.setText(message);

                                    btnYes.setText("OK");

                                    btnYes.setVisibility(View.VISIBLE);

                                    btnYes.setOnClickListener(v1 -> {
                                        InactivityManager.getInstance().startTimer(1);
                                        restoreMotorState();

                                        handler.postDelayed(() -> {
                                            if (serialPortManager != null) {
                                                serialPortManager.VCUSendToMCU("FFAA550103001510FBA8073C007800B0047800");
                                            }
                                        }, 1000);
                                        handler.postDelayed(() -> {
                                            if (serialPortManager != null) {
                                                serialPortManager.VCUSendToMCU("FFAA550103001510FBA8073C007800B0047800");
                                            }
                                        }, 2000);

                                        apuPopupDialog.dismiss();

                                        apuPopupDialog = null;
                                    });
                                }

                                @Override
                                public void onFailure(String message) {

                                    progressBar.setVisibility(View.GONE);

                                    dialogMessage.setText(message);

                                    btnYes.setText("REVERT BACK");

                                    btnYes.setVisibility(View.VISIBLE);

                                    btnYes.setOnClickListener(v2 -> {

                                        progressBar.setVisibility(View.VISIBLE);

                                        btnYes.setVisibility(View.GONE);

                                        helper[0].startRevertToMainBattery();
                                    });
                                }
                            });

            helper[0].startSwitchToAPU();
        });

        btnNo.setOnClickListener(v -> {
            StopInactivtyTimerDuringPopUp = true;
//            Log.d("CustomPopup", "USER DECLINE TO SWITCH TO APU");
                InactivityManager.getInstance().startTimer(1);

            if (apuPopupDialog != null && apuPopupDialog.isShowing()) {
                restoreMotorState();
                apuPopupDialog.dismiss();
                apuPopupDialog = null;
            }
        });

        apuPopupDialog.show();
    }

    public void showAPUDetachedPopup() {

        activity.runOnUiThread(() -> {

            if (apuPopupDialog != null &&
                    apuPopupDialog.isShowing()) {
                return;
            }

            LayoutInflater inflater =
                    LayoutInflater.from(context);

            View dialogView =
                    inflater.inflate(
                            R.layout.apupopupdialog,
                            null
                    );

            apuPopupDialog = new Dialog(activity);

            apuPopupDialog.setContentView(dialogView);

            apuPopupDialog.setCancelable(false);
            apuPopupDialog.setCanceledOnTouchOutside(false);

            Window window = apuPopupDialog.getWindow();

            if (window != null) {

                window.setBackgroundDrawableResource(
                        android.R.color.transparent
                );
                WindowManager.LayoutParams params =
                        window.getAttributes();
                params.width = 700;
                params.height =
                        WindowManager.LayoutParams.WRAP_CONTENT;
                params.gravity = Gravity.CENTER;
                window.setAttributes(params);
            }

            TextView dialogTitle =
                    dialogView.findViewById(R.id.dialogTitle);

            TextView dialogMessage =
                    dialogView.findViewById(R.id.dialogMessage);

            ProgressBar progressBar =
                    dialogView.findViewById(R.id.progressBar);

            Button btnYes =
                    dialogView.findViewById(R.id.btnYes);

            Button btnNo =
                    dialogView.findViewById(R.id.btnNo);

            dialogTitle.setText("APU Detached");

            dialogMessage.setText(
                    "The auxilary battery is removed from scooter.\nRollback to main battery"
            );

            btnYes.setText("ROLLBACK");

            btnNo.setVisibility(View.GONE);

            final BatteryPackSwitchHelper[] helper =
                    new BatteryPackSwitchHelper[1];

            helper[0] =
                    new BatteryPackSwitchHelper(
                            new SwitchCallback() {

                                @Override
                                public void onProgress(String message) {

                                    dialogMessage.setText(message);
                                }

                                @Override
                                public void onSuccess(String message) {

                                    progressBar.setVisibility(View.GONE);

                                    dialogMessage.setText(message);

                                    btnYes.setText("OK");

                                    btnYes.setVisibility(View.VISIBLE);

                                    btnYes.setOnClickListener(v -> {

                                            InactivityManager.getInstance().startTimer(1);

                                        if (apuPopupDialog != null) {
                                            restoreMotorState();
                                            apuPopupDialog.dismiss();

                                            apuPopupDialog = null;
                                        }
                                    });
                                }

                                @Override
                                public void onFailure(String message) {

                                    progressBar.setVisibility(View.GONE);

                                    dialogMessage.setText(message);

                                    btnYes.setText("OK");

                                    btnYes.setVisibility(View.VISIBLE);

                                        InactivityManager.getInstance().startTimer(1);

                                    btnYes.setOnClickListener(v -> {

                                        if (apuPopupDialog != null) {
                                            restoreMotorState();
                                            apuPopupDialog.dismiss();

                                            apuPopupDialog = null;
                                        }
                                    });
                                }
                            });

            btnYes.setOnClickListener(v -> {

                btnYes.setVisibility(View.GONE);

                progressBar.setVisibility(View.VISIBLE);

                helper[0].startEmergencyMainBatteryRestore();
            });

            Log.d("APU_POPUP", "SHOWING DETACHED POPUP");

            apuPopupDialog.show();

        });
    }

    public void showCustomPopupatSOC7(String title, String message) {

        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.popupdialog, null);

        Dialog dialog = new Dialog(activity);
        dialog.setContentView(dialogView);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        // Window styling (rounded + center)
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);

            WindowManager.LayoutParams params = window.getAttributes();
            params.width = WindowManager.LayoutParams.WRAP_CONTENT;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            params.gravity = Gravity.CENTER;
            window.setAttributes(params);
        }

        // Texts
        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        TextView dialogMessage = dialogView.findViewById(R.id.dialogMessage);
        dialogTitle.setText(title);
        dialogMessage.setText(message);

        // Buttons (using same popupdialog buttons)
        Button btnYes = dialogView.findViewById(R.id.btnYes);
        Button btnNo  = dialogView.findViewById(R.id.btnNo);

        // SOC7 only needs one button → hide No
        btnNo.setVisibility(View.GONE);
        btnYes.setText("OK"); // optional label clarity

        btnYes.setOnClickListener(v -> {
            StopInactivtyTimerDuringPopUp = true;
            dialog.dismiss();
        });

        dialog.show();
    }

    private Dialog createBasePopup(View dialogView) {
        Dialog dialog = new Dialog(activity);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setContentView(dialogView);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);

            WindowManager.LayoutParams params = window.getAttributes();
            params.width = WindowManager.LayoutParams.WRAP_CONTENT;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            params.gravity = Gravity.CENTER;
            window.setAttributes(params);
        }
        return dialog;
    }

    public void onDoubleTapAction() {
        // Your logic when user double-taps screen
        if(rpm==0){
            if (activity != null) {
                (activity).closeRecoIfOpen();
                (activity).closeQuickSettingsIfOpen();
            }
            stopReverseBeep();
            stopBlinking();
            //stopSideStandSound();
//            EVLockService.getInstance().startLockSequence();
            serialPortManager = serialApplication.getSerialPortManager();
            if(serialPortManager!=null){
                serialPortManager.VCUSendToMCU("FFAA550101000A00");


                new Handler().postDelayed(() -> {
                    serialPortManager.VCUSendToMCU("FFAA550101000A00");
//                    Intent i = new Intent(context, CameraService.class);//off rec
//                    i.setAction("STOP_REC");
//                    context.startService(i);
                }, 300);

//                new Handler().postDelayed(() -> {
//                    serialPortManager.VCUSendToMCU("FFAA550107000A00");
//                }, 6000);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if(!MainActivity.MainisActive){
//                        Log.d("TAG", "LOCK6ECONDELYCODERAN delayed command: FFAA550107000A00");
                        SerialPortManager.getInstance().VCUSendToMCU("FFAA550107000A00");
                    }
                    else {
                        // Log.d("TAG", "LOCK6ECONDELYCODERAN ELSE delayed command: FFAA550107000A00");
                    }
                }, 6000);
            }
            binding.batteryArc.setBatteryLevel(0);
            LoginActivity.markScreenJustOff();
            //Log.d("InactivityHandler", "LoginActivity launched over MainActivity.");
            try {
                PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                Method goToSleep = PowerManager.class.getMethod("goToSleep", long.class);
                goToSleep.invoke(powerManager, SystemClock.uptimeMillis());
               //InactivityManager.getInstance().startTimer(2);
            } catch (Exception e) {
                e.printStackTrace();
            }
            Context context = MainActivity.instance;
            if (context != null) {
                Intent intent = new Intent(context, LoginActivity.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                context.startActivity(intent);
               // Log.d("nolockbug","CONTEXTNOTNULLLWHILEDOUBLETAP SO YES LOGIN");

            }
            else {
               // Log.d("nolockbug","CONTEXTNULLWHILEDOUBLETAP SO NO LOGIN");
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
           // Log.w("ReverseBeep", "startReverseBeep() called before initReverseBeep()");
            return;
        }

        if (!isReverseBeeping) {
            isReverseBeeping = true;
            if (reverseBeepLoaded) {
                reverseBeepHandler.post(reverseBeepRunnable);
            } else {
              //  Log.d("ReverseBeep", "Sound not yet loaded, will start after load completes");
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

    public void release() {
        //Log.d("dfsg","REALSE CALLED");
        handlerTurnSignal.removeCallbacksAndMessages(null);
        beepHandler.removeCallbacksAndMessages(null);
        warningHandler.removeCallbacksAndMessages(null);
        blinkHandler.removeCallbacksAndMessages(null);
        stopDelayHandler.removeCallbacksAndMessages(null);
//        soundHandler.removeCallbacksAndMessages(null);
        reverseBeepHandler.removeCallbacksAndMessages(null);

        stopBeeping();
        stopReverseBeep();
        ReadystopBeeping();

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

        stopApuWatcher();

    }
    private void stopApuWatcher() {
        apuWatcherRunning = false;
        apuHandler.removeCallbacksAndMessages(null);

        // Dismiss popup if activity closes
        if (apuPopupDialog != null && apuPopupDialog.isShowing()) {
            restoreMotorState();
            apuPopupDialog.dismiss();
            apuPopupDialog = null;
        }
    }
    /* ================== TPMS STATE ================== */

    private boolean frontWarning = false;
    private boolean rearWarning  = false;

    private static final double MIN_PRESSURE_PSI = 27.0;
    private static final int MAX_TEMP_C = 40;

    private MetadataManager metadata = MetadataManager.get();

    private Double frontPsi = null, frontTemp = null;
    private Double rearPsi  = null, rearTemp  = null;

    private Integer lastFrontPsi = null;
    private Integer lastRearPsi  = null;

    /* 🔊 Shared MediaPlayer (IMPORTANT) */
    private MediaPlayer tirePlayer = null;
    private boolean istireSoundPlaying = false;

    /* ================== TPMS RECEIVER ================== */
    public boolean Play_tpmsSound_on_loggin;
    private void registerTpmsReceiver(Context ctx) {

        tpmsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if (!TpmsScanService.ACTION_TPMS_UPDATE.equals(intent.getAction())) return;

                String mac = intent.getStringExtra(TpmsScanService.EXTRA_MAC);
                if (mac == null) return;

                double psiRaw = intent.getDoubleExtra(
                        TpmsScanService.EXTRA_PSI, -1
                );
                int temp = intent.getIntExtra(
                        TpmsScanService.EXTRA_TEMP, Integer.MIN_VALUE
                );

                if (psiRaw < 0 || temp == Integer.MIN_VALUE) return;

                int psi = (int) Math.round(psiRaw);

                /* ================= FRONT ================= */
                if (mac.equals(metadata.getFrontTPMS())) {

                    frontPsi  = (double) psi;
                    frontTemp = (double) temp;

                    frontWarning = (psi < MIN_PRESSURE_PSI || temp > MAX_TEMP_C);

                    //  Play sound ONLY if PSI DROPS and BELOW 30
                    if (lastFrontPsi != null) {
                        if (psi < lastFrontPsi && psi < MIN_PRESSURE_PSI) {
                            playLowTireSound();
                        }
                    }

                    lastFrontPsi = psi;
                }

                /* ================= REAR ================= */
                if (mac.equals(metadata.getRearTPMS())) {

                    rearPsi  = (double) psi;
                    rearTemp = (double) temp;

                    rearWarning = (psi < MIN_PRESSURE_PSI || temp > MAX_TEMP_C);

                    if (lastRearPsi != null) {
                        if (psi < lastRearPsi && psi < MIN_PRESSURE_PSI) {
                            playLowTireSound();
                        }
                    }
                    lastRearPsi = psi;
                }

                String tpmsPayload = buildTirePressurePayload();
                TelemetryDataManager.getInstance()
                        .setTeleTirePressure(tpmsPayload);

              //  Log.d("TPMS-UI", "TELE_TIRE_PRESSURE = " + tpmsPayload);

                /* ================= UI ================= */

                if ((frontWarning || rearWarning) && TpmsState.sensorAvailable) {
//                    binding.tpmsicon.setVisibility(View.VISIBLE);
                    binding.tpmsicon.setImageResource(R.drawable.tpmsicon);
//                    binding.lowTireWarning.setVisibility(View.VISIBLE);
                    Play_tpmsSound_on_loggin=true;
                } else {
//                    binding.tpmsicon.setVisibility(View.GONE);
                    binding.tpmsicon.setImageResource(R.drawable.tpmsicon_greyout);
//                    binding.lowTireWarning.setVisibility(View.INVISIBLE);
                    Play_tpmsSound_on_loggin=false;
                }
            }
        };

        IntentFilter filter =
                new IntentFilter(TpmsScanService.ACTION_TPMS_UPDATE);

        LocalBroadcastManager
                .getInstance(ctx)
                .registerReceiver(tpmsReceiver, filter);

       // Log.d("TPMS-UI", "TPMS receiver registered");
    }

    /* ================== SOUND PLAYER ================== */

    public void playLowTireSound() {

        if (istireSoundPlaying) return;
        if(!MainActivity.MainisActive) return;

        if (tirePlayer == null) {
            tirePlayer = MediaPlayer.create(
                    getInstance().context,
                    R.raw.low_tire_alert_indian_v1
            );

            if (tirePlayer == null) return;

            tirePlayer.setOnCompletionListener(mp -> {
                istireSoundPlaying = false;
                mp.seekTo(0);
            });
        }

        try {
            istireSoundPlaying = true;
            tirePlayer.start();
        } catch (IllegalStateException e) {
            istireSoundPlaying = false;
        }
    }

    /* ================== PAYLOAD ================== */

    private String buildTirePressurePayload() {

        StringBuilder sb = new StringBuilder();

        if (frontPsi != null && frontTemp != null) {
            sb.append("F")
                    .append(String.format("%02d", Math.round(frontPsi)))
                    .append(String.format("%02d", frontTemp.intValue()));
        }

        if (rearPsi != null && rearTemp != null) {
            sb.append("R")
                    .append(String.format("%02d", Math.round(rearPsi)))
                    .append(String.format("%02d", rearTemp.intValue()));
        }

        return sb.length() == 0 ? "NA" : sb.toString();
    }

    /* ================== CLEANUP ================== */

    public void cleanup(Context ctx) {

        if (tpmsReceiver != null) {
            LocalBroadcastManager
                    .getInstance(ctx)
                    .unregisterReceiver(tpmsReceiver);
            tpmsReceiver = null;
        }

        if (tirePlayer != null) {
            tirePlayer.release();
            tirePlayer = null;
        }
    }

    public void updateTransportModeWarning() {

        SharedPreferences prefs = context.getSharedPreferences("feature_prefs", Context.MODE_PRIVATE);
        boolean isTransportModeOn = !prefs.getBoolean("handle_lock_enabled", true);

        //  PRIORITY 1 → TEMP (highest)
        if (isTempWarningActive) {
            binding.transportModeYellowWarning.setVisibility(View.GONE);
            return;
        }
        if (Lowbatflag) {
            binding.transportModeYellowWarning.setVisibility(View.GONE);
            return;
        }
        if (!isTransportModeOn) {
            binding.transportModeYellowWarning.setVisibility(View.VISIBLE);
        } else {
            binding.transportModeYellowWarning.setVisibility(View.GONE);
        }
    }
    private void restoreMotorState() {//After APU swicthing ON LED

        if (serialPortManager != null) {
            serialPortManager.VCUSendToMCU(
                    "FFAA550107000A01"
            );
            handler.postDelayed(() -> {
                serialPortManager.VCUSendToMCU("FFAA550107000A01");
                }, 500);
        }
    }
    private void updateRideLock(int rpm) {

        if (rpm > 0 && !touchBlocked) {

            touchBlocked = true;

            activity.enableTouchBlocker();

            Log.d("RIDE_LOCK", "Touch disabled");
        }

        else if (rpm == 0 && touchBlocked) {

            touchBlocked = false;

            activity.disableTouchBlocker();

            Log.d("RIDE_LOCK", "Touch enabled");
        }
    }
}