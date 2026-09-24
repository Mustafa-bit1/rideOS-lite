package com.fanconn.rideOS;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.fanconn.rideOS.telemetry.MqttConnectionManager;

import org.json.JSONObject;

public class MetadataManager {

    private static final String TAG = "MetadataManager";

    private static MetadataManager instance;
    private static SharedPreferences prefs;

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    public static void init(Context context) {

        if (instance == null) {

            instance =
                    new MetadataManager(
                            context.getApplicationContext()
                    );
        }
    }

    private final Context context;

    private MetadataManager(Context context) {

        this.context = context.getApplicationContext();

        try {
            MasterKey masterKey =
                    new MasterKey.Builder(context)
                            .setKeyScheme(
                                    MasterKey.KeyScheme.AES256_GCM
                            )
                            .build();

            prefs =
                    EncryptedSharedPreferences.create(
                            context,
                            "vehicle_metadata",
                            masterKey,
                            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    );

        } catch (Exception e) {
            throw new RuntimeException(
                    "Error initializing MetadataManager",
                    e
            );
        }
    }

    public static MetadataManager get() {

        if (instance == null) {

            throw new IllegalStateException(
                    "Call init() in Application class first"
            );
        }

        return instance;
    }

    // ============================================================
    // SAVE METADATA
    // ============================================================

    public void saveMetadata(
            String vin,
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

        SharedPreferences.Editor editor =
                prefs.edit();

        if (vin != null &&
                !vin.trim().isEmpty()) {

            editor.putString(
                    "vin",
                    vin
            );
        }

        editor.putString(
                "charger",
                safe(charger)
        );

        editor.putString(
                "owner",
                safe(owner)
        );

        editor.putString(
                "phone",
                safe(phone)
        );

        editor.putString(
                "controller",
                safe(controllerId)
        );

        editor.putString(
                "motor",
                safe(motorId)
        );

        editor.putString(
                "bms",
                safe(bmsId)
        );

        editor.putString(
                "smartKey",
                safe(smartKeyId)
        );

        editor.putString(
                "model",
                safe(model)
        );

        editor.putString(
                "fronttpms",
                safe(fronttpms)
        );

        editor.putString(
                "reartpms",
                safe(reartpms)
        );

        editor.putBoolean(
                "metadata_synced",
                true
        );

        editor.apply();
    }

    private String safe(String value) {

        if (value == null ||
                value.trim().isEmpty()) {

            return "UNKNOWN";
        }

        return value;
    }

    // ============================================================
    // MQTT METADATA SYNC
    // ============================================================

    public void syncMetadataFromServer(
            String vin,
            Runnable onSuccess) {

        if (vin == null ||
                vin.trim().isEmpty() ||
                vin.equals("UNKNOWN") ||
                vin.equals("UNKNOWN_VIN")) {

            Log.e(
                    TAG,
                    "Cannot request metadata. Invalid VIN."
            );

            return;
        }

        MqttConnectionManager mqttManager =
                MqttConnectionManager.getInstance(context);

        mqttManager.requestMetadata(
                vin,
                metadataJson -> {

                    try {

                        JSONObject root =
                                new JSONObject(metadataJson);

                        boolean success =
                                root.optBoolean(
                                        "success",
                                        false
                                );

                        if (!success) {

                            String error =
                                    root.optString(
                                            "error",
                                            "Unknown metadata error"
                                    );

                            Log.e(
                                    TAG,
                                    "Metadata request failed: "
                                            + error
                            );

                            return;
                        }

                        JSONObject data =
                                root.optJSONObject("data");

                        if (data == null) {

                            Log.e(
                                    TAG,
                                    "Metadata response contains no data"
                            );

                            return;
                        }

                        String responseVin =
                                getNullableString(
                                        data,
                                        "vin"
                                );

                        String owner =
                                getNullableString(
                                        data,
                                        "owner_name"
                                );

                        String phone =
                                getNullableString(
                                        data,
                                        "phone"
                                );

                        String model =
                                getNullableString(
                                        data,
                                        "model"
                                );

                        String controller =
                                getNullableString(
                                        data,
                                        "controller_id"
                                );

                        String motor =
                                getNullableString(
                                        data,
                                        "motor_id"
                                );

                        String bms =
                                getNullableString(
                                        data,
                                        "bms_id"
                                );

                        String charger =
                                getNullableString(
                                        data,
                                        "charger_id"
                                );

                        String smartKey =
                                getNullableString(
                                        data,
                                        "comfortkey_id"
                                );

                        String frontTpms =
                                getNullableString(
                                        data,
                                        "fronttpms_id"
                                );

                        String rearTpms =
                                getNullableString(
                                        data,
                                        "reartpms_id"
                                );

                        saveMetadata(
                                responseVin != null
                                        ? responseVin
                                        : vin,
                                charger,
                                owner,
                                phone,
                                controller,
                                motor,
                                bms,
                                smartKey,
                                model,
                                frontTpms,
                                rearTpms
                        );

                        Log.d(
                                TAG,
                                "Metadata synced successfully -> VIN: "
                                        + vin
                        );

                        if (onSuccess != null) {

                            mainHandler.post(
                                    onSuccess
                            );
                        }

                    } catch (Exception e) {

                        Log.e(
                                TAG,
                                "Failed to parse metadata response",
                                e
                        );
                    }
                }
        );
    }

    private String getNullableString(
            JSONObject object,
            String key) {

        if (!object.has(key) ||
                object.isNull(key)) {

            return null;
        }

        String value =
                object.optString(
                        key,
                        null
                );

        if (value == null ||
                value.trim().isEmpty()) {

            return null;
        }

        return value;
    }

    // ============================================================
    // GETTERS
    // ============================================================

    public boolean isMetadataSynced() {

        return prefs.getBoolean(
                "metadata_synced",
                false
        );
    }

    public String getVin() {

        return prefs.getString(
                "vin",
                "UNKNOWN_VIN"
        );
    }

    public String getCharger() {

        return prefs.getString(
                "charger",
                "UNKNOWN_CHARGER"
        );
    }

    public String getOwner() {

        return prefs.getString(
                "owner",
                "UNKNOWN_OWNER"
        );
    }

    public String getPhone() {

        return prefs.getString(
                "phone",
                "UNKNOWN_PHONE"
        );
    }

    public String getControllerId() {

        return prefs.getString(
                "controller",
                "UNKNOWN_CONTROLLER"
        );
    }

    public String getMotorId() {

        return prefs.getString(
                "motor",
                "UNKNOWN_MOTOR"
        );
    }

    public String getBmsId() {

        return prefs.getString(
                "bms",
                "UNKNOWN_BMS"
        );
    }

    public String getSmartKeyId() {

        String key =
                prefs.getString(
                        "smartKey",
                        ""
                );

        if (key == null ||
                key.trim().isEmpty()) {

            return "UNKNOWN_KEY";
        }

        return key;
    }

    public String getModel() {

        return prefs.getString(
                "model",
                "UNKNOWN_MODEL"
        );
    }

    public String getFrontTPMS() {

        return prefs.getString(
                "fronttpms",
                "UNKNOWN_FTPMS"
        );
    }

    public String getRearTPMS() {

        return prefs.getString(
                "reartpms",
                "UNKNOWN_RTPMS"
        );
    }

}