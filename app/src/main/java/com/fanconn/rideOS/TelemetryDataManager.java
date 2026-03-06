package com.fanconn.rideOS;

public class TelemetryDataManager {

    private static TelemetryDataManager instance;

    // ==== All telemetry fields ====
    private int teleChargingState;
    private String teleChargingStateError;
    private String telecell1to3,telecell4to6,telecell7to9,telecell10to12,telecell13to15,telecell16to18,telecell19to21,telecell22to23;

    private int teleBmsLifeCycles;
    private String teleLatLong;
    private int teleHandleLockState;
    private int teleSeatLockState;
    private String teleBMSMosStates;
    private String teleTirePressure;
    private String teleFTirePressureVoltage;
    private String teleRTirePressureVoltage;

    private float teleTrip;
    private String teleRider;
    private String teleCurrentRider;
    private float teleTotalAH;
    private float teleChargerAH;
    private float teleRegenAH;
    private float teleDischargeAH;
    private int teleEvPowerState;
    private float teleCurrentConsumption;
    private float teleChargingCurrentValue;

    private float teleBatVoltage;
    private float teleAPUBatVoltage;
    private String teleIndividualCellVoltages;
    private float teleRemainingCapacityAH;
    private float teleBMSRemainingCapacityAH;
    private float teleODO;
    private float teleSpeed;
    private int teleControllerOffabrupt;
    private float teletripkm;
    private float teleControllerMosTemp;
    private float teleMotorTemp;
    private float teleBMSMosTemp;
    private String teleNTC;
    private float teleSOC;
    private float teleBMSSOC;

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
    public String getTeleChargingStateError() {
        return teleChargingStateError;
    }

    public void setTeleChargingStateError(String teleChargingStateError) {
        this.teleChargingStateError = teleChargingStateError;
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


    public String getTeleTirePressure() {
        return teleTirePressure;
    }

    public void setTeleTirePressure(String teleTirePressure) {
        this.teleTirePressure = teleTirePressure;
    }
    public String getFTeleTirePressureVoltage() {
        return teleFTirePressureVoltage;
    }

    public void setTeleFTirePressureVoltage(String teleFTirePressureVoltage) {
        this.teleFTirePressureVoltage = teleFTirePressureVoltage;
    }
    public String getRTeleTirePressureVoltage() {
        return teleRTirePressureVoltage;
    }

    public void setTeleRTirePressureVoltage(String teleRTirePressureVoltage) {
        this.teleRTirePressureVoltage = teleRTirePressureVoltage;
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

    public float getTeleChargingCurrentValue() {
        return teleChargingCurrentValue;
    }

    public void setTeleChargingCurrentValue(float teleChargingCurrentValue) {
        this.teleChargingCurrentValue = teleChargingCurrentValue;
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

    public float getTeleAPUBatVoltage() {
        return teleAPUBatVoltage;
    }

    public void setTeleAPUBatVoltage(float teleAPUBatVoltage) {
        this.teleAPUBatVoltage = teleAPUBatVoltage;
    }
    public String getteleIndividualCellVoltages() {
        return teleIndividualCellVoltages;
    }

    public void setteleIndividualCellVoltages(String teleIndividualCellVoltages) {
        this.teleIndividualCellVoltages = teleIndividualCellVoltages;
    }

    public float getTeleRemainingCapacityAH() {
        return teleRemainingCapacityAH;
    }

    public void setTeleRemainingCapacityAH(float teleRemainingCapacityAH) {
        this.teleRemainingCapacityAH = teleRemainingCapacityAH;
    }
    public float getBMSTeleRemainingCapacityAH() {
        return teleBMSRemainingCapacityAH;
    }

    public void setBMSTeleRemainingCapacityAH(float teleBMSRemainingCapacityAH) {
        this.teleBMSRemainingCapacityAH = teleBMSRemainingCapacityAH;
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
    public int getTeleControllerAbruptOff() {
        return teleControllerOffabrupt;
    }

    public void setTeleControllerAbruptOff(int teleControllerOffabrupt) {
        this.teleControllerOffabrupt = teleControllerOffabrupt;
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
    public float getBMSTeleSOC() {
        return teleBMSSOC;
    }

    public void setBMSTeleSOC(float teleBMSSOC) {
        this.teleBMSSOC = teleBMSSOC;
    }

    public float getTeleAPUSOC() {
        return teleAPUSOC;
    }

    public void setTeleAPUSOC(float teleAPUSOC) {
        this.teleAPUSOC = teleAPUSOC;
    }



    public String getcell1TO3() {
        return telecell1to3;
    }

    public void setcell1TO3(String telecell1to3) {
        this.telecell1to3 = telecell1to3;
    }

    public String getcell4TO6() {
        return telecell4to6;
    }

    public void setcell4TO6(String telecell4to6) {
        this.telecell4to6 = telecell4to6;
    }

    public String getcell7TO9() {
        return telecell7to9;
    }

    public void setcell7TO9(String telecell7to9) {
        this.telecell7to9 = telecell7to9;
    }

    public String getcell10TO12() {
        return telecell10to12;
    }

    public void setcell10TO12(String telecell10to12) {
        this.telecell10to12 = telecell10to12;
    }

    public String getcell13TO15() {
        return telecell13to15;
    }

    public void setcell13TO15(String telecell13to15) {
        this.telecell13to15 = telecell13to15;
    }

    public String getcell16TO18() {
        return telecell16to18;
    }

    public void setcell16TO18(String telecell16to18) {
        this.telecell16to18 = telecell16to18;
    }

    public String getcell19TO21() {
        return telecell19to21;
    }

    public void setcell19TO21(String telecell19to21) {
        this.telecell19to21 = telecell19to21;
    }

    public String getcell22TO23() {
        return telecell22to23;
    }

    public void setcell22TO23(String telecell22to23) {
        this.telecell22to23 = telecell22to23;
    }
}
