package com.fanconn.rideOS;

/**
 * 心跳包1数据模型
 */
public class Heartbeat1 {
    private int dragonLock;  // 龙头锁状态
    private int seatLock;   // 坐垫锁状态
    private int sideStand;   // 侧支架状态
    private int seatLockSensor;    // 座椅锁状态
    private int batteryVoltage;    // 小电池电压
    private int externalTemperature;  // 外部温度
    private int gestureSlide; // 手势滑动状态
    private int headlights;   // 大灯状态
    private int turnSignal;   // 转向灯状态
    private int LEDSignal;

    public int getDragonLock() {
        return dragonLock;
    }

    public void setDragonLock(int dragonLock) {
        this.dragonLock = dragonLock;
    }

    public int getSeatLock() {
        return seatLock;
    }

    public void setSeatLock(int seatLock) {
        this.seatLock = seatLock;
    }

    public int getSideStand() {
        return sideStand;
    }

    public void setSideStand(int sideStand) {
        this.sideStand = sideStand;
    }

    public int getSeatLockSensor() {
        return seatLockSensor;
    }

    public void setSeatLockSensor(int seatLockSensor) {
        this.seatLockSensor = seatLockSensor;
    }

    public int getBatteryVoltage() {
        return batteryVoltage;
    }

    public void setBatteryVoltage(int batteryVoltage) {
        this.batteryVoltage = batteryVoltage;
    }

    public int getExternalTemperature() {
        return externalTemperature;
    }

    public void setExternalTemperature(int externalTemperature) {
        this.externalTemperature = externalTemperature;
    }

    public int getGestureSlide() {
        return gestureSlide;
    }

    public void setGestureSlide(int gestureSlide) {
        this.gestureSlide = gestureSlide;
    }

    public int getHeadlights() {
        return headlights;
    }

    public void setHeadlights(int headlights) {
        this.headlights = headlights;
    }

    public int getTurnSignal() {
        return turnSignal;
    }

    public void setTurnSignal(int turnSignal) {
        this.turnSignal = turnSignal;
    }

    public int getLED() {
        return LEDSignal;
    }

    public void setLED(int LEDSignal) {
        this.LEDSignal = LEDSignal;
    }

    @Override
    public String toString() {
        return "Heartbeat1{" +
                "status='" + dragonLock + '\'' +
                ", seatLock='" + seatLock + '\'' +
                ", sideStand='" + sideStand + '\'' +
                ", seatLockSensor='" + seatLockSensor + '\'' +
                ", batteryVoltage=" + batteryVoltage +
                ", externalTemperature=" + externalTemperature +
                ", gestureSlide='" + gestureSlide + '\'' +
                ", headlights=" + headlights +
                ", turnSignal='" + turnSignal + '\'' +
                '}';
    }
}
