package com.fanconn.rideOS;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.fanconn.rideOS.mcuupdate.MySerialHelper;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import tp.xmaihh.serialport.bean.ComBean;
import tp.xmaihh.serialport.utils.ByteUtil;

/**
 * 串口管理类，用于初始化和管理串口通信
 */
public class SerialPortManager {

    static {
        // 加载包含本地方法实现的库
        System.loadLibrary("native-lib");
    }
    public  native void clearSendBufferNative(int fd);
    private MySerialHelper serialHelper;  // 串口操作接口
    //private MySerialHelper mySerialHelper;
    private String sPort;  // 串口地址
    private int iBaudRate;  // 波特率
    private int RX_type;  // 数据格式类型
    private Handler mHandler;  // 处理接收数据的 Handler

    // 单例模式相关
    private static volatile SerialPortManager instance;
    public Heartbeat1 hb1;
    private Heartbeat2 hb2;
    public Heartbeat3 hb3;
    public McuAnswerData mad;

    // VCU 下发数据给 MCU
    private static final int NUM_ATTEMPTS = 20;  // 最大尝试次数
    private static final int INTERVAL_MS = 500;  // 每次尝试之间的时间间隔，单位为毫秒
    private String mcuData; // 保存传递的 MCU 数据字符串
    private int currentAttempt = 0;   // 当前的尝试次数计数

    private Handler handlerVcuToMcu = new Handler();  // 用于在主线程中调度任务的 Handler
    private Runnable VcuToMcTask = new Runnable() {   //  VCU 下发数据给 MCU
        @Override
        public void run() {
            if (currentAttempt < NUM_ATTEMPTS) {
                // 发送获取 MCU 版本信息数据的命令
                getMcuDate(mcuData);

                currentAttempt++;

                // 在指定的时间间隔后再次执行该任务
                handlerVcuToMcu.postDelayed(this, INTERVAL_MS);
            }
        }
    };
    public static SerialPortManager peekInstance() {
        return instance;
    }
    public static synchronized SerialPortManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SerialPortManager is not initialized. Call initInstance() first.");
        }
        return instance;
    }

    /**
     * 构造函数
     * @param port 串口地址
     * @param baudRate 波特率
     * @param rxType 数据格式类型（Hex 或 Txt）
     * @param handler 用于处理接收数据的 Handler
     */
    public SerialPortManager(String port, int baudRate, int rxType, Handler handler) {
        this.sPort = port;
        this.iBaudRate = baudRate;
        this.RX_type = rxType;
        this.mHandler = handler;
        this.hb1 = new Heartbeat1();
        this.hb2 = new Heartbeat2();
        this.hb3 = new Heartbeat3();
        this.mad = new McuAnswerData();
    }

    /**
     * 获取 SerialPortManager 的唯一实例
     * @param port 串口地址
     * @param baudRate 波特率
     * @param rxType 数据格式类型（Hex 或 Txt）
     * @param handler 用于处理接收数据的 Handler
     * @return SerialPortManager 实例
     */
    public static SerialPortManager getInstance(String port, int baudRate, int rxType, Handler handler) {
        if (instance == null) {
            synchronized (SerialPortManager.class) {
                if (instance == null) {
                    instance = new SerialPortManager(port, baudRate, rxType, handler);
                }
            }
        }
        return instance;
    }

    /**
     * 初始化串口配置
     * @param context 上下文
     */
    private Context context;
    public void initSerialConfig(Context context) {
        this.context = context.getApplicationContext(); // ✅ store once

        serialHelper = new MySerialHelper(sPort, iBaudRate) {
            @Override
            protected void onDataReceived(ComBean paramComBean) {
                Message message = mHandler.obtainMessage();
                message.obj = paramComBean;
                mHandler.sendMessage(message);
            }
        };

        try {
            serialHelper.open();
            OutputStream outputStream = serialHelper.getOutputStream();
            serialHelper.setOutputStream(outputStream);
            Log.d("SerialPortManager", "串口打开成功");
        } catch (IOException e) {
            Log.e("SerialPortManager", "串口打开失败", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 关闭串口
     */
    public void closeSerialPort() {
        if (serialHelper != null) {
            serialHelper.close();
            serialHelper = null;
            Log.d("SerialPortManager", "串口关闭成功");
        }
    }

    /**
     * 发送串口数据
     * @param sPortData 数据内容
     * @param type 0：type_Hex  1：type_Txt  2：type_byte
     */
    public void sendSerialPort(String sPortData, int type) {
        if (!serialHelper.isOpen()) {
            Log.d("SerialPortManager", "" + R.string.tips_serial_port_not_open);
            return;
        }
        switch (type) {
            case 1:
                serialHelper.sendTxt(sPortData);
                break;
            case 0:
                serialHelper.sendHex(sPortData);
                break;
            case 2:
                serialHelper.send(sPortData.getBytes());
                break;
            default:
                Log.d("SerialPortManager", "未知的RX_type: " + RX_type);
                break;
        }
    }

    /**
     * 发送串口数据
     * @param sPortData 数据内容
     */
    public void sendTxtSerialPort(String sPortData) {
        if (!serialHelper.isOpen()) {
            Log.d("SerialPortManager", "" + R.string.tips_serial_port_not_open);
            return;
        }
        serialHelper.sendTxt(sPortData);
    }

    /**
     * 发送串口数据
     * @param sPortData 数据内容
     */
    public void sendHexSerialPort(String sPortData) {
        if (!serialHelper.isOpen()) {
            Log.d("SerialPortManager", "" + R.string.tips_serial_port_not_open);
            return;
        }
        serialHelper.sendHex(sPortData);
    }

    /**
     * 发送串口数据
     * @param sPortData 数据内容
     */
    public void sendByteSerialPort(byte[] sPortData ) {
        if (!serialHelper.isOpen()) {
            Log.d("SerialPortManager", "" + R.string.tips_serial_port_not_open);
            return;
        }

        serialHelper.send(sPortData);
    }

    /**
     * 发送获取 MCU 数据的命令到串口。
     * 将指定的数据字符串添加校验码后，以十六进制格式发送到串口。
     *
     * @param sPortData 待发送的数据字符串，格式如 "FFAA550200000A01"
     */
    public void getMcuDate(String sPortData) {
        // Log.d("SerialPortManager", "getMcuDate");
        int crc = FrameUtils.produceChecksum(sPortData);  // 计算校验和
        String crcHexString = FrameUtils.toHexString(crc, 4);  // 将校验和转换为指定长度的十六进制字符串
        sPortData = sPortData + crcHexString;  // 将校验和添加到原始数据字符串中
        if(serialHelper!=null){
        serialHelper.sendHex(sPortData);}  // 以十六进制格式发送带有校验和的数据到串口
    }

    /**
     * VCU下发数据给MCU
     */

//    public void VCUSendToMCU(String data) {
//        mcuData = data;
//        currentAttempt = 0;
//        handlerVcuToMcu.post(VcuToMcTask);
//
//        handlerVcuToMcu.postDelayed(() -> {
//            mcuData = data;
//            currentAttempt = 0;
//            //Log.d("sdfs","VCUSENDTOMCUMETHODS "+data);
//            handlerVcuToMcu.post(VcuToMcTask);
//        }, 200);
//    }

    public void VCUSendToMCU(String data) {

        //  CHECK HANDLE LOCK CONDITION HERE
        if ("FFAA550101000A00".equalsIgnoreCase(data)) {
            if (context == null) {
                //Log.e("MCU", "Context not initialized! Sending anyway as fallback");
            } else {
                SharedPreferences prefs = context.getSharedPreferences("feature_prefs", Context.MODE_PRIVATE);
                boolean isHandleEnabled = prefs.getBoolean("handle_lock_enabled", true);

                if (isHandleEnabled) {
                    //Log.d("MCU", "Handle Lock command BLOCKED");
                    return;
                }
            }
        }

        // Normal flow
        mcuData = data;
        currentAttempt = 0;
        handlerVcuToMcu.post(VcuToMcTask);

        handlerVcuToMcu.postDelayed(() -> {
            mcuData = data;
            currentAttempt = 0;
            handlerVcuToMcu.post(VcuToMcTask);
        }, 200);
    }

    /**
     * 停止VCU下发数据给MCU
     */

    public void stopVCUSendToMCU() {
        handlerVcuToMcu.removeCallbacks(VcuToMcTask);
    }

    /**
     * 处理接收到的数据
     * @param comBean 接收到的数据
     * @param rxType 数据格式类型（Hex 或 Txt）
     * @return 处理后的数据字符串
     */
    public static String processReceivedData(ComBean comBean, int rxType) {
        String rxText = new String(comBean.bRec);
        if (rxType == 0) {  // 如果是 Hex 类型的数据格式，就转换
            rxText = ByteUtil.ByteArrToHex(comBean.bRec);
        }
        return rxText;
    }

    /**
     * 解析接收到的数据帧
     * @param receivedData 接收到的原始数据字符串
     * @return 分离出来的数据帧列表
     */
    public static List<String> splitIntoFrames(String receivedData) {
//        Log.d("sdfgh","RRFRAME------"+receivedData);

        List<String> frames = new ArrayList<>();

        int index = 0;
        boolean foundHeader = false;
        while (index < receivedData.length()) {
            // 查找下一个帧头的起始位置
            int headerIndex77 = receivedData.indexOf("FFAA77", index);
            int headerIndex66 = receivedData.indexOf("FFAA66", index);

            // 没有找到帧头，结束循环
            if (headerIndex77 == -1 && headerIndex66 == -1) {
                break;
            }

            foundHeader = true;

            // 选择最近的帧头
            int headerIndex = (headerIndex77 != -1 && (headerIndex66 == -1 || headerIndex77 < headerIndex66))
                    ? headerIndex77
                    : headerIndex66;

            // 查找下一个相同或不同的帧头的起始位置
            int nextHeaderIndex77 = receivedData.indexOf("FFAA77", headerIndex + 1);
            int nextHeaderIndex66 = receivedData.indexOf("FFAA66", headerIndex + 1);
            int nextHeaderIndex = (nextHeaderIndex77 != -1 && (nextHeaderIndex66 == -1 || nextHeaderIndex77 < nextHeaderIndex66))
                    ? nextHeaderIndex77
                    : nextHeaderIndex66;

            if (nextHeaderIndex != -1) {
                // 截取两个帧头之间的部分作为一帧数据
                String frame = receivedData.substring(headerIndex, nextHeaderIndex);
                frames.add(frame);
                index = nextHeaderIndex;
            } else {
                // 没有找到下一个帧头，说明剩余的数据都属于当前帧
                String frame = receivedData.substring(headerIndex);
                frames.add(frame);
                break;
            }
        }

        // 如果没有找到任何帧头，就返回原始数据
        if (!foundHeader) {
            frames.add(receivedData);
        }

        return frames;
    }
    /**
     * 解析数据帧
     * @param frame 数据帧
     * @param listener 回调接口
     */
    public void parseDataFrames(String frame, FrameDataListener listener) {
        if (frame.startsWith("FFAA77") && frame.length() > 14) {
            if (FrameUtils.verifyFrame(frame)||frame.startsWith("FFAA770303001518FF50E5") || frame.startsWith("FFAA770303001518954001")) {
                if(frame.startsWith("FFAA770303001518FF50E5"))Log.d("sdsfgf","frmae"+frame);
                String frameType = FrameUtils.getByteSubstring(frame,3,4);
                switch (frameType) {
                    case "0301":
                        parseHeartbeat1(frame);
                        listener.onHeartbeat1Parsed(hb1);
                        break;
                    case "0302":
                        parseHeartbeat2(frame);
                        listener.onHeartbeat2Parsed(hb2);
                        break;
                    case "0303":
                        //Log.d("SerialPortManager", "Frame verification failed--------"+frame);
                        parseHeartbeat3(frame);
                        listener.onHeartbeat3Parsed(hb3);
                        break;
                    default:
                        //Log.e("SerialPortManager", "Unknown frame type");
                        break;
                }
            }
//            if(frame.startsWith("FFAA770303001514028001")){
//                parseHeartbeat3(frame);
//                listener.onHeartbeat3Parsed(hb3);
//            }
            else {
                //Log.e("SerialPortManager", "Frame verification failed--------"+frame);
            }
        }
//        else if (frame.startsWith("FFAA66") && frame.length() > 14) {
//            String frameType = FrameUtils.getByteSubstring(frame,3,4);
//            switch (frameType) {
//                case "0101"://Handle lock
//                    parseHandleLockResponse(frame);
//                    break;
//                case "0102"://seat lock
//                    parseSeatLockResponse(frame);
//                    break;
//                case "0107"://LED
//                    parseLEDControlResponse(frame);
//                    break;
//                default:
//                    //Log.e("SerialPortManager", "Unknown frame type");
//                    break;
//            }
//        }
        else if (frame.startsWith("FFAA66") && frame.length() > 14) {
            //Log.e("SerialPortManager", "FFAA66 Frame verification failed");

            if (FrameUtils.verifyFrame(frame)) {
                McuBleMacAddressDnswer(frame);
                listener.onMcuAnswerDataParsed(mad);
                //Log.e("SerialPortManager", "FFAA66333333333 "+frame);
            } else {
                //Log.e("SerialPortManager", "FFAA66 Frame verification failed");
            }
            String frameType = FrameUtils.getByteSubstring(frame,3,4);
            switch (frameType) {
                case "0101"://Handle lock
                    parseHandleLockResponse(frame);
                    break;
                case "0102"://seat lock
                    parseSeatLockResponse(frame);
                    break;
                case "0107"://LED
                    parseLEDControlResponse(frame);
                    break;
                default:
                    //Log.e("SerialPortManager", "Unknown frame type");
                    break;
            }
        } else if (frame.length() != 0) {
            //int mcudata = Integer.parseInt(frame, 16);
           // mad.setMcuOtaAnswer((byte) mcudata);  // 存储 MCU OTA应答数据
            mad.setMcuOtaAnswer((byte) FrameUtils.getByteValue(frame,0));  // 存储 MCU OTA应答数据
            String temp = String.valueOf(mad.getMcuOtaAnswer());
           // Log.e("SerialPortManager", " McuOtaAnswer: " + temp );
        } else {
           // Log.e("SerialPortManager", " FFAA66 Invalid frame format");
        }
    }

    /**
     * 解析心跳包1
     * @param frame 数据帧
     * @return Heartbeat1 对象
     */
    public void parseHeartbeat1(String frame) {
        hb1.setDragonLock(FrameUtils.getByteValue(frame,7));   // 龙头锁状态
       // Log.d("sdfdf","HANDLE LOCK CURRENT STATE "+FrameUtils.getByteValue(frame,7));
        hb1.setSeatLock(FrameUtils.getByteValue(frame,8));   // 坐垫锁状态
        hb1.setSideStand(FrameUtils.getByteValue(frame,9));  //侧支架状态
        hb1.setSeatLockSensor(FrameUtils.getByteValue(frame,10)); // 座椅锁状态
        hb1.setBatteryVoltage(FrameUtils.getByteSubstringAsInt(frame,11,12));  // 小电池电压
        hb1.setExternalTemperature(FrameUtils.getByteSubstringAsInt(frame,13,14));  // 外部温度
        hb1.setGestureSlide(FrameUtils.getByteValue(frame,15));   // 手势滑动状态
        hb1.setHeadlights(FrameUtils.getByteValue(frame,16));  // 大灯状态
        hb1.setTurnSignal(FrameUtils.getByteValue(frame,17));   // 转向灯状态
        hb1.setLED(FrameUtils.getByteValue(frame,18));//LED response
    }
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public void sendContinuousSensorData() {
        scheduler.scheduleWithFixedDelay(() -> {
            if (MainActivity.evClient != null && MainActivity.evClient.isOpen()) {
                MainActivity.evClient.sendSensorData("HandleLock", hb1.getDragonLock());//0=Lock   1=unlock
                MainActivity.evClient.sendSensorData("SeatLock", hb1.getSeatLock());   //0=Lock   1=unlock
                MainActivity.evClient.sendSensorData("ChargingStatus", 0);  //0=unplugged   1=pluggedIn
            }
        }, 0, 1, TimeUnit.SECONDS); // Wait 1 sec after each execution before starting again
    }


    /**
     * 解析心跳包2
     * @param frame 数据帧
     * @return Heartbeat2 对象
     */
    private void parseHeartbeat2(String frame) {
        hb2.setXuAcc(FrameUtils.getByteSubstringAsInt(frame,7,10));   // x轴加速度
        hb2.setYuAcc(FrameUtils.getByteSubstringAsInt(frame,11,14));  // y轴加速度
        hb2.setZuAcc(FrameUtils.getByteSubstringAsInt(frame,15,18));  // z轴加速度
        hb2.setXGyro(FrameUtils.getByteSubstringAsInt(frame,19,22)); // x轴陀螺仪
        hb2.setYGyro(FrameUtils.getByteSubstringAsInt(frame,23,26)); // y轴陀螺仪
        hb2.setZGyro(FrameUtils.getByteSubstringAsInt(frame,27,30)); // z轴陀螺仪
    }

    /**
     * 解析心跳包3
     * @param frame 数据帧
     * @return Heartbeat3 对象
     */
    private void parseHeartbeat3(String frame) {
        if(frame.length()>6)
        {
            //Log.d("sdfgh","FRAMEPARSE11111------"+frame);

            hb3.setCanId(FrameUtils.getByteSubstring(frame,7,10));  // CAN ID
            hb3.setCanData(FrameUtils.getByteSubstring(frame,11,18));
        }  // CAN 数据}
        else {
           // Log.d("sdfgh","FRAMEPARSERETURN------"+frame);

            return;
        }
        //Log.d("dfsdgb","id's are"+FrameUtils.getByteSubstring(frame,7,10));
        // 根据 CAN ID 进一步解析 CAN 数据
        switch (hb3.getCanId()) {
            case "18FF50E5"://CHARGING STARTED STATUS
                CanDataParser.parseCanData18FF50E5(hb3, hb3.getCanData());
                break;
            case "18904001"://MAIN SOC
                CanDataParser.parseCanData18904001(hb3, hb3.getCanData());
                break;
            case "18914001"://MAIN SOC
                CanDataParser.parseCanData18914001(hb3, hb3.getCanData());
                break;
            case "18904002"://APU SOC
                CanDataParser.parseCanData18904002(hb3, hb3.getCanData());
                break;
            case "18934001"://Mos States
                CanDataParser.parseCanData18934001(hb3, hb3.getCanData());
                break;
            case "18944001"://Mos States
                CanDataParser.parseCanData18944001(hb3, hb3.getCanData());
                break;
            case "16078001"://Mos States
                CanDataParser.parseCanData16078001(hb3, hb3.getCanData());
                break;
            case "18934002"://APU Mos States
                CanDataParser.parseCanData18934002(hb3, hb3.getCanData());
                break;
            case "18964001":// NTC
                CanDataParser.parseCanData18964001(hb3, hb3.getCanData());
                break;
            case "18964002":// APU NTC
                CanDataParser.parseCanData18964002(hb3, hb3.getCanData());
                break;
            case "18954001":
                CanDataParser.parseCanData18954001(hb3, hb3.getCanData());
                break;
            case "1806E5F4":
                CanDataParser.parseCanData1806E5F4(hb3, hb3.getCanData());
                break;
            case "160B8001":// Charge AH
                CanDataParser.parseCanData160B8001(hb3, hb3.getCanData());
                break;
            case "160D8001":// Discharge AH
                CanDataParser.parseCanData160D8001(hb3, hb3.getCanData());
                break;
            case "10F807A8":
                CanDataParser.parseCanData10F807A8(hb3, hb3.getCanData());
                break;
            case "10F907A8":
                CanDataParser.parseCanData10F907A8(hb3, hb3.getCanData());
                break;
            case "10FA07A8":
                CanDataParser.parseCanData10FA07A8(hb3, hb3.getCanData());
                break;
            case "10FB07A8":
                CanDataParser.parseCanData10FB07A8(hb3, hb3.getCanData());
                break;
            case "10FC07A8":
                CanDataParser.parseCanData10FC07A8(hb3, hb3.getCanData());
                break;
            case "10FD07A8":
                CanDataParser.parseCanData10FD07A8(hb3, hb3.getCanData());
                break;
            default:
                // 处理未知的 CAN ID
                break;
        }
    }

    private void parseHandleLockResponse(String frame)
    {
        hb3.sethandleLockresponse(FrameUtils.getByteValue(frame,7));
    }

    public void parseLEDControlResponse(String frame)
    {
        hb3.setLEDresponse(FrameUtils.getByteValue(frame,7));
      //  Log.d("dsfghf","this is led response value "+frame);
    }
    private void parseSeatLockResponse(String frame)
    {
        hb3.setSeatLockresponse(FrameUtils.getByteValue(frame,7));
    }
    public Heartbeat3 getHb3() {
        return hb3;
    }
    public Heartbeat1 getHb1() {
        return hb1;
    }
    /**
     * 解析蓝牙Mac地址应答数据
     * @param frame 数据帧
     * @return McuAnswerData 对象
     */
    private void McuBleMacAddressDnswer(String frame) {
        mad.setTaskId(FrameUtils.getByteSubstring(frame,3,4));  // Task ID
        //Log.d("SerialPortManager", " FFAA66 McuBleMacDddressDnswer " + mad.getTaskId());
        // 根据 TASK ID 进一步解析数据
        switch (mad.getTaskId()) {
            case "0100":
                //Log.d("SerialPortManager", " FFAA66 setBleMacChange 0100");
                mad.setBleMacChange(FrameUtils.getByteValue(frame,7));  // Ble Mac 地址 修改状态
                stopVCUSendToMCU(); // 停止VCU下发数据 修改MCU地址
                break;
            case "0200":
                //Log.d("SerialPortManager", " FFAA66 setBleMacf 0200");
                mad.setBleMac(FrameUtils.getByteSubstring(frame,7,12));   // 截取Ble Mac 地址
                stopVCUSendToMCU(); // 停止VCU下发数据给MCU
                break;
            case "0204":
                //Log.d("SerialPortManager", " FFAA66 setMcuVersions 0240");
                mad.setMcuVersions(FrameUtils.getByteSubstringAsInt(frame,7,8));   // 截取Ble Mac 地址
                stopVCUSendToMCU(); // 停止VCU下发数据给MCU
                break;
            case "0101":
                //Log.d("SerialPortManager", " FFAA66 setMcuVersions 0101");
                mad.setFrontLockState(FrameUtils.getByteValue(frame,7));  // 龙头锁 修改状态
                stopVCUSendToMCU(); // 停止VCU下发数据给MCU
                break;
            case "0102":
                //Log.d("SerialPortManager", " FFAA66 setMcuVersions 0102");
                mad.setSeatCushionLockState(FrameUtils.getByteValue(frame,7));  // 龙头锁 修改状态
                stopVCUSendToMCU(); // 停止VCU下发数据给MCU
                break;
            case "0103":
                //Log.d("SerialPortManager", " FFAA66 setMcuVersions 0102");
                mad.setHeartbeat3State(FrameUtils.getByteValue(frame,7));  // 档位相关 修改状态
                stopVCUSendToMCU(); // 停止VCU下发数据给MCU
                break;
            default:
                // 处理未知的 CAN ID
                break;
        }
    }

//    public void clearSendBuffer() {
//        if (serialHelper != null && serialHelper.isOpen()) {
//            try {
//                // 使用反射获取 mSerialPort
//                Field serialPortField = SerialHelper.class.getDeclaredField("mSerialPort");
//                serialPortField.setAccessible(true);
//                Object serialPort = serialPortField.get(serialHelper);
//
//                // 使用反射获取 mFd
//                Field fileDescriptorField = serialPort.getClass().getDeclaredField("mFd");
//                fileDescriptorField.setAccessible(true);
//                FileDescriptor fd = (FileDescriptor) fileDescriptorField.get(serialPort);
//
//                // 使用JNI方法清理发送缓冲区
//                clearSendBufferNative(getIntFd(fd));
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }
public void clearSendBuffer() throws IOException {
        if (serialHelper != null && serialHelper.isOpen()) {
            FileDescriptor fd = serialHelper.getFileDescriptor();
            int fdInt = getFdInt(fd);
            clearSendBufferNative(fdInt);
            }
        }
    private int getFdInt(FileDescriptor fd) throws IOException {
        try {
            // 通过反射获取 FileDescriptor 中的文件描述符
            Field fdField = FileDescriptor.class.getDeclaredField("descriptor");
            fdField.setAccessible(true);
            return fdField.getInt(fd);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IOException("Failed to access file descriptor", e);
        }
    }
    public void close() {
        if (serialHelper != null) {
            serialHelper.close();  // This will close the serial port
            serialHelper = null;
        }

        // Stop any background tasks if running
        handlerVcuToMcu.removeCallbacks(VcuToMcTask);
        currentAttempt = 0;
    }

    public static void reset() {
        if (instance != null) {
            instance.close();  // call the close method we just added
            instance = null;
        }
    }

}


