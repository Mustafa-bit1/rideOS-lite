package com.fanconn.rideOS.network;

import android.util.Log;

import com.fanconn.rideOS.SerialPortManager;
import com.fanconn.rideOS.serialApplication;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;

public class EVWebSocketClient extends WebSocketClient {
    private static final String TAG = "EVWebSocketClient";

    private final String vin;
    private boolean isConnected = false;
    SerialPortManager serialPortManager = serialApplication.getSerialPortManager();

    public EVWebSocketClient(String vin, URI serverUri) {
        super(serverUri);
        this.vin = vin;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        isConnected = true;
        Log.d(TAG, " EV Connected to WebSocket");
        registerEV();
    }

    @Override
    public void onMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            Log.w(TAG, " Received empty message");
            return;
        }

        try {
            JSONObject json = new JSONObject(message);
            Log.d(TAG, " Received from server: " + json.toString());

            String type = json.optString("type", "");

            if ("command".equals(type)) {
                handleCommand(json);
            } else {
                Log.d(TAG, " Received data: " + json.toString());
            }
        } catch (JSONException e) {
            Log.e(TAG, " Error parsing JSON: " + message, e);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        isConnected = false;
        Log.w(TAG, " EV WebSocket Closed: " + reason);
        reconnectWebSocket();
    }

    @Override
    public void onError(Exception ex) {
        Log.e(TAG, " WebSocket Error: ", ex);
    }

    private void registerEV() {
        if (isConnected) {
            try {
                JSONObject json = new JSONObject();
                json.put("type", "registerEV");
                json.put("vin", vin);
                send(json.toString());
                Log.d(TAG, "🔗 EV Registered with VIN: " + vin);
            } catch (JSONException e) {
                Log.e(TAG, " Error creating JSON for registration", e);
            }
        }
    }

    public void sendSensorData(String type, int value) {
        if (isConnected) {
            try {
                JSONObject json = new JSONObject();
                json.put("type", type);
                json.put("value", value);
                json.put("vin", vin);
                send(json.toString());
                //Log.d(TAG, " Sent sensor data: " + json.toString());
            } catch (JSONException e) {
                Log.e(TAG, " Error sending sensor data", e);
            }
        } else {
            Log.w(TAG, " WebSocket not connected, cannot send data.");
        }
    }

    private void handleCommand(JSONObject json) {
        String commandType = json.optString("command", "");
        int value = json.optInt("value", -1);

        switch (commandType) {
            case "handlelockcommand":
                Log.d(TAG, " Command received: handlelockcommand");
                sendAcknowledgment(commandType, "handlelockcommand");
               // MainActivity.handlelockstate=0;
                serialPortManager.VCUSendToMCU("FFAA550101000A00");
                break;

            case "handleunlockcommand":
                Log.d(TAG, " Command received: handleunlockcommand");
                sendAcknowledgment(commandType, "handleunlockcommand");
               // MainActivity.handlelockstate=1;
                serialPortManager.VCUSendToMCU("FFAA550101000A01");
                break;

            case "seatlockcommand":
                Log.d(TAG, " Command received: seatlockcommand");
                sendAcknowledgment(commandType, "seatlockcommand");
                //MainActivity.seatlockstate=0;
                serialPortManager.VCUSendToMCU("FFAA550102000A01");
                break;

            case "seatunlockcommand":
                Log.d(TAG, "Command received: seatunlockcommand");
                sendAcknowledgment(commandType, "seatunlockcommand");
                //MainActivity.seatlockstate=1;
                serialPortManager.VCUSendToMCU("FFAA550102000A01");
                break;

            default:
                Log.w(TAG, " Unknown command received: " + commandType);
                sendAcknowledgment(commandType, "Unknown Command");
                break;
        }
    }

    private void sendAcknowledgment(String command, String message) {
        if (isConnected) {
            try {
                JSONObject json = new JSONObject();
                json.put("type", "ack");
                json.put("command", command);
                json.put("message", message);
                json.put("vin", vin);
                send(json.toString());
                Log.d(TAG, " Sent acknowledgment: " + json.toString());
            } catch (JSONException e) {
                Log.e(TAG, " Error sending acknowledgment", e);
            }
        }
    }

    private void reconnectWebSocket() {
        new Thread(() -> {
            try {
                Thread.sleep(3000); // Retry after 3 seconds
                Log.d(TAG, " Attempting to reconnect...");
                reconnect();
            } catch (InterruptedException e) {
                Log.e(TAG, " Reconnect failed", e);
            }
        }).start();
    }
}
