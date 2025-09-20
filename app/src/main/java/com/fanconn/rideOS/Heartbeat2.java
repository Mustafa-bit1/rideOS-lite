package com.fanconn.rideOS;

/**
 * 心跳包2数据模型
 */
public class Heartbeat2 {
    private int xAcc;  // x轴加速度
    private int yAcc;  // y轴加速度
    private int zAcc;  // z轴加速度
    private int xGyro;  // x轴陀螺仪
    private int yGyro;  // y轴陀螺仪
    private int zGyro;  // z轴陀螺仪

    public int getXuAcc() {
        return xAcc;
    }

    public void setXuAcc(int xAcc) {
        this.xAcc = xAcc;
    }

    public int getYuAcc() {
        return yAcc;
    }

    public void setYuAcc(int yAcc) {
        this.yAcc = yAcc;
    }

    public int getZuAcc() {
        return zAcc;
    }

    public void setZuAcc(int zAcc) {
        this.zAcc = zAcc;
    }

    public int getXGyro() {
        return xGyro;
    }

    public void setXGyro(int xGyro) {
        this.xGyro = xGyro;
    }

    public int getYGyro() {
        return yGyro;
    }

    public void setYGyro(int yGyro) {
        this.yGyro = yGyro;
    }

    public int getZGyro() {
        return zGyro;
    }

    public void setZGyro(int zGyro) {
        this.zGyro = zGyro;
    }

    @Override
    public String toString() {
        return "Heartbeat2{" +
                "xAcc=" + xAcc +
                ", yAcc=" + yAcc +
                ", zAcc=" + zAcc +
                ", xGyro=" + xGyro +
                ", yGyro=" + yGyro +
                ", zGyro=" + zGyro +
                '}';
    }
}
