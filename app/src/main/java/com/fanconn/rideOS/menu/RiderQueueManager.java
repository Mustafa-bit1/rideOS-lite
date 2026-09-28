package com.fanconn.rideOS.menu;

import android.content.Context;

import java.util.List;

public class RiderQueueManager {

    private static RiderQueueManager instance;

    private final RiderQueueDao dao;

    private RiderQueueManager(Context context) {

        dao = RiderQueueDatabase
                .getInstance(context.getApplicationContext())
                .riderQueueDao();
    }

    public static synchronized RiderQueueManager getInstance(Context context) {

        if (instance == null) {
            instance = new RiderQueueManager(context);
        }

        return instance;
    }

    public void enqueue(
            String operation,
            String payload
    ) {

        new Thread(() -> {

            RiderQueueEntity entity = new RiderQueueEntity();

            entity.operation = operation;
            entity.payload = payload;
            entity.createdAt = System.currentTimeMillis();

            dao.insert(entity);

        }).start();
    }

    public List<RiderQueueEntity> getAll() {

        return dao.getAll();
    }

    public void delete(long id) {

        new Thread(() -> dao.delete(id)).start();
    }

    public int count() {

        return dao.count();
    }
}