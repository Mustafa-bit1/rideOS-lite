package com.fanconn.rideOS;

/**
 * 心跳包3数据模型
 */
public class Heartbeat3 {
    private String canId; // CAN ID
    private String canData; // CAN 数据
    private int batteryLevel,APUbatteryLevel;
    private int mainChargingMosState,mainDisMosState,apuChargingMosState,apuDisMosState;
    private float chargingbit;// 单片机心跳包 电池电量
    private String chargingErrorbitHex;// 单片机心跳包 电池电量
    private int mainChargeMosState,MainDisMosState,ApuChargeMosState,ApuDisMosState;
    private float realCurrent;
    private float batvoltage;
    private float remainingAH;
    private float APUrealCurrent;
    private float APUbatvoltage;
    private float APUremainingAH;
    private int BMSlifecycles;
    private int BMS_MOS_Temp;
    private int Battery_size;

    private int NTC1,NTC2,NTC3,NTC4,NTC5,NTC6,NTC7,NTC8;
    private String cell1TO3,cell4TO6,cell7TO9,cell10TO12,cell13TO15,cell16TO18,cell19TO21,cell22TO23;
    private int BSG1_BYTE1;
    private int BSG1_BYTE2;
    private int BSG1_BYTE3;
    private int BSG1_BYTE4;
    private int BSG1_BYTE5;
    private int BSG1_BYTE6;
    private int BSG1_BYTE7;
    private int BSG1_BYTE8;
    private int BSG2_BYTE12;
    private int BSG2_BYTE34;
    private int BSG2_BYTE56;
    private int BSG2_BYTE78;
    private int BSG3_BYTE12;
    private String BSG3_BYTE34;
    private int BSG3_BYTE5678;
    private int BSG4_BYTE12;
    private int BSG4_BYTE34;
    private int BSG4_BYTE56;
    private int BSG4_BYTE78;
    private int BSG5_BYTE12;
    private int BSG5_BYTE34;
    private int BSG5_BYTE56;
    private int BSG5_BYTE7;
    private int BSG5_BYTE8;
    private int BSG6_BYTE1;
    private int BSG6_BYTE2;
    private int BSG6_BYTE34;
    private int BSG6_BYTE56;
    private int BSG6_BYTE7;
    private int BSG6_BYTE8;
    private int motortemp;
    private String individualcellvoltages;

    private int chargeAh,dischargeAh;

    private int HanldeLockResponse,SeatLockResponse,LEDResponse;


    public String getCanId() {
        return canId;
    }

    public void setCanId(String canId) {
        this.canId = canId;
    }

    public String getCanData() {
        return canData;
    }

    public void setCanData(String canData) {
        this.canData = canData;
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(int batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public float getbatvoltage() {
        return batvoltage;
    }

    public void setbatvoltage(int batvoltage) {
        this.batvoltage = batvoltage;
    }

    public int getAPUBatteryLevel() {
        return APUbatteryLevel;
    }

    public void setAPUBatteryLevel(int apubatterylevel) {
        this.APUbatteryLevel = apubatterylevel;
    }

    public float getChargingbit() {
        return chargingbit;
    }

    public void setChargingbit(float chargingbit) {
        this.chargingbit = chargingbit;
    }

    public String getChargingErrorbit() {
        return chargingErrorbitHex;
    }

    public void setChargingErrorbit(String chargingErrorbitHex) {
        this.chargingErrorbitHex = chargingErrorbitHex;
    }
    public float getreamainingAH() {
        return remainingAH;
    }

    public void setremainingAH(int remainingAH) {
        this.remainingAH = remainingAH;
    }
    //APU ==============


    public int getBMSlifecycles() {
        return  BMSlifecycles;
    }

    public void setBMSlifecycles(int BMSlifecycles) {
        this.BMSlifecycles = BMSlifecycles;
    }

    public int getBMS_MOS_Temp() {
        return  BMS_MOS_Temp;
    }


    public void setBMS_MOS_Temp(int BMS_MOS_Temp) {
        this.BMS_MOS_Temp = BMS_MOS_Temp;
    }

    public int getBattery_size() {
        return  Battery_size;
    }


    public void setBattery_size(int Battery_size) {
        this.Battery_size = Battery_size;
    }

    //BMS MOSFET STATES
    public int getMainChargeMosState() {
        return mainChargeMosState;
    }

    public void setMainChargeMosState(int mainChargeMosState) {
        this.mainChargeMosState = mainChargeMosState;
    }
    public int getMainDisChargeMosState() {
        return mainDisMosState;
    }

    public void setMainDisChargeMosState(int mainDisMosState) {
        this.mainDisMosState = mainDisMosState;
    }
    public int getApuChargeMosState() {
        return apuChargingMosState;
    }

    public void setApuChargeMosState(int apuChargingMosState) {
        this.apuChargingMosState = apuChargingMosState;
    }
    public int getApuDisChargeMosState() {
        return apuDisMosState;
    }

    public void setApuDisChargeMosState(int apuDisMosState) {
        this.apuDisMosState = apuDisMosState;
    }
    //----------------------------------------MOSFET STATES
    public float getrealCurrent(){return realCurrent;}
    public void setRealCurrent(int realCurrent){
        this.realCurrent=realCurrent;
    }
    public int getNTC1(){return NTC1;}
    public void setNTC1(int ntc1){this.NTC1=ntc1;}
    public int getNTC2(){return NTC2;}
    public void setNTC2(int ntc2){this.NTC2=ntc2;}
    public int getNTC3(){return NTC3;}
    public void setNTC3(int ntc3){this.NTC3=ntc3;}
    public int getNTC4(){return NTC4;}
    public void setNTC4(int ntc4){this.NTC4=ntc4;}
    //---------------------------------------------------------------
    public int getNTC5(){return NTC5;}
    public void setNTC5(int ntc5){this.NTC5=ntc5;}
    public int getNTC6(){return NTC6;}
    public void setNTC6(int ntc6){this.NTC6=ntc6;}
    public int getNTC7(){return NTC7;}
    public void setNTC7(int ntc7){this.NTC7=ntc7;}
    public int getNTC8(){return NTC8;}
    public void setNTC8(int ntc8){this.NTC8=ntc8;}

    public float getAPUrealCurrent(){return APUrealCurrent;}
    public void setAPURealCurrent(int APUrealCurrent){
        this.APUrealCurrent=APUrealCurrent;
    }
    public float getAPUbatvoltage(){return APUbatvoltage;}
    public void setAPUbatvoltage(int APUbatvoltage){
        this.APUbatvoltage=APUbatvoltage;
    }
    public float getAPUremainingAH(){return APUremainingAH;}
    public void setAPUremainingAH(int APUremainingAH){
        this.APUremainingAH=APUremainingAH;
    }

    //============================================
    public int getChargeAH(){return chargeAh;}
    public void setChargeAH(int chargeAh){this.chargeAh=chargeAh;}

    public int getDisChargeAH(){return dischargeAh;}
    public void setDisChargeAH(int dischargeAh){this.dischargeAh=dischargeAh;}

    //===========================================

    public String getIndiCellVol(){return individualcellvoltages;}
    public void setIndiCellVol(String individualcellvoltages){this.individualcellvoltages=individualcellvoltages;}

    //====================================
    public int getBSG1_BYTE1() {
        return BSG1_BYTE1;
    }

    public void setBSG1_BYTE1(int BSG1_BYTE1) {
        this.BSG1_BYTE1 = BSG1_BYTE1;
    }

    public int getBSG1_BYTE2() {
        return BSG1_BYTE2;
    }

    public void setBSG1_BYTE2(int BSG1_BYTE2) {
        this.BSG1_BYTE2 = BSG1_BYTE2;
    }

    public int getBSG1_BYTE3() {
        return BSG1_BYTE3;
    }

    public void setBSG1_BYTE3(int BSG1_BYTE3) {
        this.BSG1_BYTE3 = BSG1_BYTE3;
    }

    public int getBSG1_BYTE4() {
        return BSG1_BYTE4;
    }

    public void setBSG1_BYTE4(int BSG1_BYTE4) {
        this.BSG1_BYTE4 = BSG1_BYTE4;
    }

    public int getBSG1_BYTE5() {
        return BSG1_BYTE5;
    }

    public void setBSG1_BYTE5(int BSG1_BYTE5) {
        this.BSG1_BYTE5 = BSG1_BYTE5;
    }

    public int getBSG1_BYTE6() {
        return BSG1_BYTE6;
    }

    public void setBSG1_BYTE6(int BSG1_BYTE6) {
        this.BSG1_BYTE6 = BSG1_BYTE6;
    }

    public int getBSG1_BYTE7() {
        return BSG1_BYTE7;
    }

    public void setBSG1_BYTE7(int BSG1_BYTE7) {
        this.BSG1_BYTE7 = BSG1_BYTE7;
    }

    public int getBSG1_BYTE8() {
        return BSG1_BYTE8;
    }

    public void setBSG1_BYTE8(int BSG1_BYTE8) {
        this.BSG1_BYTE8 = BSG1_BYTE8;
    }

    public int getBSG2_BYTE12() {
        return BSG2_BYTE12;
    }

    public void setBSG2_BYTE12(int BSG2_BYTE12) {
        this.BSG2_BYTE12 = BSG2_BYTE12;
    }

    public int getBSG2_BYTE34() {
        return BSG2_BYTE34;
    }

    public void setBSG2_BYTE34(int BSG2_BYTE34) {
        this.BSG2_BYTE34 = BSG2_BYTE34;
    }

    public int getBSG2_BYTE56() {
        return BSG2_BYTE56;
    }

    public void setBSG2_BYTE56(int BSG2_BYTE56) {
        this.BSG2_BYTE56 = BSG2_BYTE56;
    }

    public int getBSG2_BYTE78() {
        return BSG2_BYTE78;
    }

    public void setBSG2_BYTE78(int BSG2_BYTE78) {
        this.BSG2_BYTE78 = BSG2_BYTE78;
    }

    public int getBSG3_BYTE12() {
        return BSG3_BYTE12;
    }

    public void setBSG3_BYTE12(int BSG3_BYTE12) {
        this.BSG3_BYTE12 = BSG3_BYTE12;
    }

    public String getBSG3_BYTE34() {
        return BSG3_BYTE34;
    }

    public void setBSG3_BYTE34(String BSG3_BYTE34) {
        this.BSG3_BYTE34 = BSG3_BYTE34;
    }

    public int getBSG3_BYTE5678() {
        return BSG3_BYTE5678;
    }

    public void setBSG3_BYTE5678(int BSG3_BYTE5678) {
        this.BSG3_BYTE5678 = BSG3_BYTE5678;
    }

    public int getBSG4_BYTE12() {
        return BSG4_BYTE12;
    }

    public void setBSG4_BYTE12(int BSG4_BYTE12) {
        this.BSG4_BYTE12 = BSG4_BYTE12;
    }

    public int getBSG4_BYTE34() {
        return BSG4_BYTE34;
    }

    public void setBSG4_BYTE34(int BSG4_BYTE34) {
        this.BSG4_BYTE34 = BSG4_BYTE34;
    }

    public int getBSG4_BYTE56() {
        return BSG4_BYTE56;
    }

    public void setBSG4_BYTE56(int BSG4_BYTE56) {
        this.BSG4_BYTE56 = BSG4_BYTE56;
    }

    public int getBSG4_BYTE78() {
        return BSG4_BYTE78;
    }

    public void setBSG4_BYTE78(int BSG4_BYTE78) {
        this.BSG4_BYTE78 = BSG4_BYTE78;
    }

    public int getBSG5_BYTE12() {
        return BSG5_BYTE12;
    }

    public void setBSG5_BYTE12(int BSG5_BYTE12) {
        this.BSG5_BYTE12 = BSG5_BYTE12;
    }

    public int getBSG5_BYTE34() {
        return BSG5_BYTE34;
    }

    public void setBSG5_BYTE34(int BSG5_BYTE34) {
        this.BSG5_BYTE34 = BSG5_BYTE34;
    }

    public int getBSG5_BYTE56() {
        return BSG5_BYTE56;
    }

    public void setBSG5_BYTE56(int BSG5_BYTE56) {
        this.BSG5_BYTE56 = BSG5_BYTE56;
    }

    public int getBSG5_BYTE7() {
        return BSG5_BYTE7;
    }

    public void setBSG5_BYTE7(int BSG5_BYTE7) {
        this.BSG5_BYTE7 = BSG5_BYTE7;
    }

    public int getBSG5_BYTE8() {
        return BSG5_BYTE8;
    }

    public void setBSG5_BYTE8(int BSG5_BYTE8) {
        this.BSG5_BYTE8 = BSG5_BYTE8;
    }

    public int getBSG6_BYTE1() {
        return BSG6_BYTE1;
    }

    public void setBSG6_BYTE1(int BSG6_BYTE1) {
        this.BSG6_BYTE1 = BSG6_BYTE1;
    }

    public int getBSG6_BYTE2() {
        return BSG6_BYTE2;
    }

    public void setBSG6_BYTE2(int BSG6_BYTE2) {
        this.BSG6_BYTE2 = BSG6_BYTE2;
    }

    public int getBSG6_BYTE34() {
        return BSG6_BYTE34;
    }

    public void setBSG6_BYTE34(int BSG6_BYTE34) {
        this.BSG6_BYTE34 = BSG6_BYTE34;
    }

    public int getBSG6_BYTE56() {
        return BSG6_BYTE56;
    }

    public void setBSG6_BYTE56(int BSG6_BYTE56) {
        this.BSG6_BYTE56 = BSG6_BYTE56;
    }

    public int getBSG6_BYTE7() {
        return BSG6_BYTE7;
    }

    public void setBSG6_BYTE7(int BSG6_BYTE7) {
        this.BSG6_BYTE7 = BSG6_BYTE7;
    }

    public int getBSG6_BYTE8() {
        return BSG6_BYTE8;
    }

    public void setBSG6_BYTE8(int BSG6_BYTE8) {
        this.BSG6_BYTE8 = BSG6_BYTE8;
    }

    public int getMotortemp(){ return motortemp;}
    public void setMotortemp(int motortemp){this.motortemp=motortemp;}

    public void sethandleLockresponse(int HL)
    {
        this.HanldeLockResponse=HL;
    }

    public void setSeatLockresponse(int SL)
    {
        this.SeatLockResponse=SL;
    }

    public void setLEDresponse(int LED)
    {
        this.LEDResponse=LED;
    }

    public int gethandleLockresponse()
    {
        return HanldeLockResponse;
    }

    public int getSeatLockresponse()
    {
        return SeatLockResponse;
    }
    public int getLEDresponse()
    {
        return LEDResponse;
    }

    public String getcell1TO3() {
        return cell1TO3;
    }

    public void setcell1TO3(String cell1TO3) {
        this.cell1TO3 = cell1TO3;
    }

    public String getcell4TO6() {
        return cell4TO6;
    }

    public void setcell4TO6(String cell4TO6) {
        this.cell4TO6 = cell4TO6;
    }

    public String getcell7TO9() {
        return cell7TO9;
    }

    public void setcell7TO9(String cell7TO9) {
        this.cell7TO9 = cell7TO9;
    }

    public String getcell10TO12() {
        return cell10TO12;
    }

    public void setcell10TO12(String cell10TO12) {
        this.cell10TO12 = cell10TO12;
    }

    public String getcell13TO15() {
        return cell13TO15;
    }

    public void setcell13TO15(String cell13TO15) {
        this.cell13TO15 = cell13TO15;
    }

    public String getcell16TO18() {
        return cell16TO18;
    }

    public void setcell16TO18(String cell16TO18) {
        this.cell16TO18 = cell16TO18;
    }

    public String getcell19TO21() {
        return cell19TO21;
    }

    public void setcell19TO21(String cell19TO21) {
        this.cell19TO21 = cell19TO21;
    }

    public String getcell22TO23() {
        return cell22TO23;
    }

    public void setcell22TO23(String cell22TO23) {
        this.cell22TO23 = cell22TO23;
    }



    @Override
    public String toString() {
        return "Heartbeat3{" +
                "canId='" + canId + '\'' +
                ", canData='" + canData + '\'' +
                ",\n batteryLevel=" + batteryLevel +
                ",\n BSG1_BYTE1=" + BSG1_BYTE1 +
                ", BSG1_BYTE2=" + BSG1_BYTE2 +
                ", BSG1_BYTE3=" + BSG1_BYTE3 +
                ", BSG1_BYTE4=" + BSG1_BYTE4 +
                ", BSG1_BYTE5=" + BSG1_BYTE5 +
                ", BSG1_BYTE6=" + BSG1_BYTE6 +
                ", BSG1_BYTE7=" + BSG1_BYTE7 +
                ", BSG1_BYTE8=" + BSG1_BYTE8 +
                ",\n BSG2_BYTE12=" + BSG2_BYTE12 +
                ", BSG2_BYTE34=" + BSG2_BYTE34 +
                ", BSG2_BYTE56=" + BSG2_BYTE56 +
                ", BSG2_BYTE78=" + BSG2_BYTE78 +
                ", BSG3_BYTE12=" + BSG3_BYTE12 +
                ", BSG3_BYTE34=" + BSG3_BYTE34 +
                ", BSG3_BYTE5678=" + BSG3_BYTE5678 +
                ",\n BSG4_BYTE12=" + BSG4_BYTE12 +
                ", BSG4_BYTE34=" + BSG4_BYTE34 +
                ", BSG4_BYTE56=" + BSG4_BYTE56 +
                ", BSG4_BYTE78=" + BSG4_BYTE78 +
                ",\n BSG5_BYTE12=" + BSG5_BYTE12 +
                ", BSG5_BYTE34=" + BSG5_BYTE34 +
                ", BSG5_BYTE56=" + BSG5_BYTE56 +
                ", BSG5_BYTE7=" + BSG5_BYTE7 +
                ", BSG5_BYTE8=" + BSG5_BYTE8 +
                ",\n BSG6_BYTE1=" + BSG6_BYTE1 +
                ", BSG6_BYTE2=" + BSG6_BYTE2 +
                ", BSG6_BYTE34=" + BSG6_BYTE34 +
                ", BSG6_BYTE56=" + BSG6_BYTE56 +
                ", BSG6_BYTE7=" + BSG6_BYTE7 +
                ", BSG6_BYTE8=" + BSG6_BYTE8 +
                '}';
    }
}
