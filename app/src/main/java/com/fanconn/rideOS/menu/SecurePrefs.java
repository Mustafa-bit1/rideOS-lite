package com.fanconn.rideOS.menu;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

public class SecurePrefs {

    private static SharedPreferences prefs;

    public static void init(Context context) {
        if (prefs != null) return;

        try {
            prefs = EncryptedSharedPreferences.create(
                    "user_data",
                    MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
                    context.getApplicationContext(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static SharedPreferences get() {
        return prefs;
    }
}