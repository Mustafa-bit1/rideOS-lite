package com.fanconn.rideOS;
/**
 * 回调接口，用于监听解析的数据
 */
public interface FrameDataListener {
    void onHeartbeat1Parsed(Heartbeat1 hb1);
    void onHeartbeat2Parsed(Heartbeat2 hb2);
    void onHeartbeat3Parsed(Heartbeat3 hb3);
    void onMcuAnswerDataParsed(McuAnswerData mad);
}
