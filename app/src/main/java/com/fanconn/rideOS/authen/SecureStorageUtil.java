package com.fanconn.rideOS.authen;

import android.content.Context;
import android.content.SharedPreferences;

public class SecureStorageUtil {

    private static final String PREF_NAME = "user_pref";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USERNAME = "username";

    private static SharedPreferences sharedPreferences;

    public static void init(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isUserLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public static String getLoggedInUsername() {
        return sharedPreferences.getString(KEY_USERNAME, "Guest");
    }

    public static void setUserLoggedIn(String username) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USERNAME, username);
        editor.apply();
    }

    public static void logOut() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, false);
        editor.putString(KEY_USERNAME, null);
        editor.apply();
    }
}
