package com.fanconn.rideOS.telemetry;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.fanconn.rideOS.telemetry.remotecommand.MqttCommandService;

import org.json.JSONObject;

public class MqttTelemetryService extends Service {

    private static final String TAG =
            "MqttTelemetryService";

    // ============================================================
    // FOREGROUND SERVICE
    // ============================================================

    private static final String CHANNEL_ID =
            "vehicle_telemetry";

    private static final int NOTIFICATION_ID =
            1001;

    // ============================================================
    // TELEMETRY
    // ============================================================

    private static final long TELEMETRY_INTERVAL_MS =
            1000L;

    // ============================================================
    // MANAGERS
    // ============================================================

    private TelemetryStagingManager stagingManager;

    private VehicleTelemetryManager telemetryManager;

    private TelemetryChangeTracker changeTracker;

    private MqttConnectionManager mqttConnectionManager;

    private MqttTelemetryPublisher mqttPublisher;

    private MqttCommandService mqttCommandService;

    // ============================================================
    // TELEMETRY THREAD
    // ============================================================

    private HandlerThread handlerThread;

    private Handler handler;

    private volatile boolean serviceRunning = false;

    // ============================================================
    // SERVICE CREATED
    // ============================================================

    @Override
    public void onCreate() {

        super.onCreate();

        Log.d(
                TAG,
                "Service created"
        );

        // --------------------------------------------------------
        // START FOREGROUND
        // --------------------------------------------------------

        startForegroundServiceNotification();

        // --------------------------------------------------------
        // TELEMETRY MANAGER
        // --------------------------------------------------------

        telemetryManager =
                VehicleTelemetryManager.getInstance();

        // --------------------------------------------------------
        // CHANGE TRACKER
        // --------------------------------------------------------

        changeTracker =
                new TelemetryChangeTracker();

        // --------------------------------------------------------
        // MQTT CONNECTION MANAGER
        // --------------------------------------------------------

        mqttConnectionManager =
                MqttConnectionManager.getInstance(
                        this
                );

        // --------------------------------------------------------
        // START NETWORK MONITORING
        // --------------------------------------------------------

        mqttConnectionManager
                .startNetworkMonitoring();

        // --------------------------------------------------------
        // MQTT PUBLISHER
        // --------------------------------------------------------

        mqttPublisher =
                new MqttTelemetryPublisher(
                        mqttConnectionManager
                );

        // --------------------------------------------------------
        // COMMAND SERVICE
        // --------------------------------------------------------

        mqttCommandService =
                new MqttCommandService(
                        mqttConnectionManager
                );

        mqttConnectionManager.setCommandService(
                mqttCommandService
        );

        // --------------------------------------------------------
        // STAGING MANAGER
        // --------------------------------------------------------

        stagingManager =
                TelemetryStagingManager.getInstance(
                        getApplicationContext()
                );
    }

    // ============================================================
    // FOREGROUND NOTIFICATION
    // ============================================================

    private void startForegroundServiceNotification() {

        // --------------------------------------------------------
        // CREATE NOTIFICATION CHANNEL
        // --------------------------------------------------------

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "Vehicle Telemetry",
                            NotificationManager.IMPORTANCE_LOW
                    );

            channel.setDescription(
                    "RIVOT vehicle telemetry service"
            );

            NotificationManager manager =
                    getSystemService(
                            NotificationManager.class
                    );

            if (manager != null) {

                manager.createNotificationChannel(
                        channel
                );
            }
        }

        // --------------------------------------------------------
        // BUILD NOTIFICATION
        // --------------------------------------------------------

        Notification notification =
                new NotificationCompat.Builder(
                        this,
                        CHANNEL_ID
                )
                        .setContentTitle(
                                "RIVOT Vehicle"
                        )
                        .setContentText(
                                "Telemetry service running"
                        )
                        .setSmallIcon(
                                android.R.drawable
                                        .ic_menu_info_details
                        )
                        .setOngoing(true)
                        .setPriority(
                                NotificationCompat.PRIORITY_LOW
                        )
                        .build();

        // --------------------------------------------------------
        // START FOREGROUND
        // --------------------------------------------------------

        startForeground(
                NOTIFICATION_ID,
                notification
        );
    }

    // ============================================================
    // START TELEMETRY LOOP
    // ============================================================

    private void startTelemetryLoop() {

        if (serviceRunning) {

//            Log.d(
//                    TAG,
//                    "Telemetry loop already running"
//            );

            return;
        }

        handlerThread =
                new HandlerThread(
                        "MqttTelemetryThread"
                );

        handlerThread.start();

        handler =
                new Handler(
                        handlerThread.getLooper()
                );

        serviceRunning = true;

        handler.post(
                telemetryRunnable
        );

        Log.d(
                TAG,
                "Telemetry loop started"
        );
    }

    // ============================================================
    // TELEMETRY RUNNABLE
    // ============================================================

    private final Runnable telemetryRunnable =
            new Runnable() {

                @Override
                public void run() {

                    if (!serviceRunning) {

                        Log.d(
                                TAG,
                                "Telemetry loop stopped: serviceRunning=false"
                        );

                        return;
                    }

                    // Confirms the 1-second telemetry loop is still alive
//                    Log.d(
//                            TAG,
//                            "Telemetry loop tick"
//                    );

                    try {

//                        Log.d(
//                                TAG,
//                                "Starting telemetry collection"
//                        );

                        collectAndSendTelemetry();

//                        Log.d(

//                                TAG,
//                                "Telemetry collection completed"
//                        );

                    } catch (Exception e) {

                        Log.e(
                                TAG,
                                "Telemetry collection error",
                                e
                        );
                    }

                    if (handler != null &&
                            serviceRunning) {

//                        Log.d(
//                                TAG,
//                                "Scheduling next telemetry tick in "
//                                        + TELEMETRY_INTERVAL_MS
//                                        + " ms"
//                        );

                        handler.postDelayed(
                                this,
                                TELEMETRY_INTERVAL_MS
                        );

                    } else {

                        Log.w(
                                TAG,
                                "Telemetry loop NOT rescheduled. "
                                        + "handler="
                                        + (handler != null)
                                        + ", serviceRunning="
                                        + serviceRunning
                        );
                    }
                }
            };

    // ============================================================
    // COLLECT + SEND TELEMETRY
    // ============================================================

    private void collectAndSendTelemetry() {

        // --------------------------------------------------------
        // TIME
        // --------------------------------------------------------

        telemetryManager.setTime(
                java.time.Instant
                        .now()
                        .toString()
        );

        // --------------------------------------------------------
        // BUILD CHANGED PAYLOAD
        // --------------------------------------------------------

        JSONObject payload =
                changeTracker.buildChangedPayload(
                        telemetryManager
                );

        // --------------------------------------------------------
        // NO CHANGES
        // --------------------------------------------------------

        if (payload.length() <= 1) {

            Log.d(
                    TAG,
                    "No telemetry changes"
            );

            return;
        }

        // --------------------------------------------------------
        // MQTT CONNECTED
        // --------------------------------------------------------

        if (mqttConnectionManager.isConnected()) {

            boolean published =
                    mqttPublisher.publish(
                            payload
                    );

            if (!published) {

                Log.w(
                        TAG,
                        "MQTT publish failed. Staging telemetry."
                );

                stagingManager.stage(
                        payload
                );
            } else {
                Log.d(TAG, "MQTT payload sent");
//                Log.d(TAG, "Telemetry sent: " + payload);
            }

            return;
        }

        // --------------------------------------------------------
        // MQTT NOT CONNECTED
        // --------------------------------------------------------

        Log.w(
                TAG,
                "MQTT not connected. Staging telemetry."
        );

        stagingManager.stage(
                payload
        );
    }

    // ============================================================
    // GET TELEMETRY MANAGER
    // ============================================================

    public VehicleTelemetryManager getTelemetryManager() {

        return telemetryManager;
    }

    // ============================================================
    // SERVICE START
    // ============================================================

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId) {

        Log.d(
                TAG,
                "Service started"
        );

        // --------------------------------------------------------
        // ENSURE FOREGROUND
        // --------------------------------------------------------

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            startForegroundServiceNotification();
        }

        // --------------------------------------------------------
        // MQTT CONNECT
        // --------------------------------------------------------

        mqttConnectionManager.connect();

        // --------------------------------------------------------
        // TELEMETRY LOOP
        // --------------------------------------------------------

        startTelemetryLoop();

        // --------------------------------------------------------
        // SERVICE RESTART BEHAVIOR
        // --------------------------------------------------------

        return START_STICKY;
    }

    // ============================================================
    // SERVICE DESTROYED
    // ============================================================

    @Override
    public void onDestroy() {

        Log.d(
                TAG,
                "Service destroyed"
        );

        serviceRunning = false;

        // --------------------------------------------------------
        // STOP TELEMETRY LOOP
        // --------------------------------------------------------

        if (handler != null) {

            handler.removeCallbacksAndMessages(
                    null
            );
        }

        if (handlerThread != null) {

            handlerThread.quitSafely();

            handlerThread = null;
        }

        handler = null;

        /*
         * IMPORTANT:
         *
         * Do NOT call:
         *
         * mqttConnectionManager.disconnect();
         *
         * here.
         */

        super.onDestroy();
    }

    // ============================================================
    // NO BINDING
    // ============================================================

    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }
}