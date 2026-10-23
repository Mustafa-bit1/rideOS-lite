package com.fanconn.rideOS;

/**
 * CanDataParser 类用于解析不同 CAN ID 的 CAN 数据。
 * 每个解析方法将特定的 CAN 数据字符串解析为 Heartbeat3 对象中的相应字段。
 */
public class CanDataParser {

    /**
     * 解析 CAN ID 为 18904001 的数据
     *
     * @param hb3 Heartbeat3 对象，用于存储解析后的数据
     * @param canData CAN 数据字符串
     */
    public static  void parseCanData18904001(Heartbeat3 hb3, String canData) {
        // 实现 18904001 CAN 数据的解析逻辑
        // 提取心跳包 电池电量 值
        hb3.setBatteryLevel(FrameUtils.getByteSubstringAsInt(canData,6,7));
    }

    /**
     * 解析 CAN ID 为 10F807A8 的数据
     *
     * @param hb3 Heartbeat3 对象，用于存储解析后的数据
     * @param canData CAN 数据字符串
     */
    public static void parseCanData10F807A8(Heartbeat3 hb3, String canData) {
        // 实现 10F807A8 CAN 数据的解析逻辑
        hb3.setBSG1_BYTE1(FrameUtils.getByteValue(canData,0));  // 提取心跳包 档位相关 值
        hb3.setBSG1_BYTE2(FrameUtils.getByteValue(canData,1));  // 提取心跳包 档位相关 值
        hb3.setBSG1_BYTE3(FrameUtils.getByteValue(canData,2));  // 提取心跳包 档位相关 值
        hb3.setBSG1_BYTE4(FrameUtils.getByteValue(canData,3));  // 提取心跳包 档位相关 值
        hb3.setBSG1_BYTE5(FrameUtils.getByteValue(canData,4));  // 提取心跳包 转速 值
        hb3.setBSG1_BYTE6(FrameUtils.getByteValue(canData,5));  // 提取心跳包 转速 值
        hb3.setBSG1_BYTE7(FrameUtils.getByteValue(canData,6));  // 提取心跳包 电机温度 值
        hb3.setBSG1_BYTE8(FrameUtils.getByteValue(canData,7));  // 提取心跳包 控制器温度 值
    }

    /**
     * 解析 CAN ID 为 10F907A8 的数据
     *
     * @param hb3 Heartbeat3 对象，用于存储解析后的数据
     * @param canData CAN 数据字符串
     */
    public static void parseCanData10F907A8(Heartbeat3 hb3, String canData) {
        // 实现 10F907A8 CAN 数据的解析逻辑
        hb3.setBSG2_BYTE12(FrameUtils.getByteSubstringAsInt(canData,0,1));  // 提取心跳包 控制数据 值
        hb3.setBSG2_BYTE34(FrameUtils.getByteSubstringAsInt(canData,2,3));  // 提取心跳包 当前扭矩 值
        hb3.setBSG2_BYTE56(FrameUtils.getByteSubstringAsInt(canData,4,5));  // 提取心跳包 当前相电 值
        hb3.setBSG2_BYTE78(FrameUtils.getByteSubstringAsInt(canData,7,6));  // 提取心跳包 当前转速 值
    }

    /**
     * 解析 CAN ID 为 10FA07A8 的数据
     *
     * @param hb3 Heartbeat3 对象，用于存储解析后的数据
     * @param canData CAN 数据字符串
     */
    public static void parseCanData10FA07A8(Heartbeat3 hb3, String canData) {
        // 实现 10FA07A8 CAN 数据的解析逻辑
        hb3.setBSG3_BYTE12(FrameUtils.getByteSubstringAsInt(canData,0,1));    // 提取心跳包 当前母线电压 值
        hb3.setBSG3_BYTE34(FrameUtils.getByteSubstringAsInt(canData,2,3));    // 提取心跳包 当前母线电流 值
        hb3.setBSG3_BYTE5678(FrameUtils.getByteSubstringAsInt(canData,4,7));  // 提取心跳包 圈数 32 位 值
    }

    /**
     * 解析 CAN ID 为 10FB07A8 的数据
     *
     * @param hb3 Heartbeat3 对象，用于存储解析后的数据
     * @param canData CAN 数据字符串
     */
    public static void parseCanData10FB07A8(Heartbeat3 hb3, String canData) {
        // 实现 10FB07A8 CAN 数据的解析逻辑
        hb3.setBSG4_BYTE12(FrameUtils.getByteSubstringAsInt(canData,0,1));  // 提取心跳包 最大允许放电电流（最大母线单位 1/4A）（正数） 值
        hb3.setBSG4_BYTE34(FrameUtils.getByteSubstringAsInt(canData,2,3));  // 提取心跳包 最大允许充电电流（停止回流 1/4A）（负数和 7.1.2 相反） 值
        hb3.setBSG4_BYTE56(FrameUtils.getByteSubstringAsInt(canData,4,5));  // 提取心跳包 最大允许放电相电流（最大相线 1/4A）（正数） 值
        hb3.setBSG4_BYTE78(FrameUtils.getByteSubstringAsInt(canData,7,6));  // 提取心跳包 最大允许充电相电流（最大回流 1/4A）（负数和 7.1.2 相反） 值
    }

    /**
     * 解析 CAN ID 为 10FC07A8 的数据
     *
     * @param hb3 Heartbeat3 对象，用于存储解析后的数据
     * @param canData CAN 数据字符串
     */
    public static void parseCanData10FC07A8(Heartbeat3 hb3, String canData) {
        // 实现 10FC07A8 CAN 数据的解析逻辑
        hb3.setBSG5_BYTE12(FrameUtils.getByteSubstringAsInt(canData,0,1));  // 提取心跳包 最高转速（RPM 按 4 对极计算） 值
        hb3.setBSG5_BYTE34(FrameUtils.getByteSubstringAsInt(canData,2,3));  // 提取心跳包 扭矩系数（256~16384） 值
        hb3.setBSG5_BYTE56(FrameUtils.getByteSubstringAsInt(canData,4,5));  // 提取心跳包 欠压（0.1V） 值
        hb3.setBSG5_BYTE7(FrameUtils.getByteValue(canData,6));  // 提取心跳包 BOOST 时间（0~131s,131 秒表示无限制时间） 值
        hb3.setBSG5_BYTE8(FrameUtils.getByteValue(canData,7));  // 提取心跳包 BOOST 间隔时间（0~131s， 0 表示可以持续） 值
    }

    /**
     * 解析 CAN ID 为 10FD07A8 的数据
     *
     * @param hb3 Heartbeat3 对象，用于存储解析后的数据
     * @param canData CAN 数据字符串
     */
    public static void parseCanData10FD07A8(Heartbeat3 hb3, String canData) {
        // 实现 10FD07A8 CAN 数据的解析逻辑
        hb3.setBSG6_BYTE1(FrameUtils.getByteValue(canData,0));  // 提取心跳包 档位相关 值
        hb3.setBSG6_BYTE2(FrameUtils.getByteValue(canData,1));  //提取心跳包 保留 值
        hb3.setBSG6_BYTE34(FrameUtils.getByteSubstringAsInt(canData,2,3));  // 提取心跳包 最大后退转速（RPM 按 4 对极计算） 值
        hb3.setBSG6_BYTE56(FrameUtils.getByteSubstringAsInt(canData,4,5));  // 提取心跳包 最大允许放电相电流（最大相线 1/4A）（正数） 值
        hb3.setBSG6_BYTE7(FrameUtils.getByteValue(canData,6));  // 提取心跳包 保留 值
        hb3.setBSG6_BYTE8(FrameUtils.getByteValue(canData,7));  // 提取心跳包 保留 值
    }
}
