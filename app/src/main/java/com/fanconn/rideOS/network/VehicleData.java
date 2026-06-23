package com.fanconn.rideOS.network;

import com.google.gson.annotations.SerializedName;

public class VehicleData {

    @SerializedName("vinNumber")
    private String vinNumber;

    @SerializedName("model")
    private String model;

    @SerializedName("ownerName")
    private String ownerName;

    @SerializedName("phoneNumber")
    private String phoneNumber;

    @SerializedName("controllerID")
    private String controllerID;

    @SerializedName("motorID")
    private String motorID;

    @SerializedName("bmsID")
    private String bmsID;

    @SerializedName("chargerID")
    private String chargerID;

    @SerializedName("rideOSversion")
    private String rideOSversion;

    @SerializedName("smartKeyID")
    private String smartKeyID;

    @SerializedName("odo")
    private double odo;

//    @SerializedName("tripkm")
//    private float tripkm;

    @SerializedName("chargingState")
    private int chargingState;

    @SerializedName("handleLockState")
    private int handleLockState;

    @SerializedName("seatLockState")
    private int seatLockState;

    @SerializedName("bmsMosStates")
    private String bmsMosStates;

    @SerializedName("riders")
    private String riders;

    @SerializedName("bmslifecycles")
    private int bmslifecycles;

    @SerializedName("lat_long")
    private String lat_long;

    @SerializedName("time")
    private String time;


    public VehicleData(String vinNumber, String model, String ownerName, String phoneNumber,
                       String controllerID, String motorID, String bmsID, String chargerID,
                       String rideOSversion, String smartKeyID, double odo,
                       int chargingState, int handleLockState, int seatLockState,
                       String bmsMosStates, String riders, int bmslifecycles,
                       String lat_long, String time) {

        this.vinNumber = vinNumber;
        this.model = model;
        this.ownerName = ownerName;
        this.phoneNumber = phoneNumber;
        this.controllerID = controllerID;
        this.motorID = motorID;
        this.bmsID = bmsID;
        this.chargerID = chargerID;
        this.rideOSversion = rideOSversion;
        this.smartKeyID = smartKeyID;
        this.odo = odo;
        this.chargingState = chargingState;
        this.handleLockState = handleLockState;
        this.seatLockState = seatLockState;
        this.bmsMosStates = bmsMosStates;
        this.riders = riders;
        this.bmslifecycles = bmslifecycles;
        this.lat_long = lat_long;
        this.time = time;
    }

    // Getters (if needed)



    public String getLatLong() {
        return lat_long;
    }

    public int getBmslifecycles() {
        return bmslifecycles;
    }

    public String getVinNumber() {
        return vinNumber;
    }

    // You can generate more getters if required.
}
