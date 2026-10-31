package com.fanconn.rideOS;

/**
 * 心跳包3数据模型
 */
public class Heartbeat3 {
    private String canId; // CAN ID
    private String canData; // CAN 数据
    private int ChargingState; // 单片机心跳包 电池电量
    private int batteryLevel; // 单片机心跳包 电池电量
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


    public int getChargingState() {
        return ChargingState;
    }

    public void setChargingState(int ChargingState) {
        this.ChargingState = ChargingState;
    }
    public int getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(int batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

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
