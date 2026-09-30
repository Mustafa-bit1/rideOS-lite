package com.fanconn.rideOS;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.Log;

import java.util.List;

/**
 * 用于处理十六进制数据帧操作的工具类。
 */
public class FrameUtils {

    /**
     * 验证数据帧的长度和 CRC 校验。
     *
     * @param frame 数据帧的十六进制字符串表示
     * @return 如果数据帧有效返回 true，否则返回 false
     */
    public static boolean verifyFrame(String frame) {
        int length = getByteSubstringAsInt(frame, 5, 6);
        int crc = getLastTwoBytesValue(frame);
        int calculatedCrc = calculateChecksum(frame);

        // 检查计算的字节长度是否与指定长度匹配
        if (calculateByteCount(frame) != length) {
            return false;
        }

        // 比较 CRC 值
        return crc == calculatedCrc;
    }

    /**
     * 获取数据帧的最后两个字节作为整数值（CRC）。
     * @param hexString 十六进制字符串
     * @return CRC 值作为整数
     */
    public static int getLastTwoBytesValue(String hexString) {
        // 检查是否是有效的十六进制字符串
        if (!isValidHexString(hexString)) {
            throw new IllegalArgumentException("Invalid hexadecimal string");
        }

        // 获取最后四个字符（两个字节）的子串
        String lastTwoBytesString = hexString.substring(hexString.length() - 4);

        // 将子串解析为整数
        int lastTwoBytesValue = Integer.parseInt(lastTwoBytesString, 16);

        return lastTwoBytesValue;
    }

    /**
     * 获取指定位置的单个字节值
     * @param hexString  十六进制字符串
     * @param index 截取字节(两个字符等于一个字节)的位置，从零开始
     * @return 返回被截取字节的整数值
     */
    public static int getByteValue(String hexString, int index) {
        // 检查是否是有效的十六进制字符串
        if (!isValidHexString(hexString)) {
            throw new IllegalArgumentException("Invalid hexadecimal string");
        }

        // 确保索引在有效范围内
        if (index < 0 || index >= hexString.length() / 2) {
            throw new IndexOutOfBoundsException("Index out of range");
        }

        // 获取指定位置的子串（两个字符）
        String byteString = hexString.substring(index * 2, index * 2 + 2);

        // 将子串解析为整数
        int byteValue = Integer.parseInt(byteString, 16);

        return byteValue;
    }

    /**
     * 获取指定位置的多个字节子串
     * @param hexString 十六进制字符串
     * @param startIndex 截取字节(两个字符等于一个字节)的开始位置，从零开始
     * @param endIndex 截取字节(两个字符等于一个字节)的结束位置，从零开始
     * @return  返回被截取字节的整数值
     */

    public static int getByteSubstringAsInt(String hexString, int startIndex, int endIndex) {
        // 获取字节子串
        String byteSubstring = getByteSubstring(hexString, startIndex, endIndex);

        // 将字节子串解析为整数
        //int intValue = Integer.parseInt(byteSubstring, 16);
        int intValue = (int) Long.parseLong(byteSubstring, 16);
        return intValue;
    }

    /**
     * 获取指定位置的多个字节子串 ,兼容不同高低位：
     * getByteSubstringAsInt(serialData, 16, 15);  // 高位在后
     * getByteSubstringAsInt(serialData, 15, 16);  // 高位在前
     * @param hexString 十六进制字符串
     * @param startIndex 截取字节(两个字符等于一个字节)的开始位置，从零开始
     * @param endIndex 截取字节(两个字符等于一个字节)的结束位置，从零开始
     * @return  返回被截取字节的字符串
     */
    public static String getByteSubstring(String hexString, int startIndex, int endIndex) {
        // 检查是否是有效的十六进制字符串
        if (!isValidHexString(hexString)) {
            throw new IllegalArgumentException("Invalid hexadecimal string");
        }

        // 确保起始索引和结束索引在有效范围内
        if (startIndex < 0 || startIndex >= hexString.length() / 2 ||
                endIndex < 0 || endIndex >= hexString.length() / 2) {
            return "00";
            //throw new IndexOutOfBoundsException("Index out of range");
        }

        String byteSubstring = "";

        // 判断高位和低位的顺序
        boolean isHighLowOrder = (startIndex > endIndex);
        if (isHighLowOrder) {
            // 反转字节子串
            byteSubstring = hexString.substring(endIndex * 2, (startIndex + 1) * 2);
            int length = byteSubstring.length();
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < length; i += 2) {
                String byteValue = byteSubstring.substring(i, i + 2);
                stringBuilder.insert(0, byteValue);
            }
            byteSubstring = stringBuilder.toString();
        } else {
            // 获取指定范围的子串（多个字节）
            byteSubstring = hexString.substring(startIndex * 2, (endIndex + 1) * 2);
        }

        return byteSubstring;
    }


    public static String getExactHexByteAString(String hexString, int byteIndex) {
        // Invalid/null data
        if (hexString == null || hexString.isEmpty()) {
            return "00";
        }

        // Invalid byte index
        if (byteIndex < 0) {
            return "00";
        }

        // Remove spaces if CAN data is provided as "88 00 00 02..."
        hexString = hexString.replaceAll("\\s+", "");

        // Invalid hex string
        if (!isValidHexString(hexString)) {
            return "00";
        }

        // Not enough characters for the requested byte
        int startIndex = byteIndex * 2;

        if (startIndex < 0 || startIndex + 2 > hexString.length()) {
            return "00";
        }

        return hexString.substring(startIndex, startIndex + 2);
    }
    /**
     * 计算十六进制字符串包含的字节数
     * @param hexString
     * @return
     */
    public static int calculateByteCount(String hexString) {
        // 检查是否是有效的十六进制字符串
        if (!isValidHexString(hexString)) {
            throw new IllegalArgumentException("Invalid hexadecimal string");
        }

        // 计算字节数
        int byteCount = hexString.length() / 2;

        return byteCount;
    }

    /**
     * 计算完整数据的心跳包校验码
     * @param hexString 十六进制字符串
     * @return 数值累加总和 （获取除了最后两个字节以外的子串进行求和）
     */
    public static int calculateChecksum(String hexString) {
        // 检查是否是有效的十六进制字符串
        if (!isValidHexString(hexString)) {
            throw new IllegalArgumentException("Invalid hexadecimal string");
        }

        int sum = 0;

        // 获取除了最后两个字节以外的子串进行求和
        String dataSubstring = hexString.substring(0, hexString.length() - 4);

        for (int i = 0; i < dataSubstring.length(); i += 2) {
            String byteString = dataSubstring.substring(i, i + 2);
            int byteValue = Integer.parseInt(byteString, 16);
            sum += byteValue;
        }

        return sum;
    }

    /**
     * 计算十六进制字符串的校验和
     * @param hexString 十六进制字符串
     * @return 数值累加总和(校验和值)
     */

//    public static int produceChecksum(String hexString) {
//
//        if (hexString == null) {
//            Log.e("FrameUtils", "Checksum failed: hexString is null");
//            return 0;
//        }
//
//        hexString = hexString.trim();
//
//        if (hexString.isEmpty()) {
//            Log.e("FrameUtils", "Checksum failed: hexString is empty");
//            return 0;
//        }
//
//        // Hex string MUST be even length
//        if ((hexString.length() % 2) != 0) {
//            Log.e("FrameUtils", "Checksum failed: odd length hex string = "
//                    + hexString.length() + " data=" + hexString);
//            return 0;
//        }
//
//        // Optional but good: validate hex chars
//        if (!hexString.matches("[0-9A-Fa-f]+")) {
//            Log.e("FrameUtils", "Checksum failed: invalid hex chars = " + hexString);
//            return 0;
//        }
//
//        int sum = 0;
//
//        try {
//            for (int i = 0; i < hexString.length(); i += 2) {
//
//                // Extra safety (never crashes)
//                if (i + 2 > hexString.length()) {
//                    Log.e("FrameUtils", "Checksum loop overflow at index " + i);
//                    break;
//                }
//
//                String byteString = hexString.substring(i, i + 2);
//                int byteValue = Integer.parseInt(byteString, 16);
//                sum += byteValue;
//            }
//        } catch (Exception e) {
//            Log.e("FrameUtils", "Checksum exception: " + hexString, e);
//            return 0;
//        }
//
//        return sum;
//    }

    public static int produceChecksum(String hexString) {
        // 检查是否是有效的十六进制字符串
        if (!isValidHexString(hexString)) {
            Log.e("FrameUtils", "Invalid hex string for checksum: " + hexString);
            return 0; // or throw only if in debug
        }

        int sum = 0;

        // 获取除了最后两个字节以外的子串进行求和
        //String dataSubstring = hexString.substring(0, hexString.length() - 4);

        for (int i = 0; i < hexString.length(); i += 2) {
            String byteString = hexString.substring(i, i + 2);
            int byteValue = Integer.parseInt(byteString, 16);
            sum += byteValue;
        }

        return sum;
    }

    /**
     * 将整数值转换为指定长度的十六进制字符串。
     * @param value 整数值
     * @param length 目标字符串长度
     * @return 指定长度的十六进制字符串
     */
    public static String toHexString(int value, int length) {
        String hexString = Integer.toHexString(value).toUpperCase();
        // 添加前导零以确保字符串长度符合目标长度
        while (hexString.length() < length) {
            hexString = "0" + hexString;
        }
        return hexString;
    }

    /**
     * 检查输入字符串是否为有效的十六进制字符串。
     * @param hexString  待验证的字符串
     * @return 如果字符串是有效的十六进制字符串返回 true，否则返回 false
     */
    public static boolean isValidHexString(String hexString) {
        // 使用正则表达式验证是否为有效的十六进制字符串
        String regex = "^[0-9A-Fa-f]+$";
        return hexString.matches(regex);
    }

    /**
     * 从整数值中提取指定位。
     *
     * @param value  整数值
     * @param startBit 起始位索引（从零开始）
     * @param endBit 结束位索引（从零开始）
     * @return 提取的位作为整数值
     *
     *  extractBits("89", 0, 1); // 提取 bit1-bit0
     * extractBits("89", 7, 7); // 提取 bit7
     */
    public static int extractBits(int value, int startBit, int endBit) {
        // int value = Integer.parseInt(hexString, 16); // 将十六进制字符串转换为整数

        int mask = ((1 << (endBit - startBit + 1)) - 1) << startBit; // 生成位掩码

        int extractedBits = (value & mask) >> startBit; // 提取指定位

        return extractedBits;
    }

    /**
     * 将蓝牙MAC地址字符串转换为标准的MAC地址格式。
     *
     * @param macStr 蓝牙MAC地址字符串，例如 "5100231101DE"
     * @return 标准的MAC地址格式字符串，例如 "51:00:23:11:01:DE"
     */
    public static String formatMacAddress(String macStr) {
        StringBuilder formattedMac = new StringBuilder();
        for (int i = 0; i < macStr.length(); i += 2) {
            formattedMac.append(macStr, i, i + 2);
            if (i < macStr.length() - 2) {
                formattedMac.append(":");
            }
        }
        return formattedMac.toString();
    }

    /**
     * 验证蓝牙Mac地址格式
     * @param mac 蓝牙Mac地址
     * @return 是否有效
     */
    public static  boolean isBluetoothMacValid(String mac) {
        return mac.length() == 12 && mac.matches("[0-9A-Fa-f]+");
    }

    /**
     * 创建蓝牙数据字符串
     * @param formattedInput 蓝牙Mac地址
     * @return 带校验码的蓝牙数据字符串
     */
    public static String createBluetoothData(String formattedInput) {
        String data = "FFAA550100000F" + formattedInput;
        int crc = FrameUtils.produceChecksum(data);
        String crcHexString = FrameUtils.toHexString(crc, 4);
        return data + crcHexString;
    }

    /**
     * 判断SD是否存在
     * 缺陷：插入U盘可能误判
     */
    public static boolean isSdCardAvailable(Context context) {
        StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);

        // 获取所有存储卷
        List<StorageVolume> storageVolumesList = storageManager.getStorageVolumes();

        // 检查每个存储卷，查找包含 SD 卡的存储卷
        for (StorageVolume storageVolume : storageVolumesList) {
            if (storageVolume.isRemovable()) {
                Log.d("isSdCardAvailable", "\nSD 卡已插入 ");
                return true; // 存在可移除的存储卷，即 SD 卡已插入
            }
        }

        Log.d("isSdCardAvailable", "\nSD 卡未插入 ");
        return false; // 未找到可移除的存储卷，即 SD 卡未插入
    }

    /**
     * 检测网络
     * @param activity
     * @return
     */
    public static boolean isNetworkAvailable(Activity activity) {
        ConnectivityManager connectivityManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) {
            return false;
        }

        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork != null) {
            NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
            if (networkCapabilities != null) {
                if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    Log.d("isNetworkAvailable", "Connected to WiFi");
                    return true;
                } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    Log.d("isNetworkAvailable", "Connected to Cellular Network");
                    return true;
                }
            }
        }

        Log.i("isNetworkAvailable", "No Internet");
        return false;
    }
    public static boolean isNetworkAvailable(Context context) {

        if (context instanceof Activity) {
            // Reuse EXACT same logic
            return isNetworkAvailable((Activity) context);
        }

        // Fallback for Application / Service / MQTT
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) {
            return false;
        }

        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork != null) {
            NetworkCapabilities networkCapabilities =
                    connectivityManager.getNetworkCapabilities(activeNetwork);

            if (networkCapabilities != null) {
                return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
            }
        }

        Log.i("isNetworkAvailable", "No Internet (context)");
        return false;
    }

    /**
     * 获取方向档位对应的文本
     * @param directionalGears 方向档位值
     * @return 对应的方向档位文本
     */

    public static String getDirection(int directionalGears) {
        switch (directionalGears) {
            case 0:
                return "N";
            case 1:
                return "D";
            case 2:
                return "R";
            default:
                return "D";
        }
    }

    /**
     * 获取速度档位对应的文本
     * @param speedGears 速度档位值
     * @return 对应的速度档位文本
     */
    public static String getSpeedGear(int speedGears) {
        switch (speedGears) {
            case 0:
                return "1";
            case 1:
                return "2";
            case 2:
                return "3";
            case 3:
                return "4";
            case 4:
                return "5";
            case 5:
                return "6";
            case 6:
                return "7";
            case 7:
                return "8";
            default:
                return "2";
        }
    }

}
