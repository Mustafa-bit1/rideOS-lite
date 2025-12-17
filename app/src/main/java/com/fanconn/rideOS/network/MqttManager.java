//package com.fanconn.rideOS.network;
//
//
//import android.content.Context;
//import android.util.Log;
//
//import org.eclipse.paho.android.service.MqttAndroidClient;
//import org.eclipse.paho.client.mqttv3.IMqttActionListener;
//import org.eclipse.paho.client.mqttv3.IMqttToken;
//import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
//
//public class MqttManager {
//
//    private static MqttManager instance;
//    private MqttAndroidClient client;
//
//    public static MqttManager getInstance() {
//        if (instance == null) instance = new MqttManager();
//        return instance;
//    }
//
//    public void init(Context context, String serverUrl, String clientId) {
//        client = new MqttAndroidClient(context, serverUrl, clientId);
//    }
//
//    public void connect() {
//        MqttConnectOptions options = new MqttConnectOptions();
//        options.setAutomaticReconnect(true);
//        options.setCleanSession(true);
//
//        try {
//            client.connect(options, null, new IMqttActionListener() {
//                @Override
//                public void onSuccess(IMqttToken asyncActionToken) {
//                    Log.d("MQTT", "Connected to MQTT Broker");
//                }
//
//                @Override
//                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
//                    Log.e("MQTT", "Failed to connect", exception);
//                }
//            });
//        } catch (Exception e) {
//            Log.e("MQTT", "Connect Error", e);
//        }
//    }
//
//    public MqttAndroidClient getClient() {
//        return client;
//    }
//}
