package com.fanconn.rideOS.telemetry;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONObject;

import java.util.List;

public class TelemetryUploadWorker extends Worker {

    private static final String TAG =
            "TelemetryUploadWorker";

    public TelemetryUploadWorker(
            @NonNull Context context,
            @NonNull WorkerParameters workerParams) {

        super(
                context,
                workerParams
        );
    }

    @NonNull
    @Override
    public Result doWork() {

        Log.d(
                TAG,
                "========================================"
        );

        Log.d(
                TAG,
                "STAGED TELEMETRY UPLOAD STARTED"
        );

        TelemetryStagingManager stagingManager =
                TelemetryStagingManager.getInstance(
                        getApplicationContext()
                );

        MqttConnectionManager connectionManager =
                MqttConnectionManager.getInstance(
                        getApplicationContext()
                );

        MqttTelemetryPublisher publisher =
                new MqttTelemetryPublisher(
                        connectionManager
                );

        try {

            // ----------------------------------------------------
            // MQTT CHECK
            // ----------------------------------------------------

            if (!connectionManager.isConnected()) {

                Log.w(
                        TAG,
                        "MQTT not connected."
                                + " Keeping staged telemetry."
                );

                return Result.retry();
            }

            // ----------------------------------------------------
            // UPLOAD LOOP
            // ----------------------------------------------------

            while (true) {

                // ------------------------------------------------
                // CHECK MQTT BEFORE EACH BATCH
                // ------------------------------------------------

                if (!connectionManager.isConnected()) {

                    Log.w(
                            TAG,
                            "MQTT disconnected during staged upload."
                    );

                    return Result.retry();
                }

                // ------------------------------------------------
                // GET BATCH
                // ------------------------------------------------

                List<StagedTelemetryEntity> batch =
                        stagingManager.getBatch();

                if (batch == null
                        || batch.isEmpty()) {

                    Log.d(
                            TAG,
                            "========================================"
                    );

                    Log.d(
                            TAG,
                            "STAGED TELEMETRY UPLOAD COMPLETE"
                    );

                    Log.d(
                            TAG,
                            "No staged telemetry remaining."
                    );

                    return Result.success();
                }

                Log.d(
                        TAG,
                        "Uploading staged batch -> "
                                + batch.size()
                                + " records"
                );

                // ------------------------------------------------
                // UPLOAD BATCH
                // ------------------------------------------------

                for (StagedTelemetryEntity entity : batch) {

                    if (entity == null) {
                        continue;
                    }

                    // --------------------------------------------
                    // MQTT CHECK
                    // --------------------------------------------

                    if (!connectionManager.isConnected()) {

                        Log.w(
                                TAG,
                                "MQTT disconnected."
                                        + " Stopping staged upload."
                        );

                        return Result.retry();
                    }

                    try {

                        JSONObject payload =
                                new JSONObject(
                                        entity.payload
                                );

                        boolean published =
                                publisher.publish(
                                        payload
                                );

                        if (!published) {

                            Log.w(
                                    TAG,
                                    "Staged publish failed."
                                            + " id="
                                            + entity.id
                            );

                            return Result.retry();
                        }

                        /*
                         * IMPORTANT:
                         *
                         * Delete ONLY after publish()
                         * reports success.
                         */
                        stagingManager.deleteSync(
                                entity
                        );

                        Log.d(
                                TAG,
                                "STAGED UPLOAD SUCCESS"
                                        + " | id="
                                        + entity.id
                        );

                    } catch (Exception e) {

                        Log.e(
                                TAG,
                                "Failed staged record"
                                        + " | id="
                                        + entity.id,
                                e
                        );

                        return Result.retry();
                    }
                }

                Log.d(
                        TAG,
                        "Staged batch completed."
                                + " Checking for next batch."
                );
            }

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Staged telemetry upload failed",
                    e
            );

            return Result.retry();

        } finally {

            /*
             * MqttTelemetryPublisher owns an executor.
             * Make sure this worker releases it.
             */
            try {

                publisher.shutdown();

            } catch (Exception e) {

                Log.w(
                        TAG,
                        "Failed to shutdown MQTT publisher",
                        e
                );
            }
        }
    }
}