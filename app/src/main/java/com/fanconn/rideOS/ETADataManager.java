package com.fanconn.rideOS;

import com.fanconn.rideOS.util.NumberUtils;

public class ETADataManager {

    private static ETADataManager instance;
    private float etaTime = 0f;

    private ETADataManager() {
    }

    public static synchronized ETADataManager getInstance() {

        if (instance == null) {
            instance = new ETADataManager();
        }

        return instance;
    }

    public void setEtaTime(float etaTime) {
        this.etaTime = etaTime;
    }

    public float getEtaTime() {
        return (float) NumberUtils.round(etaTime,2);
    }
}