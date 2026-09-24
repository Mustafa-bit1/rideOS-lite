package com.fanconn.rideOS.telemetry.remotecommand;

import android.util.Log;

import com.fanconn.rideOS.telemetry.MqttConnectionManager;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class MqttCommandService {

    private static final String TAG = "MqttCommandService";

    private final MqttConnectionManager connectionManager;

    public MqttCommandService(
            MqttConnectionManager connectionManager) {

        this.connectionManager = connectionManager;
    }

    /**
     * Subscribe to commands for this scooter.
     */
    public void subscribe(String vin) {

        if (vin == null || vin.trim().isEmpty()) {

            Log.e(
                    TAG,
                    "Cannot subscribe to commands. VIN is empty."
            );

            return;
        }

        if (!connectionManager.isConnected()) {

            Log.w(
                    TAG,
                    "MQTT not connected. "
                            + "Command subscription skipped."
            );

            return;
        }

        String topic =
                "scooter/"
                        + vin.trim()
                        + "/command";

        try {

            MqttClient client =
                    connectionManager.getMqttClient();

            if (client == null) {

                Log.e(
                        TAG,
                        "MQTT client is null."
                );

                return;
            }

            client.subscribe(
                    topic,
                    1,
                    new IMqttMessageListener() {

                        @Override
                        public void messageArrived(
                                String topic,
                                MqttMessage message) {

                            handleCommand(
                                    topic,
                                    message
                            );
                        }
                    }
            );

            Log.d(
                    TAG,
                    "Subscribed to command topic: "
                            + topic
            );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Command subscription failed",
                    e
            );
        }
    }

    /**
     * Receive and process command.
     */
    private void handleCommand(
            String topic,
            MqttMessage message) {

        try {

            String payload =
                    new String(
                            message.getPayload(),
                            StandardCharsets.UTF_8
                    );

            Log.d(
                    TAG,
                    "Command received. "
                            + "Topic=" + topic
                            + " | Payload=" + payload
            );

            JSONObject json =
                    new JSONObject(payload);

            Command command =
                    new Command();

            command.setCommandId(
                    json.optString(
                            "commandId",
                            ""
                    )
            );

            command.setVin(
                    json.optString(
                            "vin",
                            ""
                    )
            );

            command.setCommand(
                    json.optString(
                            "command",
                            ""
                    )
            );

            command.setValue(
                    json.optString(
                            "value",
                            ""
                    )
            );

            command.setTimestamp(
                    json.optString(
                            "timestamp",
                            ""
                    )
            );

            if (!validateCommand(command)) {
                return;
            }

            executeCommand(command);

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Failed to process command",
                    e
            );
        }
    }

    /**
     * Validate incoming command.
     */
    private boolean validateCommand(
            Command command) {

        if (command.getCommandId() == null
                || command.getCommandId().trim().isEmpty()) {

            Log.e(
                    TAG,
                    "Command rejected: commandId missing"
            );

            return false;
        }

        if (command.getVin() == null
                || command.getVin().trim().isEmpty()) {

            Log.e(
                    TAG,
                    "Command rejected: VIN missing"
            );

            return false;
        }

        if (command.getCommand() == null
                || command.getCommand().trim().isEmpty()) {

            Log.e(
                    TAG,
                    "Command rejected: command type missing"
            );

            return false;
        }

        return true;
    }

    /**
     * Execute command.
     *
     * ACTUAL SCOOTER ACTIONS WILL BE ADDED LATER.
     */
    private void executeCommand(
            Command command) {

        String commandType =
                command.getCommand().trim().toUpperCase();

        String value =
                command.getValue();

        String ackStatus = "ACK";
        String message;

        switch (commandType) {

            case "POWER":

                if ("1".equals(value)) {

                    // TODO:
                    // UNLOCK SCOOTER

                    Log.d(
                            TAG,
                            "Remote unlock scooter"
                    );

                    message =
                            "Power unlock command received";

                } else if ("0".equals(value)) {

                    // TODO:
                    // LOCK SCOOTER

                    Log.d(
                            TAG,
                            "Remote lock scooter"
                    );

                    message =
                            "Power lock command received";

                } else {

                    ackStatus = "FAILED";

                    message =
                            "Invalid command value";
                }

                break;

            case "HANDLE_LOCK":

                if ("1".equals(value)) {

                    // TODO:
                    // HANDLE LOCK

                    Log.d(
                            TAG,
                            "Remote handle lock"
                    );

                    message =
                            "Handle lock command received";

                } else if ("0".equals(value)) {

                    // TODO:
                    // HANDLE UNLOCK

                    Log.d(
                            TAG,
                            "Remote handle unlock"
                    );

                    message =
                            "Handle unlock command received";

                } else {

                    ackStatus = "FAILED";

                    message =
                            "Invalid HANDLE_LOCK value";
                }

                break;

            case "CAN":

                // TODO:
                // Parse CAN ID + CAN DATA
                // and send through existing CAN implementation.

                Log.d(
                        TAG,
                        "Remote CAN command received. "
                                + "Value=" + value
                );

                message =
                        "CAN command received";

                break;

            default:

                ackStatus = "FAILED";

                message =
                        "Unknown command type: "
                                + commandType;

                Log.w(
                        TAG,
                        "Unknown remote command: "
                                + commandType
                );

                break;
        }

        sendAck(
                command,
                ackStatus,
                message
        );
    }

    /**
     * Send ACK back to backend.
     */
    private void sendAck(
            Command command,
            String status,
            String message) {

        if (!connectionManager.isConnected()) {

            Log.e(
                    TAG,
                    "Cannot send command ACK. "
                            + "MQTT not connected."
            );

            return;
        }

        try {

            String topic =
                    "scooter/"
                            + command.getVin()
                            + "/command/response";

            JSONObject response =
                    new JSONObject();

            response.put(
                    "commandId",
                    command.getCommandId()
            );

            response.put(
                    "vin",
                    command.getVin()
            );

            response.put(
                    "command",
                    command.getCommand()
            );

            response.put(
                    "status",
                    status
            );

            response.put(
                    "message",
                    message
            );

            response.put(
                    "timestamp",
                    Instant.now().toString()
            );

            MqttMessage mqttMessage =
                    new MqttMessage(
                            response.toString()
                                    .getBytes(
                                            StandardCharsets.UTF_8
                                    )
                    );

            mqttMessage.setQos(1);
            mqttMessage.setRetained(false);

            MqttClient client =
                    connectionManager.getMqttClient();

            if (client == null
                    || !client.isConnected()) {

                Log.e(
                        TAG,
                        "MQTT client unavailable "
                                + "while sending ACK"
                );

                return;
            }

            Log.d(
                    TAG,
                    "Sending command ACK -> Topic: "
                            + topic
            );

            Log.d(
                    TAG,
                    "Command ACK payload: "
                            + response
            );

            client.publish(
                    topic,
                    mqttMessage
            );

            Log.d(
                    TAG,
                    "Command ACK sent successfully"
            );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Failed to send command ACK",
                    e
            );
        }
    }
}
