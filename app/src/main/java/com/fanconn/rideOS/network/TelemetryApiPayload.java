package com.fanconn.rideOS.network;

import com.google.gson.annotations.SerializedName;

public class TelemetryApiPayload {

    @SerializedName("vinNumber")
    public String vinNumber;

    @SerializedName("time")
    public String time;

    // ===== ADD THESE =====
    @SerializedName("rideos_version")
    public String rideOsVersion;

    @SerializedName("serial_number")
    public String serialNumber;


    @SerializedName("speed_kmph")
    public int speedKmph;

    @SerializedName("tripkm")
    public float tripKm;

    @SerializedName("odo")
    public float odo;


    @SerializedName("soc")
    public int soc;


    @SerializedName("apusoc")
    public int apuSoc;

    @SerializedName("batvoltage")
    public float batVoltage;

    @SerializedName("currentconsumption")
    public float currentConsumption;

    @SerializedName("remainingcapacity_ah")
    public float remainingCapacityAh;

    @SerializedName("inah")
    public float inAh;

    @SerializedName("inah_by_charger")
    public float inAhByCharger;

    @SerializedName("inah_by_regen")
    public float inAhByRegen;

    @SerializedName("outah")
    public float outAh;

    @SerializedName("motortemp")
    public float motorTemp;

    @SerializedName("controllermostemp")
    public float controllerMosTemp;

    @SerializedName("bmsmostemp")
    public float bmsMosTemp;

    @SerializedName("tirepressure")
    public String tirePressure;

    @SerializedName("ntc")
    public String ntc;

    //  RAW VALUES (0 / 1)
    @SerializedName("ev_power_state")
    public String evPowerState;

    @SerializedName("charging_state")
    public int chargingState;

    @SerializedName("currentrider")
    public String currentRider;

    @SerializedName("lat_long")
    public String latLong;
}