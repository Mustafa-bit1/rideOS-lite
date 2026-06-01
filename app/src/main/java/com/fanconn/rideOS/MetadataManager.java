package com.fanconn.rideOS;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.fanconn.rideOS.network.ApiService;
import com.fanconn.rideOS.network.RetrofitClient;
import com.fanconn.rideOS.network.VehicleMetadataResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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

    public void saveMetadata(String vin,
                             String charger,
                             String owner,
                             String phone,
                             String controllerId,
                             String motorId,
                             String bmsId,
                             String smartKeyId,
                             String model,
                             String fronttpms,
                             String reartpms) {

        SharedPreferences.Editor editor = prefs.edit();

        if (vin != null && !vin.trim().isEmpty()) {
            editor.putString("vin", vin);
        }

        editor.putString("charger", safe(charger));
        editor.putString("owner", safe(owner));
        editor.putString("phone", safe(phone));
        editor.putString("controller", safe(controllerId));
        editor.putString("motor", safe(motorId));
        editor.putString("bms", safe(bmsId));
        editor.putString("smartKey", safe(smartKeyId));
        editor.putString("model", safe(model));
        editor.putString("fronttpms", safe(fronttpms));
        editor.putString("reartpms", safe(reartpms));

        editor.putBoolean("metadata_synced", true);

        editor.apply();

//        Log.d("SAVE_METADATA",
//                "VIN SAVED = " + prefs.getString("vin", "NULL"));
    }
    private String safe(String value) {

        if (value == null || value.trim().isEmpty()) {
            return "UNKNOWN";
        }

        return value;
    }
    public boolean isMetadataSynced() {
        return prefs.getBoolean("metadata_synced", false);
    }


    // Getters with safe default values
    public String getVin() { return prefs.getString("vin", "UNKNOWN_VIN"); }

    public String getCharger() { return prefs.getString("charger", "UNKNOWN_CHARGER"); }
    public String getOwner() { return prefs.getString("owner", "UNKNOWN_OWNER"); }
    public String getPhone() { return prefs.getString("phone", "UNKNOWN_PHONE"); }
    public String getControllerId() { return prefs.getString("controller", "UNKNOWN_CONTROLLER");
    }
    public String getMotorId() { return prefs.getString("motor", "UNKNOWN_MOTOR"); }
    public String getBmsId() { return prefs.getString("bms", "UNKNOWN_BMS"); }
    public String getSmartKeyId() {
        String key = prefs.getString("smartKey", "");
        if (key == null || key.trim().isEmpty()) {
            return "UNKNOWN_KEY";
        }
        return key;
    }
    public void syncMetadataFromServer(String vin, Runnable onSuccess) {
//        Log.d("VIN_DEBUG", "VIN SENT = [" + vin + "]");
        ApiService apiService =
                RetrofitClient.getClient().create(ApiService.class);

        apiService.getVehicleMetadata(vin)
                .enqueue(new Callback<VehicleMetadataResponse>() {

                    @Override
                    public void onResponse(
                            Call<VehicleMetadataResponse> call,
                            Response<VehicleMetadataResponse> response) {

                        if (!response.isSuccessful()
                                || response.body() == null) {

//                            Log.e("METADATA_SYNC",
//                                    "Metadata not found. Code="
//                                            + response.code());
                            return;
                        }

                        VehicleMetadataResponse data = response.body();
//                        Log.d("METADATA_RAW",
//                                "VIN=" + data.getVinNumber()
//                                        + " controller=" + data.getControllerID()
//                                        + " motor=" + data.getMotorID()
//                                        + " charger=" + data.getChargerID());
                        MetadataManager.get().saveMetadata(
                                data.getVinNumber(),
                                data.getChargerID(),
                                data.getOwnerName(),
                                data.getPhoneNumber(),
                                data.getControllerID(),
                                data.getMotorID(),
                                data.getBmsID(),
                                data.getSmartKeyID(),
                                data.getModel(),
                                data.getFronttpmsid(),
                                data.getReartpmsid()
                        );

//                        Log.d("METADATA_SYNC",
//                                "Metadata synced successfully");

                        if (onSuccess != null) {
                            onSuccess.run();
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<VehicleMetadataResponse> call,
                            Throwable t) {

//                        Log.e("METADATA_SYNC",
//                                "Sync failed", t);
                    }
                });
    }
    public String getModel() { return prefs.getString("model", "UNKNOWN_MODEL"); }
    public String getFrontTPMS() { return prefs.getString("fronttpms", "UNKNOWN_FTPMS"); }
    public String getRearTPMS() { return prefs.getString("reartpms", "UNKNOWN_RTPMS"); }
}
