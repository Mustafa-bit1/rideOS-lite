//package com.fanconn.rideOS.network;
//
//import android.content.Context;
//import android.util.Log;
//
//import org.eclipse.paho.client.mqttv3.IMqttActionListener;
//import org.eclipse.paho.client.mqttv3.IMqttToken;
//import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
//import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
//import org.eclipse.paho.client.mqttv3.MqttException;
//
//public class ScooterMqttManager {
//
//    // ---------------- Singleton ----------------
//    private static ScooterMqttManager instance;
//
//    public static ScooterMqttManager getInstance() {
//        if (instance == null) {
//            instance = new ScooterMqttManager();
//        }
//        return instance;
//    }
//
//    // -------------------------------------------
//
//    private MqttAsyncClient client;
//    private String serverUri;
//    private String clientId;
//
//    public MqttAsyncClient getClient() {
//        return client;
//    }
//
//    public void init(Context context, String serverUri, String clientId) {
//        try {
//            this.serverUri = serverUri;
//            this.clientId = clientId;
//
//            client = new MqttAsyncClient(serverUri, clientId, null);
//
//            Log.d("MQTT", "MQTT init ok");
//
//        } catch (Exception e) {
//            Log.e("MQTT", "Init failed", e);
//        }
//    }
//
//    public void connect(OnMqttConnected callback) {
//        try {
//            MqttConnectOptions options = new MqttConnectOptions();
//            options.setCleanSession(true);
//            options.setAutomaticReconnect(true);
//
//            client.connect(options, null, new IMqttActionListener() {
//                @Override
//                public void onSuccess(IMqttToken asyncActionToken) {
//                    Log.d("MQTT", "Connected successfully");
//                    callback.onConnected();
//                }
//
//                @Override
//                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
//                    Log.e("MQTT", "Failed to connect", exception);
//                }
//            });
//
//        } catch (MqttException e) {
//            Log.e("MQTT", "Connect error", e);
//        }
//    }
//
//    public interface OnMqttConnected {
//        void onConnected();
//    }
//
//    // ---------------- PUBLISH ----------------
//    public void publish(String topic, String payload) {
//        try {
//            if (client != null && client.isConnected()) {
//                client.publish(topic, payload.getBytes(), 1, false);
//                Log.d("MQTT", "Published to " + topic + ": " + payload);
//            }
//        } catch (Exception e) {
//            Log.e("MQTT", "Publish failed", e);
//        }
//    }
//}
