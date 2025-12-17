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

import com.fanconn.rideOS.SerialPortManager;
import com.fanconn.rideOS.UIUpdater;
import com.fanconn.rideOS.authen.LoginActivity;
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
        options.setAutomaticReconnect(true);
        options.setCleanSession(false); // IMPORTANT for reconnects
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(20);

        try {
            client.setCallback(new MqttCallback() {

                @Override
                public void connectionLost(Throwable cause) {
                    Log.e(TAG, "❌ MQTT connection lost", cause);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());

                    Log.d(TAG, "📥 Command received");
                    Log.d(TAG, "   Topic   : " + topic);
                    Log.d(TAG, "   Payload : " + payload);

                    handleCommand(payload);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.d(TAG, "✅ MQTT delivery complete");
                }
            });

            if (client.isConnected()) {
                Log.d(TAG, "⚠ MQTT already connected");
                return;
            }

            client.connect(options, null, new IMqttActionListener() {

                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "✅ Connected to MQTT broker");

                    String topic = COMMAND_PREFIX + vin;
                    try {
                        subscribe(topic);
                    } catch (MqttException e) {
                        Log.e(TAG, "❌ Subscribe failed", e);
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "❌ MQTT connect failed", exception);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "❌ MQTT setup error", e);
        }
    }

    private void subscribe(String topic) throws MqttException {
        client.subscribe(topic, 1);
        Log.d(TAG, "📡 Subscribed to: " + topic);
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

            Log.d(TAG, "➡ CommandType : " + commandType);
            Log.d(TAG, "➡ Value       : [" + value + "]");
            Log.d(TAG, "➡ ClientId    : " + clientId);

            String ackStatus = "success";

            // ❌ STRICT VALIDATION — reject ANY whitespace in value
            if (value.matches(".*\\s+.*")) {
                Log.e(TAG, "❌ Invalid command value (contains spaces): [" + value + "]");
                sendAck(clientId, "invalidcommand");
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
                        if (UIUpdater.getInstance() != null) {
                            ackStatus = "success";
                            UIUpdater.getInstance().onDoubleTapAction();
                            Log.d(TAG, "🔒 Remote lock scooter");
                        }
                    } else {
                        ackStatus = "invalidcommand";
                    }
                    break;

                case "CAN":
                    if (serialApplication.getSerialPortManager() != null) {
                        if(value.startsWith("FFAA5501")){
                            if(serialApplication.getSerialPortManager().getHb3().getBSG2_BYTE78()==0){
                                serialApplication.getSerialPortManager().VCUSendToMCU(value);
                                ackStatus = "success";
                                Log.d(TAG, "📡 Handle lock command sent to MCU");
                            }
                            else {
                                Log.d(TAG, "📡 CAN command failed because rpm is not 0");
                                ackStatus = "scooter in motion";
                            }
                        }
                        else {
                            serialApplication.getSerialPortManager().VCUSendToMCU(value);
                            ackStatus = "success";
                            Log.d(TAG, "📡 CAN command sent to MCU");
                        }

                    } else {
                        Log.d(TAG, "📡 CAN srial port is null");
                        ackStatus = "invalidcommand";
                    }
                    break;

                case "RESTART":
                    Log.d(TAG, "🔄 Restart command received");
                    if ("1".equals(value)) {
                        // ACK will be sent immediately
                        ackStatus = "success";
                        Log.d(TAG, "🔄 Restart command Succeeded");

                        // Restart AFTER ACK delivery (5 sec safe delay)
                        DeviceNetworkController.restartWithDelay(context, 5000);

                    } else {
                        ackStatus = "invalidcommand";
                    }
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
                        ackStatus = DeviceNetworkController.setAirplaneMode(context, true)
                                ? "success" : "failed";
                    } else if ("0".equals(value)) {
                        ackStatus = DeviceNetworkController.setAirplaneMode(context, false)
                                ? "success" : "failed";
                    } else {
                        ackStatus = "invalidcommand";
                    }
                    break;

                default:
                    Log.e(TAG, "❌ Unknown commandType: " + commandType);
                    ackStatus = "invalidcommand";
            }

            // ✅ Send ACK only once
            sendAck(clientId, ackStatus);

        } catch (Exception e) {
            Log.e(TAG, "❌ Command parse failed", e);
        }
    }
    private void showLoginAndUnlock(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager != null && !powerManager.isInteractive()) {
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                    PowerManager.FULL_WAKE_LOCK |
                            PowerManager.ACQUIRE_CAUSES_WAKEUP |
                            PowerManager.ON_AFTER_RELEASE,
                    "MyApp:WakeLockTag"
            );
            wakeLock.acquire(3000); // Wake screen for 3 seconds
        }

        // Delay slightly to ensure Activity is visible
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (LoginActivity.instance != null) {
                LoginActivity.instance.loginSuccess("Key Rider");
                Log.d(TAG, "🔓 Remote unlock executed after screen shown");
            } else {
                Log.e(TAG, "❌ LoginActivity instance still null");
            }
        }, 500); // 300–500 ms is enough
    }

    // ----------------------------------------------------
    // ACK BACK TO SPRING BOOT
    // ----------------------------------------------------

    private void sendAck(String clientId, String status) {

        if (client == null || !client.isConnected()) {
            Log.e(TAG, "❌ Cannot send ACK, MQTT not connected");
            return;
        }

        try {
            JSONObject ack = new JSONObject();
            ack.put("clientId", clientId);
            ack.put("status", status);

            String topic = STATUS_PREFIX + vin;

            client.publish(
                    topic,
                    ack.toString().getBytes(),
                    1,
                    false
            );

            Log.d(TAG, "📤 ACK sent");
            Log.d(TAG, "   Topic   : " + topic);
            Log.d(TAG, "   Payload : " + ack);

        } catch (Exception e) {
            Log.e(TAG, "❌ ACK publish failed", e);
        }
    }

    // ----------------------------------------------------
    // OPTIONAL CLEANUP (NOT REQUIRED NOW)
    // ----------------------------------------------------

    public void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                Log.d(TAG, "🔌 MQTT disconnected");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ MQTT disconnect error", e);
        }
    }
}
