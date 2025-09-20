package com.fanconn.rideOS;

public class TelemetryDataManager {

    private static TelemetryDataManager instance;

    // ==== All telemetry fields ====
    private int teleChargingState;
    private int teleBmsLifeCycles;
    private String teleLatLong;
    private int teleHandleLockState;
    private int teleSeatLockState;
    private String teleBMSMosStates;
    private float teleTirePressure;
    private float teleTrip;
    private String teleRider;
    private String teleCurrentRider;
    private float teleTotalAH;
    private float teleChargerAH;
    private float teleRegenAH;
    private float teleDischargeAH;
    private int teleEvPowerState;
    private float teleCurrentConsumption;
    private float teleBatVoltage;
    private float teleRemainingCapacityAH;
    private float teleODO;
    private float teleSpeed;
    private float teletripkm;
    private float teleControllerMosTemp;
    private float teleMotorTemp;
    private float teleBMSMosTemp;
    private String teleNTC;
    private float teleSOC;
    private float teleAPUSOC;

    // ===== Singleton =====
    private TelemetryDataManager() {
        // Prevent direct instantiation
    }

    public static synchronized TelemetryDataManager getInstance() {
        if (instance == null) {
            instance = new TelemetryDataManager();
        }
        return instance;
    }

    // ====== Getters and Setters ======

    public int getTeleChargingState() {
        return teleChargingState;
    }

    public void setTeleChargingState(int teleChargingState) {
        this.teleChargingState = teleChargingState;
    }

    public int getTeleBmsLifeCycles() {
        return teleBmsLifeCycles;
    }

    public void setTeleBmsLifeCycles(int teleBmsLifeCycles) {
        this.teleBmsLifeCycles = teleBmsLifeCycles;
    }

    public String getTeleLatLong() {
        return teleLatLong;
    }

    public void setTeleLatLong(String teleLatLong) {
        this.teleLatLong = teleLatLong;
    }

    public int getTeleHandleLockState() {
        return teleHandleLockState;
    }

    public void setTeleHandleLockState(int teleHandleLockState) {
        this.teleHandleLockState = teleHandleLockState;
    }

    public int getTeleSeatLockState() {
        return teleSeatLockState;
    }

    public void setTeleSeatLockState(int teleSeatLockState) {
        this.teleSeatLockState = teleSeatLockState;
    }

    public String getTeleBMSMosStates() {
        return teleBMSMosStates;
    }

    public void setTeleBMSMosStates(String teleBMSMosStates) {
        this.teleBMSMosStates = teleBMSMosStates;
    }

    public float getTeleTirePressure() {
        return teleTirePressure;
    }

    public void setTeleTirePressure(float teleTirePressure) {
        this.teleTirePressure = teleTirePressure;
    }


    public String getTeleRider() {
        return teleRider;
    }

    public void setTeleRider(String teleRider) {
        this.teleRider = teleRider;
    }

    public String getTeleCurrentRider() {
        return teleCurrentRider;
    }

    public void setTeleCurrentRider(String teleCurrentRider) {
        this.teleCurrentRider = teleCurrentRider;
    }

    public float getTeleTotalAH() {
        return teleTotalAH;
    }

    public void setTeleTotalAH(float teleTotalAH) {
        this.teleTotalAH = teleTotalAH;
    }

    public float getTeleChargerAH() {
        return teleChargerAH;
    }

    public void setTeleChargerAH(float teleChargerAH) {
        this.teleChargerAH = teleChargerAH;
    }

    public float getTeleRegenAH() {
        return teleRegenAH;
    }

    public void setTeleRegenAH(float teleRegenAH) {
        this.teleRegenAH = teleRegenAH;
    }

    public float getTeleDischargeAH() {
        return teleDischargeAH;
    }

    public void setTeleDischargeAH(float teleDischargeAH) {
        this.teleDischargeAH = teleDischargeAH;
    }

    public int getTeleEvPowerState() {
        return teleEvPowerState;
    }

    public void setTeleEvPowerState(int teleEvPowerState) {
        this.teleEvPowerState = teleEvPowerState;
    }

    public float getTeleCurrentConsumption() {
        return teleCurrentConsumption;
    }

    public void setTeleCurrentConsumption(float teleCurrentConsumption) {
        this.teleCurrentConsumption = teleCurrentConsumption;
    }

    public float getTeleBatVoltage() {
        return teleBatVoltage;
    }

    public void setTeleBatVoltage(float teleBatVoltage) {
        this.teleBatVoltage = teleBatVoltage;
    }

    public float getTeleRemainingCapacityAH() {
        return teleRemainingCapacityAH;
    }

    public void setTeleRemainingCapacityAH(float teleRemainingCapacityAH) {
        this.teleRemainingCapacityAH = teleRemainingCapacityAH;
    }

    public float getTeleODO() {
        return teleODO;
    }

    public void setTeleODO(float teleODO) {
        this.teleODO = teleODO;
    }

    public float getTeleSpeed() {
        return teleSpeed;
    }

    public void setTeleSpeed(float teleSpeed) {
        this.teleSpeed = teleSpeed;
    }
    public float getTeleTrip() {
        return teleTrip;
    }

    public void setTeleTrip(float teleTrip) {
        this.teleTrip = teleTrip;
    }

    public float getTeleControllerMosTemp() {
        return teleControllerMosTemp;
    }

    public void setTeleControllerMosTemp(float teleControllerMosTemp) {
        this.teleControllerMosTemp = teleControllerMosTemp;
    }

    public float getTeleMotorTemp() {
        return teleMotorTemp;
    }

    public void setTeleMotorTemp(float teleMotorTemp) {
        this.teleMotorTemp = teleMotorTemp;
    }

    public float getTeleBMSMosTemp() {
        return teleBMSMosTemp;
    }

    public void setTeleBMSMosTemp(float teleBMSMosTemp) {
        this.teleBMSMosTemp = teleBMSMosTemp;
    }

    public String getTeleNTC() {
        return teleNTC;
    }

    public void setTeleNTC(String teleNTC) {
        this.teleNTC = teleNTC;
    }

    public float getTeleSOC() {
        return teleSOC;
    }

    public void setTeleSOC(float teleSOC) {
        this.teleSOC = teleSOC;
    }

    public float getTeleAPUSOC() {
        return teleAPUSOC;
    }

    public void setTeleAPUSOC(float teleAPUSOC) {
        this.teleAPUSOC = teleAPUSOC;
    }
}
