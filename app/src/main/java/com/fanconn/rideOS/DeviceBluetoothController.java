package com.fanconn.rideOS;

import android.bluetooth.BluetoothAdapter;
import android.util.Log;

public class DeviceBluetoothController {

    private static final String TAG = "BLUETOOTH_CTRL";

    public static boolean setBluetooth(boolean enable) {
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

            if (adapter == null) {
                Log.e(TAG, "❌ Bluetooth not supported on this device");
                return false;
            }

            if (enable) {
                if (!adapter.isEnabled()) {
                    boolean result = adapter.enable();
                    Log.d(TAG, "🔵 Bluetooth enabling...");
                    return result;
                } else {
                    Log.d(TAG, "ℹ Bluetooth already ON");
                    return true;
                }
            } else {
                if (adapter.isEnabled()) {
                    boolean result = adapter.disable();
                    Log.d(TAG, "🔴 Bluetooth disabling...");
                    return result;
                } else {
                    Log.d(TAG, "ℹ Bluetooth already OFF");
                    return true;
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Bluetooth toggle failed", e);
            return false;
        }
    }
}
