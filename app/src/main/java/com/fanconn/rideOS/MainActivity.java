package com.fanconn.rideOS;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

import tp.xmaihh.serialport.bean.ComBean;

import com.fanconn.rideOS.authen.LoginActivity;
import com.fanconn.rideOS.databinding.ActivityMainBinding;
import com.fanconn.rideOS.mcuupdate.YMode;

/**
 * 主活动类，负责界面初始化和串口通信管理
 */
public class MainActivity extends AppCompatActivity implements FrameDataListener {
    public SerialPortManager serialPortManager;  // 串口管理类
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
    private UIUpdater uiUpdater;   // UI 更新管理器，用于更新界面上的数据和视图
    private Runnable mRunnableTime;  // 用于定时更新系统时间显示的任务
    private boolean isSIMCardExist = false;
    private boolean isSDCardExist = false;
    private Handler mHandlerTime = new Handler();
    private YMode yMode;
    private ProgressDialog progressDialog;

//    private ImageButton menubackbtn;

    // 处理接收到的数据的 Handler
    public Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            ComBean comBean = (ComBean) msg.obj;
            rxText = SerialPortManager.processReceivedData(comBean, RX_type);
            //Log.e("handleMessage", "原始串口数据: " + rxText);

            mHandler.postDelayed(new Runnable() { // 延迟处理数据，避免频繁操作导致 CPU 占用率过高
                @Override
                public void run() {
                    List<String> frames = SerialPortManager.splitIntoFrames(rxText);
                    for (String frame : frames) {
                        if(frame.startsWith("FFAA770303001518904001")){
                            Log.d("BATTRY","THIS IS BAT UI FRAME"+frame);

                        }
                        Log.e("handleMessage", "数据帧: " + frame);

                        parseFrame(frame); // 根据帧头处理不同类型的数据
                    }

                    rxText = "";   // 清空 rxText，避免数据过大
                }
            }, 10);  // 每 10 毫秒处理一次数据

            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot()); // 将生成的根视图设置为活动的内容视图

        // 隐藏状态栏和导航栏，设置为全屏
        setFullScreen();

        updateWifiIpAddress();
// Keep the dashboard display on while MainActivity is visible.
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );
//        menubackbtn = findViewById(R.id.menubackbtn);
//        menubackbtn.setOnClickListener(v -> {
//            Intent intent = new Intent(
//                    MainActivity.this,
//                    LoginActivity.class
//            );
//            startActivity(intent);
//        });
        // 初始化class
        serialPortManager = SerialPortManager.getInstance(sPort, iBaudRate, RX_type, mHandler);
        serialPortManager.initSerialConfig(this);
        uiUpdater = new UIUpdater(binding, this);
        uiUpdater.setupSpeedDoubleTap();
        yMode = new YMode(this);

        // 获取蓝牙MAC地址和 mcu ota版本
        serialPortManager.VCUSendToMCU("FFAA550200000A01"); // 获取蓝牙 MAC 地址
        serialPortManager.VCUSendToMCU("FFAA550204000A01"); // 获取 MCU 版本信息
        acquisitionTime();
        startSocRequests();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isLoginActivityLaunching = false;
        acquisitionTime();

        // Initialize UART only if it is not already open.
        serialPortManager.initSerialConfig(this);
        serialPortManager.VCUSendToMCU("FFAA550107000A01");
        // Request unlock.
        scooterPowerManager.unlock(
                command -> serialPortManager.VCUSendToMCU(command)
        );

        // Start charging status monitoring while dashboard is visible.
        if (uiUpdater != null) {
            uiUpdater.startChargingUiWatcher();
        }

        mHandlerTime.removeCallbacks(mRunnableTime);
        mHandlerTime.postDelayed(mRunnableTime, 0);
    }
    public static boolean isLoginActivityLaunching = false;

    @Override
    protected void onPause() {
        super.onPause();
        if (uiUpdater != null) {
            uiUpdater.stopChargingUiWatcher();
        }
        if (isLoginActivityLaunching) {
            // Lock only when LoginActivity is being launched.
            scooterPowerManager.lock(
                    command -> serialPortManager.VCUSendToMCU(command)
            );


            Log.d("MainActivity", "LoginActivity launching: scooter lock requested");
        } else {
            Log.d("MainActivity", "Activity paused: lock skipped");
        }

        mHandlerTime.removeCallbacks(mRunnableTime);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 关闭串口
        serialPortManager.closeSerialPort();

        // 停止 UI 更新
        uiUpdater.stopUpdating();

        //销毁更新时间的任务
        mHandlerTime.removeCallbacks(mRunnableTime);
    }

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
     * 解析数据帧
     * @param frame 数据帧
     */
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

    private void setFullScreen() {
        // 设置窗口标志，启用全屏模式
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // 获取装饰视图
        View decorView = getWindow().getDecorView();

        // 设置系统 UI 选项，隐藏导航栏和状态栏，启用粘性沉浸模式
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
    }

    private void updateWifiIpAddress() {
        TextView ipTextView = findViewById(R.id.ip);

        WifiManager wifiManager =
                (WifiManager) getApplicationContext()
                        .getSystemService(Context.WIFI_SERVICE);

        if (wifiManager == null) {
            ipTextView.setText("IP: Unavailable");
            return;
        }

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        if (wifiInfo != null && wifiInfo.getNetworkId() != -1) {
            int ipAddress = wifiInfo.getIpAddress();

            if (ipAddress != 0) {
                String ip = Formatter.formatIpAddress(ipAddress);
                ipTextView.setText("IP: " + ip);
            } else {
                ipTextView.setText("IP: Unavailable");
            }
        } else {
            ipTextView.setText("IP: Wi-Fi disconnected");
        }
    }

    private HandlerThread socRequestThread;
    private Handler socRequestHandler;

    private static final long SOC_REQUEST_INTERVAL_MS = 2000;

    private final Runnable socRequestRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                SerialPortManager.getInstance().VCUSendToMCU(
                        "FFAA5501030015189001400001020304050607"
                );
            } catch (Exception e) {
                Log.e("SOC_REQUEST", "Failed to send SOC request", e);
            }

            // Schedule the next request after 2 seconds
            if (socRequestHandler != null) {
                socRequestHandler.postDelayed(
                        this,
                        SOC_REQUEST_INTERVAL_MS
                );
            }
        }
    };

    private void startSocRequests() {
        // Start only once; never create duplicate threads
        if (socRequestThread != null) {
            return;
        }

        socRequestThread = new HandlerThread("SOCRequestThread");
        socRequestThread.start();

        socRequestHandler = new Handler(socRequestThread.getLooper());

        // Send the first request immediately
        socRequestHandler.post(socRequestRunnable);
    }

}
