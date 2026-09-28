//package com.fanconn.rideOS;
//
//import android.content.Context;
//import android.os.Handler;
//import android.os.HandlerThread;
//
//public class StagedTelemetryController {
//
//    private static StagedTelemetryController instance;
//
//    private final Context context;
//    private final HandlerThread uploadThread;
//    private final Handler handler;
//
//    private volatile boolean sending = false;
//    private volatile boolean running = false;
//
//    private Runnable uploadRunnable;
//
//    private StagedTelemetryController(Context context) {
//        this.context = context.getApplicationContext();
//
//        uploadThread = new HandlerThread("StagedTelemetryUploader");
//        uploadThread.start();
//
//        handler = new Handler(uploadThread.getLooper());
//    }
//
//    public static synchronized StagedTelemetryController getInstance(Context context) {
//        if (instance == null) {
//            instance = new StagedTelemetryController(context);
//        }
//        return instance;
//    }
//
//    /**
//     * Called once from TelemetryService to provide the upload task.
//     */
//    public synchronized void initialize(Runnable uploadRunnable) {
//        this.uploadRunnable = uploadRunnable;
//    }
//
//    /**
//     * Starts staged telemetry upload.
//     * Calling this multiple times is safe.
//     */
//    public synchronized void startSending() {
//
//        if (running)
//            return;
//
//        if (uploadRunnable == null)
//            return;
//
//        sending = true;
//        running = true;
//
//        handler.post(uploadRunnable);
//    }
//
//    /**
//     * Stops upload.
//     * Queue remains intact.
//     */
//    public synchronized void stopSending() {
//        sending = false;
//    }
//
//    /**
//     * Called by upload runnable.
//     */
//    public boolean shouldContinue() {
//        return sending;
//    }
//
//    /**
//     * Called when queue becomes empty.
//     */
//    public synchronized void finished() {
//        sending = false;
//        running = false;
//    }
//
//    /**
//     * True if upload loop already running.
//     */
//    public boolean isRunning() {
//        return running;
//    }
//
//    /**
//     * Pending payload count.
//     */
//    public int getPendingPayloadCount() {
//        return TelemetryStagingManager
//                .getInstance(context)
//                .size();
//    }
//
//    /**
//     * Clears entire staged queue.
//     */
//    public void clearQueue() {
//        TelemetryStagingManager
//                .getInstance(context)
//                .clear();
//    }
//}