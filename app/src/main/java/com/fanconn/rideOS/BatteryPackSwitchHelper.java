package com.fanconn.rideOS;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class BatteryPackSwitchHelper {

    private final SerialPortManager serialPortManager;
    private final Handler handler;

    private final SwitchCallback callback;

    private boolean isSequenceActive = false;

    private int retryCount = 0;

    private final int MAX_RETRIES = 15;

    public BatteryPackSwitchHelper(SwitchCallback callback) {

        this.callback = callback;

        serialPortManager = serialApplication.getSerialPortManager();

        handler = new Handler(Looper.getMainLooper());
    }

    private void turnMotorOFF() {

        if (serialPortManager != null) {
            serialPortManager.VCUSendToMCU(
                    "FFAA550107000A00"
            );
            handler.postDelayed(() -> {
                serialPortManager.VCUSendToMCU("FFAA550107000A00");
            }, 1000);
        }
    }

    private void turnMotorON() {

        if (serialPortManager != null) {

            serialPortManager.VCUSendToMCU(
                    "FFAA550107000A01"
            );
        }
    }
    public void startSwitchToAPU() {

        isSequenceActive = true;
        InactivityManager.getInstance().stopTimer();
        turnMotorOFF();
        callback.onProgress("Turning OFF Main Battery...");

        retryCount = 0;

        handler.postDelayed(() -> {
            mainChargeOFF();
        }, 2000);
    }
    private void mainChargeOFF() {

        if (!isSequenceActive) return;

        serialPortManager.VCUSendToMCU(
                "FFAA5501030015162A01800001020304050607"
        );

        handler.postDelayed(() -> {

            Heartbeat3 hb3 = serialPortManager.getHb3();

            boolean success =
                    hb3 != null &&
                            hb3.getMainChargeMosState() == 0;

            if (success) {

                callback.onProgress("Main Battery OFF confirmed");

                retryCount = 0;

                apuChargeON();

            } else {

                retryCount++;

                if (retryCount < MAX_RETRIES) {

                    mainChargeOFF();

                } else {

                    failSequence(
                            "Failed to turn OFF Main Battery"
                    );
                }
            }

        }, 1000);
    }
    private void apuChargeON() {

        callback.onProgress("Turning ON APU (1/2)...");

        serialPortManager.VCUSendToMCU(
                "FFAA5501030015162A02800101020304050607"
        );

        handler.postDelayed(() -> {

            Heartbeat3 hb3 = serialPortManager.getHb3();

            boolean success =
                    hb3 != null &&
                            hb3.getApuChargeMosState() == 1;

            if (success) {

                retryCount = 0;

                apuDischargeON();

            } else {

                retryCount++;

                if (retryCount < MAX_RETRIES) {

                    apuChargeON();

                } else {

                    failSequence(
                            "Failed to turn ON rescue pack charge switch"
                    );
                }
            }

        }, 1000);
    }
    private void apuDischargeON() {

        callback.onProgress("Turning ON rescue pack (2/2)...");

        serialPortManager.VCUSendToMCU(
                "FFAA5501030015162C02800101020304050607"
        );

        handler.postDelayed(() -> {

            Heartbeat3 hb3 = serialPortManager.getHb3();

            boolean success =
                    hb3 != null &&
                            hb3.getApuDisChargeMosState() == 1;

            if (success) {

                isSequenceActive = false;
                UIUpdater.apuflag = true;

                callback.onSuccess(
                        "Successfully switched to rescue pack"
                );

            } else {

                retryCount++;

                if (retryCount < MAX_RETRIES) {

                    apuDischargeON();

                } else {

                    failSequence(
                            "Failed to turn ON rescue pack discharge switch"
                    );
                }
            }

        }, 1000);
    }
    private void failSequence(String message) {

        isSequenceActive = false;
        callback.onFailure(message);
    }
    public void startRevertToMainBattery() {

        isSequenceActive = true;
        InactivityManager.getInstance().stopTimer();
        turnMotorOFF();
        retryCount = 0;

        callback.onProgress(
                "Turning OFF rescue pack discharge..."
        );
        handler.postDelayed(() -> {
            apuDischargeOFF();
        }, 2000);
    }
    private void apuDischargeOFF() {

        serialPortManager.VCUSendToMCU(
                "FFAA5501030015162C02800001020304050607"
        );

        handler.postDelayed(() -> {

            Heartbeat3 hb3 = serialPortManager.getHb3();

            boolean success =
                    hb3 != null &&
                            hb3.getApuDisChargeMosState() == 0;

            if (success) {

                retryCount = 0;

                mainChargeON();

            } else {

                retryCount++;

                if (retryCount < MAX_RETRIES) {

                    apuDischargeOFF();

                } else {

                    failSequence(
                            "Failed to turn OFF rescue pack discharge"
                    );
                }
            }

        }, 1000);
    }
    private void mainChargeON() {

        callback.onProgress(
                "Turning ON Main Charge..."
        );

        serialPortManager.VCUSendToMCU(
                "FFAA5501030015162A01800101020304050607"
        );

        handler.postDelayed(() -> {

            Heartbeat3 hb3 = serialPortManager.getHb3();

            boolean success =
                    hb3 != null &&
                            hb3.getMainChargeMosState() == 1;

            if (success) {

                isSequenceActive = false;
                UIUpdater.apuflag = false;

                callback.onSuccess(
                        "Successfully reverted to Main Battery"
                );

            } else {

                retryCount++;

                if (retryCount < MAX_RETRIES) {

                    mainChargeON();

                } else {

                    failSequence(
                            "Failed to turn ON Main Charge"
                    );
                }
            }

        }, 1000);
    }
    public void startEmergencyMainBatteryRestore() {

        isSequenceActive = true;
        InactivityManager.getInstance().stopTimer();
        turnMotorOFF();
        retryCount = 0;

        callback.onProgress(
                "Restoring Main Battery..."
        );
        handler.postDelayed(() -> {
            emergencyMainChargeON();
        }, 2000);
    }
    private void emergencyMainChargeON() {

        serialPortManager.VCUSendToMCU(
                "FFAA5501030015162A01800101020304050607"
        );

        handler.postDelayed(() -> {

            Heartbeat3 hb3 = serialPortManager.getHb3();

            boolean success =
                    hb3 != null &&
                            hb3.getMainChargeMosState() == 1;

            if (success) {

                isSequenceActive = false;
                UIUpdater.apuflag = false;

                callback.onSuccess(
                        "Successfully restored main battery"
                );

            } else {

                retryCount++;

                if (retryCount < MAX_RETRIES) {

                    emergencyMainChargeON();

                } else {

                    isSequenceActive = false;
                    callback.onFailure(
                            "Failed to restore main battery.\nContact RIVOT-care 8988984646."
                    );
                }
            }
        }, 1000);
    }
}