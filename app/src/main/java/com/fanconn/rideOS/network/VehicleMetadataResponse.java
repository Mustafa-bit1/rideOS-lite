package com.fanconn.rideOS.network;

import com.google.gson.annotations.SerializedName;

public class VehicleMetadataResponse {

    @SerializedName("vinnumber")
    private String vinNumber;

    @SerializedName("model")
    private String model;

    @SerializedName("ownername")
    private String ownerName;

    @SerializedName("phonenumber")
    private String phoneNumber;

    @SerializedName("controllerid")
    private String controllerID;

    @SerializedName("motorid")
    private String motorID;

    @SerializedName("bmsid")
    private String bmsID;

    @SerializedName("chargerid")
    private String chargerID;

    @SerializedName("smartkeyid")
    private String smartKeyID;

    @SerializedName("fronttpmsid")
    private String fronttpmsid;

    @SerializedName("reartpmsid")
    private String reartpmsid;

    public String getVinNumber() {
        return vinNumber;
    }

    public String getModel() {
        return model;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getControllerID() {
        return controllerID;
    }

    public String getMotorID() {
        return motorID;
    }

    public String getBmsID() {
        return bmsID;
    }

    public String getChargerID() {
        return chargerID;
    }

    public String getSmartKeyID() {
        return smartKeyID;
    }

    public String getFronttpmsid() {
        return fronttpmsid;
    }

    public String getReartpmsid() {
        return reartpmsid;
    }
}


