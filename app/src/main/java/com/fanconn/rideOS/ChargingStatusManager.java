package com.fanconn.rideOS;

public class ChargingStatusManager {

    private static ChargingStatusManager instance;

    private boolean charging;

    private ChargingStatusManager() {
    }

    public static synchronized ChargingStatusManager getInstance() {
        if (instance == null) {
            instance = new ChargingStatusManager();
        }
        return instance;
    }

    public void setCharging(boolean charging) {
        this.charging = charging;
    }

    public boolean isCharging() {
        return charging;
    }
}