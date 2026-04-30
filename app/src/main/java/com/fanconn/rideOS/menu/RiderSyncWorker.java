package com.fanconn.rideOS.menu;

import android.content.Context;
import android.content.SharedPreferences;

import com.fanconn.rideOS.NetworkUtil;
import com.fanconn.rideOS.network.ApiService;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;

public class RiderSyncWorker {
    private static boolean isSyncRunning = false;
    public static synchronized void sync(Context ctx, ApiService apiService) {

        if (isSyncRunning) {

            android.util.Log.d("SYNC_LOCK",
                    "Sync already running");

            return;
        }

        isSyncRunning = true;

        if (!NetworkUtil.isInternetAvailable(ctx)) {

            isSyncRunning = false;

            android.util.Log.e("SYNC",
                    "No internet → skip sync");

            return;
        }

        new Thread(() -> {

            RiderSyncManager manager = RiderSyncManager.getInstance(ctx);

            android.util.Log.d("SYNC", "Starting sync...");

            while (manager.hasData()) {

                List<RiderSync> batch = manager.getBatch();

                android.util.Log.d("SYNC", "Processing batch size = " + batch.size());

                List<Long> ids = new ArrayList<>();

                for (RiderSync item : batch) {

                    try {

                        android.util.Log.d("SYNC_ITEM", "Processing → " + item.operation);

                        Map<String, Object> data =
                                new Gson().fromJson(item.payload, Map.class);

                        switch (item.operation) {

                            case "ADD":

                                retrofit2.Response<ResponseBody> res =
                                        apiService.addRider(data).execute();

                                if (res.isSuccessful() && res.body() != null) {

                                    String responseStr = res.body().string();

                                    org.json.JSONObject json =
                                            new org.json.JSONObject(responseStr);

                                    int riderId = json.getInt("id");

                                    String name = (String) data.get("name");

                                    SharedPreferences prefs = SecurePrefs.get();

                                    prefs.edit()
                                            .putInt("user_" + name + "_backend_id", riderId)
                                            .apply();

                                    android.util.Log.d("SYNC_FIX",
                                            "Saved backend_id = " + riderId);

                                    ids.add(item.id);

                                } else {

                                    android.util.Log.e("SYNC_ADD_FAIL",
                                            "ADD failed code = " + res.code());
                                }

                                break;

                            case "UPDATE":

                                android.util.Log.d("SYNC_API", "Calling UPDATE API");

                                int riderId = ((Double) data.get("id")).intValue();

                                if (riderId == -1) {

                                    String riderName = (String) data.get("name");

                                    SharedPreferences prefs = SecurePrefs.get();

                                    riderId = prefs.getInt(
                                            "user_" + riderName + "_backend_id",
                                            -1
                                    );

                                    android.util.Log.d("SYNC_FIX",
                                            "Fetched latest backend_id = " + riderId);
                                }

                                // still missing → retry later
                                if (riderId == -1) {

                                    android.util.Log.e("SYNC_UPDATE",
                                            "backend_id still missing");

                                    break;
                                }

                                // REMOVE id from request body
                                data.remove("id");

                                android.util.Log.d("SYNC_UPDATE_DEBUG",
                                        "ID=" + riderId + " DATA=" + data.toString());

                                retrofit2.Response<ResponseBody> response =
                                        apiService.updateRider(riderId, data).execute();

                                if (response.isSuccessful()) {

                                    android.util.Log.d("SYNC_UPDATE",
                                            "Update SUCCESS for ID=" + riderId);

                                    ids.add(item.id);

                                } else {

                                    android.util.Log.e("SYNC_UPDATE_FAIL",
                                            "Failed code=" + response.code());
                                }

                                break;
                            case "DELETE":
                                int deleteId =
                                        ((Double) data.get("id")).intValue();
                                retrofit2.Response<ResponseBody> deleteResponse =
                                        apiService.deleteRider(deleteId).execute();
                                if (deleteResponse.isSuccessful()) {
                                    android.util.Log.d("SYNC_DELETE",
                                            "Delete success ID=" + deleteId);
                                    ids.add(item.id);
                                } else {
                                    android.util.Log.e("SYNC_DELETE_FAIL",
                                            "Delete failed code=" + deleteResponse.code());
                                }
                                break;
                        }
                    } catch (Exception e) {
                        android.util.Log.e("SYNC_ERROR", "Failed → " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                manager.delete(ids);
                android.util.Log.d("SYNC", "Deleted synced items: " + ids.size());
            }
            android.util.Log.d("SYNC", "Sync complete");
            isSyncRunning = false;
        }).start();
    }
}