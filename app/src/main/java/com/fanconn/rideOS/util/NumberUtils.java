package com.fanconn.rideOS.util;

public class NumberUtils {

    // 86.869 -> 86.8 (truncate)
    public static double truncate1(double value) {
        return Math.floor(value * 10) / 10.0;
    }

    // 86.869 -> 86.9 (normal rounding)
    public static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    // Generic truncate to N decimal places
    public static double truncate(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.floor(value * factor) / factor;
    }

    // Generic round to N decimal places
    public static double round(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }

    //Remove decimal and round off to nearest integer
    public static int roundToInt(double value) {
        return (int) Math.round(value);
    }
}