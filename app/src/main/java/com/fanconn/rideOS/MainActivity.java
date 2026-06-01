package com.fanconn.rideOS;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;
import android.Manifest;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.InputType;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.style.ForegroundColorSpan;
import android.text.style.MetricAffectingSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.fanconn.rideOS.authen.LoginActivity;
import com.fanconn.rideOS.databinding.ActivityMainBinding;
import com.fanconn.rideOS.mcuupdate.FileDownloader;
import com.fanconn.rideOS.mcuupdate.YMode;
import com.fanconn.rideOS.menu.menu;
import com.fanconn.rideOS.network.EVWebSocketClient;
import com.fanconn.rideOS.network.ApiService;
//import com.fanconn.rideOS.network.ScooterMqttManager;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;
import tp.xmaihh.serialport.BuildConfig;
import java.text.NumberFormat;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements FrameDataListener {
    public  SerialPortManager serialPortManager;  // 串口管理类
    private ActivityMainBinding binding; // 自动生成的绑定类实例，用于访问布局文件中的视图组件
    private String sPort = "/dev/ttyHS1";  // 串口地址
    private int iBaudRate = 115200;  // 波特率
    private int type_Hex = 0;  // Hex 类型的数据格式
    private int type_Txt = 1;  // Txt 类型的数据格式
    private int type_byte = 2;  // byte 类型的数据格式
    private int RX_type = type_Hex;  // 数据格式类型
    private String rxText = "";  // 接收到的数据
    private String version_URL = "http://ota.rivotmotors.com/OTA/sc126/ElectricBike/version.json";
    private String saveDir = "/sdcard";
    public static UIUpdater uiUpdater;
    private boolean isSIMCardExist = false;
    private boolean isSDCardExist = false;
    //    private CameraHandler cameraHandler;
//    private MusicPlayer musicPlayer;  // 音乐播放器
    private int cameraState = 0; // 0: 无预览, 1: 后摄预览, 2: 前摄预览
    private int loopState = 0; // 0: 全部循环, 1: 单曲循环, 2: 随机播放
    private File mFirmwareFile;
    private YMode yMode;
    private ProgressDialog progressDialog;
    private PopupWindow popupWindow;
    public static Switch frontLockSW, seatCushionLockSW;
    public static TextView frontLockSWSTV, seatCushionLockSWSTV, neutralGear, driveGear, reverseGear, speedThreshold1, speedThreshold2, speedThreshold3;
    static HashMap<Integer, Integer> speedImageMap = new HashMap<>();
    static HashMap<Integer, Integer> batteryImageMap = new HashMap<>();
    static HashMap<Integer, Integer> introbatteryImageMap = new HashMap<>();

    public static HashMap<Integer, Integer> regenImageMap = new HashMap<>();

    static HashMap<Integer, Integer> APUbatteryImageMap = new HashMap<>();
    private int lastBatteryLevel = -1; // Store last battery level to avoid redundant updates
    private int lastSpeed = -1;
    // 处理接收到的数据的 Handler
//    public Handler mHandler = new Handler(new Handler.Callback() {
//        @Override
//        public boolean handleMessage(Message msg) {
//            ComBean comBean = (ComBean) msg.obj;
//
//            rxText = SerialPortManager.processReceivedData(comBean, RX_type);
//            //Log.e("handleMessage", "原始串口数据: " + rxText);
//
//            mHandler.postDelayed(new Runnable() { // 延迟处理数据，避免频繁操作导致 CPU 占用率过高
//                @Override
//                public void run() {
//                    List<String> frames = SerialPortManager.splitIntoFrames(rxText);
//                    for (String frame : frames) {
//                        if (frame.startsWith("FFAA660101"))
//                            Log.d("DATA FRAME", " frame " + frame);
//
////                    else if (frame.startsWith("FFAA770303001504068")){Log.d("DATA FRAME", "New BMS  " + frame);}
////
////                    else if ( frame.startsWith("FFAA770303001518934001") || frame.startsWith("FFAA770303001518914001") ) {
////                            Log.d("DATA FRAME", "OLD BMS  " + frame);
////                        }
//
//                        parseFrame(frame); // 根据帧头处理不同类型的数据
//                    }
//
//                    rxText = "";   // 清空 rxText，避免数据过大
//                }
//            }, 10);  // 每 10 毫秒处理一次数据
//
//            return false;
//        }
//    });

    private final BroadcastReceiver frameReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String frame = intent.getStringExtra("frame_data");
//            if(frame.startsWith("FFAA77030300151890"))
                Log.d("FRAME", "Received FRAME 6666666 " + frame);
            //Log.d("kdfsg","SERIAL FRAME : "+frame);
//            if(frame.length()>22){
//            if(frame.startsWith("FFAA77030300151806E5"))
//                Log.d("FRAME", "Received inNNNNNNNN " + getClass().getSimpleName() + ": " + frame);
////            }

            // Do something with frame
            parseFrame(frame);
        }
    };

    private static final String TAG = "MainActivity";
    private static final String PREFS_NAME = "AppPreferences";
    private static final String PREF_IS_UPDATED = "isUpdated";
    private ApiService apiService;
    final private String VINNUMBER = MetadataManager.get().getVin();//"ME9NC1202F1172002";//ME9NC1202F1172001
    static EVWebSocketClient evClient;
    ImageButton mapbtn,menubtn;//,lowtire_pressure_button;
    TripDistanceManager tripManager;
    GestureDetector gestureDetector;
    private Handler delayedUnlockHandler  = new Handler(Looper.getMainLooper());
    private Runnable delayedUnlockRunnable;
    public static MainActivity instance;
    private boolean isStatusReceiverRegistered = false;
    private boolean isRecording = false;
    private ImageButton tripResetBtn;
    private TextView tripTextView;
    private boolean showingTrip = false; // false = Odo, true = Trip
    private Handler mHandlerTime = new Handler();
    private Runnable mRunnableTime;  // 用于定时更新系统时间显示的任务
    ImageView keyImage;
    QuickSettingsView qsView;
    public static String VIN_On_CAN;
    public static char currentgear;
    private VelocityTracker velocityTracker;
    private SpeedArcView speedArc;
    private View dot1, dot2;//, dot3, dot4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); //
        serialPortManager = serialApplication.getSerialPortManager();
        IntentFilter filter = new IntentFilter("FRAME_RECEIVED");
        LocalBroadcastManager.getInstance(this).registerReceiver(frameReceiver, new IntentFilter("FRAME_RECEIVED"));

//        serialPortManager.initSerialConfig(this);
        //Checking whteher logged in or No or else return back to login window
        SharedPreferences prefs = null;
        try {
            prefs = EncryptedSharedPreferences.create(
                    "user_data",
                    MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
                    getApplicationContext(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Failed to initialize EncryptedSharedPreferences", e);
        }

        boolean isLoggedIn = prefs.getBoolean("is_logged_in", false);
        if (!isLoggedIn) {
            startActivity(new Intent(this, LoginActivity.class));
            setNeutralGear();
            LockHandle();
            finish();
            return;
        }
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
        }
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        TemperatureLogger.logImportantThermals();

        setFullScreen();
        dot1 = findViewById(R.id.dot1);
        dot2 = findViewById(R.id.dot2);
//        dot3 = findViewById(R.id.dot3);
//        dot4 = findViewById(R.id.dot4);

        statsValueText = findViewById(R.id.statsValueText);
        statsLabelText = findViewById(R.id.statsLabelText);
        statsIcon = findViewById(R.id.statsIcon);
//        inactivityManager = new InactivityManager(this);
//        inactivityManager.stopTimer();
        // 初始化class

        instance = this;
        startService(new Intent(this, WatchdogService.class));//wacthdog service




        IntentFilter lockbtnpressedofsettingclass = new IntentFilter("com.ev.LOCK");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(lockReceiver, lockbtnpressedofsettingclass, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(lockReceiver, lockbtnpressedofsettingclass);
        }
//        menu menu = new menu(this); // 'this' refers to MainActivity
        uiUpdater = new UIUpdater(binding, this);


//        ScreenController.init(serialPortManager);///passing serialPoertManager to ScreenController Class
//        cameraHandler = new CameraHandler(binding, this);
//        musicPlayer = new MusicPlayer(binding, this);

        yMode = new YMode(this);
        //setGearState("D");        // 获取蓝牙MAC地址和 mcu ota版本
        serialPortManager.VCUSendToMCU("FFAA550200000A01"); // 获取蓝牙 MAC 地址
        serialPortManager.VCUSendToMCU("FFAA550204000A01"); // 获取 MCU 版本信息
//        serialPortManager.getHb3().setLEDresponse(0);
//        serialPortManager.getHb3().sethandleLockresponse(0);
        if (delayedUnlockRunnable != null) {
            delayedUnlockHandler.removeCallbacks(delayedUnlockRunnable);
        }

        TextView tv = findViewById(R.id.Forward_instruction);

        SpannableString s = new SpannableString("Apply brake & press F to start the ride");

        int b1 = s.toString().indexOf("brake");
        s.setSpan(new StyleSpan(Typeface.BOLD), b1, b1+5, 0);
        s.setSpan(new ForegroundColorSpan(Color.parseColor("#06FFD1")), b1, b1+5, 0);
        s.setSpan(new MetricAffectingSpan() {
            @Override
            public void updateDrawState(TextPaint tp) {
                tp.setFakeBoldText(true);   // EXTRA THICK
            }

            @Override
            public void updateMeasureState(TextPaint tp) {
                tp.setFakeBoldText(true);
            }
        }, b1, b1+5, 0);

        int f1 = s.toString().indexOf("F");
        s.setSpan(new StyleSpan(Typeface.BOLD), f1, f1+1, 0);
        s.setSpan(new ForegroundColorSpan(Color.parseColor("#06FFD1")), f1, f1+1, 0);
        s.setSpan(new MetricAffectingSpan() {
            @Override
            public void updateDrawState(TextPaint tp) {
                tp.setFakeBoldText(true);
            }

            @Override
            public void updateMeasureState(TextPaint tp) {
                tp.setFakeBoldText(true);
            }
        }, f1, f1+1, 0);

        tv.setText(s);
        acquisitionTime();

        //animateBatteryLevelOnce();
        //animateSpeedLevelOnce();
//        InactivityManager.getInstance().setCallback(this);

        SpeedAndSOCANimation();         //Feature Startups
        IntroBatteryAnimation();
        OTAUpdateOFApp();               //Feature Startups
        //Telemetry();                    //Feature Startups
//        uiUpdater.startTelemetry(VINNUMBER);///START TELEMETRY

        //This is what start sending telemetry data
//        if(!MetadataManager.get().getVin().startsWith("UNKNOWN_VIN"))
//        {
//            Log.d("MainActivity", " Starting Telemetry for VIN: " + VINNUMBER);
//            Intent intent = new Intent(this, TelemetryService.class);
//            intent.putExtra("VIN_NUMBER", VINNUMBER);
//            startService(intent);
//        }

        Intent tpmsIntent = new Intent(this, TpmsScanService.class);
        startService(tpmsIntent);//TPMS SERVICE

        //startRequestCycle();
        //bmsHandler.post(bmsRunnable);
//       requestbms();
//       Feature Startups
        bmsHandler.post(bmsRunnable);

        mapbtn  = findViewById(R.id.mapbtn);
        menubtn = findViewById(R.id.menubtn);
//        lowtire_pressure_button= findViewById(R.id.low_tire_warning);
        mapbtn.setOnTouchListener((v, event) -> {

            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:

                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);

                    v.setScaleX(0.94f);
                    v.setScaleY(0.94f);
                    v.setAlpha(0.85f);

                    return false;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:

                    v.setScaleX(1f);
                    v.setScaleY(1f);
                    v.setAlpha(1f);

                    return false;
            }

            return false;
        });

        mapbtn.setOnClickListener(v -> {

            if (serialPortManager != null &&
                    serialPortManager.getHb3().getBSG2_BYTE78() < 10) {

                Intent intent =
                        new Intent(MainActivity.this, Maps.class);

                startActivity(intent);

                overridePendingTransition(
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                );
            }
        });
        menubtn.setOnTouchListener((v, event) -> {

            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:

                    // Instant tactile feedback
                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);

                    // Quick visual feedback
                    v.setScaleX(0.94f);
                    v.setScaleY(0.94f);
                    v.setAlpha(0.85f);

                    return false;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:

                    // Restore instantly
                    v.setScaleX(1f);
                    v.setScaleY(1f);
                    v.setAlpha(1f);

                    return false;
            }

            return false;
        });

        menubtn.setOnClickListener(v -> {

            if (serialPortManager != null &&
                    serialPortManager.getHb3().getBSG2_BYTE78() < 10) {

                serialPortManager.VCUSendToMCU("FFAA550107000A01");

                Intent intent =
                        new Intent(MainActivity.this, menu.class);

                startActivity(intent);

                overridePendingTransition(
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                );
            }
        });

//        lowtire_pressure_button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(MainActivity.this, TPMS_activity.class);
//                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
//                startActivity(intent);
//            }
//        });
        gestureDetector = new GestureDetector(this, new GestureListener());

        // 1️ Create QuickSettingsView
        qsView = new QuickSettingsView(this);

        // 2️ HIDE IT IMMEDIATELY (IMPORTANT)
        qsView.setTranslationY(-10000f);   // push off-screen BEFORE draw

        // 3️ Add to root layout
        FrameLayout root = findViewById(R.id.root);
        root.addView(qsView);
        qsView.bringToFront();

        // 4️ After layout, set correct hidden position
        qsView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        qsView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                        qsHeight = qsView.getHeight();

                        // Final hidden position (top, off-screen)
                        qsView.setTranslationY(-qsHeight);
                    }
                });

        tripTextView = binding.tripTextView;
        tripResetBtn = binding.tripresetbtn;
        updateDistanceDisplay();


        tripManager = new TripDistanceManager(this);
        uiUpdater.setTripManager(tripManager);

        setupDoubleTapDetector();
        keyImage = findViewById(R.id.keyOutImage); // reference to the image

        //===wifi dynamic img ======

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // Initial icon update
        updateWifiIcon(wifiManager);
        updateBluetoothIcon();
        updateSignalIcon();

        // Register receiver for updates
        IntentFilter signalfilter = new IntentFilter();
        signalfilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        signalfilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        signalfilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        signalfilter.addAction("android.intent.action.SERVICE_STATE");
        signalfilter.addAction("android.intent.action.ANY_DATA_STATE");
        registerReceiver(statusReceiver, signalfilter);
        getFreshLatLong(this);
        isStatusReceiverRegistered = true;

//        commandHandler = new CommandHandler((commandType, value, clientId) -> {
//            Log.d("CMD_MAIN", "Command Received: " + commandType + " = " + value);
//
//            // TODO: UART Send here
//            // serialPortManager.sendCommandToMcu(commandType, value);
//
//            sendAckToServer("WHITEBOARD", commandType, value, "success", clientId);
//        });

        //initMqtt();
        binding.recoContainer.setOnClickListener(v -> showRecoInfo());
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(
                new FragmentManager.FragmentLifecycleCallbacks() {
                    @Override
                    public void onFragmentDestroyed(@NonNull FragmentManager fm,
                                                    @NonNull Fragment f) {
                        if (f instanceof RecoInfoFragment) {
                            View overlay = findViewById(R.id.overlayContainer);
                            if (overlay != null) {
                                overlay.setVisibility(View.GONE);
                            }
                        }
                    }
                }, false);

        initRegenImages();

        setVinOnCanLast8Safe(VINNUMBER);

        speedArc = findViewById(R.id.speedArc);



        if (!MetadataManager.get().isMetadataSynced()) {

            startActivity(new Intent(this, MetadataActivity.class));
            finish();
            return;
        }
        //startTestAnimation();
    }//END OF ONCREATE METHOD
    private void startTestAnimation() {

        ValueAnimator animator = ValueAnimator.ofFloat(0, 100);
        animator.setDuration(3000); // 3 seconds

        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);

        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            speedArc.setSpeed(value,0);
        });

        animator.start();
    }
    private void initRegenImages() {
        for (int i = 0; i <= 50; i++) {
            int resId = getResources().getIdentifier(
                    "regen" + i,
                    "drawable",
                    getPackageName()
            );
            if (resId != 0) {
                MainActivity.regenImageMap.put(i, resId);
            }
        }
    }

    public static void setVinOnCanLast8Safe(String vin) {

        if (vin == null) {
            vin = "";
        }

        String data;

        if (vin.length() >= 8) {
            // Normal case → last 8
            data = vin.substring(vin.length() - 8);
        } else {
            // Test case → pad with '0'
            StringBuilder sb = new StringBuilder();
            int pad = 8 - vin.length();

            for (int i = 0; i < pad; i++) {
                sb.append('0');   // padding
            }
            sb.append(vin);
            data = sb.toString();
        }

        // ASCII to bytes
        byte[] payload = new byte[8];
        for (int i = 0; i < 8; i++) {
            payload[i] = (byte) data.charAt(i);
        }

        // Bytes → HEX (no spaces)
        StringBuilder hex = new StringBuilder();
        for (byte b : payload) {
            hex.append(String.format("%02X", b));
        }

        VIN_On_CAN = hex.toString();
    }
    //private CommandHandler commandHandler;
    private void forceStopSelfAndLaunchOther() {
        String myPkg = getPackageName();
        String targetPkg = "com.fanconn.rideOSfw";

        try {
            Intent intent = getPackageManager().getLaunchIntentForPackage(targetPkg);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
            Runtime.getRuntime().exec(new String[]{
                    "sh", "-c", "am force-stop " + myPkg
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("MissingPermission")
    private void getFreshLatLong(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (lastLocation != null) {
           // Log.d("GPS", "Last Known Location: " + lastLocation.getLatitude() + ", " + lastLocation.getLongitude());
        }

        // Fresh update
        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
              //  Log.d("GPS", "Fresh Location: " + latitude + ", " + longitude);

                // Stop updates after first fix
                locationManager.removeUpdates(this);
            }

            @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
            @Override public void onProviderEnabled(String provider) {}
            @Override public void onProviderDisabled(String provider) {}
        };

        // Request updates every 1 sec, 0 meters
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, listener);
    }

    private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            updateWifiIcon(wifiManager);
            updateBluetoothIcon();
            updateSignalIcon();
        }
    };


    private void updateWifiIcon(WifiManager wifiManager) {
        ImageView wifiIcon = findViewById(R.id.wifi);

        if (wifiManager == null || wifiIcon == null) return;

        if (!wifiManager.isWifiEnabled()) {
            wifiIcon.setImageResource(R.drawable.wifioffimg);
        } else {
            WifiInfo info = wifiManager.getConnectionInfo();
            if (info != null && info.getNetworkId() != -1 && !"<unknown ssid>".equals(info.getSSID())) {
                wifiIcon.setImageResource(R.drawable.wifionimg);
            } else {
                wifiIcon.setImageResource(R.drawable.wifiidleimg);
            }
        }
    }


    private void updateBluetoothIcon() {
        ImageView btIcon = findViewById(R.id.Bluetooth_IV); // Update your ImageView id
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        if (btIcon != null && adapter != null) {
            if (adapter.isEnabled()) {
                btIcon.setImageResource(R.drawable.bluetoothonimg);
            } else {
                btIcon.setImageResource(R.drawable.bluetoothoffimg);
            }
        }
    }

    private void updateSignalIcon() {
        ImageView signalIcon = findViewById(R.id.network); // Your ImageView ID
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        if (tm == null || signalIcon == null) return;

        // Check for required permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.ACCESS_FINE_LOCATION},
                    1001);
            return;
        }

        int dataState = tm.getDataState(); // Detect if mobile data is ON
        ServiceState serviceState = tm.getServiceState();
        SignalStrength signalStrength = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            signalStrength = tm.getSignalStrength();
        }

        // Handle SIM not present or out of service or signal unavailable
        if (serviceState == null || serviceState.getState() != ServiceState.STATE_IN_SERVICE || signalStrength == null) {
            signalIcon.setImageResource(R.drawable.signalabsentimg);
            return;
        }

        int level = signalStrength.getLevel();

        if (level <= 1) {
            signalIcon.setImageResource(R.drawable.signalweakimg);
        } else if (dataState == TelephonyManager.DATA_CONNECTED) {
            signalIcon.setImageResource(R.drawable.signaldataimg);
        } else {
            signalIcon.setImageResource(R.drawable.signalstrongimg);
        }
    }


    //===================================================================================

    private void setupDoubleTapDetector() {
        View rootView = getWindow().getDecorView().getRootView();
        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                runOnDoubleTap();
                return true;
            }
        });

        rootView.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
    }

    public void runOnDoubleTap() {
        if (uiUpdater != null) {
            uiUpdater.onDoubleTapAction(); // Delegate to UIUpdater
        }
    }
    private void showPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Only Admin");

        final EditText input = new EditText(MainActivity.this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String enteredPassword = input.getText().toString();
            if (enteredPassword.equals("tovir")) {
                InactivityManager.getInstance().stopTimer();
                serialPortManager.VCUSendToMCU("FFAA550107000A01");

//                Intent intent = new Intent(MainActivity.this, menu.class);
//                startActivity(intent);
            } else {
                Toast.makeText(MainActivity.this, "Incorrect password", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private final BroadcastReceiver lockReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish(); // Finish MainActivity when LOCK is triggered
        }
    };

    public static boolean batteryAnimationCompleted = false;
    public static boolean speedAnimationCompleted = false;

    private void animateBatteryLevelOnce() {

        runOnUiThread(() -> {
        //Log.d("dasfdsgf","ENTERoCNEANIMA");
            final Handler handler = new Handler(Looper.getMainLooper());
            final int[] level = {0};
            final boolean[] increasing = {true};

            batteryAnimationCompleted = false;

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //Log.d("dasfdsgf","ENTERoCNEANIMA111");

                    int mappedBattery = (level[0] / 10) * 10;
                    if (MainActivity.introbatteryImageMap.containsKey(mappedBattery)) {
                        binding.introbatteryImageView.setImageResource(MainActivity.introbatteryImageMap.get(mappedBattery));
                    }

                    if (increasing[0]) {
                        level[0] += 10;
                        if (level[0] >= 100) {
                            increasing[0] = false;
                        }
                    } else {
                        level[0] -= 10;
                        if (level[0] < 0) {
                            handler.postDelayed(() -> {
                                ReloggedIn=true;
//                                uiUpdater.updateBatteryAnimation((int)AhTrackerManager.getInstance(instance).getSOC());
                                if(serialPortManager.getHb3()!=null){
                                    uiUpdater.updateBatteryAnimation((int)serialPortManager.getHb3().getBatteryLevel() /10);
                                }
                            }, 1000);
                            handler.postDelayed(() -> {
                                batteryAnimationCompleted = true;
                            }, 1100);
                            return;
                        }
                    }
                    handler.postDelayed(this, 60);
                }
            }, 50);
        });
    }


//    private void animateSpeedLevelOnce() {
//        final Handler handler = new Handler();
//        final int[] level = {0};
//        final boolean[] increasing = {true};
//
//        speedAnimationCompleted = false; // Optional flag if you want to track
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
//                        speedAnimationCompleted = true; // ✅ Animation done
//                        return;
//                    }
//                }
//
//                handler.postDelayed(this, 60); // Control animation speed
//            }
//        }, 50);
//    }

    public void SpeedAndSOCANimation() {
        for (int i = 0; i <= 100; i++) {
            int resId = getResources().getIdentifier("speed_" + i, "drawable", getPackageName());
            if (resId != 0) {
                speedImageMap.put(i, resId);
            }
        }
        speedImageMap.put(101, getResources().getIdentifier("speed_100", "drawable", getPackageName()));


//BAttery Animation

        for (int i = 0; i <= 100; i += 2) {
            int resId = getResources().getIdentifier("bat_" + i, "drawable", getPackageName());
            if (resId != 0) {
                batteryImageMap.put(i, resId);
            }
        }
        batteryImageMap.put(101, getResources().getIdentifier("bat_100", "drawable", getPackageName()));

        for (int i = 0; i <= 100; i += 2) {
            int resId = getResources().getIdentifier("apubat_" + i, "drawable", getPackageName());
            if (resId != 0) {
                APUbatteryImageMap.put(i, resId);
            }
        }
        APUbatteryImageMap.put(101, getResources().getIdentifier("apubat_100", "drawable", getPackageName()));
    }

    public void IntroBatteryAnimation() {

        for (int i = 0; i <= 100; i += 2) {
            int resId = getResources().getIdentifier("bati_" + i, "drawable", getPackageName());
            if (resId != 0) {
                introbatteryImageMap.put(i, resId);
            }
        }
        introbatteryImageMap.put(101, getResources().getIdentifier("bati_100", "drawable", getPackageName()));

    }

    public void OTAUpdateOFApp() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isUpdated = prefs.getBoolean(PREF_IS_UPDATED, false);

        if (!isUpdated) {
           // Log.d(TAG, "App not updated, checking for updates...");
            startUpdateService();
        } else {
           // Log.d(TAG, "App is already updated.");
        }
    }
    private void startUpdateService() {
        // Start the UpdateService to check and prompt the user for an update
        Intent intent = new Intent(this, UpdateService.class);
        startService(intent);
    }

    public void navLeft(View view) {
        showingTrip = !showingTrip;
        updateDistanceDisplay();
    }

    // Right button click
    public void navRight(View view) {
        showingTrip = !showingTrip;
        updateDistanceDisplay();
    }
    private void updateDistanceDisplay() {
        if (uiUpdater == null || uiUpdater.tripManager == null) return;

        double value = showingTrip
                ? uiUpdater.tripManager.getTripDistance()
                : uiUpdater.tripManager.getOdoDistance();

        String label = showingTrip ? "Trip" : "Odo.";
        tripTextView.setText(String.format("%s %.2f km", label, value));

        if(serialPortManager!=null && extractBits(serialPortManager.getHb3().getBSG1_BYTE1(), 0, 1)==1){
            tripResetBtn.setVisibility(showingTrip ? View.VISIBLE : View.GONE);
        }
    }
    public void refreshDistance() {
        runOnUiThread(this::updateDistanceDisplay);
    }

    // Trip Reset click handler
    public void Trip_Reset(View view) {
        if (tripManager != null) {
            uiUpdater.tripManager.resetTrip();
            updateDistanceDisplay(); // refresh after reset
        }
    }

    //petrol meter
    private TextView statsTextView;
    private int statsIndex = 0; // 0 to 3
    private TextView statsValueText;
    private TextView statsLabelText;
    private ImageView statsIcon;
    public void statsNavLeft(View view) {
        statsIndex--;
        if (statsIndex < 0) statsIndex = 1;
        updateStatsDisplay();
        updateStatsDots();
    }

    public void statsNavRight(View view) {
        statsIndex++;
        if (statsIndex > 1) statsIndex = 0;
        updateStatsDisplay();
        updateStatsDots();
    }
    private void updateStatsDisplay() {

        if (uiUpdater == null || uiUpdater.tripManager == null) return;

        double odo = uiUpdater.tripManager.getOdoDistance();
        double regenKm = UIUpdater.getInstance().getTotalRecoKm();

        switch (statsIndex) {

            // ================= ODO =================
            case 1:

                statsIcon.setImageResource(R.drawable.odo_icon);

                statsValueText.setText(
                        String.format("%.1f km", odo)
                );

                statsLabelText.setText("Odometer");

                break;


            // ================= PETROL SAVED =================
//            case 1:
//
//                double litres =
//                        PetrolCostManager.getPetrolSavedLitres(odo);
//
//                statsIcon.setImageResource(R.drawable.ic_money_saved);
//
//                statsValueText.setText(
//                        String.format("%.2f L", litres)
//                );
//
//                statsLabelText.setText("Petrol saved");
//
//                break;
//
//
//            // ================= CHARGING COST =================
//            case 2:
//
//                double cost =
//                        PetrolCostManager.getChargingCost(odo, regenKm);
//
//                statsIcon.setImageResource(R.drawable.ic_money_saved);
//
//                statsValueText.setText(
//                        String.format("₹ %.2f", cost)
//                );
//
//                statsLabelText.setText("nx100 charging cost");
//
//                break;


            // ================= MONEY SAVED =================
            case 0:

                double saved = PetrolCostManager.getMoneySaved(odo, regenKm);
                statsIcon.setImageResource(R.drawable.ic_money_saved);
                statsValueText.setText(
                        "₹" + formatMoney(saved)
                );
                statsLabelText.setText("Saved");
                break;
        }
    }

    private String formatMoney(double amount) {

        NumberFormat formatter =
                NumberFormat.getNumberInstance(new Locale("en", "IN"));

        formatter.setMaximumFractionDigits(0);
        formatter.setMinimumFractionDigits(0);

        return formatter.format(amount);
    }
    private void updateStatsDots() {

        dot1.setBackgroundResource(R.drawable.dot_unselected);
        dot2.setBackgroundResource(R.drawable.dot_unselected);
//        dot3.setBackgroundResource(R.drawable.dot_unselected);
//        dot4.setBackgroundResource(R.drawable.dot_unselected);

        switch (statsIndex) {
            case 0: dot1.setBackgroundResource(R.drawable.dot_selected); break;
            case 1: dot2.setBackgroundResource(R.drawable.dot_selected); break;
//            case 2: dot3.setBackgroundResource(R.drawable.dot_selected); break;
//            case 3: dot4.setBackgroundResource(R.drawable.dot_selected); break;
        }
    }

    public void refreshStats() {
        runOnUiThread(this::updateStatsDisplay);
    }

///reco logic
public void recoNavLeft(View view) {
    UIUpdater.getInstance().toggleRecoMode();
}
    public void recoNavRight(View view) {
        UIUpdater.getInstance().toggleRecoMode();
    }

    private void parseFrame(String frame) {
        serialPortManager.parseDataFrames(frame, MainActivity.this);
    }

    @Override
    public void onHeartbeat1Parsed(Heartbeat1 hb1) {
        // --- Update UI or perform other actions ---
        //Log.d("MainActivity", "Heartbeat1 parsed: " + hb1.toString());

        // 更新解析出来的 Heartbeat1 串口数据到UI组件中
        uiUpdater.updateHeartbeat1UI(hb1);
    }

    @Override
    public void onHeartbeat2Parsed(Heartbeat2 hb2) {
        // --- Update UI or perform other actions ---
        //Log.d("MainActivity", "Heartbeat2 parsed: " + hb2.toString());

        // 更新解析出来的 Heartbeat2 串口数据到UI组件中
        uiUpdater.updateHeartbeat2UI(hb2);
    }

    @Override
    public void onHeartbeat3Parsed(Heartbeat3 hb3) {
        // --- Update UI or perform other actions ---
        //Log.d("MainActivity", "Heartbeat3 parsed: " + hb3.toString());

        // 更新解析出来的 Heartbeat3 串口数据到UI组件中
        uiUpdater.updateHeartbeat3UI(hb3);
    }

    @Override
    public void onMcuAnswerDataParsed(McuAnswerData mad) {
        // --- Update UI or perform other actions ---

        //Log.d("MainActivity", "McuAnswerData parsed: " + mad.toString());
        uiUpdater.updateMcuAnswerDataUI(mad);
    }

    @Override
    public void onCanTimeout() {
        runOnUiThread(() -> {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Warning")
                    .setMessage("Controller switched off abruptly or CAN line issue")
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .setCancelable(false)
                    .show();
        });
    }

    private class FetchVersionTask extends AsyncTask<String, Void, Integer> {

        @Override
        protected Integer doInBackground(String... urls) {
            try {
                return FileDownloader.fetchVersion(urls[0]);
            } catch (IOException e) {
                e.printStackTrace();
                return -1;
            }
        }

        @Override
        protected void onPostExecute(Integer mcuOtaVer_new) {
            int mcuOtaVer_local = serialPortManager.mad.getMcuVersions();
          //  Log.d("UIUpdater", "updateMacVersions mcuOtaVer_local: " + mcuOtaVer_local);
         //   Log.d("UIUpdater", "updateMacVersions mcuOtaVer_new: " + mcuOtaVer_new);
            if (mcuOtaVer_new != -1) {
                if (mcuOtaVer_new > mcuOtaVer_local) {
                    showUpdateDialog();
                } else {
                    Toast.makeText(MainActivity.this, R.string.tips_mcuota_latest_version, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(MainActivity.this, R.string.tips_mcuota_file_fetch_version, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showUpdateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(getString(R.string.tips_mcuota_update))
                .setMessage(getString(R.string.tips_mcuota_update_content))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(getString(R.string.tips_mcuota_confirm), (dialog, which) -> startFirmwareDownload())
                .setNegativeButton(getString(R.string.tips_mcuota_cancel), (dialog, which) -> Toast.makeText(MainActivity.this, R.string.tips_mcuota_cancle_update, Toast.LENGTH_SHORT).show())
                .setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        dialog.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
        dialog.show();
    }

    private void startFirmwareDownload() {
        new Thread(() -> {
            String fileUrl;
            try {
                fileUrl = FileDownloader.fetchDownloadUrl(version_URL);
                // 下载文件到本地
                FileDownloader.downloadFile(fileUrl, saveDir);
                mFirmwareFile = new File(saveDir, fileUrl.substring(fileUrl.lastIndexOf('/') + 1));
                if (mFirmwareFile.exists()) {
                    runOnUiThread(() -> showProgressDialog());
                    yMode.send(mFirmwareFile, progress -> runOnUiThread(() -> updateProgressDialog(progress)));
                    runOnUiThread(() -> hideProgressDialog());
                } else {
                    Toast.makeText(MainActivity.this, R.string.tips_mcuota_file_unexist, Toast.LENGTH_SHORT).show();
                }
            } catch (IOException | InterruptedException e) {
              //  Log.e("startFirmwareDownload", "Error downloading or sending firmware", e);
                Toast.makeText(MainActivity.this, R.string.tips_mcuota_Error_downloading_or_sending_firmware, Toast.LENGTH_SHORT).show();
            }

        }).start();
    }

    // 显示进度条对话框并设置初始进度
    private void showProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Firmware Update");
        progressDialog.setMessage("Updating firmware, please wait...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(100);
        progressDialog.setProgress(0);
        progressDialog.setCancelable(false);
        progressDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        progressDialog.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
        progressDialog.show();
    }

    // 更新进度条进度
    private void updateProgressDialog(int progress) {
        if (progressDialog != null) {
            progressDialog.setProgress(progress);
        }
    }

    // 隐藏进度条对话框
    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();

        }
    }

//    // Start the cycle
//    private void startRequestCycle() {
//        bmsHandler.post(send1896); // Kick off the loop
//    }


//    private final Handler bmsHandler = new Handler(Looper.getMainLooper());
//    private int requestIndex = 0;
//
//    private final Runnable bmsRunnable = new Runnable() {
//        @Override
//        public void run() {
//            switch (requestIndex) {
//                case 0:
//                    Log.d("dsfsg","QUERED---------------");
//                    serialPortManager.VCUSendToMCU(bms1890);
//                    break;
//
//                case 1:
//                    serialPortManager.VCUSendToMCU(bms1896);
//                    break;
//
//                case 2:
//                    serialPortManager.VCUSendToMCU("FFAA5501030015189002400001020304050607");
//                    break;
//
//                case 3:
//                    serialPortManager.VCUSendToMCU("FFAA5501030015189602400001020304050607");
//                    break;
//
//                case 4:
//                    serialPortManager.VCUSendToMCU("FFAA5501030015189302400001020304050607");
//                    break;
//
//                case 5:
//                    serialPortManager.VCUSendToMCU("FFAA5501030015189301400001020304050607");
//                    break;
//
//                case 6:
//                    serialPortManager.VCUSendToMCU("FFAA5501030015189801400001020304050607");
//                    break;
//            }
//
//            requestIndex = (requestIndex + 1) % 7; // 0 → 1 → ... → 5 → 0
//            bmsHandler.postDelayed(this, 500); // Adjust interval if needed
//        }
//    };
//
    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void setNeutralGear() {
//        int data = 0;
//        int seep_data = 0;
//        String sProt = "";
//        String hexData = "";
//        String hexSeepData = "";
      //  Log.d("sadfgfh", "neutarla entered");
//        // 空档
//        data = (serialPortManager.hb3.getBSG1_BYTE1() & 0xfc) + 0;
//        seep_data = (serialPortManager.hb3.getBSG1_BYTE4() & 0xfc) + FrameUtils.extractBits(serialPortManager.hb3.getBSG1_BYTE1(), 2, 4);
//
//        // 使用String.format来确保两位数的十六进制表示
//        hexData = String.format("%02x", data);
//        hexSeepData = String.format("%02x", seep_data);
//        sProt = "FFAA550103001510F8A807" + hexData + "FF00" + hexSeepData + "00000000";
        String setneutral = "FFAA550103001510F8A8070400000200000000";
        serialPortManager.VCUSendToMCU(setneutral);
    }

    public void LockHandle() {
        serialPortManager.VCUSendToMCU("FFAA550101000A00");
    }

    private void MenuUpdateUI() {
        // 获取方向档位
        int directionalGears = FrameUtils.extractBits(serialPortManager.hb3.getBSG1_BYTE1(), 0, 1);

        // 获取速度档位
        int speedGears = FrameUtils.extractBits(serialPortManager.hb3.getBSG1_BYTE1(), 2, 4);

        // 获取龙头锁状态
        int dragonLock = serialPortManager.hb1.getDragonLock();


        // 获取坐垫锁状态
        int SeatLock = serialPortManager.hb1.getSeatLock();

        // 初始化按钮状态
        frontLockSW.setChecked(dragonLock == 1);
        frontLockSWSTV.setText(dragonLock == 1 ? "开" : "关");
        seatCushionLockSW.setChecked(SeatLock == 1);
        seatCushionLockSWSTV.setText(SeatLock == 1 ? "开" : "关");

        switch (directionalGears) {
            case 0:
                neutralGear.setBackgroundColor(Color.parseColor("#ADD8E6")); // 淡蓝色
                break;
            case 1:
                driveGear.setBackgroundColor(Color.parseColor("#ADD8E6")); // 淡蓝色
                break;
            case 2:
                reverseGear.setBackgroundColor(Color.parseColor("#ADD8E6")); // 淡蓝色
                break;
            default:
                driveGear.setBackgroundColor(Color.parseColor("#ADD8E6")); // 淡蓝色
                break;
        }

        switch (speedGears) {
            case 0:
                speedThreshold1.setBackgroundColor(Color.parseColor("#ADD8E6")); // 淡蓝色
                break;
            case 1:
                speedThreshold2.setBackgroundColor(Color.parseColor("#ADD8E6")); // 淡蓝色
                break;
            case 2:
                speedThreshold3.setBackgroundColor(Color.parseColor("#ADD8E6")); // 淡蓝色
                break;
            default:
                speedThreshold2.setBackgroundColor(Color.parseColor("#ADD8E6")); // 淡蓝色
                break;
        }

    }

    /**
     * 获取系统时间
     */
    private void acquisitionTime() {

        TimeManager timeManager = TimeManager.getInstance(this);

        mRunnableTime = new Runnable() {
            @Override
            public void run() {

                // Get corrected formatted time
                String formattedTime =
                        timeManager.getFormattedTime("hh:mm a");

                binding.timeTextView.setText(formattedTime);

                // Update every second
                mHandlerTime.postDelayed(this, 1000);
            }
        };

        mHandlerTime.post(mRunnableTime);
    }


    /**
     * 设置全屏模式，隐藏导航栏和状态栏。
     */
    private void setFullScreen() {
        // 设置窗口标志，启用全屏模式
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // 获取装饰视图
        View decorView = getWindow().getDecorView();

        // 设置系统 UI 选项，隐藏导航栏和状态栏，启用粘性沉浸模式
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
    }
    private String getSimState(TelephonyManager telephonyManager, int slotIndex) {
        int simState = telephonyManager.getSimState(slotIndex);
        switch (simState) {
            case TelephonyManager.SIM_STATE_READY:
                return "SIM卡已启用";
            case TelephonyManager.SIM_STATE_NOT_READY:
                return "SIM卡未准备好";
            case TelephonyManager.SIM_STATE_ABSENT:
                return "SIM卡缺失";
            case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
                return "SIM卡需要网络解锁";
            case TelephonyManager.SIM_STATE_PIN_REQUIRED:
                return "SIM卡需要PIN解锁";
            case TelephonyManager.SIM_STATE_PUK_REQUIRED:
                return "SIM卡需要PUK解锁";
            case TelephonyManager.SIM_STATE_UNKNOWN:
                return "SIM卡状态未知";
            default:
                return "SIM卡状态异常";
        }
    }
    private boolean getSimInfo(TelephonyManager telephonyManager) {
        isSIMCardExist = false;
        SubscriptionManager subscriptionManager = getSystemService(SubscriptionManager.class);
        List<SubscriptionInfo> subscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
        if (subscriptionInfoList != null) {
            for (SubscriptionInfo info : subscriptionInfoList) {
                isSIMCardExist = true;
                int slotIndex = info.getSimSlotIndex();
                String simState = getSimState(telephonyManager, slotIndex);
                String simOperatorName = info.getCarrierName().toString();
                String simNumber = info.getNumber();
                Log.d("getSimInfo", "IM Card Slot Index: " + slotIndex);
                Log.d("getSimInfo", "\nSIM State: " + simState);
                Log.d("getSimInfo", "\nSIM Number: " + simNumber);
            }
        }
        if (!isSIMCardExist) {
            isSIMCardExist = false;
            Log.d("getSimInfo", "\n   未检测到任何SIM卡");
        }
        return isSIMCardExist;
    }
    public void Lockhandle()
    {
      //  Log.d("oikujhdagvb","Locking handle");serialPortManager.VCUSendToMCU("FFAA550101000A00");
    }
    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        //stopSequence();
        InactivityManager.getInstance().onUserInteraction(); // Reset timer on touch or key press
        //Log.d("dsafsd","onUserInteraction() NETER");
    }

//    @Override
//    public void onInactivityTimeout(int code) {
//        Log.d("MainActivity", "Inactivity timeout triggered with code: " + code);
//
//        switch (code) {
//            case 1:
//                Log.d("sadfgg","********2");
//                Log.d("MainActivity", "Locking of code1 app and Handle, returning to login screen.");
////
//////                serialPortManager.VCUSendToMCU("FFAA550101000A00");
////                Log.d("MainActivity", "Lock command sent to MCU.");
////
////                // Delay navigation to ensure serial command is sent before activity finishes
////                new Handler(Looper.getMainLooper()).postDelayed(() -> {
//////                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
//////                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//////                    startActivity(intent);
////                    finish();
////                }, 200); // small delay (200ms)
////                break;
////                if (! LoginActivity.isSequenceActive) {
////                    LoginActivity.isSequenceActive = true; // Start sequence
////                    serialPortManager.getHb3().setLEDresponse(1);
////                    serialPortManager.getHb3().sethandleLockresponse(1);
//                    EVLockService.getInstance().startLockSequence();    // Start lock
//                    finish();
////                } else {
////                    Log.d("Already", "Already running a sequence");
////                }
//                break;
//            case 2:
////                PowerManager powerManager = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
////                if (powerManager != null) {
////                    PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
////                            PowerManager.FULL_WAKE_LOCK |
////                                    PowerManager.ACQUIRE_CAUSES_WAKEUP |
////                                    PowerManager.ON_AFTER_RELEASE,
////                            "MyApp::ScreenOnWakeLock"
////                    );
////                    wakeLock.acquire(3000); // Keep screen on for 3 seconds
////                }
////                if (!isSequenceActive) {
////                    isSequenceActive = true; // Start sequence
////                    serialPortManager.getHb3().setLEDresponse(0);
////                    serialPortManager.getHb3().sethandleLockresponse(0);
////                    EVlockSequence(); // This will run task1 → task2
////                } else {
////                    Log.d("Already", "Already running a sequence");
////                }
//                Log.d("SHIFT", "DRIVE - NEUTRAL");
//
//                break;
//            case 7:
//                Log.d("MainActivity", "After 15 seconds of locking EV Now Screen is Getting OFFED");
//                try {
//                    PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
//                    Method goToSleep = PowerManager.class.getMethod("goToSleep", long.class);
//                    goToSleep.invoke(powerManager, SystemClock.uptimeMillis());
////                InactivityManager.getInstance().startTimer(2);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//                break;
//
//            default:
//                Log.d("MainActivity", "Unhandled timeout code: " + code);
//                break;
//        }
//    }


//    private boolean isSequenceActive = false;
//    public  static boolean universalflag=false;

//    public void startUnlockSequence() {
//        LoginActivity.isSequenceActive = true;
//        task111(); // start task1 directly
//    }
//
//    private void task111() {
//        if (! LoginActivity.isSequenceActive) return; // Stop if sequence was cancelled
//
//        serialPortManager.VCUSendToMCU("FFAA550101000A01");
//        Log.d("CommandSequence", "Sent command 1");
//
//        Heartbeat3 hb3 = serialPortManager.getHb3();
//        command1Success = (hb3.gethandleLockresponse() == 1);
//        Log.d("CommandSequence", "Handle == " + hb3.gethandleLockresponse());
//
//        if (!command1Success && retryCount1 < MAX_RETRIES) {
//            retryCount1++;
//            Log.d("CommandSequence", "Retrying command 1, attempt " + retryCount1);
//            new Handler(Looper.getMainLooper()).postDelayed(() -> {
//                if ( LoginActivity.isSequenceActive) task111();
//            }, 500);
//        } else if (command1Success) {
//            Log.i("CommandSequence", "Executed Command 1 successfully");
//            retryCount1 = 0;
//            hb3.sethandleLockresponse(0);
//            task222();
//        } else {
//            Log.e("CommandSequence", "Command 1 failed after retries");
//        }
//    }
//
//    private void task222() {
//        if (! LoginActivity.isSequenceActive) return; // Stop if sequence was cancelled
//
//        serialPortManager.VCUSendToMCU("FFAA550107000A01");
//
//        Heartbeat3 hb3 = serialPortManager.getHb3();
//        command2Success = (hb3.getLEDresponse() == 1);
//        Log.d("CommandSequence", "LED == " + hb3.getLEDresponse());
//
//        if (!command2Success && retryCount2 < MAX_RETRIES) {
//            retryCount2++;
//            Log.d("CommandSequence", "Retrying command 2, attempt " + retryCount2);
//            new Handler(Looper.getMainLooper()).postDelayed(() -> {
//                if ( LoginActivity.isSequenceActive) task222();
//            }, 500);
//        } else if (command2Success) {
//            LoginActivity.universalflag=true;
//            LoginActivity.isSequenceActive = false;
//            Log.d("CommandSequence", "Unlock sequence completed successfully");
//            retryCount2 = 0;
//            hb3.setLEDresponse(0);
//        } else {
//            Log.e("CommandSequence", "Command 2 failed after retries");
//            LoginActivity.universalflag=true;
//            LoginActivity.isSequenceActive = false;
//        }
//    }
//
//    private void EVlockSequence() {
//        LoginActivity.isSequenceActive = true;
//        evlocktask1(); // start task1 directly
//    }
//
//    private void evlocktask1() {
//        if (! LoginActivity.isSequenceActive) return; // Exit if stopped
//
//        serialPortManager.VCUSendToMCU("FFAA550101000A00");
//        Log.d("CommandSequence", "Sent command 1");
//
//        Heartbeat3 hb3 = serialPortManager.getHb3();
//        boolean EVloccommand1Success = (hb3.gethandleLockresponse() == 1);
//        Log.d("CommandSequence", "Handleresponse == " + hb3.gethandleLockresponse());
//
//        if (!EVloccommand1Success && retryCount1 < MAX_RETRIES) {
//            retryCount1++;
//            Log.d("CommandSequence", "Retrying command 1, attempt " + retryCount1);
//            new Handler(Looper.getMainLooper()).postDelayed(() -> {
//                if (LoginActivity.isSequenceActive) evlocktask1();
//            }, 1000);
//        } else if (EVloccommand1Success) {
//            Log.i("CommandSequence", "Executed Command 1 successfully");
//            retryCount1 = 0;
//            hb3.sethandleLockresponse(0);
//            evlocktask2();
//        } else {
//            Log.e("CommandSequence", "Command 1 failed after retries");
//            evlocktask2();
//        }
//    }
//
//    private void evlocktask2() {
//        if (! LoginActivity.isSequenceActive) return; // Exit if stopped
//
//        serialPortManager.VCUSendToMCU("FFAA550107000A00");
//        Log.d("CommandSequence", "Sent command 2");
//
//        Heartbeat3 hb3 = serialPortManager.getHb3();
//        boolean EVloccommand2Success = (hb3.getLEDresponse() == 1);
//        Log.d("CommandSequence", "LEDResponse == " + hb3.getLEDresponse());
//
//        if (!EVloccommand2Success && retryCount2 < MAX_RETRIES) {
//            retryCount2++;
//            Log.d("CommandSequence", "Retrying command 2, attempt " + retryCount2);
//            new Handler(Looper.getMainLooper()).postDelayed(() -> {
//                if ( LoginActivity.isSequenceActive) evlocktask2();
//            }, 1000);
//        } else if (EVloccommand2Success) {
//            Log.d("CommandSequence" , "Lock sequence completed successfully");
//            LoginActivity.universalflag=true;
//            retryCount2 = 0;
//            hb3.setLEDresponse(0);
//            LoginActivity.isSequenceActive = false;
//            finish();
//        } else {
//            Log.e("CommandSequence", "Command 2 failed after retries");
//            LoginActivity.isSequenceActive = false;
//            LoginActivity.universalflag=true;
//            finish();
//        }
//    }
//
//    public void stopSequence() {
//        LoginActivity.isSequenceActive = false;
//        Log.d("CommandSequence", "Unlock sequence stopped manually.");
//    }
    private boolean isReceiverRegistered = false;
    public static boolean LoginisActive = false;
    public static boolean MainisActive = false;
    public static boolean isVisible = false;
    public static boolean isrideOSOTA = false;
    public static boolean justLoggedIn = false;
    @Override
    protected void onResume() {
        super.onResume();
        acquisitionTime();
        LoginisActive = true;
        UIUpdater.islock=true;
        isVisible = true;
        MainActivity.isrideOSOTA = false;
        MainisActive = true;

        if (justLoggedIn) {
            justLoggedIn = false;
            binding.getRoot().post(() -> {

                serialPortManager = serialApplication.getSerialPortManager();
                if(serialPortManager!=null){
                    serialPortManager.VCUSendToMCU("FFAA550101000A01");

                    new Handler().postDelayed(() -> {
                        serialPortManager.VCUSendToMCU("FFAA550107000A01");
                        }, 300);
                }
                BatteryArcView batteryView = binding.batteryArc;
                batteryView.blockInitialDraw(true);
                batteryView.setBatteryLevel(0);
                batteryView.blockInitialDraw(false);
                runPostLoginInit();
            });
        }

//        if (!isReceiverRegistered) {
//            IntentFilter filter = new IntentFilter("FRAME_RECEIVED");
//            LocalBroadcastManager.getInstance(this).registerReceiver(frameReceiver, filter);
//            isReceiverRegistered = true;
//        }
//        if(UIUpdater.directionalGears==0){
//            InactivityManager.getInstance().startTimer(1);
//            //Log.d("sadfg","INRESUMEOFMAINSTARTINGTIMERWITH1");
//        }
        if (UIUpdater.directionalGears==0)
        {
            InactivityManager.getInstance().startTimer(1);
        }
     //   Log.d("sadfg","INRESUMEOFMAINSTARTINGTIMERWITH1");
//         If you need to restart anything like timers here, do so
//         mHandlerTime.postDelayed(mRunnableTime, 0);


        // If deferred update exists, show it
//        if (UpdateService.pendingUpdateApkUrl != null) {
            //Log.d("MainActivity", "Showing deferred OTA update dialog...");
        Intent intent = new Intent(this, UpdateService.class);
        startService(intent);   // auto check (respects remind-later timer)

        if (!isStatusReceiverRegistered) {
            IntentFilter signalFilter = new IntentFilter();
            signalFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
            signalFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
            signalFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            signalFilter.addAction("android.intent.action.SERVICE_STATE");
            signalFilter.addAction("android.intent.action.ANY_DATA_STATE");

            registerReceiver(statusReceiver, signalFilter);
            isStatusReceiverRegistered = true;
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        LoginisActive = false;
        isVisible = false;
        //Log.d("MainActivity", "PAUSE OF MAINACTIVTY ");

//        MainisActive = false;

//        if(KeepsoundsOrStop)
//        {
//            uiUpdater.ReadystopBeeping();
//            uiUpdater.stopReverseBeep();
//            uiUpdater.stopBlinking();
//
//            //uiUpdater.stopSideStandSound();
////            Log.d("sadsfgd","IN MAIN PAUSE ENTERIF LOOP");
//        }

//        CameraService.stopRecording(this);

        try {
            unregisterReceiver(statusReceiver);
            isStatusReceiverRegistered = false;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

//        if (isReceiverRegistered) {
//            LocalBroadcastManager.getInstance(this).unregisterReceiver(frameReceiver);
//            isReceiverRegistered = false;
//        }

        // Stop runnables
        mHandlerTime.removeCallbacks(mRunnableTime);

        if (delayedUnlockRunnable != null) {
            delayedUnlockHandler.removeCallbacks(delayedUnlockRunnable);
           // Log.d("MainActivity", "Delayed unlock cancelled");
        }
//        if (isReceiverRegistered) {
//            LocalBroadcastManager.getInstance(this).unregisterReceiver(frameReceiver);
//            isReceiverRegistered = false;
//        }
        // Stop tasks if needed
        // mHandlerTime.removeCallbacks(mRunnableTime);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(statusReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace(); // Receiver was not registered or already unregistered
        }
        instance = null; // clean up
        // Make sure the receiver is unregistered
        if (isReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(frameReceiver);
            isReceiverRegistered = false;
        }

        if (UIUpdater.getInstance() != null) {
            UIUpdater.getInstance().cleanup(this);
        }

        // Reset SerialPortManager
        //SerialPortManager.reset();
        //serialPortManager.closeSerialPort();

        // Stop any pending runnables
        mHandlerTime.removeCallbacks(mRunnableTime);

        if (delayedUnlockRunnable != null) {
            delayedUnlockHandler.removeCallbacks(delayedUnlockRunnable);
           // Log.d("MainActivity", "Delayed unlock cancelled");
        }
        // Release other resources if necessary
        // cameraHandler.stop();
        // cameraHandler.releaseCamera();
        // if (musicPlayer != null) musicPlayer.onDestroy();
    }
    public void resetMosfets()
    {
        serialPortManager.VCUSendToMCU("FFAA5501030015162A01800101020304050607");//Main BatPack Charging ON
        serialPortManager.VCUSendToMCU("FFAA5501030015162C01800101020304050607");//Main BatPack DisCharging ON
        serialPortManager.VCUSendToMCU("FFAA5501030015162C01800001020304050607");//Main APU DisCharging OFF
    }
    public void updateStatusText(String message) {
        if (binding.textstatus != null) {
            binding.textstatus.setText(message);
        }
    }

    public static boolean ReloggedIn;
//    public void runPostLoginInit() {
//        uiUpdater.lastCanUpdateTime = System.currentTimeMillis();
//       // Log.d("MainActivity", "Running unlock sequence from post-login...");
//        //animateBatteryLevelOnce();
//        currentgear='O';
//        EVLockService.getInstance().startUnlockSequence();  // Start initial unlock attempt
//        //TelemetryService.shdtelemetrywhenunlocked=true;
//        new Handler(Looper.getMainLooper()).postDelayed(() -> {
//            if(uiUpdater != null && uiUpdater.Play_tpmsSound_on_loggin){
//                uiUpdater.playLowTireSound();
//            }
//        }, 2100);
//        binding.batteryArc.setBatteryLevel(0);
//        binding.getRoot().postDelayed(() -> {
//            binding.batteryArc.setIntroMode(true);
//            runBatteryIntroAnimation();
//        }, 1000); // 1000ms = 1 second
//        uiUpdater.onTripReset();

        //        new Handler(Looper.getMainLooper()).postDelayed(() -> {
//            long minRequired = 50 * 1024 * 1024; // 50 MB minimum free space
//            if (isSdCardHasSpace(minRequired)) {
//                Log.d("MainActivity", "Starting CameraService recording...");
//                Intent i = new Intent(MainActivity.this, CameraService.class);
//                i.setAction("START_REC");
//                startService(i);
//            } else {
//                Log.e("MainActivity", "SD card full, cannot start recording!");
//                Toast.makeText(this, "SD card full, cannot start recording!", Toast.LENGTH_SHORT).show();
//            }
//        }, 8000);
//============================================================================================================
//        new Handler(Looper.getMainLooper()).postDelayed(() -> {
//            Log.d("MainActivity", "Starting CameraService recording...");
//            Intent i = new Intent(MainActivity.this, CameraService.class);
//            i.setAction("START_REC");   //  match service action name
//            startService(i);
//        }, 8000); // 8 seconds

//        delayedUnlockRunnable = () -> {
//            Heartbeat1 hb1 = serialPortManager.getHb1();
//            if ((hb1.getDragonLock() == 0 || hb1.getLED() == 0)&& LoginisActive) {
//                Log.d("MainActivity", "&&&&&&&&&&&&&&&&&&&&&&&Unlocking again after delay...");
//                EVLockService.getInstance().startUnlockSequence();
//            } else {
//                //Log.d("MainActivity", "Scooter already unlocked, skipping re-unlock.");
//            }
//        };
//        delayedUnlockHandler.postDelayed(delayedUnlockRunnable, 8000);
//    }
public void runPostLoginInit() {

    //  ALWAYS run UI work on UI thread
    runOnUiThread(() -> {

        uiUpdater.lastCanUpdateTime = System.currentTimeMillis();

        currentgear = 'O';

        //  Start unlock (no change)
        EVLockService.getInstance().startUnlockSequence();
        refreshStats();
        //  Slight delay for UI stability (IMPORTANT)
        binding.getRoot().post(() -> {

            //  Reset battery instantly

            //  Start intro AFTER UI is ready (smooth now)
            binding.batteryArc.setBatteryLevel(0);
            binding.batteryArc.setIntroMode(true);
            runBatteryIntroAnimation();

//            binding.speedArc.setSpeed(0);
//            binding.speedArc.setIntroMode(true);
//            runSpeedIntroAnimation();
        });

        // TPMS sound (unchanged)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (uiUpdater != null && uiUpdater.Play_tpmsSound_on_loggin) {
                uiUpdater.playLowTireSound();
            }
        }, 2100);

        //Trip reset (unchanged)
        uiUpdater.onTripReset();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            MediaPlayer mp = MediaPlayer.create(MainActivity.this, R.raw.unlocked);
            if (mp != null) {
                mp.setOnCompletionListener(MediaPlayer::release);
                mp.start();
            }

        }, 300);
    });
}
    public void startSpeedIntroFromUpdater() {
        runOnUiThread(() -> {
            binding.speedArc.setSpeed(0,0);
            binding.speedArc.setIntroMode(true);
            runSpeedIntroAnimation();
        });
    }
    private void runBatteryIntroAnimation() {

        runOnUiThread(() -> {   // FIX HERE

            UIUpdater.getInstance().setBatteryIntroRunning(true);

            BatteryArcView batteryView = binding.batteryArc;

            //  Step 1: 0 → 100
            ValueAnimator up = ValueAnimator.ofFloat(0, 100);
            up.setDuration(900);

            //  Step 2: 100 → 0
            ValueAnimator down = ValueAnimator.ofFloat(100, 0);
            down.setDuration(900);

            up.addUpdateListener(a ->
                    batteryView.setBatteryLevel((float) a.getAnimatedValue())
            );

            down.addUpdateListener(a ->
                    batteryView.setBatteryLevel((float) a.getAnimatedValue())
            );

            up.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    down.start();
                }
            });

            down.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {

                    batteryView.postDelayed(() -> {

                        float lastBattery = UIUpdater.getInstance().getLastKnownBattery();

                        batteryView.setIntroMode(false);
                        batteryView.setBatteryLevel(lastBattery);

                        UIUpdater.getInstance().setBatteryIntroRunning(false);

                    }, 300);
                }
            });
            up.start();
        });
    }
    private void runSpeedIntroAnimation() {

        runOnUiThread(() -> {

            SpeedArcView speedView = binding.speedArc;

            speedView.setIntroMode(true);
            ValueAnimator up = ValueAnimator.ofFloat(0, 100);
            up.setDuration(870);

            ValueAnimator down = ValueAnimator.ofFloat(100, 0);
            down.setDuration(930);

            up.addUpdateListener(a ->
                    speedView.setSpeed((float) a.getAnimatedValue(),0)
            );

            down.addUpdateListener(a ->
                    speedView.setSpeed((float) a.getAnimatedValue(),0)
            );

            up.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    down.start();
                }
            });

            down.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {

                    speedView.postDelayed(() -> {

                        speedView.setIntroMode(false); //  BACK TO NORMAL

                        // optionally reset to 0
                        speedView.setSpeed(0,0);

                    }, 500);
                }
            });

            up.start();
        });
    }
    @Override
    public void onBackPressed() {
        // Do nothing
    }

    private final Handler bmsHandler = new Handler(Looper.getMainLooper());
    private int requestIndex = 0;

//    private final Runnable bmsRunnable = new Runnable() {
//        @Override
//        public void run() {
//            switch (requestIndex) {
//                case 0://Log.d("sfadsgf","ENTERREQUSTLOOP");
//                    serialPortManager.VCUSendToMCU("FFAA5501030015160B01800001020304050607");
//                    break;
//
//                case 1:
//                    serialPortManager.VCUSendToMCU("FFAA5501030015160D01800001020304050607");
//                    break;
//
//                case 2:
//                    serialPortManager.VCUSendToMCU("FFAA5501030015189101400001020304050607");
//                    break;
//
//            }
//            requestIndex = (requestIndex + 1) % 3;
//            bmsHandler.postDelayed(this, 1000); // 650ms interval
//        }
//    };


    private final Runnable bmsRunnable = new Runnable() {
        @Override
        public void run() {
            switch (requestIndex) {
                case 0://Log.d("sfadsgf","ENTERREQUSTLOOP");
                    serialPortManager.VCUSendToMCU("FFAA5501030015189001400001020304050607");
                    break;

                case 1:
                    serialPortManager.VCUSendToMCU("FFAA5501030015189002400001020304050607");
                    break;

                case 2:
                    serialPortManager.VCUSendToMCU("FFAA5501030015189601400001020304050607");
                    break;

                case 3:
                    serialPortManager.VCUSendToMCU("FFAA5501030015189602400001020304050607");
                    break;

                case 4:
                    serialPortManager.VCUSendToMCU("FFAA5501030015189302400001020304050607");
                    break;

                case 5:
                    serialPortManager.VCUSendToMCU("FFAA5501030015189301400001020304050607");
                    break;

                case 6:
                    serialPortManager.VCUSendToMCU("FFAA5501030015160B01800001020304050607");
                    break;

                case 7:
                    serialPortManager.VCUSendToMCU("FFAA5501030015160D01800001020304050607");
                    break;

                case 8:
                    serialPortManager.VCUSendToMCU("FFAA5501030015189801400001020304050607");
                    break;
                case 9:
                    serialPortManager.VCUSendToMCU("FFAA5501030015189101400001020304050607");
                    break;
                case 10:
                    serialPortManager.VCUSendToMCU("FFAA5501030015189501400001020304050607");
                    break;
                case 11:
                    serialPortManager.VCUSendToMCU("FFAA550103001518FF50A5"+VIN_On_CAN);
                    break;
                case 12:
                    serialPortManager.VCUSendToMCU("FFAA5501030015189401400001020304050607");
                    break;

            }
            requestIndex = (requestIndex + 1) % 13;
            bmsHandler.postDelayed(this, 700); // 650ms interval
        }
    };

    private boolean isDraggingQS = false;
    private float qsStartY = 0f;
    private float lastY = 0f;
    private int qsHeight;

    // gesture features =================================
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        private static final int SWIPE_THRESHOLD = 35;
        private static final int SWIPE_VELOCITY_THRESHOLD = 40;
        private static final int EDGE_SWIPE_THRESHOLD = 20;

        @Override
        public boolean onDown(MotionEvent e) {
            return true; // REQUIRED for onScroll to work
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                float distanceX, float distanceY) {

            if (e1 == null || e2 == null) return false;

            if (!isDraggingQS) {
                if (e1.getY() > EDGE_SWIPE_THRESHOLD) return false;

                if (Math.abs(e2.getY() - e1.getY()) < 6) return false;

                isDraggingQS = true;
                dragDistance = 0f;   // reset
            }

            float dy = e2.getY() - e1.getY();
            dragDistance = dy;  // track drag

            float newY = -qsHeight + dy;

            newY = Math.max(-qsHeight, Math.min(0, newY));

            float currentY = qsView.getTranslationY();
            float smoothY = currentY + (newY - currentY) * 0.65f;
            qsView.setTranslationY(smoothY);

            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2,
                               float velocityX, float velocityY) {

            // FIRST: if QS is being dragged → finish it
            if (isDraggingQS) {
                finishQuickSettings(velocityY);
                isDraggingQS = false;
                return true;
            }


            // 🔁 BELOW IS YOUR ORIGINAL CODE (UNCHANGED)

            if (e1 == null || e2 == null) return false;

            float diffX = e2.getX() - e1.getX();
            float diffY = e2.getY() - e1.getY();

            float startX = e1.getX();
            float startY = e1.getY();

            int screenWidth = getResources().getDisplayMetrics().widthPixels;

            if (Math.abs(diffX) > Math.abs(diffY)) {

                if (Math.abs(diffX) > SWIPE_THRESHOLD &&
                        Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {

                    if (diffX > 0 && startX < EDGE_SWIPE_THRESHOLD) {
                        openMenu();
                        return true;
                    }

                    if (diffX < 0 &&
                            startX > (screenWidth - EDGE_SWIPE_THRESHOLD)) {
                        openMaps();
                        return true;
                    }
                }

            } else {

                if (diffY > 15 && velocityY > 300) {

                    if (startY < EDGE_SWIPE_THRESHOLD) {

                        qsView.open();   // direct open
                        return true;
                    }
                }
            }

            return false;
        }
    }

    public void closeRecoIfOpen() {

        Fragment f = getSupportFragmentManager()
                .findFragmentById(R.id.overlayContainer);

        // Only close if RecoInfoFragment is actually open
        if (f instanceof RecoInfoFragment) {

            getSupportFragmentManager()
                    .beginTransaction()
                    .remove(f)
                    .commitAllowingStateLoss();

            // Hide overlay container
            View overlay = findViewById(R.id.overlayContainer);
            if (overlay != null) {
                overlay.setVisibility(View.GONE);
            }

            // Optional: restart inactivity timer if needed
            if (UIUpdater.directionalGears == 0) {
                InactivityManager.getInstance().startTimer(1);
            }
        }
    }
    public void closeQuickSettingsIfOpen() {

        if (qsView == null) return;

        // Check if QS is visible/open
        if (qsView.getTranslationY() > -qsHeight + 5f) {   // tolerance

            // Hard close (safe, no animation)
            qsView.setTranslationY(-qsHeight);

            // Reset drag state
            isDraggingQS = false;
            dragDistance = 0f;
        }
    }
    private float dragDistance = 0f;   // add this as class variable

    private void finishQuickSettings(float velocityY) {

        isDraggingQS = false;

        boolean open;

        float currentY = qsView.getTranslationY();
        if (currentY > -qsHeight * 0.5f) {
            open = true;
        } else {
            open = false;
        }
        if (Math.abs(dragDistance) > 50) {
            if (Math.abs(velocityY) > 600) {
                open = velocityY > 0;
            }
        }

        if (open) {
            qsView.open();
        } else {
            qsView.smoothClose();
        }
    }


    private void openMenu() {
        if (serialPortManager.getHb3().getBSG2_BYTE78() < 10) {
//            InactivityManager.getInstance().stopTimer();
            serialPortManager.VCUSendToMCU("FFAA550107000A01");
            Intent intent = new Intent(MainActivity.this, menu.class);
            startActivity(intent);
            // No animation
        }
    }

    private void openMaps() {
        InactivityManager.getInstance().stopTimer();
        Intent intent = new Intent(MainActivity.this, Maps.class);
        startActivity(intent);
    }

    private void openQuickSettings() {
        Intent intent = new Intent(MainActivity.this, QuickSettingsView.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_down, R.anim.slide_out_up);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }

        velocityTracker.addMovement(event);

        switch (event.getAction()) {

            case MotionEvent.ACTION_UP:

                velocityTracker.computeCurrentVelocity(1000);

                float velocityY = velocityTracker.getYVelocity();

                // FAST swipe detection
                if (velocityY > 1800) {

                    float startY = event.getY();

                    if (startY < 150) { // top area
                        qsView.open();
                        resetVelocity();
                        return true;
                    }
                }

                resetVelocity();
                break;
        }

        return gestureDetector.onTouchEvent(event)
                || super.onTouchEvent(event);
    }

    private void resetVelocity() {
        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }
    }
    public static int extractBits(int value, int startBit, int endBit) {
        int mask = ((1 << (endBit - startBit + 1)) - 1) << startBit;
        int extractedBits = (value & mask) >> startBit;
        return extractedBits;
    }
    public void showRecoInfo() {
        FrameLayout overlay = findViewById(R.id.overlayContainer);
        overlay.setVisibility(View.VISIBLE);

        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.overlayContainer, new RecoInfoFragment())
                .commit();
    }

    public void updateKeyImage(boolean keyInRange) {
        if (keyImage == null) return;

        runOnUiThread(() -> {
            if (keyInRange) {
                keyImage.setImageResource(R.drawable.keyin);   // key in range
            } else {
                keyImage.setImageResource(R.drawable.keyout);  // key out of range
            }
        });
    }
    private static final String PREFS_OTA = "ota_prefs";
    private static final String KEY_LAST_MCU_VERSION = "last_mcu_version";

    private void saveLastMcuVersion(int version) {
        getSharedPreferences(PREFS_OTA, MODE_PRIVATE)
                .edit()
                .putInt(KEY_LAST_MCU_VERSION, version)
                .apply();
    }

    private int getLastSavedMcuVersion() {
        return getSharedPreferences(PREFS_OTA, MODE_PRIVATE)
                .getInt(KEY_LAST_MCU_VERSION, 0);
    }

    private int getEffectiveLocalMcuVersion() {
        int liveVersion = 0;

        if (serialPortManager != null && serialPortManager.mad != null) {
            liveVersion = serialPortManager.mad.getMcuVersions();
        }

        if (liveVersion > 0) {
            // ✅ MCU responded correctly → store it
            saveLastMcuVersion(liveVersion);
           // Log.d("OTA", "Using LIVE MCU version: " + liveVersion);
            return liveVersion;
        }

        // ⚠️ MCU failed → fallback
        int storedVersion = getLastSavedMcuVersion();
       // Log.w("OTA", "MCU version invalid, fallback to stored: " + storedVersion);
        return storedVersion;
    }

    public static int fetchServerVersion(String versionUrl) {
        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            URL url = new URL(versionUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                //Log.e("OTA", "HTTP error: " + connection.getResponseCode());
                return -1;
            }

            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder json = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                json.append(line);
            }

            // Example version.json:
            // { "version": 12, "url": "xxx.bin" }
            JSONObject obj = new JSONObject(json.toString());
            return obj.getInt("version");

        } catch (Exception e) {
            //Log.e("OTA", "Failed to fetch version", e);
            return -1;
        } finally {
            try {
                if (reader != null) reader.close();
            } catch (Exception ignored) {}
            if (connection != null) connection.disconnect();
        }
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