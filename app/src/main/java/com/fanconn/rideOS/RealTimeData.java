package com.fanconn.rideOS;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.Expose;
@Entity(tableName = "telemetry_queue")
public class RealTimeData {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String vinNumber;
    public double speed_kmph;
    public double tripkm;
    public double controllerMosTemp;
    public double motorTemp;
    public double bmsMosTemp;
    public String ntc;
    public double currentconsumption;
    public double batvoltage;
    public double remainingCapacity_AH;
    public double inAH;
    public double inAH_by_charger;
    public double inAH_by_regen;
    public double outAH;
    public String ev_power_state;
    public String currentRider;
    public String tirepressure;
    public double soc;
    public double apuSoc;
    public String lat_long;
    public String time;
    public String data;

    public String bmsmos;
    public String chargingstatus;
    public String singleminmaxvoltage;
    public Double apuvoltage;
    public String uptime;
    public String gear;

    public Double range;
    public Double bmssoc;
    public Double bmsremainingcap;
    public Double vcutemp;
    public String recoengine;
    public Double maxtotalvoltage;
    public Double allowablechargingcurrent;
    public String cell1to3;
    public String cell4to6;
    public String cell7to9;
    public String cell10to12;
    public String cell13to15;
    public String cell16to18;
    public String cell19to21;
    public String cell22to23;
}
