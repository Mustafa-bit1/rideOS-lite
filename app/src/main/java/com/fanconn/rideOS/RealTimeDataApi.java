package com.fanconn.rideOS;
import com.google.gson.annotations.SerializedName;
public class RealTimeDataApi {

    @SerializedName("v")
    public String vinNumber;

    @SerializedName("s")
    public Double speed_kmph;

    @SerializedName("k")
    public Double tripkm;

    @SerializedName("c")
    public Double controllerMosTemp;

    @SerializedName("m")
    public Double motorTemp;
    @SerializedName("b")
    public Double bmsMosTemp;

    @SerializedName("n")
    public String ntc;

    @SerializedName("y")
    public Double currentconsumption;
    @SerializedName("bv")
    public Double batvoltage;
    @SerializedName("r")
    public Double remainingCapacity_AH;

    @SerializedName("i")
    public Double inAH;

    @SerializedName("ic")
    public Double inAH_by_charger;

    @SerializedName("ir")
    public Double inAH_by_regen;

    @SerializedName("o")
    public Double outAH;

    @SerializedName("e")
    public String ev_power_state;

    @SerializedName("d")
    public String currentRider;

    @SerializedName("tp")
    public String tirepressure;

    @SerializedName("so")
    public Double soc;

    @SerializedName("ap")
    public Double apuSoc;
    @SerializedName("l")
    public String lat_long;

    @SerializedName("t")
    public String time;

    @SerializedName("q")
    public String data;

    @SerializedName("bm")
    public String bmsmos;

    @SerializedName("cc")
    public String chargingstatus;

    @SerializedName("g")
    public String singleminmaxvoltage;

    @SerializedName("w")
    public Double apuvoltage;

    @SerializedName("u")
    public String uptime;
    @SerializedName("f")
    public String gear;

    @SerializedName("j")
    public Double range;
    @SerializedName("x")
    public Double bmssoc;

    @SerializedName("xc")
    public Double bmsremainingcap;
    @SerializedName("z")
    public Double vcutemp;
    @SerializedName("re")
    public String recoengine;

    @SerializedName("mt")
    public Double maxtotalvoltage;

    @SerializedName("mc")
    public Double allowablechargingcurrent;

    @SerializedName("1t3")
    public String cell1to3;

    @SerializedName("4t6")
    public String cell4to6;
    @SerializedName("7t9")
    public String cell7to9;
    @SerializedName("10t12")
    public String cell10to12;
    @SerializedName("13t15")
    public String cell13to15;
    @SerializedName("16t18")
    public String cell16to18;
    @SerializedName("19t21")
    public String cell19to21;
    @SerializedName("22t23")
    public String cell22to23;
}