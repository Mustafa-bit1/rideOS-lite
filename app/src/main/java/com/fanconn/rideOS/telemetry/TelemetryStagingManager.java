package com.fanconn.rideOS.telemetry;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TelemetryStagingManager {

    private static final String TAG = "MQTT TelemetryStaging";

    private static final int MAX_STAGED_PAYLOADS = 43200;// 12 hours stagging
    private static final int UPLOAD_BATCH_SIZE = 10;

    private static TelemetryStagingManager instance;

    private final StagedTelemetryDao dao;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private TelemetryStagingManager(Context context) {

        TelemetryDatabase database =
                TelemetryDatabase.getInstance(context);

        dao = database.stagedTelemetryDao();
    }

    public static synchronized TelemetryStagingManager getInstance(
            Context context) {

        if (instance == null) {
            instance =
                    new TelemetryStagingManager(context);
        }

        return instance;
    }

    /**
     * Store telemetry asynchronously.
     */

    /**
     * Store telemetry asynchronously.
     */
    public void stage(JSONObject payload) {

        if (payload == null) {
            Log.e(TAG, "Cannot stage null payload");
            return;
        }

        executor.execute(() -> {

            try {

                String vin =
                        payload.optString("v", "").trim();

                if (vin.isEmpty()) {

                    Log.e(
                            TAG,
                            "Cannot stage telemetry. VIN missing: "
                                    + payload
                    );

                    return;
                }

                int currentCount =
                        dao.getCount();

                /*
                 * Maximum queue = 600 records.
                 *
                 * When full, remove the oldest record
                 * and keep the newest telemetry.
                 */
                if (currentCount >= MAX_STAGED_PAYLOADS) {

                    dao.deleteOldest(1);

                    Log.w(
                            TAG,
                            "QUEUE FULL -> Removed oldest record. "
                                    + "Max=" + MAX_STAGED_PAYLOADS
                    );
                }

                StagedTelemetryEntity entity =
                        new StagedTelemetryEntity(
                                vin,
                                payload.toString(),
                                System.currentTimeMillis()
                        );

                long id =
                        dao.insert(entity);

                int count =
                        dao.getCount();

                Log.d(
                        TAG,
                        "STAGED -> id=" + id
                                + " | Room queue=" + count
                                + "/" + MAX_STAGED_PAYLOADS
                );

            } catch (Exception e) {

                Log.e(
                        TAG,
                        "Failed to stage telemetry",
                        e
                );
            }
        });
    }


    /**
     * IMPORTANT:
     * Called only from background Worker.
     */
    /**
     * IMPORTANT:
     * Called only from background Worker.
     */
    public List<StagedTelemetryEntity> getBatch() {

        List<StagedTelemetryEntity> batch =
                dao.getOldest(UPLOAD_BATCH_SIZE);

        int remainingCount =
                dao.getCount();

        Log.d(
                TAG,
                "BATCH FETCH -> fetched="
                        + batch.size()
                        + " | Room queue before delete="
                        + remainingCount
                        + "/" + MAX_STAGED_PAYLOADS
        );

        return batch;
    }



    /**
     * IMPORTANT:
     * Called only from background Worker.
     *
     * Delete synchronously so the Worker knows the
     * record is actually deleted before continuing.
     */

    /**
     * IMPORTANT:
     * Called only from background Worker.
     *
     * Delete synchronously so the Worker knows the
     * record is actually deleted before continuing.
     */
    public void deleteSync(
            StagedTelemetryEntity entity) {

        if (entity == null) {
            return;
        }

        dao.delete(entity);

        int remainingCount =
                dao.getCount();

        Log.d(
                TAG,
                "DELETED -> id=" + entity.id
                        + " | Room queue="
                        + remainingCount
                        + "/" + MAX_STAGED_PAYLOADS
        );
    }



    /**
     * Current staged count.
     *
     * Call only from a background thread.
     */
    public int getCount() {

        return dao.getCount();
    }

    public static int getUploadBatchSize() {

        return UPLOAD_BATCH_SIZE;
    }
}