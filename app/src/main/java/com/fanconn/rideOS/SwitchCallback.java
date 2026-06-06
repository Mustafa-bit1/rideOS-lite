package com.fanconn.rideOS;

public interface SwitchCallback {

    void onProgress(String message);

    void onSuccess(String message);

    void onFailure(String message);

}