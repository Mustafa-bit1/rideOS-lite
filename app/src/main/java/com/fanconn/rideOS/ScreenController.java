package com.fanconn.rideOS;

import android.util.Log;

public class ScreenController {
    private static SerialPortManager serialPortManager;

    public static void init(SerialPortManager manager) {
        serialPortManager = manager;
    }

    public static void ONScreen()
        {
            if (serialPortManager != null) {
        serialPortManager.VCUSendToMCU("FFAA550105000A01");
         }
    }
    public static void setNeutralGear()
    {    if (serialPortManager != null) {
        serialPortManager.VCUSendToMCU("FFAA550105000A00");
    }
    }
}
