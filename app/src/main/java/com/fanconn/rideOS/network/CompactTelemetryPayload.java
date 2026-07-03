package com.fanconn.rideOS.network;

import com.google.gson.annotations.SerializedName;

public class CompactTelemetryPayload {

    @SerializedName("vn")
    public String vinNumber;

    @SerializedName("tm")
    public String time;

    @SerializedName("soc")
    public Integer soc;

    @SerializedName("odo")
    public Float odo;

    @SerializedName("pwr")
    public String scooterState;

    @SerializedName("tkm")
    public Float tripKm;

    @SerializedName("rdr")
    public String rider;

    @SerializedName("ftms")
    public String frontTpms;

    @SerializedName("rtms")
    public String rearTpms;

    @SerializedName("ntc")
    public String ntc;

    @SerializedName("ct")
    public Float controllerTemp;

    @SerializedName("mt")
    public Float motorTemp;

    @SerializedName("vt")
    public Float vcuTemp;

    @SerializedName("bt")
    public Float bmsTemp;

    @SerializedName("ll")
    public String location;

    @SerializedName("sp")
    public Integer speedKmph;

    @SerializedName("cs")
    public Integer chargingState;

    @SerializedName("cc")
    public Float chargingCurrent;

    @SerializedName("cht")
    public Float estChargingTime;

    @SerializedName("re")
    public String recoEngine;

    @SerializedName("rng")
    public Float range;

    @SerializedName("rosv")
    public String rideOsVersion;
}