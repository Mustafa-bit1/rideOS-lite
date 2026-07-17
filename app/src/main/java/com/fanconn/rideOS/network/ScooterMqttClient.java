package com.fanconn.rideOS.network;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.os.Handler;

import info.mqtt.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.*;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.fanconn.rideOS.DeviceBluetoothController;
import com.fanconn.rideOS.FrameUtils;
import com.fanconn.rideOS.MainActivity;
import com.fanconn.rideOS.SerialPortManager;
import com.fanconn.rideOS.StagedTelemetryController;
import com.fanconn.rideOS.UIUpdater;
import com.fanconn.rideOS.UpdateService;
import com.fanconn.rideOS.aosp_mainActivity;
import com.fanconn.rideOS.authen.LoginActivity;
import com.fanconn.rideOS.mcuupdate.McuOtaExecutor;
import com.fanconn.rideOS.serialApplication;

import org.json.JSONObject;

public class ScooterMqttClient {

    private static final String TAG = "SCOOTER_MQTT";

    private MqttAndroidClient client;
    private final String vin;
    private final Context context;

    // MUST MATCH SPRING BOOT
    private static final String COMMAND_PREFIX = "commands/";
    private static final String STATUS_PREFIX  = "status/";
    private static final String CAN_HEADER = "FFAA5501030015";
    private static final String CAN_HEADER_UART = "FFAA5501";

    private static final int CAN_FRAME_LEN = 38;


    public ScooterMqttClient(Context context, String brokerUrl, String vin) {
        this.context = context.getApplicationContext();
        this.vin = vin;

        // UNIQUE CLIENT ID (VERY IMPORTANT)
        String clientId = "SCOOTER_" + vin;
        client = new MqttAndroidClient(this.context, brokerUrl, clientId);
    }

    // ----------------------------------------------------
    // CONNECT + SUBSCRIBE
    // ----------------------------------------------------

    public void connectAndSubscribe() {

        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);   // Auto reconnect
        options.setCleanSession(true);          // Drop queued commands
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(20);

        try {

            client.setCallback(new MqttCallbackExtended() {

                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                   // Log.d(TAG, " MQTT connectComplete | reconnect = " + reconnect);

                    String topic = COMMAND_PREFIX + vin;
                    client.subscribe(topic, 1);
                   // Log.d(TAG, "📡 Subscribed to: " + topic);
                }

                @Override
                public void connectionLost(Throwable cause) {
                    //Log.e(TAG, " MQTT connection lost", cause);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());

//                    Log.d(TAG, " Command received");
//                    Log.d(TAG, "   Topic   : " + topic);
//                    Log.d(TAG, "   Payload : " + payload);

                    handleCommand(payload);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    //Log.d(TAG, " MQTT delivery complete");
                }
            });

            if (client.isConnected()) {
                //Log.d(TAG, "⚠ MQTT already connected");
                return;
            }

            client.connect(options, null, new IMqttActionListener() {

                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                   // Log.d(TAG, " Connected to MQTT broker");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    //Log.e(TAG, " MQTT connect failed", exception);
                }
            });

        } catch (Exception e) {
            //Log.e(TAG, " MQTT setup error", e);
        }
    }

    private void subscribe(String topic) throws MqttException {
        client.subscribe(topic, 1);
        //Log.d(TAG, " Subscribed to: " + topic);
    }

    // ----------------------------------------------------
    // COMMAND HANDLING (LOG ONLY)
    // ----------------------------------------------------

    private void handleCommand(String payload) {
        try {
            JSONObject json = new JSONObject(payload);

            String commandType = json.optString("commandType", "UNKNOWN");
            String value = json.optString("value", "UNKNOWN");
            String clientId = json.optString("clientId", "UNKNOWN");
            long commandId = json.optLong("commandId", -1);   // ⬅ ADD

//            Log.d(TAG, "➡ CommandType : " + commandType);
//            Log.d(TAG, "➡ Value       : [" + value + "]");
//            Log.d(TAG, "➡ ClientId    : " + clientId);

            String ackStatus = "success";

            if (value.matches(".*\\s+.*")) {
               // Log.e(TAG, " Invalid command value (contains spaces): [" + value + "]");
                sendAck(vin, clientId, commandType, value, commandId, ackStatus);
                return;
            }

            switch (commandType) {

                case "POWER":
                    if ("1".equals(value)) {
                        // UNLOCK SCOOTER
                        ackStatus = "success";
                        showLoginAndUnlock(context);
                    } else if ("0".equals(value)) {
                        // LOCK SCOOTER
                        if (UIUpdater.getInstance() != null && serialApplication.getSerialPortManager().getHb3().getBSG2_BYTE78() == 0) {
                            ackStatus = "success";
                            UIUpdater.getInstance().onDoubleTapAction();
                            Log.d(TAG, " Remote lock scooter");
                        }
                        else {
                            ackStatus = "Scooter in motion";
                        }
                    } else {
                        ackStatus = "invalidcommand";
                    }
                    break;

                case "BLUETOOTH":
                    if ("1".equals(value)) {
                        ackStatus = DeviceBluetoothController.setBluetooth(true)
                                ? "success" : "failed";
                    } else if ("0".equals(value)) {
                        ackStatus = DeviceBluetoothController.setBluetooth(false)
                                ? "success" : "failed";
                    } else {
                        ackStatus = "invalidcommand";
                    }
                    break;

                case "CAN":

                    if (serialApplication.getSerialPortManager() == null) {
                        ackStatus = "invalidcommand";
                        Log.e(TAG, "CANWRONG SerialPortManager is null");
                        break;
                    }

                    if (value == null || (value.length() % 2) != 0) {
                        ackStatus = "invalidframe";
                        Log.e(TAG, "CANWRONG Null or odd-length frame: " + value);
                        break;
                    }

                    // ---- CAN FRAME ----
                    if (value.startsWith(CAN_HEADER)) {

                        if (value.length() != CAN_FRAME_LEN) {
                            ackStatus = "invalidcommand";
                            Log.e(TAG, "CANWRONG Invalid CAN frame length: "
                                    + value.length() + " data=" + value);
                            break;
                        }

                        Log.d(TAG, "CAN CANWRONG OK frame: " + value);
                        serialApplication.getSerialPortManager().VCUSendToMCU(value);
                        ackStatus = "success";
                        break;
                    }

                    // ---- UART (NON-CAN) FRAME ----
                    else if (value.startsWith(CAN_HEADER_UART)) {

                        Log.d(TAG, "UART frame: " + value);

                        if ("FFAA550101000A00".equals(value)
                                && serialApplication.getSerialPortManager().getHb3() != null
                                && serialApplication.getSerialPortManager()
                                .getHb3().getBSG2_BYTE78() != 0) {

                            ackStatus = "Scooter in motion";
                            break;
                        }

                        serialApplication.getSerialPortManager().VCUSendToMCU(value);
                        ackStatus = "success";
                        break;
                    }

                    // ---- UNKNOWN FRAME ----
                  //  Log.d(TAG, "Unknown frame ignored: " + value);
                    ackStatus = "invalidcommand";
                    break;
                case "HANDLELOCK":
                    if ("1".equals(value)) {
                        // UNLOCK HANDLE
                        if(serialApplication.getSerialPortManager()!=null){
                            serialApplication.getSerialPortManager().VCUSendToMCU("FFAA550101000A01");
                            ackStatus = "success";
                        }

                    } else if ("0".equals(value)) {
                        // LOCK HANDLE
                        if (UIUpdater.getInstance() != null && serialApplication.getSerialPortManager().getHb3().getBSG2_BYTE78() == 0) {
                            ackStatus = "success";
                            serialApplication.getSerialPortManager().VCUSendToMCU("FFAA550101000A00");
                        }
                        else {
                            ackStatus = "Scooter in motion";
                        }
                    } else {
                        ackStatus = "invalidcommand";
                    }
                    break;

                case "LED":
                    if ("1".equals(value)) {
                        // UNLOCK HANDLE
                        if(serialApplication.getSerialPortManager()!=null){
                            serialApplication.getSerialPortManager().VCUSendToMCU("FFAA550107000A01");
                            ackStatus = "success";
                        }

                    } else if ("0".equals(value)) {
                        // LOCK HANDLE
                        if (UIUpdater.getInstance() != null && serialApplication.getSerialPortManager().getHb3().getBSG2_BYTE78() == 0) {
                            ackStatus = "success";
                            serialApplication.getSerialPortManager().VCUSendToMCU("FFAA550107000A00");
                        }
                        else {
                            ackStatus = "Scooter in motion";
                        }
                    } else {
                        ackStatus = "invalidcommand";
                    }
                    break;

                case "SEATLOCK":
                 if ("1".equals(value)) {
                        // LOCK HANDLE
                        if (UIUpdater.getInstance() != null && serialApplication.getSerialPortManager().getHb3().getBSG2_BYTE78() == 0) {
                            ackStatus = "success";
                            serialApplication.getSerialPortManager().VCUSendToMCU("FFAA550102000A01");
                        }
                        else {
                            ackStatus = "Scooter in motion";
                        }
                    } else {
                        ackStatus = "invalidcommand";
                    }
                    break;

                case "BOARDRESTART":
                   // Log.d(TAG, "🔄 Restart command received");
                    if ("1".equals(value)) {
                        // ACK will be sent immediately
                        ackStatus = "success";
                        //Log.d(TAG, "🔄 Restart command Succeeded");

                        // Restart AFTER ACK delivery (5 sec safe delay)
                        DeviceNetworkController.restartWithDelay(context, 4000);

                    } else {
                        ackStatus = "invalidcommand";
                    }
                    break;

                case "rideOSRESTART":
                    if ("1".equals(value)) {
                        ackStatus = "success";

                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            AppRestartManager.relaunchAppFresh(context);
                        }, 3000); // 3 sec safe delay
                    } else {
                        ackStatus = "invalidcommand";
                    }
                    break;

                case "AOSPOTA":
                    ackStatus = "success";
                    openAospOtaScreen(context);
                    break;

                case "MCUOTA":
                    handleMcuOtaCommandAsync(
                            context,
                            vin,
                            clientId,
                            commandType,
                            value,
                            commandId
                    );
                    return;

                case "MCURESTART":
                    if ("1".equals(value)) {
                        // LOCK HANDLE
                        if (UIUpdater.getInstance() != null && serialApplication.getSerialPortManager().getHb3().getBSG2_BYTE78() == 0) {
                            ackStatus = "success";
                            serialApplication.getSerialPortManager().VCUSendToMCU("FFAA550109000A01");
                        }
                        else {
                            ackStatus = "Scooter in motion";
                        }
                    } else {
                        ackStatus = "invalidcommand";
                    }
                    break;

                case "rideOSOTA":
                    ackStatus = "success";
                    startRideOSRemoteOta(context);
                    break;

                case "DATA":
                    if ("1".equals(value)) {
                        ackStatus = DeviceNetworkController.setMobileData(context, true)
                                ? "success" : "failed";
                    } else if ("0".equals(value)) {
                        ackStatus = DeviceNetworkController.setMobileData(context, false)
                                ? "success" : "failed";
                    } else {
                        ackStatus = "invalidcommand";
                    }
                    break;

                case "AIRPLANE":
                    if ("1".equals(value)) {
                        ackStatus = "success";
                       // Log.d("AIRPLANE", " Airplane mode auto-disabled");
                        DeviceNetworkController.enableAirplaneModeWithAutoOff(
                                context,
                                3000   // 3 seconds
                        );

                    } else {
                        ackStatus = "invalidcommand";
                    }
                    break;

                case "STARTSTAGE":
                    if ("1".equals(value)) {
                        StagedTelemetryController
                                .getInstance(context)
                                .startSending();
                    } else {
                        ackStatus = "invalidcommand";
                    }
                    break;

                case "STOPSTAGE":
                    if ("1".equals(value)) {
                        StagedTelemetryController
                                .getInstance(context)
                                .stopSending();
                    } else {
                        ackStatus = "invalidcommand";
                    }
                    break;

                case "CLEARSTAGE":
                    if ("1".equals(value)) {
                        StagedTelemetryController
                                .getInstance(context)
                                .clearQueue();
                    } else {
                        ackStatus = "invalidcommand";
                    }
                    break;

                default:
                    //Log.e(TAG, " Unknown commandType: " + commandType);
                    ackStatus = "invalidcommand";
            }

            //  Send ACK only once
            sendAck(vin, clientId, commandType, value, commandId, ackStatus);
        } catch (Exception e) {
            //Log.e(TAG, " Command parse failed", e);
        }
    }


    private void showLoginAndUnlock(Context context) {
//        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
//        if (powerManager != null && !powerManager.isInteractive()) {
//            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
//                    PowerManager.FULL_WAKE_LOCK |
//                            PowerManager.ACQUIRE_CAUSES_WAKEUP |
//                            PowerManager.ON_AFTER_RELEASE,
//                    "MyApp:WakeLockTag"
//            );
//            wakeLock.acquire(3000); // Wake screen for 3 seconds
//        }
//        new Handler(Looper.getMainLooper()).postDelayed(() -> {
//
//            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
//
//            if (powerManager != null && !powerManager.isInteractive()) {
//
//                PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
//                        PowerManager.FULL_WAKE_LOCK |
//                                PowerManager.ACQUIRE_CAUSES_WAKEUP |
//                                PowerManager.ON_AFTER_RELEASE,
//                        "MyApp:WakeLockTag"
//                );
//
//                wakeLock.acquire(1000); // screen stays awake 3 seconds
//            }
//        }, 2000); // wait 2 seconds before executing

        // Delay slightly to ensure Activity is visible
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (LoginActivity.instance != null) {
                LoginActivity.instance.loginSuccess("Key Rider");
               // Log.d(TAG, " Remote unlock executed after screen shown");
            } else {
                //Log.e(TAG, " LoginActivity instance still null");
            }

            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

            if (powerManager != null && !powerManager.isInteractive()) {

                PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                        PowerManager.FULL_WAKE_LOCK |
                                PowerManager.ACQUIRE_CAUSES_WAKEUP |
                                PowerManager.ON_AFTER_RELEASE,
                        "MyApp:WakeLockTag"
                );

                wakeLock.acquire(1000); // screen stays awake 3 seconds
            }

        }, 500); // 300–500 ms is enough
    }

    // ----------------------------------------------------
    // ACK BACK TO SPRING BOOT
    // ----------------------------------------------------

    private void sendAck(
            String vin,
            String clientId,
            String commandType,
            String value,
            long commandId,
            String status
    ) {

        if (client == null || !client.isConnected()) {
//            Log.e(TAG, " Cannot send ACK, MQTT not connected");
            return;
        }

        try {
            JSONObject ack = new JSONObject();
            ack.put("vin", vin);
            ack.put("clientId", clientId);
            ack.put("commandType", commandType);
            ack.put("value", value);
            ack.put("commandId", commandId);
            ack.put("status", status);

            String topic = STATUS_PREFIX + vin;

            client.publish(
                    topic,
                    ack.toString().getBytes(),
                    1,
                    false
            );

//            Log.d(TAG, " ACK sent → " + ack.toString());

        } catch (Exception e) {
//            Log.e(TAG, " ACK publish failed", e);
        }
    }
    // ----------------------------------------------------
    // OPTIONAL CLEANUP (NOT REQUIRED NOW)
    // ----------------------------------------------------

    public void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                //Log.d(TAG, " MQTT disconnected");
            }
        } catch (Exception e) {
            //Log.e(TAG, " MQTT disconnect error", e);
        }
    }

    private void openAospOtaScreen(Context context) {

        Intent intent = new Intent(context, aosp_mainActivity.class);

        //  Silent OTA flag
        intent.putExtra(aosp_mainActivity.EXTRA_SILENT_OTA, true);

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        context.startActivity(intent);
    }
    private void startRideOSRemoteOta(Context context) {

        if (!FrameUtils.isNetworkAvailable(context)) {
            return;
        }

        Intent intent = new Intent(context, UpdateService.class);

        intent.putExtra(UpdateService.EXTRA_REMOTE_SILENT, true);

        // ALWAYS Production
        intent.putExtra(
                UpdateService.EXTRA_BRANCH,
                UpdateService.BRANCH_PRODUCTION
        );

        context.startService(intent);
    }

    private void triggerMcuOtaFromMqtt() {
        //Log.i(TAG, "MCU OTA command received from MQTT");
        if (!FrameUtils.isNetworkAvailable(context)) {
           // Log.e(TAG, "No internet → MCU OTA blocked");
            return;
        }
        McuOtaExecutor.startMcuOta(context);
    }

    private String MCU_VERSION_URL = "http://ota.rivotmotors.com/OTA/sc126/ElectricBike/version.json";

    private void handleMcuOtaCommandAsync(
            Context context,
            String vin,
            String clientId,
            String commandType,
            String value,
            long commandId
    ) {

        new Thread(() -> {

            //Log.i("MQTT", "MCUOTA command received");

            String ackStatus;

            if (!FrameUtils.isNetworkAvailable(context)) {
                ackStatus = "no-network";
                sendAck(vin, clientId, commandType, value, commandId, ackStatus);
                return;
            }

            int localVersion =
                    McuOtaExecutor.getEffectiveLocalMcuVersion(context);

            int serverVersion =
                    McuOtaExecutor.fetchServerVersion(MCU_VERSION_URL);

//            Log.i("MQTT", "MCU local=" + localVersion +
//                    ", server=" + serverVersion);

            if (serverVersion <= 0) {
                ackStatus = "version-fetch-failed";

            } else if (serverVersion <= localVersion) {
                ackStatus = "up-to-date";
            } else {
                ackStatus = "success";
                McuOtaExecutor.startMcuOta(context);
            }

            sendAck(vin, clientId, commandType, value, commandId, ackStatus);
        }).start();
    }
}
