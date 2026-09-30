package com.fanconn.rideOS.telemetry;

import com.fanconn.rideOS.UptimeManager;
import com.fanconn.rideOS.telemetry.VehicleTelemetryManager;

import org.json.JSONException;
import org.json.JSONObject;

public class TelemetryChangeTracker {

    private boolean firstPayload = true;

    private VehicleTelemetryManager previousData;

    /**
     * Builds telemetry payload using compact MQTT keys.
     *
     * First payload:
     *     v + time + all available fields
     *
     * Subsequent payloads:
     *     v + time + only changed fields
     *
     * VIN and time are always included.
     * VIN and time are NOT part of change detection.
     */
    public synchronized JSONObject buildChangedPayload(
            VehicleTelemetryManager currentData) {

        JSONObject payload = new JSONObject();

        if (currentData == null) {
            return payload;
        }

        try {

            // ========================================================
            // VIN
            // ========================================================
            // Always included.
            // Not part of change detection.

            put(payload, "v", currentData.getVin());

            // ========================================================
            // TIME
            // ========================================================
            // Always included.
            // Not part of change detection.

            put(payload, "time", currentData.getTime());

            // ========================================================
            // FIRST PAYLOAD
            // ========================================================

            if (firstPayload) {

                addAllFields(payload, currentData);

                saveCurrentData(currentData);

                firstPayload = false;

                return payload;
            }

            // ========================================================
            // SUBSEQUENT PAYLOADS
            // ========================================================

            addChangedFields(payload, currentData);

            saveCurrentData(currentData);

        } catch (JSONException e) {

            throw new RuntimeException(
                    "Failed to build changed telemetry payload",
                    e
            );
        }

        return payload;
    }

    // ================================================================
    // FIRST PAYLOAD
    // ================================================================

    private void addAllFields(
            JSONObject payload,
            VehicleTelemetryManager data) throws JSONException {

        put(payload, "s", data.getSpeedKmph());
        put(payload, "trip", data.getTripKm());
        put(payload, "ct", data.getControllerMosTemp());
        put(payload, "mt", data.getMotorTemp());
        put(payload, "bt", data.getBmsMosTemp());
        put(payload, "ntc", data.getNtc());
        put(payload, "cc", data.getCurrentConsumption());
        put(payload, "bv", data.getBatVoltage());
        put(payload, "rc", data.getRemainingCapAh());
        put(payload, "ia", data.getInAh());
        put(payload, "iac", data.getInAhCharger());
        put(payload, "iar", data.getInAhRegen());
        put(payload, "oa", data.getOutAh());
        put(payload, "eps", data.getEvPowerState());
        put(payload, "r", data.getCurrentRider());
        put(payload, "tp", data.getTirePressure());
        put(payload, "soc", data.getSoc());
        put(payload, "asoc", data.getApuSoc());
        VehicleTelemetryManager.GpsTelemetryState gps =
                data.getGpsTelemetryState();

        put(payload, "ll", gps.latLong);
        put(payload, "d", gps.data);
        put(payload, "bm", data.getBmsMos());
        put(payload, "cs", data.getChargingStatus());
        put(payload, "mmv", data.getSingleMinMaxVoltage());
        put(payload, "av", data.getApuVoltage());
        payload.put("up", UptimeManager.getUptime());
        put(payload, "g", data.getGear());
        put(payload, "rng", data.getRangeKm());
        put(payload, "bsoc", data.getBmsSoc());
        put(payload, "brc", data.getBmsRemainingCap());
        put(payload, "vt", data.getVcuTemp());
        put(payload, "re", data.getRecoEngine());
        put(payload, "mv", data.getMaxTotalVoltage());
        put(payload, "acc", data.getAllowableChargingCurrent());
        put(payload, "hl", data.getHandleLock());
        put(payload, "odo", data.getOdometer());
        put(payload, "sv", data.getSoftwareVersions());
        put(payload, "sim", data.getSimInfo());

        put(payload, "c13", data.getCell1To3());
        put(payload, "c46", data.getCell4To6());
        put(payload, "c79", data.getCell7To9());
        put(payload, "c1012", data.getCell10To12());
        put(payload, "c1315", data.getCell13To15());
        put(payload, "c1618", data.getCell16To18());
        put(payload, "c1921", data.getCell19To21());
        put(payload, "c2223", data.getCell22To23());
        put(payload, "lt", data.getLastTrip());
        put(payload, "dkm", data.getDayKm());
        put(payload, "ce", data.getControllerErrors());
        put(payload, "be", data.getBmsErrors());
    }

    // ================================================================
    // CHANGED FIELDS
    // ================================================================

    private void addChangedFields(
            JSONObject payload,
            VehicleTelemetryManager current)
            throws JSONException {

        VehicleTelemetryManager.GpsTelemetryState currentGps =
                current.getGpsTelemetryState();

        VehicleTelemetryManager.GpsTelemetryState previousGps =
                previousData.getGpsTelemetryState();

        putIfChanged(
                payload,
                "s",
                current.getSpeedKmph(),
                previousData.getSpeedKmph()
        );

        putIfChanged(
                payload,
                "trip",
                current.getTripKm(),
                previousData.getTripKm()
        );

        putIfChanged(
                payload,
                "ct",
                current.getControllerMosTemp(),
                previousData.getControllerMosTemp()
        );

        putIfChanged(
                payload,
                "mt",
                current.getMotorTemp(),
                previousData.getMotorTemp()
        );

        putIfChanged(
                payload,
                "bt",
                current.getBmsMosTemp(),
                previousData.getBmsMosTemp()
        );

        putIfChanged(
                payload,
                "ntc",
                current.getNtc(),
                previousData.getNtc()
        );

        putIfChanged(
                payload,
                "cc",
                current.getCurrentConsumption(),
                previousData.getCurrentConsumption()
        );

        putIfChanged(
                payload,
                "bv",
                current.getBatVoltage(),
                previousData.getBatVoltage()
        );

        putIfChanged(
                payload,
                "rc",
                current.getRemainingCapAh(),
                previousData.getRemainingCapAh()
        );

        putIfChanged(
                payload,
                "ia",
                current.getInAh(),
                previousData.getInAh()
        );

        putIfChanged(
                payload,
                "iac",
                current.getInAhCharger(),
                previousData.getInAhCharger()
        );

        putIfChanged(
                payload,
                "iar",
                current.getInAhRegen(),
                previousData.getInAhRegen()
        );

        putIfChanged(
                payload,
                "oa",
                current.getOutAh(),
                previousData.getOutAh()
        );

        putIfChanged(
                payload,
                "eps",
                current.getEvPowerState(),
                previousData.getEvPowerState()
        );

        putIfChanged(
                payload,
                "r",
                current.getCurrentRider(),
                previousData.getCurrentRider()
        );

        putIfChanged(
                payload,
                "tp",
                current.getTirePressure(),
                previousData.getTirePressure()
        );

        putIfChanged(
                payload,
                "soc",
                current.getSoc(),
                previousData.getSoc()
        );

        putIfChanged(
                payload,
                "asoc",
                current.getApuSoc(),
                previousData.getApuSoc()
        );

        putIfChanged(
                payload,
                "ll",
                currentGps.latLong,
                previousGps.latLong
        );

        putIfChanged(
                payload,
                "d",
                currentGps.data,
                previousGps.data
        );

        putIfChanged(
                payload,
                "bm",
                current.getBmsMos(),
                previousData.getBmsMos()
        );

        putIfChanged(
                payload,
                "cs",
                current.getChargingStatus(),
                previousData.getChargingStatus()
        );

        putIfChanged(
                payload,
                "mmv",
                current.getSingleMinMaxVoltage(),
                previousData.getSingleMinMaxVoltage()
        );

        putIfChanged(
                payload,
                "av",
                current.getApuVoltage(),
                previousData.getApuVoltage()
        );

        payload.put("up", UptimeManager.getUptime());

        putIfChanged(
                payload,
                "g",
                current.getGear(),
                previousData.getGear()
        );

        putIfChanged(
                payload,
                "rng",
                current.getRangeKm(),
                previousData.getRangeKm()
        );

        putIfChanged(
                payload,
                "bsoc",
                current.getBmsSoc(),
                previousData.getBmsSoc()
        );

        putIfChanged(
                payload,
                "brc",
                current.getBmsRemainingCap(),
                previousData.getBmsRemainingCap()
        );

        putIfChanged(
                payload,
                "vt",
                current.getVcuTemp(),
                previousData.getVcuTemp()
        );

        putIfChanged(
                payload,
                "re",
                current.getRecoEngine(),
                previousData.getRecoEngine()
        );

        putIfChanged(
                payload,
                "mv",
                current.getMaxTotalVoltage(),
                previousData.getMaxTotalVoltage()
        );

        putIfChanged(
                payload,
                "acc",
                current.getAllowableChargingCurrent(),
                previousData.getAllowableChargingCurrent()
        );

        putIfChanged(
                payload,
                "hl",
                current.getHandleLock(),
                previousData.getHandleLock()
        );

        putIfChanged(
                payload,
                "odo",
                current.getOdometer(),
                previousData.getOdometer()
        );

        putIfChanged(
                payload,
                "sv",
                current.getSoftwareVersions(),
                previousData.getSoftwareVersions()
        );

        putIfChanged(
                payload,
                "sim",
                current.getSimInfo(),
                previousData.getSimInfo()
        );

        putIfChanged(
                payload,
                "c13",
                current.getCell1To3(),
                previousData.getCell1To3()
        );

        putIfChanged(
                payload,
                "c46",
                current.getCell4To6(),
                previousData.getCell4To6()
        );

        putIfChanged(
                payload,
                "c79",
                current.getCell7To9(),
                previousData.getCell7To9()
        );

        putIfChanged(
                payload,
                "c1012",
                current.getCell10To12(),
                previousData.getCell10To12()
        );

        putIfChanged(
                payload,
                "c1315",
                current.getCell13To15(),
                previousData.getCell13To15()
        );

        putIfChanged(
                payload,
                "c1618",
                current.getCell16To18(),
                previousData.getCell16To18()
        );

        putIfChanged(
                payload,
                "c1921",
                current.getCell19To21(),
                previousData.getCell19To21()
        );

        putIfChanged(
                payload,
                "c2223",
                current.getCell22To23(),
                previousData.getCell22To23()
        );

        putIfChanged(
                payload,
                "lt",
                current.getLastTrip(),
                previousData.getLastTrip()
        );

        putIfChanged(
                payload,
                "dkm",
                current.getDayKm(),
                previousData.getDayKm()
        );

        putIfChanged(
                payload,
                "ce",
                current.getControllerErrors(),
                previousData.getControllerErrors()
        );

        putIfChanged(
                payload,
                "be",
                current.getBmsErrors(),
                previousData.getBmsErrors()
        );
    }

    // ================================================================
    // VALUE COMPARISON
    // ================================================================

    private void putIfChanged(
            JSONObject payload,
            String key,
            Object currentValue,
            Object previousValue)
            throws JSONException {

        if (currentValue == null && previousValue == null) {
            return;
        }

        if (currentValue == null || previousValue == null) {
            payload.put(key, currentValue);
            return;
        }

        if (!currentValue.equals(previousValue)) {
            payload.put(key, currentValue);
        }
    }

    // ================================================================
    // NULL-SAFE JSON PUT
    // ================================================================

    private void put(
            JSONObject payload,
            String key,
            Object value)
            throws JSONException {

        if (value != null) {
            payload.put(key, value);
        }
    }

    // ================================================================
    // SAVE CURRENT SNAPSHOT
    // ================================================================

    private void saveCurrentData(
            VehicleTelemetryManager current) {

        previousData = current.createSnapshot();
    }
}

