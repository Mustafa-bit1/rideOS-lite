package com.fanconn.rideOS.telemetry;

import android.util.Log;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MqttTelemetryPublisher {

    private static final String TAG =
            "MqttTelemetryPublisher";

    private static final int MQTT_QOS =
            0;

    private static final long PUBLISH_TIMEOUT_SECONDS =
            2;

    private final MqttConnectionManager connectionManager;

    /*
     * IMPORTANT:
     *
     * Do NOT use a single-thread executor here.
     *
     * If an old MQTT client blocks during a Wi-Fi -> cellular
     * transition, a single thread would block every future
     * telemetry publish.
     */
    private final ExecutorService publishExecutor =
            Executors.newCachedThreadPool();

    public MqttTelemetryPublisher(
            MqttConnectionManager connectionManager) {

        this.connectionManager =
                connectionManager;
    }

    public boolean publish(
            JSONObject payload) {

        // ========================================================
        // PAYLOAD CHECK
        // ========================================================

        if (payload == null) {

            Log.e(
                    TAG,
                    "Payload is null"
            );

            return false;
        }

        // ========================================================
        // MQTT CONNECTION CHECK
        // ========================================================

        if (!connectionManager.isConnected()) {

            Log.w(
                    TAG,
                    "MQTT not connected. Payload not published."
            );

            return false;
        }

        // ========================================================
        // VIN
        // ========================================================

        String vin =
                payload.optString(
                        "v",
                        ""
                ).trim();

        if (vin.isEmpty()) {

            Log.e(
                    TAG,
                    "VIN missing: "
                            + payload
            );

            return false;
        }

        // ========================================================
        // TOPIC
        // ========================================================

        String topic =
                "scooter/"
                        + vin
                        + "/telemetry";

        // ========================================================
        // MESSAGE
        // ========================================================

        MqttMessage message =
                new MqttMessage(
                        payload.toString()
                                .getBytes(
                                        StandardCharsets.UTF_8
                                )
                );

        message.setQos(
                MQTT_QOS
        );

        message.setRetained(
                false
        );

        // ========================================================
        // CAPTURE CURRENT CLIENT
        // ========================================================

        final MqttClient client =
                connectionManager.getMqttClient();

        if (client == null) {

            Log.w(
                    TAG,
                    "MQTT client is null. "
                            + "Payload not published."
            );

            return false;
        }

        if (!client.isConnected()) {

            Log.w(
                    TAG,
                    "MQTT client is not connected. "
                            + "Payload not published."
            );

            return false;
        }

        // ========================================================
        // PUBLISH
        // ========================================================

        Future<?> future =
                publishExecutor.submit(
                        () -> {

                            /*
                             * IMPORTANT:
                             *
                             * Use the exact client that was
                             * verified above.
                             *
                             * Do not fetch another client here.
                             */
                            if (!client.isConnected()) {

                                throw new IllegalStateException(
                                        "MQTT client disconnected "
                                                + "before publish"
                                );
                            }

                            try {

                                client.publish(
                                        topic,
                                        message
                                );

                            } catch (MqttException e) {

                                throw new RuntimeException(
                                        e
                                );
                            }
                        }
                );

        // ========================================================
        // WAIT FOR PUBLISH
        // ========================================================

        try {

            future.get(
                    PUBLISH_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
            );

            return true;

        } catch (TimeoutException e) {

            /*
             * IMPORTANT:
             *
             * The old Paho operation may still be internally
             * blocked. But because we are NOT using a
             * single-thread executor, future telemetry
             * publishes are not blocked behind it.
             */
            future.cancel(
                    true
            );

            Log.w(
                    TAG,
                    "Telemetry publish timed out -> "
                            + topic
            );

            return false;

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Telemetry publish failed -> "
                            + topic,
                    e
            );

            return false;
        }
    }

    // ============================================================
    // SHUTDOWN
    // ============================================================

    public void shutdown() {

        try {

            publishExecutor.shutdownNow();

        } catch (Exception e) {

            Log.w(
                    TAG,
                    "Failed to shutdown publish executor",
                    e
            );
        }
    }
}