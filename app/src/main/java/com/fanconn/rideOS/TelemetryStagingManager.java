package com.fanconn.rideOS;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Crash-safe staging manager with:
 * 1. Max queue limit to avoid memory overflow
 * 2. Batch processing to prevent app freeze while flushing
 */
public class TelemetryStagingManager {

    private static final int MAX_ENTRIES = 600;   // Reduced to avoid heavy memory usage
    private static final int BATCH_SIZE = 20;     // Send 20 at a time to prevent blocking
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
            queue.removeFirst(); // Remove oldest to avoid crash
        }
        queue.addLast(data);
    }

    /** Poll ONE element (old individual usage) */
    public synchronized RealTimeData poll() {
        return queue.isEmpty() ? null : queue.removeFirst();
    }

    /** Fetch batch of staged data (safe for API flushing) */
    public synchronized List<RealTimeData> pollBatch() {
        List<RealTimeData> batch = new ArrayList<>();
        int count = Math.min(BATCH_SIZE, queue.size());
        for (int i = 0; i < count; i++) {
            batch.add(queue.removeFirst());
        }
        return batch;
    }

    /** Peek total staged count */
    public synchronized int size() {
        return queue.size();
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
