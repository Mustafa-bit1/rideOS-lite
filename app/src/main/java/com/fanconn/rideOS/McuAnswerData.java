package com.fanconn.rideOS;

public class McuAnswerData {
    private String taskId; // CAN ID
    private String BleMac;  // 蓝牙Mac地址
    private int BleMacChange;  // 蓝牙Mac地址修改状态
    private int MacVersions;  // MCU版本
    private byte McuOtaAnswer; // MCU OTA 应答数据
    private int frontLockState;  // 龙头锁修改状态
    private int seatCushionLockState;  // 坐垫锁修改状态
    private int Heartbeat3State;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getBleMac() {
        return BleMac;
    }

    public void setBleMac(String BleMac) {
        this.BleMac = BleMac;
    }

    public int getBleMacChange() {
        return BleMacChange;
    }

    public void setBleMacChange(int BleMacChange) {
        this.BleMacChange = BleMacChange;
    }

    public int getMcuVersions() {
        return MacVersions;
    }

    public void setMcuVersions(int MacVersions) {
        this.MacVersions = MacVersions;
    }

    public byte getMcuOtaAnswer() {
        return McuOtaAnswer;
    }

    public void setMcuOtaAnswer(byte McuOtaAnswer) {
        this.McuOtaAnswer = McuOtaAnswer;
    }

    public void setFrontLockState(int frontLockState) {
        this.frontLockState = frontLockState;
    }

    public int getFrontLockState() {
        return frontLockState;
    }

    public void setSeatCushionLockState(int seatCushionLockState) {
        this.seatCushionLockState = seatCushionLockState;
    }

    public int getSeatCushionLockState() {
        return seatCushionLockState;
    }

    public void setHeartbeat3State(int Heartbeat3State) {
        this.Heartbeat3State = Heartbeat3State;
    }

    public int getHeartbeat3State() {
        return Heartbeat3State;
    }

    @Override
    public String toString() {
        return "McuAnswerData{" +
                "taskId='" + taskId + '\'' +
                ", BleMac='" + BleMac + '\'' +
                ", BleMacChange=" + BleMacChange +
                ", MacVersions=" + MacVersions +
                ", McuOtaAnswer=" + McuOtaAnswer +
                '}';
    }
}
