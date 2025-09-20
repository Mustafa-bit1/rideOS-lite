package com.fanconn.rideOS;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

public class MetadataManager {
    private static MetadataManager instance;
    private static SharedPreferences prefs;

    public static void init(Context context) {
        if (instance == null) {
            instance = new MetadataManager(context.getApplicationContext());
        }
    }

    private MetadataManager(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            prefs = EncryptedSharedPreferences.create(
                    context,
                    "vehicle_metadata",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            throw new RuntimeException("Error initializing MetadataManager", e);
        }
    }

    public static MetadataManager get() {
        if (instance == null) throw new IllegalStateException("Call init() in Application class first");
        return instance;
    }

    public void saveMetadata(String vin, String charger, String owner, String phone,
                             String controllerId, String motorId, String bmsId, String smartKeyId,
                             String model,String fronttpms,String reartpms) {
        prefs.edit()
                .putString("vin", vin)
                .putString("charger", charger)
                .putString("owner", owner)
                .putString("phone", phone)
                .putString("controller", controllerId)
                .putString("motor", motorId)
                .putString("bms", bmsId)
                .putString("smartKey", smartKeyId)
                .putString("model", model)
                .putString("fronttpms", fronttpms)
                .putString("reartpms", reartpms)
                .apply();
    }


    // Getters with safe default values
    public String getVin() { return prefs.getString("vin", "UNKNOWN_VIN"); }
    public String getCharger() { return prefs.getString("charger", "UNKNOWN_CHARGER"); }
    public String getOwner() { return prefs.getString("owner", "UNKNOWN_OWNER"); }
    public String getPhone() { return prefs.getString("phone", "UNKNOWN_PHONE"); }
    public String getControllerId() { return prefs.getString("controller", "UNKNOWN_CONTROLLER"); }
    public String getMotorId() { return prefs.getString("motor", "UNKNOWN_MOTOR"); }
    public String getBmsId() { return prefs.getString("bms", "UNKNOWN_BMS"); }
    public String getSmartKeyId() { return prefs.getString("smartKey", "UNKNOWN_KEY"); }
    public String getModel() { return prefs.getString("model", "UNKNOWN_MODEL"); }
    public String getFrontTPMS() { return prefs.getString("fronttpms", "UNKNOWN_FTPMS"); }
    public String getRearTPMS() { return prefs.getString("reartpms", "UNKNOWN_RTPMS"); }
}
