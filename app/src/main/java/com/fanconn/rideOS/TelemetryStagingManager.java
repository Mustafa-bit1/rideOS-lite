package com.fanconn.rideOS;

import java.util.LinkedList;

public class TelemetryStagingManager {

    private static final int MAX_ENTRIES = 60;
    private final LinkedList<RealTimeData> queue = new LinkedList<>();
    private static TelemetryStagingManager instance;

    private TelemetryStagingManager() {}

    public static synchronized TelemetryStagingManager getInstance() {
        if (instance == null) instance = new TelemetryStagingManager();
        return instance;
    }

    /** Add new data, drop oldest if full */
    public synchronized void addData(RealTimeData data) {
        if (queue.size() >= MAX_ENTRIES) {
            queue.removeFirst(); // remove oldest
        }
        queue.addLast(data);
    }

    /** Poll one element (retrieve + remove), null if empty */
    public synchronized RealTimeData poll() {
        return queue.isEmpty() ? null : queue.removeFirst();
    }

    /** Get a copy of all staged data (optional, still available) */
    public synchronized LinkedList<RealTimeData> getAllData() {
        return new LinkedList<>(queue);
    }

    /** Clear everything */
    public synchronized void clear() {
        queue.clear();
    }

    /** Check if any data is staged */
    public synchronized boolean hasData() {
        return !queue.isEmpty();
    }
}
