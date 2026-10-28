
package com.fanconn.rideOS;

import android.os.SystemClock;
import android.util.Log;

public final class ChargingStatusManager {

    private static final String TAG = "ChargingStatusManager";

    private static final ChargingStatusManager INSTANCE =
            new ChargingStatusManager();

    // Mark charger CAN as missing after 3 seconds without a valid message.
    private static final long CAN_TIMEOUT_MS = 3000L;

    public enum ChargingState {
        NOT_CHARGING,
        PLUGGED_IN,
        CHARGING
    }

    private long lastCanReceivedTime = 0L;
    private double chargingCurrent = 0.0;

    private ChargingStatusManager() {
    }

    public static ChargingStatusManager getInstance() {
        return INSTANCE;
    }

    /**
     * Call only when a valid 18FF50E5 charger CAN message is received.
     */
    public synchronized void onChargerCanReceived(String canData) {
        if (canData == null || canData.length() < 4) {
            return;
        }

        try {
            int rawCurrent =
                    FrameUtils.getByteSubstringAsInt(canData, 2, 3);

            chargingCurrent = rawCurrent / 10.0;
            lastCanReceivedTime = SystemClock.elapsedRealtime();

        } catch (Exception e) {
            Log.w(TAG, "Ignoring malformed charger CAN data", e);
        }
    }

    private boolean isTimedOut() {
        return lastCanReceivedTime == 0L
                || SystemClock.elapsedRealtime() - lastCanReceivedTime
                > CAN_TIMEOUT_MS;
    }

    public synchronized ChargingState getChargingState() {
        if (isTimedOut()) {
            return ChargingState.NOT_CHARGING;
        }

        return chargingCurrent > 0.0
                ? ChargingState.CHARGING
                : ChargingState.PLUGGED_IN;
    }

    public synchronized double getChargingCurrent() {
        return isTimedOut() ? 0.0 : chargingCurrent;
    }

    public synchronized boolean isChargerCanPresent() {
        return !isTimedOut();
    }
}
