package com.fanconn.rideOS.menu;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.List;

public class RiderMqttSyncWorker {

    private static final String TAG =
            "RiderMqttSyncWorker";

    private static boolean running = false;

    public static synchronized void sync(
            Context context
    ) {

        if (running) {
            return;
        }

        RiderMqttManager mqttManager =
                RiderMqttManager.getInstance(context);

        if (!mqttManager.isConnected()) {
            return;
        }

        running = true;

        new Thread(() -> {

            try {

                RiderQueueManager queue =
                        RiderQueueManager
                                .getInstance(context);

                List<RiderQueueEntity> items =
                        queue.getAll();

                Gson gson = new Gson();

                for (RiderQueueEntity item : items) {

                    try {

                        RiderMqttMessage message =
                                gson.fromJson(
                                        item.payload,
                                        RiderMqttMessage.class
                                );

                        if (message.vin == null
                                || message.vin.trim().isEmpty()) {
                            continue;
                        }

                        /*
                         * We need the actual MQTT client.
                         * RiderMqttManager publishes normal
                         * operations; queued operations are
                         * handled there.
                         */

                        boolean success =
                                publishQueued(
                                        context,
                                        message.vin,
                                        item.payload
                                );

                        if (success) {

                            queue.delete(item.id);

                            Log.d(
                                    TAG,
                                    "Staged rider uploaded: "
                                            + item.id
                            );
                        }

                    } catch (Exception e) {

                        Log.e(
                                TAG,
                                "Failed staged rider",
                                e
                        );

                        break;
                    }
                }

            } finally {

                running = false;
            }

        }).start();
    }

    private static boolean publishQueued(
            Context context,
            String vin,
            String payload
    ) {

        /*
         * Queued publishing will be exposed through
         * RiderMqttManager in the next step.
         */

        return RiderMqttManager
                .getInstance(context)
                .publishQueued(vin, payload);
    }
}