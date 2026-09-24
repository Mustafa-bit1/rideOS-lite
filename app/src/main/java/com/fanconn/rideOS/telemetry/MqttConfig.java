package com.fanconn.rideOS.telemetry;

public final class MqttConfig {

    private MqttConfig() {
        // Prevent instantiation
    }


    // ============================================================
    // BROKER
    // ============================================================

    public static final String BROKER_URL =
            "tcp://103.174.148.249:1883";

    public static final String MQTT_USERNAME =
            "telemetry_backend";

    public static final String MQTT_PASSWORD =
            "MQTT@RIVOTM0tors";


    // ============================================================
    // MQTT SETTINGS
    // ============================================================

    public static final int PORT = 1883;

    public static final int TELEMETRY_QOS = 0;

    public static final String TELEMETRY_TOPIC_PREFIX =
            "scooter/";

    public static final String TELEMETRY_TOPIC_SUFFIX =
            "/telemetry";
}