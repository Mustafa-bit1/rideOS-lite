package com.fanconn.rideOS.telemetry;

import com.fanconn.rideOS.MainActivity;
import com.fanconn.rideOS.SerialPortManager;
import com.fanconn.rideOS.UptimeManager;
import com.fanconn.rideOS.util.NumberUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.telephony.TelephonyManager;

public class VehicleTelemetryManager {

    // ============================================================
    // VEHICLE IDENTIFICATION
    // ============================================================

    private String vin;
    private static final String PREFS_OTA = "ota_prefs";
    private static final String KEY_LAST_MCU_VERSION = "last_mcu_version";


    private Context context;
    public synchronized void init(Context context) {
        this.context = context.getApplicationContext();
    }
    // ============================================================
    // TELEMETRY FIELDS
    // ============================================================

    private Double speedKmph;
    private Double tripKm;
    private Double controllerMosTemp;
    private Double motorTemp;
    private Double bmsMosTemp;
    private String ntc;
    private Double currentConsumption;
    private Double batVoltage;
    private Double remainingCapAh;
    private Double inAh;
    private Double inAhCharger;
    private Double inAhRegen;
    private Double outAh;
    private String evPowerState;
    private String currentRider;
    private String tirePressure;
    private Double soc;
    private Double apuSoc;
    private String latLong;
    private String time;
    private String data;
    private String bmsMos;
    private String chargingStatus;
    private String singleMinMaxVoltage;
    private Double apuVoltage;
    private String gear;
    private Double rangeKm;
    private Double bmsSoc;
    private Double bmsRemainingCap;
    private Double vcuTemp;
    private String recoEngine;
    private Double maxTotalVoltage;
    private Double allowableChargingCurrent;
    private Double handleLock;
    private String softwareVersions;
    private String simInfo;

    private Double odometer;
    private String cell1To3;
    private String cell4To6;
    private String cell7To9;
    private String cell10To12;
    private String cell13To15;
    private String cell16To18;
    private String cell19To21;
    private String cell22To23;
    private String lastTrip;
    private String dayKm;
    private String controllerErrors;
    private String bmsErrors;
    private volatile boolean gpsTelemetryEnabled = true;
    private static final String GPS_DISABLED_LOCATION = "0.000000, 0.000000";
    private static VehicleTelemetryManager instance;
    // ============================================================
    // VIN
    // ============================================================
    private VehicleTelemetryManager() {
    }

    public static synchronized VehicleTelemetryManager getInstance() {

        if (instance == null) {
            instance = new VehicleTelemetryManager();
        }

        return instance;
    }

    public synchronized String getVin() {
        return vin;
    }

    public synchronized void setVin(String vin) {
        this.vin = vin;
    }


    // ============================================================
    // SPEED
    // ============================================================

    public synchronized Double getSpeedKmph() {
        return speedKmph;
    }

    public synchronized void setSpeedKmph(Double speedKmph) {
        this.speedKmph = speedKmph;
    }


    // ============================================================
    // TRIP
    // ============================================================

    public synchronized Double getTripKm() {
        if (tripKm != null) {
            return NumberUtils.truncate1(tripKm);
        }
        return tripKm;
    }

    public synchronized void setTripKm(Double tripKm) {
        this.tripKm = tripKm;
    }


    // ============================================================
    // TEMPERATURES
    // ============================================================

    public synchronized Double getControllerMosTemp() {
        return controllerMosTemp;
    }

    public synchronized void setControllerMosTemp(Double controllerMosTemp) {
        this.controllerMosTemp = controllerMosTemp;
    }

    public synchronized Double getMotorTemp() {
        return motorTemp;
    }

    public synchronized void setMotorTemp(Double motorTemp) {
        this.motorTemp = motorTemp;
    }

    public synchronized Double getBmsMosTemp() {
        return bmsMosTemp;
    }

    public synchronized void setBmsMosTemp(Double bmsMosTemp) {
        this.bmsMosTemp = bmsMosTemp;
    }

    public synchronized Double getVcuTemp() {
        return (double) getCriticalTemps();
    }

    public synchronized void setVcuTemp(Double vcuTemp) {
        this.vcuTemp = vcuTemp;
    }


    // ============================================================
    // BMS / BATTERY
    // ============================================================

    public synchronized String getNtc() {
        return ntc;
    }

    public synchronized void setNtc(String ntc) {
        this.ntc = ntc;
    }

    public synchronized Double getCurrentConsumption() {
        return currentConsumption;
    }

    public synchronized void setCurrentConsumption(Double currentConsumption) {
        this.currentConsumption = currentConsumption;
    }

    public synchronized Double getBatVoltage() {
        if (batVoltage == null) {
            return 0.0;
        }
        return NumberUtils.truncate1(batVoltage);
    }

    public synchronized void setBatVoltage(Double batVoltage) {
        this.batVoltage = batVoltage;
    }

    public synchronized Double getRemainingCapAh() {
        return remainingCapAh;
    }

    public synchronized void setRemainingCapAh(Double remainingCapAh) {
        this.remainingCapAh = remainingCapAh;
    }

    public synchronized Double getInAh() {
        if (inAh != null) {
            return NumberUtils.round(inAh,3);
        }
        return inAh;
    }

    public synchronized void setInAh(Double inAh) {
        this.inAh = inAh;
    }

    public synchronized Double getInAhCharger() {
        if (inAhCharger != null) {
            return NumberUtils.round(inAhCharger,3);
        }
        return inAhCharger;
    }

    public synchronized void setInAhCharger(Double inAhCharger) {
        this.inAhCharger = inAhCharger;
    }

    public synchronized Double getInAhRegen() {
        if (inAhRegen != null) {
            return NumberUtils.round(inAhRegen,3);
        }
        return inAhRegen;
    }

    public synchronized void setInAhRegen(Double inAhRegen) {
        this.inAhRegen = inAhRegen;
    }

    public synchronized Double getOutAh() {
        if (outAh == null) {
            return 0.0;
        }
        return NumberUtils.round(outAh,3);
    }

    public synchronized void setOutAh(Double outAh) {
        this.outAh = outAh;
    }

    public synchronized Double getSoc() {
        if (soc != null) {
            return NumberUtils.truncate1(soc);
        }
        return soc;
    }

    public synchronized void setSoc(Double soc) {
        this.soc = soc;
    }

    public synchronized Double getApuSoc() {
        return apuSoc;
    }

    public synchronized void setApuSoc(Double apuSoc) {
        this.apuSoc = apuSoc;
    }

    public synchronized String getBmsMos() {
        return bmsMos;
    }

    public synchronized void setBmsMos(String bmsMos) {
        this.bmsMos = bmsMos;
    }

    public synchronized String getChargingStatus() {
        return chargingStatus;
    }

    public synchronized void setChargingStatus(String chargingStatus) {
        this.chargingStatus = chargingStatus;
    }

    public synchronized String getSingleMinMaxVoltage() {
        return singleMinMaxVoltage;
    }

    public synchronized void setSingleMinMaxVoltage(String singleMinMaxVoltage) {
        this.singleMinMaxVoltage = singleMinMaxVoltage;
    }

    public synchronized Double getApuVoltage() {
        return apuVoltage;
    }

    public synchronized void setApuVoltage(Double apuVoltage) {
        this.apuVoltage = apuVoltage;
    }

    public synchronized Double getMaxTotalVoltage() {
        return maxTotalVoltage;
    }

    public synchronized void setMaxTotalVoltage(Double maxTotalVoltage) {
        this.maxTotalVoltage = maxTotalVoltage;
    }

    public synchronized Double getAllowableChargingCurrent() {
        return allowableChargingCurrent;
    }

    public synchronized void setAllowableChargingCurrent(Double allowableChargingCurrent) {
        this.allowableChargingCurrent = allowableChargingCurrent;
    }

    public synchronized Double getBmsSoc() {
        return bmsSoc;
    }

    public synchronized void setBmsSoc(Double bmsSoc) {
        this.bmsSoc = bmsSoc;
    }

    public synchronized Double getBmsRemainingCap() {
        return bmsRemainingCap;
    }

    public synchronized void setBmsRemainingCap(Double bmsRemainingCap) {
        this.bmsRemainingCap = bmsRemainingCap;
    }


    // ============================================================
    // VEHICLE STATE
    // ============================================================

    public synchronized String getEvPowerState() {
        return evPowerState;
    }

    public synchronized void setEvPowerState(String evPowerState) {
        this.evPowerState = evPowerState;
    }

    public synchronized String getCurrentRider() {
        return currentRider;
    }

    public synchronized void setCurrentRider(String currentRider) {
        this.currentRider = currentRider;
    }

    public synchronized String getTirePressure() {
        return tirePressure;
    }

    public synchronized void setTirePressure(String tirePressure) {
        this.tirePressure = tirePressure;
    }

    public synchronized String getLatLong() {

        if (snapshot) {
            return latLong;
        }

        if (!gpsTelemetryEnabled) {
            return GPS_DISABLED_LOCATION;
        }

        if (latLong == null || latLong.isEmpty()) {
            return GPS_DISABLED_LOCATION;
        }

        return latLong;
    }

    public synchronized void setLatLong(String latLong) {
        this.latLong = latLong;
    }
    public synchronized String getTime() {
        return time;
    }

    public synchronized void setTime(String time) {
        this.time = time;
    }


    public synchronized String getData() {

        if (snapshot) {
            return data;
        }

        return gpsTelemetryEnabled ? "GPS_ON" : "GPS_OFF";
    }

    public synchronized void setData(String data) {
        this.data = data;
    }

    public synchronized String getUptime() {
        return UptimeManager.getUptime();
    }

    public synchronized String getGear() {
        return gear;
    }

    public synchronized void setGear(String gear) {
        this.gear = gear;
    }

    public synchronized Double getRangeKm() {
        if (rangeKm == null) {
            return 0.0;
        }
        return NumberUtils.truncate1(rangeKm);
    }

    public synchronized void setRangeKm(Double rangeKm) {
        this.rangeKm = rangeKm;
    }

    public synchronized String getRecoEngine() {
        return recoEngine;
    }

    public synchronized void setRecoEngine(String recoEngine) {
        this.recoEngine = recoEngine;
    }

    public synchronized Double getHandleLock() {
        return handleLock;
    }

    public synchronized void setHandleLock(Double handleLock) {
        this.handleLock = handleLock;
    }

    public synchronized Double getOdometer() {
        if (odometer != null) {
            return NumberUtils.truncate1(odometer);
        }
        return odometer;
    }

    public synchronized void setOdometer(Double odometer) {
        this.odometer = odometer;
    }


    // ============================================================
    // SOFTWARE / SIM
    // ============================================================
    public synchronized String getStoredSoftwareVersions() {
        return softwareVersions;
    }

    public synchronized String getStoredSimInfo() {
        return simInfo;
    }

    private boolean snapshot;
    public synchronized String getSoftwareVersions() {

        if (snapshot) {
            return softwareVersions;
        }

        String appVersion = "unknown";
        String aospVersion = Build.DISPLAY;
        int mcuVersion = getEffectiveMcuVersion();

        try {
            if (context != null) {

                PackageInfo packageInfo =
                        context.getPackageManager()
                                .getPackageInfo(
                                        context.getPackageName(),
                                        0
                                );

                if (packageInfo.versionName != null) {
                    appVersion = packageInfo.versionName;
                }
            }
        } catch (Exception e) {
            // Keep default
        }

        return appVersion + ";" + mcuVersion + ";" + aospVersion;
    }
    public String getSimInfo() {
        if (snapshot) {
            return simInfo;
        }
        if (context == null) {
            return null;
        }

        try {
            TelephonyManager telephonyManager =
                    (TelephonyManager) context.getSystemService(
                            Context.TELEPHONY_SERVICE
                    );

            if (telephonyManager == null) {
                return null;
            }

            String operatorName = telephonyManager.getSimOperatorName();
            String simSerial = null;

            try {
                simSerial = telephonyManager.getSimSerialNumber();
            } catch (SecurityException e) {
                // SIM serial is permission-restricted
            }

            if (operatorName == null) {
                operatorName = "";
            }

            if (simSerial == null) {
                simSerial = "";
            }

            if (operatorName.isEmpty() && simSerial.isEmpty()) {
                return null;
            }

            return operatorName + "-" + simSerial;

        } catch (Exception e) {
            return null;
        }
    }
    public synchronized void setSoftwareVersions(String softwareVersions) {
        this.softwareVersions = softwareVersions;
    }

    public synchronized void setSimInfo(String simInfo) {
        this.simInfo = simInfo;
    }
    // ============================================================
    // CELL VOLTAGES
    // ============================================================

    public synchronized String getCell1To3() {
        return cell1To3;
    }
    public synchronized void setCell1To3(String cell1To3) {
        this.cell1To3 = cell1To3;
    }


    public synchronized String getCell4To6() {
        return cell4To6;
    }


    public synchronized void setCell4To6(String cell4To6) {
        this.cell4To6 = cell4To6;
    }


    public synchronized String getCell7To9() {
        return cell7To9;
    }

    public synchronized void setCell7To9(String cell7To9) {
        this.cell7To9 = cell7To9;
    }


    public synchronized String getCell10To12() {
        return cell10To12;
    }


    public synchronized void setCell10To12(String cell10To12) {
        this.cell10To12 = cell10To12;
    }


    public synchronized String getCell13To15() {
        return cell13To15;
    }

    public synchronized void setCell13To15(String cell13To15) {
        this.cell13To15 = cell13To15;
    }


    public synchronized String getCell16To18() {
        return cell16To18;
    }

    public synchronized void setCell16To18(String cell16To18) {
        this.cell16To18 = cell16To18;
    }


    public synchronized String getCell19To21() {
        return cell19To21;
    }

    public synchronized void setCell19To21(String cell19To21) {
        this.cell19To21 = cell19To21;
    }

    public synchronized String getCell22To23() {
        return cell22To23;
    }
    public synchronized void setCell22To23(String cell22To23) {
        this.cell22To23 = cell22To23;
    }

    // ============================================================
// TRIP / ERROR INFORMATION
// ============================================================

    public synchronized String getLastTrip() {
        return lastTrip;
    }

    public synchronized void setLastTrip(String lastTrip) {
        this.lastTrip = lastTrip;
    }

    public synchronized String getDayKm() {
        return dayKm;
    }

    public synchronized void setDayKm(String dayKm) {
        this.dayKm = dayKm;
    }

    public synchronized String getControllerErrors() {
        return controllerErrors;
    }

    public synchronized void setControllerErrors(String controllerErrors) {
        this.controllerErrors = controllerErrors;
    }

    public synchronized String getBmsErrors() {
        return bmsErrors;
    }

    public synchronized void setBmsErrors(String bmsErrors) {
        this.bmsErrors = bmsErrors;
    }
    public synchronized VehicleTelemetryManager createSnapshot() {

        VehicleTelemetryManager.GpsTelemetryState gps =
                this.getGpsTelemetryState();

        VehicleTelemetryManager snapshot =
                new VehicleTelemetryManager();

        snapshot.snapshot = true;

        snapshot.setVin(this.vin);
        snapshot.setSpeedKmph(this.speedKmph);
        snapshot.setTripKm(this.tripKm);
        snapshot.setControllerMosTemp(this.controllerMosTemp);
        snapshot.setMotorTemp(this.motorTemp);
        snapshot.setBmsMosTemp(this.bmsMosTemp);
        snapshot.setNtc(this.ntc);
        snapshot.setCurrentConsumption(this.currentConsumption);
        snapshot.setBatVoltage(this.batVoltage);
        snapshot.setRemainingCapAh(this.remainingCapAh);
        snapshot.setInAh(this.inAh);
        snapshot.setInAhCharger(this.inAhCharger);
        snapshot.setInAhRegen(this.inAhRegen);
        snapshot.setOutAh(this.outAh);
        snapshot.setEvPowerState(this.evPowerState);
        snapshot.setCurrentRider(this.currentRider);
        snapshot.setTirePressure(this.tirePressure);
        snapshot.setSoc(this.soc);
        snapshot.setApuSoc(this.apuSoc);
        snapshot.setLatLong(gps.latLong);
        snapshot.setData(gps.data);
        snapshot.setTime(this.time);
        snapshot.setBmsMos(this.bmsMos);
        snapshot.setChargingStatus(this.chargingStatus);
        snapshot.setSingleMinMaxVoltage(this.singleMinMaxVoltage);
        snapshot.setApuVoltage(this.apuVoltage);
        snapshot.setGear(this.gear);
        snapshot.setRangeKm(this.rangeKm);
        snapshot.setBmsSoc(this.bmsSoc);
        snapshot.setBmsRemainingCap(this.bmsRemainingCap);
        snapshot.setVcuTemp(this.getVcuTemp());
        snapshot.setRecoEngine(this.recoEngine);
        snapshot.setMaxTotalVoltage(this.maxTotalVoltage);
        snapshot.setAllowableChargingCurrent(this.allowableChargingCurrent);
        snapshot.setHandleLock(this.handleLock);
        snapshot.setOdometer(this.odometer);
        snapshot.setSoftwareVersions(this.getSoftwareVersions());
        snapshot.setSimInfo(this.getSimInfo());
        snapshot.setCell1To3(this.cell1To3);
        snapshot.setCell4To6(this.cell4To6);
        snapshot.setCell7To9(this.cell7To9);
        snapshot.setCell10To12(this.cell10To12);
        snapshot.setCell13To15(this.cell13To15);
        snapshot.setCell16To18(this.cell16To18);
        snapshot.setCell19To21(this.cell19To21);
        snapshot.setCell22To23(this.cell22To23);
        snapshot.setLastTrip(this.lastTrip);
        snapshot.setDayKm(this.dayKm);
        snapshot.setControllerErrors(this.controllerErrors);
        snapshot.setBmsErrors(this.bmsErrors);
        return snapshot;
    }

    public int getCriticalTemps() {


        int cpu1=0;


        try {
            File thermalDir = new File("/sys/class/thermal/");
            File[] zones = thermalDir.listFiles();

            if (zones != null) {
                for (File zone : zones) {

                    if (zone.getName().startsWith("thermal_zone")) {

                        File typeFile = new File(zone, "type");
                        File tempFile = new File(zone, "temp");

                        if (typeFile.exists() && tempFile.exists()) {

                            String type = readLine(typeFile);
                            String tempRaw = readLine(tempFile);

                            if (type == null || tempRaw == null) continue;

                            float tempC = Float.parseFloat(tempRaw) / 1000f;
                            cpu1 = (int)tempC;
                        }
                    }
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }

        return
                cpu1;
    }

    private String readLine(File file) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            return br.readLine();
        } catch (Exception e) {
            return null;
        }
    }

    private int getLastSavedMcuVersion() {

        if (context == null) {
            return 0;
        }

        return context
                .getSharedPreferences(PREFS_OTA, Context.MODE_PRIVATE)
                .getInt(KEY_LAST_MCU_VERSION, 0);
    }
    private int getEffectiveMcuVersion() {

        int liveVersion = 0;

        try {

            SerialPortManager manager =
                    SerialPortManager.getInstance();

            if (manager != null && manager.mad != null) {
                liveVersion = manager.mad.getMcuVersions();
            }

        } catch (Exception e) {

            // Ignore and use saved version
        }

        if (liveVersion > 0) {

            if (context != null) {

                context
                        .getSharedPreferences(
                                PREFS_OTA,
                                Context.MODE_PRIVATE
                        )
                        .edit()
                        .putInt(KEY_LAST_MCU_VERSION, liveVersion)
                        .apply();
            }

            return liveVersion;
        }

        return getLastSavedMcuVersion();
    }

    public synchronized boolean isGpsTelemetryEnabled() {
        return gpsTelemetryEnabled;
    }
    public synchronized void setGpsTelemetryEnabled(boolean enabled) {
        gpsTelemetryEnabled = enabled;
    }

    public static class GpsTelemetryState {

        public final String latLong;
        public final String data;

        public GpsTelemetryState(String latLong, String data) {
            this.latLong = latLong;
            this.data = data;
        }
    }

    public synchronized GpsTelemetryState getGpsTelemetryState() {

        if (snapshot) {
            return new GpsTelemetryState(
                    latLong,
                    data
            );
        }

        if (!gpsTelemetryEnabled) {
            return new GpsTelemetryState(
                    GPS_DISABLED_LOCATION,
                    "GPS_OFF"
            );
        }

        String location =
                (latLong == null || latLong.isEmpty())
                        ? GPS_DISABLED_LOCATION
                        : latLong;

        return new GpsTelemetryState(
                location,
                "GPS_ON"
        );
    }
}