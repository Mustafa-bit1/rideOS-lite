package com.fanconn.rideOS;

import android.content.Context;
import android.content.SharedPreferences;

public class OtaBranchManager {

    private static final String PREF = "ota_branch_pref";
    private static final String KEY_BRANCH = "branch";

    public static void saveBranch(Context context, String branch) {

        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_BRANCH, branch)
                .apply();
    }

    public static String getBranch(Context context) {

        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getString(
                        KEY_BRANCH,
                        UpdateService.BRANCH_PRODUCTION
                );
    }

    public static boolean isRnd(Context context) {
        return UpdateService.BRANCH_RND.equals(
                getBranch(context)
        );
    }
}