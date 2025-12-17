//package com.fanconn.rideOS.network;
//
//import android.util.Log;
//
//import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
//import org.json.JSONObject;
//
//public class CommandListener implements IMqttMessageListener {
//
//    private final CommandHandler handler;
//
//    public CommandListener(CommandHandler handler) {
//        this.handler = handler;
//    }
//
//    @Override
//    public void messageArrived(String topic, org.eclipse.paho.client.mqttv3.MqttMessage message) {
//        String json = new String(message.getPayload());
//        Log.d("CMD", "Received: " + json);
//
//        try {
//            JSONObject obj = new JSONObject(json);
//
//            String commandType = obj.getString("commandType");
//            int value = obj.getInt("value");
//            String clientId = obj.optString("clientId", "ANDROID_APP");
//
//            handler.handleCommand(commandType, value, clientId);
//
//        } catch (Exception e) {
//            Log.e("CMD", "JSON parse error", e);
//        }
//    }
//}
