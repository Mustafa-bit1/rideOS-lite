package com.fanconn.rideOS.menu;

import android.content.Context;
import android.util.Log;

import com.fanconn.rideOS.MetadataManager;

import com.google.gson.Gson;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

public class RiderMqttManager {

    private static final String TAG = "RiderMqttManager";

    private static final String MQTT_HOST =
            "tcp://103.174.148.249:1883";

    private static final String MQTT_USERNAME =
            "TESTVIN001";

    private static final String MQTT_PASSWORD =
            "MQTT@RIVOTM0tors";
    private static RiderMqttManager instance;

    private final Context context;

    private final Gson gson = new Gson();

    private MqttClient mqttClient;

    private RiderMqttManager(Context context) {

        this.context = context.getApplicationContext();
    }

    public static synchronized RiderMqttManager getInstance(
            Context context
    ) {

        if (instance == null) {
            instance = new RiderMqttManager(context);
        }

        return instance;
    }

    public String generateRiderId() {

        return UUID.randomUUID().toString();
    }

    public boolean isConnected() {

        return mqttClient != null
                && mqttClient.isConnected();
    }

    public void connect() {

        new Thread(() -> {

            try {

                if (isConnected()) {
                    return;
                }

                String clientId =
                        "rideOS-rider-" + UUID.randomUUID();

                mqttClient = new MqttClient(
                        MQTT_HOST,
                        clientId,
                        null
                );

                org.eclipse.paho.client.mqttv3.MqttConnectOptions options =
                        new org.eclipse.paho.client.mqttv3.MqttConnectOptions();

                options.setUserName(MQTT_USERNAME);
                options.setPassword(
                        MQTT_PASSWORD.toCharArray()
                );

                options.setAutomaticReconnect(true);
                options.setCleanSession(true);

                mqttClient.connect(options);

                Log.d(TAG, "Rider MQTT connected");

                // Sync riders that were queued while offline.
                RiderMqttSyncWorker.sync(context);

            } catch (Exception e) {

                Log.e(
                        TAG,
                        "Rider MQTT connection failed: "
                                + e.getMessage(),
                        e
                );
            }

        }).start();
    }
    public void addRider(
            String riderId,
            String name,
            String phone,
            int age,
            float topSpeed,
            String topTorque,
            int pickupLevel,
            int brakingLevel
    ) {

        RiderMqttMessage message =
                new RiderMqttMessage();

        message.operation = "ADD";
        message.rider_id = riderId;
        message.vin = getVin();

        message.name = name;
        message.phone = phone;
        message.age = age;

        message.top_speed = topSpeed;
        message.top_torque = topTorque;

        message.pickup_level = pickupLevel;
        message.braking_level = brakingLevel;

        message.created_at = now();

        publishOrQueue(message);
    }

    public void updateRider(
            String riderId,
            String name,
            String phone,
            int age,
            float topSpeed,
            String topTorque,
            int pickupLevel,
            int brakingLevel
    ) {

        RiderMqttMessage message =
                new RiderMqttMessage();

        message.operation = "UPDATE";
        message.rider_id = riderId;
        message.vin = getVin();

        message.name = name;
        message.phone = phone;
        message.age = age;

        message.top_speed = topSpeed;
        message.top_torque = topTorque;

        message.pickup_level = pickupLevel;
        message.braking_level = brakingLevel;

        message.updated_at = now();

        publishOrQueue(message);
    }

    public void deleteRider(String riderId) {

        RiderMqttMessage message =
                new RiderMqttMessage();

        message.operation = "DELETE";
        message.rider_id = riderId;
        message.vin = getVin();

        message.deleted_at = now();

        publishOrQueue(message);
    }

    private void publishOrQueue(
            RiderMqttMessage message
    ) {

        String payload =
                gson.toJson(message);

        if (message.vin == null
                || message.vin.trim().isEmpty()
                || message.vin.equals("UNKNOWN_VIN")) {

            Log.e(TAG,
                    "Cannot sync rider. VIN unavailable.");

            return;
        }

        if (!isConnected()) {

            Log.d(TAG,
                    "MQTT unavailable. Staging rider.");

            RiderQueueManager
                    .getInstance(context)
                    .enqueue(
                            message.operation,
                            payload
                    );

            return;
        }

        publish(
                message.vin,
                payload,
                message.operation
        );
    }

    private void publish(
            String vin,
            String payload,
            String operation
    ) {

        new Thread(() -> {

            try {

                String topic =
                        "scooter/"
                                + vin
                                + "/rider";

                MqttMessage mqttMessage =
                        new MqttMessage(
                                payload.getBytes()
                        );

                mqttMessage.setQos(1);
                mqttMessage.setRetained(false);

                mqttClient.publish(
                        topic,
                        mqttMessage
                );

                Log.d(
                        TAG,
                        operation
                                + " rider published"
                );

            } catch (Exception e) {

                Log.e(
                        TAG,
                        "Rider publish failed. Staging.",
                        e
                );

                RiderQueueManager
                        .getInstance(context)
                        .enqueue(
                                operation,
                                payload
                        );
            }

        }).start();
    }

    private String getVin() {

        String vin =
                MetadataManager.get().getVin();

        if (vin == null) {
            return "";
        }

        return vin.trim();
    }

    private String now() {

        SimpleDateFormat sdf =
                new SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                        Locale.US
                );

        sdf.setTimeZone(
                TimeZone.getTimeZone("Asia/Kolkata")
        );

        return sdf.format(new Date());
    }

    public boolean publishQueued(
            String vin,
            String payload
    ) {

        try {

            if (!isConnected()) {
                return false;
            }

            String topic =
                    "scooter/" + vin + "/rider";

            MqttMessage message =
                    new MqttMessage(payload.getBytes());

            message.setQos(1);
            message.setRetained(false);

            mqttClient.publish(topic, message);

            return true;

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Queued rider publish failed",
                    e
            );

            return false;
        }
    }
}