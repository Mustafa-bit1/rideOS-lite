package com.fanconn.rideOS.menu;

import android.content.Context;

import com.fanconn.rideOS.TelemetryDatabase;

import java.util.List;

public class RiderSyncManager {

    private static RiderSyncManager instance;
    private final RiderSyncDao dao;

    private RiderSyncManager(Context ctx) {
        dao = TelemetryDatabase.getInstance(ctx).riderSyncDao();
    }

    public static synchronized RiderSyncManager getInstance(Context ctx) {
        if (instance == null) {
            instance = new RiderSyncManager(ctx);
        }
        return instance;
    }

    public void store(String operation, String json) {
        new Thread(() -> {

            RiderSync r = new RiderSync();
            r.operation = operation;
            r.payload = json;
            r.timestamp = System.currentTimeMillis();

            dao.insert(r);

            android.util.Log.d("SYNC_DB", "Stored → " + operation + " | " + json);

        }).start();
    }

    public List<RiderSync> getBatch() {
        return dao.getBatch(5);
    }

    public void delete(List<Long> ids) {
        dao.deleteByIds(ids);
    }

    public boolean hasData() {
        return dao.count() > 0;
    }
}