package com.fanconn.rideOS.telemetry;

import com.fanconn.rideOS.telemetry.VehicleTelemetryManager;

import org.json.JSONException;
import org.json.JSONObject;

public class TelemetryPayloadBuilder {

    /**
     * Builds a complete telemetry snapshot using the
     * compact MQTT keys expected by the Go backend.
     *
     * This class only converts Android field names to
     * compact MQTT field names.
     *
     * Change detection is handled by TelemetryChangeTracker.
     */
    public JSONObject buildFullPayload(VehicleTelemetryManager manager) {

        JSONObject payload = new JSONObject();

        try {

            // ========================================================
            // VIN
            // Go backend key: v
            // ========================================================

            put(payload, "v", manager.getVin());

            // ========================================================
            // TELEMETRY
            // ========================================================

            put(payload, "s", manager.getSpeedKmph());
            put(payload, "trip", manager.getTripKm());
            put(payload, "ct", manager.getControllerMosTemp());
            put(payload, "mt", manager.getMotorTemp());
            put(payload, "bt", manager.getBmsMosTemp());
            put(payload, "ntc", manager.getNtc());
            put(payload, "cc", manager.getCurrentConsumption());
            put(payload, "bv", manager.getBatVoltage());
            put(payload, "rc", manager.getRemainingCapAh());
            put(payload, "ia", manager.getInAh());
            put(payload, "iac", manager.getInAhCharger());
            put(payload, "iar", manager.getInAhRegen());
            put(payload, "oa", manager.getOutAh());
            put(payload, "eps", manager.getEvPowerState());
            put(payload, "r", manager.getCurrentRider());
            put(payload, "tp", manager.getTirePressure());
            put(payload, "soc", manager.getSoc());
            put(payload, "asoc", manager.getApuSoc());
            put(payload, "ll", manager.getLatLong());

            // Time is handled separately by TelemetryChangeTracker.
            put(payload, "time", manager.getTime());

            put(payload, "d", manager.getData());
            put(payload, "bm", manager.getBmsMos());
            put(payload, "cs", manager.getChargingStatus());
            put(payload, "mmv", manager.getSingleMinMaxVoltage());
            put(payload, "av", manager.getApuVoltage());
            put(payload, "up", manager.getUptime());
            put(payload, "g", manager.getGear());
            put(payload, "rng", manager.getRangeKm());
            put(payload, "bsoc", manager.getBmsSoc());
            put(payload, "brc", manager.getBmsRemainingCap());
            put(payload, "vt", manager.getVcuTemp());
            put(payload, "re", manager.getRecoEngine());
            put(payload, "mv", manager.getMaxTotalVoltage());
            put(payload, "acc", manager.getAllowableChargingCurrent());
            put(payload, "hl", manager.getHandleLock());
            put(payload, "odo", manager.getOdometer());
            put(payload, "sv", manager.getSoftwareVersions());
            put(payload, "sim", manager.getSimInfo());

            // ========================================================
            // CELL GROUPS
            // ========================================================

            put(payload, "c13", manager.getCell1To3());
            put(payload, "c46", manager.getCell4To6());
            put(payload, "c79", manager.getCell7To9());
            put(payload, "c1012", manager.getCell10To12());
            put(payload, "c1315", manager.getCell13To15());
            put(payload, "c1618", manager.getCell16To18());
            put(payload, "c1921", manager.getCell19To21());
            put(payload, "c2223", manager.getCell22To23());
            put(payload, "lt", manager.getLastTrip());
            put(payload, "dkm", manager.getDayKm());
            put(payload, "ce", manager.getControllerErrors());
            put(payload, "be", manager.getBmsErrors());

        } catch (JSONException e) {

            throw new RuntimeException(
                    "Failed to build telemetry payload",
                    e
            );
        }

        return payload;
    }

    /**
     * Adds only non-null values.
     */
    private void put(
            JSONObject payload,
            String key,
            Object value) throws JSONException {

        if (value != null) {
            payload.put(key, value);
        }
    }
}

