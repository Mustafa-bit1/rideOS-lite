
package com.fanconn.rideOS;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public final class scooterPowerManager {

    private static final String TAG = "McuCommandManager";
    private static final long COMMAND_DELAY_MS = 600;

    private static final Handler handler =
            new Handler(Looper.getMainLooper());

    private static Runnable pendingSequence;

    public interface McuSender {
        void send(String command);
    }

    private scooterPowerManager() {
        // Utility class.
    }

    public static synchronized void unlock(McuSender sender) {
        sendSequence(
                sender,
                "FFAA550107000A01",
                "FFAA550101000A01"
        );
    }

    public static synchronized void lock(McuSender sender) {
        sendSequence(
                sender,
                "FFAA550101000A00",
                "FFAA550107000A00"
        );
    }

    private static synchronized void sendSequence(
            McuSender sender,
            String command1,
            String command2) {

        if (sender == null) {
            Log.e(TAG, "Cannot send commands: sender is null");
            return;
        }

        // Cancel a sequence that has not finished.
        if (pendingSequence != null) {
            handler.removeCallbacksAndMessages(null);
            pendingSequence = null;
        }

        pendingSequence = new Runnable() {
            @Override
            public void run() {
                try {
                    sender.send(command1);

                    handler.postDelayed(() -> {
                        try {
                            sender.send(command2);
                        } catch (Exception e) {
                            Log.e(TAG, "Second MCU command failed", e);
                        } finally {
                            synchronized (scooterPowerManager.class) {
                                pendingSequence = null;
                            }
                        }
                    }, COMMAND_DELAY_MS);

                } catch (Exception e) {
                    Log.e(TAG, "First MCU command failed", e);
                    pendingSequence = null;
                }
            }
        };

        handler.post(pendingSequence);
    }
}
