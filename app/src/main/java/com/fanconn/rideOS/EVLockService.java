package com.fanconn.rideOS;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class EVLockService {
    private static EVLockService instance;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SerialPortManager serialPortManager;
    private int retryCount1 = 0, retryCount2 = 0;
    private static final int MAX_RETRIES = 10;

    private enum SequenceType { LOCK, UNLOCK }
    private SequenceType currentSequenceType = null;

    private EVLockService() {
        this.serialPortManager = serialApplication.getSerialPortManager(); // ✅ Use global instance
    }

    public static synchronized EVLockService getInstance() {
        if (instance == null) {
            instance = new EVLockService();
        }
        return instance;
    }
    private Context appContext;

    public static synchronized EVLockService getInstance(Context context) {
        if (instance == null) {
            instance = new EVLockService();
            instance.appContext = context.getApplicationContext();  // ✅ Safe context
        }
        return instance;
    }

    public void startUnlockSequence() {
        stopSequence();  // Stop any existing sequence
        playLockSound();  // 🔊 Play the sound once
        currentSequenceType = SequenceType.UNLOCK;
        taskUnlock2();
        TelemetryDataManager.getInstance().setTeleHandleLockState(1);
    }
    private MediaPlayer lockPlayer;

    public void startLockSequence() {
        playLockSound();
        //Log.d("dasfsgd","STARTED LOCKING");
        stopSequence();  // Stop any existing sequence
        currentSequenceType = SequenceType.LOCK;
        taskLock1();
        TelemetryDataManager.getInstance().setTeleHandleLockState(0);
    }

    public void stopSequence() {
        handler.removeCallbacksAndMessages(null);
        retryCount1 = retryCount2 = 0;
        currentSequenceType = null;
        Log.d("EVLockService", "Sequence stopped");
    }

    // --- UNLOCK TASKS ---
    private void taskUnlock1() {
        if (currentSequenceType != SequenceType.UNLOCK) return;

        serialPortManager.VCUSendToMCU("FFAA550101000A01");
        new Handler(Looper.getMainLooper()).postDelayed(
                () -> serialPortManager.VCUSendToMCU("FFAA550101000A01"), 400);

        Heartbeat1 hb1 = serialPortManager.getHb1();
        boolean success = hb1.getDragonLock() == 1;

        if (!success && retryCount1 < MAX_RETRIES) {
            retryCount1++;
            handler.postDelayed(this::taskUnlock1, 500);
        } else if (success) {
            retryCount1 = 0;
            currentSequenceType = null;
            //Log.d("EVLockService", "Unlock Sequence Completed");
//            hb3.sethandleLockresponse(0);

        } else {
            Log.e("EVLockService", "Unlock Command 1 failed");
        }
    }

    private void taskUnlock2() {
        serialPortManager.VCUSendToMCU("FFAA550107000A01");
        new Handler(Looper.getMainLooper()).postDelayed(
                () -> serialPortManager.VCUSendToMCU("FFAA550107000A01"), 400);
        if (currentSequenceType != SequenceType.UNLOCK) return;

        serialPortManager.VCUSendToMCU("FFAA550107000A01");
        new Handler(Looper.getMainLooper()).postDelayed(
                () -> serialPortManager.VCUSendToMCU("FFAA550107000A01"), 400);

        Heartbeat1 hb1 = serialPortManager.getHb1();
        boolean success = hb1.getLED() == 1;

        if (!success && retryCount2 < MAX_RETRIES) {
            retryCount2++;
            handler.postDelayed(this::taskUnlock2, 500);
        } else if (success) {
            retryCount2 = 0;
//            hb3.setLEDresponse(0);
            taskUnlock1();
        } else {
            //Log.e("EVLockService", "Unlock Command 2 failed");
        }
    }

    // --- LOCK TASKS ---
    private void taskLock1() {
        if (currentSequenceType != SequenceType.LOCK) return;

        serialPortManager.VCUSendToMCU("FFAA550101000A00");
        new Handler(Looper.getMainLooper()).postDelayed(
                () -> serialPortManager.VCUSendToMCU("FFAA550101000A00"), 400);

        //Log.d("EVLockService", "Sent Lock Commandd 1");

        Heartbeat1 hb1 = serialPortManager.getHb1();
        boolean success = hb1.getDragonLock() == 0;
        //Log.d("EVLockService", "-------------------l---H "+hb1.getDragonLock());

        if (!success && retryCount1 < MAX_RETRIES) {
            retryCount1++;
            handler.postDelayed(this::taskLock1, 1000);
        } else if (success) {
            retryCount1 = 0;
//            hb3.sethandleLockresponse(0);
            taskLock2();
        } else {
            //Log.e("EVLockService", "Lock Command 1 failed");
            taskLock2();  // Continue anyway
        }
    }

    private void taskLock2() {
        if (currentSequenceType != SequenceType.LOCK) return;

        serialPortManager.VCUSendToMCU("FFAA550107000A00");
        //Log.d("EVLockService", "Sent Lock Commandd -------- 2");

        Heartbeat1 hb1 = serialPortManager.getHb1();
        boolean success = hb1.getLED() == 0;
        //Log.d("EVLockService", "-------------------l---L "+hb1.getLED());

        if (!success && retryCount2 < MAX_RETRIES) {
            retryCount2++;
            handler.postDelayed(this::taskLock2, 1000);
        } else {
            retryCount2 = 0;
//            hb3.setLEDresponse(0);
            currentSequenceType = null;
            //Log.d("EVLockService", "Lock Sequence Completed");
        }
    }

    private void playLockSound() {
        try {
            // Always stop + release before reuse
            if (lockPlayer != null) {
                if (lockPlayer.isPlaying()) {
                    lockPlayer.stop();
                }
                lockPlayer.release();
                lockPlayer = null;
            }

            if (appContext != null) {
                lockPlayer = MediaPlayer.create(appContext, R.raw.locksound);

                if (lockPlayer != null) {
                    lockPlayer.setOnCompletionListener(mp -> {
                        try {
                            mp.release();
                        } catch (Exception e) {
                            //Log.e("LockSound", "Error releasing MediaPlayer", e);
                        }
                        lockPlayer = null;
                        //Log.d("LockSound", "✅ Sound completed and released");
                    });
                    lockPlayer.start();
                } else {
                    //Log.e("LockSound", "❌ Failed to create MediaPlayer");
                }
            } else {
                //Log.e("LockSound", "⚠️ Context is null");
            }
        } catch (Exception e) {
            //Log.e("LockSound", "🎵 Exception in playLockSound", e);
        }
    }



}
