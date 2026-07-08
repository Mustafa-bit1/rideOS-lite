package com.fanconn.rideOS;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;

public class TelemetryStagingManager {

    private static final int BATCH_SIZE = 10;
    private static TelemetryStagingManager instance;

    private final TelemetryDao dao;

    private TelemetryStagingManager(Context context) {
        dao = TelemetryDatabase.getInstance(context).telemetryDao();
    }

    public static synchronized TelemetryStagingManager getInstance(Context context) {
        if (instance == null) {
            instance = new TelemetryStagingManager(context);
        }
        return instance;
    }

    public void addData(RealTimeData data) {
        new Thread(() -> {
            dao.insert(data);
            dao.keepLast24Hours();
        }).start();
    }

    public List<RealTimeData> pollBatch() {
        List<RealTimeData> batch = dao.getBatch(BATCH_SIZE);

        if (!batch.isEmpty()) {
            List<Long> ids = new ArrayList<>();
            for (RealTimeData d : batch) {
                ids.add(d.id);
            }
            dao.deleteByIds(ids);
        }

        return batch;
    }

    public boolean hasData() {
        return dao.count() > 0;
    }

    public int size() {
        return dao.count();
    }

    public void clear() {
        new Thread(() -> dao.deleteAll()).start();
    }
}