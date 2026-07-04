package com.fanconn.rideOS.network;

import com.google.gson.annotations.SerializedName;

public class VehicleData {

    @SerializedName("v")
    private String vinNumber;

    @SerializedName("m")
    private String model;

    @SerializedName("o")
    private String ownerName;

    @SerializedName("p")
    private String phoneNumber;

    @SerializedName("c")
    private String controllerID;

    @SerializedName("r")
    private String motorID;

    @SerializedName("b")
    private String bmsID;

    @SerializedName("q")
    private String chargerID;

    @SerializedName("s")
    private String rideOSversion;

    @SerializedName("e")
    private String smartKeyID;

    @SerializedName("z")
    private Double odo;
//    @SerializedName("tripkm")
//    private float tripkm;

    @SerializedName("x")
    private Integer chargingState;

    @SerializedName("h")
    private Integer handleLockState;

    @SerializedName("g")
    private Integer seatLockState;

    @SerializedName("n")
    private String bmsMosStates;

    @SerializedName("y")
    private String riders;

    @SerializedName("j")
    private Integer bmslifecycles;

    @SerializedName("l")
    private String lat_long;

    @SerializedName("t")
    private String time;


    public VehicleData(
            String vinNumber,
            String model,
            String ownerName,
            String phoneNumber,
            String controllerID,
            String motorID,
            String bmsID,
            String chargerID,
            String rideOSversion,
            String smartKeyID,
            Double odo,
            Integer chargingState,
            Integer handleLockState,
            Integer seatLockState,
            String bmsMosStates,
            String riders,
            Integer bmslifecycles,
            String lat_long,
            String time)
    {
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
