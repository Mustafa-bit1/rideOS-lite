package com.fanconn.rideOS.authen;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

//import com.fanconn.rideOS.InactivityManager;
import com.fanconn.rideOS.AhTrackerManager;
import com.fanconn.rideOS.CanDataParser;
import com.fanconn.rideOS.EVLockService;
import com.fanconn.rideOS.FrameUtils;
import com.fanconn.rideOS.Heartbeat1;
import com.fanconn.rideOS.Heartbeat3;
import com.fanconn.rideOS.InactivityManager;
import com.fanconn.rideOS.MainActivity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.fanconn.rideOS.MetadataManager;
import com.fanconn.rideOS.R;
import com.fanconn.rideOS.SerialPortManager;
import com.fanconn.rideOS.Smartkey.KeyPresenceManager;
import com.fanconn.rideOS.SoundPlayer;
import com.fanconn.rideOS.TelemetryDataManager;
import com.fanconn.rideOS.TelemetryService;
import com.fanconn.rideOS.UIUpdater;
import com.fanconn.rideOS.batteryPackSwitcher;
import com.fanconn.rideOS.chargingUI;
import com.fanconn.rideOS.databinding.ActivityLoginBinding;
import com.fanconn.rideOS.databinding.ActivityMainBinding;
import com.fanconn.rideOS.mcuupdate.YMode;
import com.fanconn.rideOS.serialApplication;
//import com.fanconn.rideOS.ScreenController;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class LoginActivity extends AppCompatActivity{
    private static boolean isFrameReceiverRegistered = false;
    public static LoginActivity instance;

    public  static SerialPortManager loginserialPortManager;  // 串口管理类
    private ActivityLoginBinding binding;
    private String sPort = "/dev/ttyHS1";  // 串口地址
    private int iBaudRate = 115200;  // 波特率
    private int type_Hex = 0;  // Hex 类型的数据格式
    private int type_Txt = 1;  // Txt 类型的数据格式
    private int type_byte = 2;  // byte 类型的数据格式
    private int RX_type = type_Hex;  // 数据格式类型
    private String rxText = "";  // 接收到的数据
    private String version_URL = "http://ota.rivotmotors.com/OTA/sc126/ElectricBike/version.json";
    private String saveDir = "/sdcard";
    private UIUpdater uiUpdater;   // UI 更新管理器，用于更新界面上的数据和视图
    private Runnable mRunnableTime;  // 用于定时更新系统时间显示的任务
    private boolean isSIMCardExist = false;
    private boolean isSDCardExist = false;
    private Handler mHandlerTime = new Handler();
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
    private int lastBatteryLevel = -1; // Store last battery level to avoid redundant updates
    private int lastSpeed = -1;
    private EditText etCredential;
    private PatternView patternView;
    private SharedPreferences prefs;
    private final StringBuilder actualPin = new StringBuilder();
    private Vibrator vibrator;
    GestureDetector gestureDetector;
    SerialPortManager serialPortManager;
    public static boolean ChargingUiClosedByUserFlag= false;
    public static boolean isChargingUIActive = false;
//    public static boolean shouldshowchargingicononmainonot = false;
    public static boolean forceStandUIDUEToCharging=false;


    public static Handler chargingFrameTimeoutHandler = new Handler(Looper.getMainLooper());
    public static Runnable chargingFrameTimeoutRunnable;
    private static long lastChargingUILaunchTime = 0;
    MediaPlayer mediaPlayer;
    private static int prevMod2 = -1; // -1 indicates first-time setup
    private TextView tempWarningMessage;


    private final BroadcastReceiver frameReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String frame = intent.getStringExtra("frame_data");
            if(frame.startsWith("FFAA770303001518FF50E5"))
                //Log.d("FRAME", "Received in4001NNNNNNNN " + getClass().getSimpleName() + ": " + frame);
                //if(frame.startsWith("FFAA770303001518FF50E5"))Log.d("sdfgh","frameofCHARGING"+frame);

            if (frame.length() > 15 && FrameUtils.verifyFrame(frame)){

                if (frame == null || frame.length() < 38) return;

                if (frame.startsWith("FFAA770303001510F807A8")) {
                    // Log.d("sdfgh","frame"+frame);
                    handleWakeFrame(context, frame);
                }
                if (frame.startsWith("FFAA770303001518904001")) {
                    //Log.d("dsfgdfh", "SOC frame ; " + frame);
                    handleLoginFrame();
                }

                if (frame.startsWith("FFAA770301")) {
//                    Log.d("sdfgg","heartbeat packet 1-> "+frame);
                    if(serialPortManager!=null) {
                        serialPortManager.parseHeartbeat1(frame);
                    }
                }
//                if (frame.startsWith("FFAA660107")) {
//                    serialPortManager.parseLEDControlResponse(frame);
//                }18FF50E5
            }
            if (frame.length() >= 38 && frame.startsWith("FFAA770303001518FF50E5")) {
                BatPackSwitcher.startSwitchingToNormalState();
                // wakeScreenIfNeeded();
                Heartbeat3 hb3 = serialPortManager.getHb3();
                binding.loginchargingicon.setVisibility(View.VISIBLE);
//                shouldshowchargingicononmainonot=true;
                UIUpdater.shouldshowchargingicononmainonot=true;
                forceStandUIDUEToCharging=true;
//                Log.d("sdfdgfh","CHRAGINGBITINFEILDeeeeeeee___________"+frame);
                String candata=FrameUtils.getByteSubstring(frame,11,18);
                CanDataParser.parseCanData18FF50E5(hb3, candata);
                float chargingbit = hb3.getChargingbit();
                Log.d("sdfdgfh","CHRAGINGBITINFEILDeeeeeeee___________"+chargingbit);

//                    UIUpdater.telechargingstate=hb3.getChargingbit();
//                    String byt = frame.substring(22, 24);
//                    int chargingbit = Integer.parseInt(byt, 16);

                if(chargingbit==0){
                    binding.loginchargingicon.setVisibility(View.INVISIBLE);
                    TelemetryDataManager.getInstance().setTeleChargingState(2);
                }
                if(chargingbit>0) {
//                  Log.d("sdfdgfh","CHRAGINGBITINFEILD___________"+chargingbit);
                    binding.loginchargingicon.setVisibility(View.VISIBLE);
                    TelemetryDataManager.getInstance().setTeleChargingState(1);
                }
//                    Log.d("Frame", "Charging frame received");
//                    binding.loginchargingicon.setVisibility(View.VISIBLE);

                long currentTime = System.currentTimeMillis();
                boolean canLaunchUI = !isChargingUIActive && !ChargingUiClosedByUserFlag;
//                    Log.d("sdfdgfh","flague  "+isChargingUIActive);
//                    Log.d("sdfdgfh","flague2  "+ChargingUiClosedByUserFlag);

                if (canLaunchUI && (currentTime - lastChargingUILaunchTime > 1000)) {
                    isChargingUIActive = true;

                    lastChargingUILaunchTime = currentTime;
                    TelemetryDataManager.getInstance().setTeleChargingState(1);


                    Intent intent0 = new Intent(context, chargingUI.class);
                    intent0.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    context.startActivity(intent0);
                }

                // Timeout logic: reset handler every time
                chargingFrameTimeoutHandler.removeCallbacks(chargingFrameTimeoutRunnable);
                chargingFrameTimeoutRunnable = () -> {
                    //Log.d("Frame", "Charging frame timeout — closing UI");

                    isChargingUIActive = false;
                    ChargingUiClosedByUserFlag=false;
//                    shouldshowchargingicononmainonot = false;
                    UIUpdater.shouldshowchargingicononmainonot=false;


                    binding.loginchargingicon.setVisibility(View.INVISIBLE);

                    // Send broadcast to close the UI
                    Intent closeIntent = new Intent("ACTION_CLOSE_CHARGING_UI");
                    context.sendBroadcast(closeIntent);
                };

                chargingFrameTimeoutHandler.postDelayed(chargingFrameTimeoutRunnable, 5000); // 1 second


            }
            else {
            }
        }

        private void handleLoginFrame() {
//            String dataSegment = frame.substring(22, 38);
            //Log.d("CAN_DEBUG", "Parsed frame segment: " + dataSegment);

//            int soc = FrameUtils.getByteSubstringAsInt(dataSegment, 6, 7) / 10;
            int socInt = (int) ahTrackerManager.getSOC();   // removes decimal part

            if (socInt <= 100) {
                binding.loginSoc.setText("Battery " + socInt + "%");
            }

        }

        // Declare this class-level variable to track previous mod value
        private void handleWakeFrame(Context context, String frame) {
            String hexByte = frame.substring(24, 26); // 11th byte
            int byteValue = Integer.parseInt(hexByte, 16);
            int currentMod2 = byteValue % 2;
// First-time case: just record it, don’t wake
            if (prevMod2 == -1) {
                prevMod2 = currentMod2;
                return;
            }
            // true only when 1 → 0 transition happens
            boolean isWakeFrame = (prevMod2 == 1 && currentMod2 == 0);
            prevMod2 = currentMod2; // update for next check

            if (!isWakeFrame) {
                markNonWakeFrameSeen();
            }

            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (isWakeFrame &&
                    powerManager != null &&
                    !powerManager.isInteractive() &&
                    hasSeenNonWakeFrame()) {
                wakeScreen();
            }

            // Optional: debug log
//             Log.d("Byte Tracker", "11th byte: " + hexByte + ", value: " + byteValue + ", isWakeFrame: " + isWakeFrame);
        }


    };

    private BroadcastReceiver tempWarningReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String msg = intent.getStringExtra("warning_message");
            if (msg != null && !msg.isEmpty()) {
                showTempWarning(msg);
            } else {
                hideTempWarning();
            }
        }
    };
    public void showTempWarning(String msg) {
        runOnUiThread(() -> {
            tempWarningMessage.setText(msg);
            tempWarningMessage.setVisibility(View.VISIBLE);
        });
    }

    public void hideTempWarning() {
        runOnUiThread(() -> tempWarningMessage.setVisibility(View.GONE));
    }
    //================================================
    private static boolean seenNonWakeFrameSinceScreenOff = true;

    public static void markScreenJustOff() {
        seenNonWakeFrameSinceScreenOff = false;
    }

    public void markNonWakeFrameSeen() {
        seenNonWakeFrameSinceScreenOff = true;
    }

    public boolean hasSeenNonWakeFrame() {
        return seenNonWakeFrameSinceScreenOff;
    }

    batteryPackSwitcher BatPackSwitcher;
    private MediaPlayer tapSoundPlayer;
    private View rootView;
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;
    private Handler handler = new Handler();
    private boolean isRunning = false;
    private Runnable screenToggleRunnable;
    private boolean isScreenOn = true;
    //private Button btnRegister;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_login);
        serialPortManager = serialApplication.getSerialPortManager();
        IntentFilter filter = new IntentFilter("FRAME_RECEIVED");

        if (!isFrameReceiverRegistered) {
            LocalBroadcastManager.getInstance(this)
                    .registerReceiver(frameReceiver, new IntentFilter("FRAME_RECEIVED"));
            isFrameReceiverRegistered = true;
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(
                tempWarningReceiver, new IntentFilter("TEMP_WARNING_EVENT"));
        SoundPlayer.playOnce(this, R.raw.locksound2);  // `this` is Activity context

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
        tapSoundPlayer = MediaPlayer.create(this, R.raw.dottap33);
        TelemetryService.shdtelemetrywhenunlocked=false;
        instance = this;
        this.BatPackSwitcher = new batteryPackSwitcher();
        //bmsHandler.post(bmsRunnable);
        // Start inactivity timer with a unique code (e.g., 3 for login timeout)
        InactivityManager.getInstance().startTimer(3);


        etCredential = findViewById(R.id.etCredential);
        patternView = findViewById(R.id.patternView);
       // btnRegister = findViewById(R.id.btnRegister);
        tempWarningMessage = findViewById(R.id.tempWarningMessage);

        SwitchCompat switchAuthMethod = findViewById(R.id.switchAuthMethod);
        switchAuthMethod.setThumbDrawable(ContextCompat.getDrawable(this, R.drawable.custom_thumb_selector));
        switchAuthMethod.setTrackDrawable(ContextCompat.getDrawable(this, R.drawable.custom_track));
        View keypad = findViewById(R.id.customKeypad);

        patternView.setLoginMode(true);
        etCredential.setShowSoftInputOnFocus(false);

        try {
            prefs = EncryptedSharedPreferences.create(
                    "user_data",
                    MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
                    getApplicationContext(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            // --- DEFAULT PIN INITIALIZATION (runs only first time) ---
            SharedPreferences.Editor defaultEditor = prefs.edit();

            if (!prefs.contains("default_pin_enabled")) {
                defaultEditor.putBoolean("default_pin_enabled", true);
                defaultEditor.putInt("default_pin_remaining_attempts", 3);
                defaultEditor.apply();
            }

        } catch (Exception e) {
            e.printStackTrace();
            showCustomToast("Secure storage error");
            return;
        }

//        btnRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
//        btnRegister.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (getRegisteredRiderCount() >= 4) {
//                    showCustomToasterror("Maximum 4 Riders");
//                } else {
//                    Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
//                    startActivity(intent);
//                }
//            }
//        });


        // PIN entry listener
        etCredential.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (actualPin.length() == 4) {
                    validatePin();
                }
            }
        });
        // 添加开关状态变化监听器

        // Pattern login listener
        patternView.setOnPatternCompleteListener(pattern -> {
            String inputPattern = TextUtils.join("", pattern);
            for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
                if (entry.getKey().endsWith("_pattern") && inputPattern.equals(entry.getValue())) {
                    String username = entry.getKey().replace("user_", "").replace("_pattern", "");
                    loginSuccess(username);
                    return;
                }
            }
            playErrorSound();
            showCustomToasterror("Invalid Pattern OR Smart Key Not Found");
            vibrateError();
        });

        // Default: Show pattern
        switchToPatternMode();

        // Auth method toggle
        switchAuthMethod.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                switchToPinMode();
                playTapSound();
                switchAuthMethod.setText("Use Pattern instead");
            } else {
                switchToPatternMode();
                playTapSound();
                switchAuthMethod.setText("Use PIN instead");
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vibratorManager = (VibratorManager) getSystemService(VIBRATOR_MANAGER_SERVICE);
            vibrator = vibratorManager.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        }


        rootView = getWindow().getDecorView().getRootView();

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                //Log.d("Gesture", "Double tap detected on LoginActivity");
                onLoginDoubleTap(); // Custom method
                return true;
            }
        });

        // Attach touch listener to root view
        rootView.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));

//        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
//        wakeLock = powerManager.newWakeLock(
//                PowerManager.FULL_WAKE_LOCK |
//                        PowerManager.ACQUIRE_CAUSES_WAKEUP |
//                        PowerManager.ON_AFTER_RELEASE,
//                "MyApp:WakeLockTag"
//        );
//
//        View rootView = getWindow().getDecorView().getRootView();
//        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
//            @Override
//            public boolean onDoubleTap(MotionEvent e) {
//                toggleSequence();
//                return true;
//            }
//        });
//
//        rootView.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
//
        setupToggleRunnable();
        ahTrackerManager=AhTrackerManager.getInstance(this);
    }// END OF ONCREATE METHOD

    private void playErrorSound() {
        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.errorsoundtrim);
        if (mediaPlayer != null) {
            mediaPlayer.setOnCompletionListener(mp -> {
                mp.release(); // free resources
            });
            mediaPlayer.start();
        }
    }

    private void toggleSequence() {
        if (isRunning) {
            handler.removeCallbacks(screenToggleRunnable);
            isRunning = false;
            //Log.d("ScreenTest", "Sequence stopped.");
        } else {
            isRunning = true;
            handler.post(screenToggleRunnable);
            //Log.d("ScreenTest", "Sequence started.");
        }
    }

    private void setupToggleRunnable() {
        screenToggleRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isRunning) return;

                if (isScreenOn) {
                    // Turn screen off
                    try {
                        Method goToSleep = PowerManager.class.getMethod("goToSleep", long.class);
                        goToSleep.invoke(powerManager, SystemClock.uptimeMillis());
                        isScreenOn = false;
                        //Log.d("ScreenTest", "Screen turned OFF");
                    } catch (Exception e) {
                        //Log.e("ScreenTest", "Failed to turn off screen", e);
                    }
                    handler.postDelayed(this, 2000); // Wait 3s before turning ON
                } else {
                    // Turn screen on
                    if (!wakeLock.isHeld()) wakeLock.acquire(2000); // ON for 2s, then auto release
                    isScreenOn = true;
                    //Log.d("ScreenTest", "Screen turned ON");
                    handler.postDelayed(this, 4000); // Wait 4s before turning OFF
                }
            }
        };
    }
    private void onLoginDoubleTap() {
        try {
            LoginActivity.markScreenJustOff();
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            Method goToSleep = PowerManager.class.getMethod("goToSleep", long.class);
            goToSleep.invoke(powerManager, SystemClock.uptimeMillis());
//      InactivityManager.getInstance().startTimer(2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playTapSound() {
        if (tapSoundPlayer != null) {
            tapSoundPlayer.start();
        }
    }

    public void Onscreen()
    {
//        serialPortManager.VCUSendToMCU("");
    }

    private void switchToPatternMode() {
        etCredential.setVisibility(View.GONE);
        patternView.setVisibility(View.VISIBLE);
        findViewById(R.id.customKeypad).setVisibility(View.GONE);
        actualPin.setLength(0);
        etCredential.setText("");
    }

    private void switchToPinMode() {
        etCredential.setVisibility(View.VISIBLE);
        patternView.setVisibility(View.GONE);
        findViewById(R.id.customKeypad).setVisibility(View.VISIBLE);
        actualPin.setLength(0);
        etCredential.setText("");
    }

    private int failedAttempts = 0;
    private static final int MAX_ATTEMPTS = 20;

    private void validatePin() {
        String inputPin = actualPin.toString();

        try {
            SharedPreferences encryptedPrefs = EncryptedSharedPreferences.create(
                    "user_data",   // <- must match riderProfilesFragment
                    MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
                    this,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            // ==============================
            // DEFAULT PIN CHECK (1234)
            // ==============================
            boolean defaultEnabled = encryptedPrefs.getBoolean("default_pin_enabled", false);
            int remaining = encryptedPrefs.getInt("default_pin_remaining_attempts", 0);

            if (defaultEnabled && inputPin.equals("1234")) {
                if (remaining > 0) {
                    remaining--;
                    encryptedPrefs.edit().putInt("default_pin_remaining_attempts", remaining).apply();

                    if (remaining == 0) {
                        encryptedPrefs.edit().putBoolean("default_pin_enabled", false).apply();
                    }

                    loginSuccess("RIVOT ADMIN(" + remaining + " tries left)");
                    return;
                } else {
                    encryptedPrefs.edit().putBoolean("default_pin_enabled", false).apply();
                    actualPin.setLength(0);
                    etCredential.setText("");
                    return;
                }
            }
            // END DEFAULT PIN CHECK
            // ==============================

            // Iterate all saved pins and compare
            for (Map.Entry<String, ?> entry : encryptedPrefs.getAll().entrySet()) {
                if (entry.getKey().endsWith("_pin") ) {
                    String username = entry.getKey().replace("user_", "").replace("_pin", "");
                    String storedSalt = encryptedPrefs.getString("user_" + username + "_salt", "");
                    String storedHash = (String) entry.getValue();

                    // if there's no salt or hash skip
                    if (storedSalt == null || storedSalt.isEmpty() || storedHash == null) continue;

                    String hashedInput = hashWithSalt(inputPin, storedSalt);

                    if (hashedInput != null && hashedInput.equals(storedHash)) {
                        failedAttempts = 0; // reset
                        loginSuccess(username);
                        return;
                    }
                }
            }

            // FAILED ATTEMPTS
            failedAttempts++;
            if (failedAttempts >= MAX_ATTEMPTS) {
                showCustomToasterror("Too many failed attempts. Please try again later.");
                playErrorSound();
                vibrateError();
            } else {
                playErrorSound();
                showCustomToasterror("Invalid PIN");
                vibrateError();
            }

            actualPin.setLength(0);
            etCredential.setText("");

        } catch (Exception e) {
            e.printStackTrace();
            playErrorSound();
            showCustomToasterror("Error during PIN validation");
            actualPin.setLength(0);
            etCredential.setText("");
        }
    }

    int Level1_Line_Current = 35;
    int Level2_Line_Current = 65;
    int Level3_Line_Current = 95;
    int Level4_Line_Current = 110;

    public void loginSuccess(String username) {
        showCustomToast("Welcome " + username + " !");
        if(serialPortManager!=null){
            serialPortManager.VCUSendToMCU("FFAA550101000A01");
            new Handler(Looper.getMainLooper()).postDelayed(
                    () -> serialPortManager.VCUSendToMCU("FFAA550107000A01"), 100);
        }
//        UIUpdater.telecurrentrider=username;
        TelemetryDataManager.getInstance().setTeleCurrentRider(username);
        actualPin.setLength(0);
        etCredential.setText("");
        ChargingUiClosedByUserFlag = false;

        //InactivityManager.getInstance().stopTimer();

        UIUpdater.isAPUPresent=false;

        // Launch MainActivity first (so UI doesn't freeze)
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();

        // Run heavy logic and sound AFTER activity transition
        new Thread(() -> {

            if (MainActivity.instance != null) {
                MainActivity.instance.runPostLoginInit();
            }

            SharedPreferences.Editor editor = prefs.edit();

            String prevRider = prefs.getString("last_logged_in_user", null);
            boolean isSameRider = username.equals(prevRider);

            if (!isSameRider && !username.equals("Key Rider")) {
                // Fetch user data
                String age = prefs.getString("user_" + username + "_age", "N/A");
//                String maxSpeedStr = prefs.getString("user_" + username + "_lucky", "0");
                String maxSpeedStr = prefs.getString("user_" + username + "_maxspeed", "0");


                Log.d("UserData", "Username: " + username);
                Log.d("UserData", "Age: " + age);
                Log.d("UserData", "Max Speed: " + maxSpeedStr);
//                Log.d("UserData", "Max Torque: " + maxTorque);

                // Convert speed string to int
                int maxSpeed = 0;
                try {
                    maxSpeed = Integer.parseInt(maxSpeedStr);
                } catch (NumberFormatException e) {
                    Log.e("ParseError", "Invalid maxSpeed: " + maxSpeedStr);
                }

                // Speed calculation
                double numerator = maxSpeed * 3.3125 * 1000;
                double denominator = 60.0 * 1.4825;
                int xRounded = (int) Math.round(numerator / denominator);
                String speedHex = String.format("%04X", xRounded);
                String speedByte01 = speedHex.substring(2, 4); // low byte
                String speedByte02 = speedHex.substring(0, 2); // high byte
                editor.putString("user_" + username + "_speedByte01", speedByte01);
                editor.putString("user_" + username + "_speedByte02", speedByte02);
                editor.putString("last_logged_in_user", username);
                editor.commit(); // synchronous write (okay in background thread)

                // Torque mapping
//                int lineCurrent;
//                switch (maxTorque) {
//                    case "Medium": lineCurrent = Level2_Line_Current; break;
//                    case "Rush":   lineCurrent = Level3_Line_Current; break;
//                    case "Insane": lineCurrent = Level4_Line_Current; break;
//                    case "Easy":
//                    default:       lineCurrent = Level1_Line_Current; break;
//                }
//
//                int torqueValue = lineCurrent * 4;
//                String torqueHex = String.format("%04X", torqueValue);
//                String torqueByte01 = torqueHex.substring(2, 4); // low byte
//                String torqueByte02 = torqueHex.substring(0, 2); // high byte

                // Frame templates
                SharedPreferences boostPrefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                String durationHex = boostPrefs.getString("boost_duration_hex", "00");
                String intervalHex = boostPrefs.getString("boost_interval_hex", "00");

                String finalSpeedFrame = "FFAA550103001510FCA807" + speedByte01 + speedByte02 + "00000000" + durationHex + intervalHex;
//                String finalTorqueFrame = "FFAA550103001510FBA807" + torqueByte01 + torqueByte02 + "000088040000";
//                String finalTorqueFrame = "FFAA550103001510FBA807" + torqueByte01 + torqueByte02 + "7800B004A000";//max phase 300A ,  regen currents as 30A(7800) and 40A(A000)


//                Log.d("SeekBarStable", "Final Speed Frame (with boost): " + finalSpeedFrame);
//                Log.d("SeekBarStable", "Final Torque Frame: " + finalTorqueFrame);
//                Log.d("SeekBarStable", "MAXSPEED=" + maxSpeed + ", x=" + xRounded + ", Hex=" + speedByte02 + " " + speedByte01);
//                Log.d("SeekBarStable", "torque=" + lineCurrent + ", x=" + torqueHex + ", Hex=" + torqueByte02 + " " + torqueByte01);
//
//                Log.d("asdfdg","ENTER Login");

                // Send frames
                if (serialPortManager != null) {
                    serialPortManager.VCUSendToMCU(finalSpeedFrame);
                    Log.d("asdfdg","ENTER Succes Login cmd snet  "+finalSpeedFrame);
                    new Handler(Looper.getMainLooper()).postDelayed(() -> serialPortManager.VCUSendToMCU(finalSpeedFrame), 1000);
                    new Handler(Looper.getMainLooper()).postDelayed(() -> serialPortManager.VCUSendToMCU(finalSpeedFrame), 2000);
//                    new Handler(Looper.getMainLooper()).postDelayed(() -> serialPortManager.VCUSendToMCU("FFAA550103001510F8A8070000000000007387"), 2500);
//                    new Handler(Looper.getMainLooper()).postDelayed(() -> serialPortManager.VCUSendToMCU("FFAA550103001510F8A8070000000000007387"), 3000);

                }
                else{
                    Log.d("asdfsgd","THE BATTERY IS "+serialPortManager.getHb3().getBatteryLevel()/10) ;
                }
            }//END OF SAME USER

            String maxTorque = prefs.getString("user_" + username + "_torque", "Easy");
            Log.d("fhghjjk","THISISMAXTORUE "+maxTorque);
            int lineCurrent;
            switch (maxTorque) {
                case "Medium": lineCurrent = Level2_Line_Current; break;
                case "Rush":   lineCurrent = Level3_Line_Current; break;
                case "Insane": lineCurrent = Level4_Line_Current; break;
                case "Easy":   lineCurrent = Level1_Line_Current; break;
                default:       lineCurrent = Level4_Line_Current; break;
            }

            int torqueValue = lineCurrent * 4;
            String torqueHex = String.format("%04X", torqueValue);
            String torqueByte01 = torqueHex.substring(2, 4); // low byte
            String torqueByte02 = torqueHex.substring(0, 2); // high byte


            //                    (serialPortManager.getHb3().getBatteryLevel() / 10 < 80)
//                            ? "FFAA550103001510FBA807" + torqueByte01 + torqueByte02 + "8C00B004B400"//to set regen currents as 30A(7800) and 40A(A000)- 35A(8C00),45A(B400)
//                            : "FFAA550103001510FBA807" + torqueByte01 + torqueByte02 + "1800B0042000";//to set regen currents as 6A(1800) and 8A(2000)
            String torqueFrame;
//            int batteryLevel = serialPortManager.getHb3().getBatteryLevel() / 10;
            float batteryLevel = ahTrackerManager.getSOC();


            if (batteryLevel ==0) { // phase max , all normal
                torqueFrame = "FFAA550103001510FBA807" + torqueByte01 + torqueByte02 + "A000B004C800";
                Log.d("SEDINGSOCFRAMEAS","0 "+torqueFrame);
            }
            else if (batteryLevel <= 10 && !UIUpdater.apuflag) { // phase 0 , all normal
                torqueFrame = "FFAA550103001510FBA807" + torqueByte01 + torqueByte02 + "A0000000C800";
                Log.d("SEDINGSOCFRAMEAS","10 "+torqueFrame);

            }
            else if (batteryLevel <= 20) { //  line 15A, phase max, all normal
                torqueFrame = "FFAA550103001510FBA8073C00A000B004C800";
                Log.d("SEDINGSOCFRAMEAS","20 "+torqueFrame);

            } else if (batteryLevel <= 90) { // phase max, all normal  A000(45A regen) - C800(50A)
                torqueFrame = "FFAA550103001510FBA807" + torqueByte01 + torqueByte02 + "A000B004C800";
                Log.d("SEDINGSOCFRAMEAS","90 "+torqueFrame);

            } else if(batteryLevel<=100) { // batteryLevel > 80 && <= 100, phase max, Regen 6 and 8 A
                torqueFrame = "FFAA550103001510FBA807" + torqueByte01 + torqueByte02 + "1800B0042000";
                Log.d("SEDINGSOCFRAMEAS","100 "+torqueFrame);

            }
            else{//all max
                torqueFrame = "FFAA550103001510FBA807" + torqueByte01 + torqueByte02 + "A000B004C800";
                Log.d("SEDINGSOCFRAMEAS","else "+torqueFrame);

            }

            final String finalTorqueFrame = torqueFrame; // make it final for lambdas
            Log.d("dacsdvsf","THISISCONTROLECOMS  "+finalTorqueFrame);
            new Handler(Looper.getMainLooper()).postDelayed(
                    () -> serialPortManager.VCUSendToMCU(finalTorqueFrame), 3500);
            new Handler(Looper.getMainLooper()).postDelayed(
                    () -> serialPortManager.VCUSendToMCU(finalTorqueFrame), 4000);
            new Handler(Looper.getMainLooper()).postDelayed(
                    () -> serialPortManager.VCUSendToMCU(finalTorqueFrame), 4500);
            new Handler(Looper.getMainLooper()).postDelayed(
                    () -> serialPortManager.VCUSendToMCU(finalTorqueFrame), 5000);
            new Handler(Looper.getMainLooper()).postDelayed(
                    () -> serialPortManager.VCUSendToMCU(finalTorqueFrame), 5500);
            new Handler(Looper.getMainLooper()).postDelayed(
                    () -> serialPortManager.VCUSendToMCU(finalTorqueFrame), 6000);
//            new Handler(Looper.getMainLooper()).postDelayed(
//                    () -> serialPortManager.VCUSendToMCU("FFAA550103001510F8A8070000000000007387"), 6500);

            // Always set login status (apply is safe here)
            editor.putBoolean("is_logged_in", true);
            editor.apply();

            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    UIUpdater.ignorefirstblink = false;
                }
            }, 4200); // 4200 milliseconds = 4.2 seconds

            // 🎵 Play unlock sound on UI thread after delay
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                MediaPlayer mp = MediaPlayer.create(LoginActivity.this, R.raw.unlocked);
                if (mp != null) {
                    mp.setOnCompletionListener(MediaPlayer::release);
                    mp.start();
                }

            }, 200);

        }).start();
    }




//below is b4 7/7
//        mediaPlayer = MediaPlayer.create(this, R.raw.unlocked);
//        mediaPlayer.start();
//
//        Intent intent = new Intent(this, MainActivity.class);
//        startActivity(intent);
//        overridePendingTransition(0, 0); // or fade_in/fade_out
//
//        //finish(); // optional, to avoid returning back to login
//    }
    //---------------------------
    //finish(); // optional, to avoid returning back to login


    private String hashWithSalt(String input, String saltBase64) {
        try {
            byte[] saltBytes = Base64.decode(saltBase64, Base64.NO_WRAP);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(saltBytes);

            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
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
    private void showCustomToasterror(String message) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_toast_errror, null);
        ImageView icon = layout.findViewById(R.id.toast_icon);
        TextView text = layout.findViewById(R.id.toast_text);
        icon.setImageResource(R.drawable.toastlogo);
        text.setText(message);
        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }


    public void onKeypadButtonClick(View view) {

        playTapSound();
        String tag = view.getTag().toString();
        vibrateTap();
        if ("del".equals(tag)) {
            if (actualPin.length() > 0) {
                actualPin.deleteCharAt(actualPin.length() - 1);
            }
        } else if (actualPin.length() < 4) {
            actualPin.append(tag);
        }

        // Update EditText with dots only
        StringBuilder masked = new StringBuilder();
        for (int i = 0; i < actualPin.length(); i++) {
            masked.append("•");
        }
        etCredential.setText(masked.toString());

        //  NEW PART: Check if PIN entered fully
        if (actualPin.length() == 4) {
            validatePin();
        }
    }

    private void vibrateError() {
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        }
    }

    private void vibrateTap() {
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        }
    }

    //    @Override
//    public void onInactivityTimeout(int code) {
//        Log.d("LoginActivity", "Inactivity timeout triggered with code: " + code);
//
//        if (code == 3) {
//            Log.d("LoginActivity", "Timeout in login screen. Turning screen off...");
//
//            try {
//                markScreenJustOff();
//                PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
//                Method goToSleep = PowerManager.class.getMethod("goToSleep", long.class);
//                goToSleep.invoke(powerManager, SystemClock.uptimeMillis());
////                InactivityManager.getInstance().startTimer(2);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//        }
//
//    }
    public void wakeScreen() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager != null && !powerManager.isInteractive()) {
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                    PowerManager.FULL_WAKE_LOCK |
                            PowerManager.ACQUIRE_CAUSES_WAKEUP |
                            PowerManager.ON_AFTER_RELEASE,
                    "MyApp:WakeLockTag"
            );
            wakeLock.acquire(3000); // Wake screen for 3 seconds
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        InactivityManager.getInstance().onUserInteraction();
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //InactivityManager.getInstance().stopTimer();
        instance = null; // clean up
        TelemetryService.shdtelemetrywhenunlocked=true;
        LocalBroadcastManager.getInstance(this).unregisterReceiver(tempWarningReceiver);

//        if(!UIUpdater.shouldshowchargingicononmainonot){
//            Log.d("dsfdbg","UNRegistering");
//                    try {
//                        Log.d("dsfdbg","INSIDETRYUNRegistering");
//            LocalBroadcastManager.getInstance(this).unregisterReceiver(frameReceiver);
//        } catch (IllegalArgumentException e) {
//            e.printStackTrace(); // safe guard
//        }
//        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        InactivityManager.getInstance().stopTimer();

        delayedLockHandler.removeCallbacks(delayedLockRunnable);
        //Log.d("LoginActivity", "Cancelled delayed lock due to pause");


        if (chargingUIFlagRunnable != null) {
            chargingUIFlagHandler.removeCallbacks(chargingUIFlagRunnable);
            Log.d("LoginActivity", "Charging UI flag delay cancelled");
        }
    }

    private Handler delayedLockHandler = new Handler(Looper.getMainLooper());
    private Runnable delayedLockRunnable;
    private Handler chargingUIFlagHandler = new Handler(Looper.getMainLooper());
    private Runnable chargingUIFlagRunnable;
    private Handler resumehandler = new Handler(Looper.getMainLooper());
    private Runnable resetChargingUIFlagRunnable;
    AhTrackerManager ahTrackerManager;
    @Override
    protected void onResume() {
        super.onResume();
        MainActivity.MainisActive = false;
        MainActivity.isVisible = true;

        if(UIUpdater.getInstance()!=null){
            UIUpdater.getInstance().ReadystopBeeping();
            UIUpdater.getInstance().stopReverseBeep();
            UIUpdater.getInstance().stopBlinking();
        }

//        int batteryLevel=serialPortManager.getHb3().getBatteryLevel() / 10;
        int batteryLevel=(int)ahTrackerManager.getSOC();

        String sBatteryLevel = String.valueOf(batteryLevel);
        if(batteryLevel>=0 && batteryLevel<=100){
            binding.loginSoc.setText("Battery " + batteryLevel + "%");
        }
        UIUpdater.ignorefirstblink=true;
        UIUpdater.islock=false;
        TelemetryService.shdtelemetrywhenunlocked=false;

//        serialPortManager.getHb3().setLEDresponse(1);
//        serialPortManager.getHb3().sethandleLockresponse(1);
        EVLockService.getInstance().startLockSequence();    // Start lock
//        serialPortManager.VCUSendToMCU("FFAA550101000A00");
//        new Handler(Looper.getMainLooper()).postDelayed(
//                () -> serialPortManager.VCUSendToMCU("FFAA550107000A00"), 100);
//        // First command (immediately)
//        serialPortManager.VCUSendToMCU("FFAA550101000A00");
//
//// Second command after 1 second
//        new Handler(Looper.getMainLooper()).postDelayed(() -> {
//            serialPortManager.VCUSendToMCU("FFAA550107000A00");
//        }, 1000);
//
//// Repeat first command after 2 seconds
//        new Handler(Looper.getMainLooper()).postDelayed(() -> {
//            serialPortManager.VCUSendToMCU("FFAA550101000A00");
//        }, 2000);
//
//// Repeat second command after 3 seconds
//        new Handler(Looper.getMainLooper()).postDelayed(() -> {
//            serialPortManager.VCUSendToMCU("FFAA550107000A00");
//        }, 3000);

//        // 在活动恢复时重新打开串口
//        serialPortManager.initSerialConfig(this);
//        mHandlerTime.postDelayed(mRunnableTime, 0);
        // Re-assign the callback and restart the timer when coming back to LoginActivity

        InactivityManager.getInstance().startTimer(3);
        // Cancel any previous delayed task to avoid overlap


        if (resetChargingUIFlagRunnable != null) {
            resumehandler.removeCallbacks(resetChargingUIFlagRunnable);
        }


        // Post it to run after 10 seconds (10,000 milliseconds)
        resumehandler.postDelayed(resetChargingUIFlagRunnable, 10_000);
        // Cancel any previous callback to avoid stacking
        if (chargingUIFlagRunnable != null) {
            chargingUIFlagHandler.removeCallbacks(chargingUIFlagRunnable);
        }

        chargingUIFlagRunnable = () -> {
            ChargingUiClosedByUserFlag = false;
            isChargingUIActive = false;
            //Log.d("LoginActivity", "Charging UI flags reset after delay");
        };

        //this was bcz when user comes back from chargingUI on login then wait on login for 15 seconds then start again chargingUI
        chargingUIFlagHandler.postDelayed(chargingUIFlagRunnable, 15000);

//        delayedLockRunnable = () -> {
//            Heartbeat1 hb1 = serialPortManager.getHb1();
//            if (hb1.getDragonLock() == 1 || hb1.getLED() == 1) {
//                //Log.d("LoginActivity", "Locking after delay...");
//                EVLockService.getInstance().startLockSequence();
//            } else {
//                //Log.d("LoginActivity", "Scooter already locked, skipping.");
//            }
//        };
//        delayedLockHandler.postDelayed(delayedLockRunnable, 5000);
    }



    private  String canId;

    private String getloginCanid()
    {
        return canId;
    }
    public void loginsetcanid(String i)
    {
        this.canId=i;
    }
    private String getloginCandata()
    {
        return canId;
    }
    public void loginsetcandata(String i)
    {
        this.canId=i;
    }

    //------------------------------------------------------

//    public  static  boolean isSequenceActive = false;
//    public  static boolean universalflag=false;
//    private final int MAX_RETRIES = 70;
//    public  static  int retryCount1 = 0;
//    public  static  int retryCount2 = 0;
//    public void EVlockSequence() {
//        isSequenceActive = true;
//        evlocktask1(); // start task1 directly
//    }
//
//    private void evlocktask1() {
//        if (!isSequenceActive) return; // Exit if stopped
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
//                if (isSequenceActive) evlocktask1();
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
//        if (!isSequenceActive) return; // Exit if stopped
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
//                if (isSequenceActive) evlocktask2();
//            }, 1000);
//        } else if (EVloccommand2Success) {
//            Log.d("CommandSequence" , "Lock sequence completed successfully");
//            universalflag=true;
//            retryCount2 = 0;
//            hb3.setLEDresponse(0);
//            isSequenceActive = false;
//        } else {
//            Log.e("CommandSequence", "Command 2 failed after retries");
//            isSequenceActive = false;
//            universalflag=true;
//        }
//    }

    private int getRegisteredRiderCount() {
        int count = 0;
        try {
            SharedPreferences sharedPreferences = EncryptedSharedPreferences.create(
                    "user_data",
                    MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
                    LoginActivity.this ,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            Set<String> uniqueUsernames = new HashSet<>();
            for (Map.Entry<String, ?> entry : sharedPreferences.getAll().entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("user_") && key.contains("_age")) {
                    String username = key.replace("user_", "").replace("_age", "");
                    uniqueUsernames.add(username);
                }
            }
            count = uniqueUsernames.size();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    private void wakeScreenIfNeeded()
    {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager != null && !powerManager.isInteractive()) {
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "YourApp:ChargingWakeLock"
            );
            wakeLock.acquire(3000); // keep screen on for 3 seconds
        }
    }

    @Override
    public void onBackPressed() {
        // Prevent user from bypassing login screen
    }

    private final Handler bmsHandler = new Handler(Looper.getMainLooper());
    private int requestIndex = 0;

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
            }

            requestIndex = (requestIndex + 1) % 8; // 0 → 1 → ... → 6 → 0
            bmsHandler.postDelayed(this, 650); // 650ms interval
        }
    };
}

