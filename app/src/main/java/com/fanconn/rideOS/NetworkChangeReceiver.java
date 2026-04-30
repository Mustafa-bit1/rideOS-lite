package com.fanconn.rideOS;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.fanconn.rideOS.menu.RiderSyncWorker;
import com.fanconn.rideOS.network.ApiService;
import com.fanconn.rideOS.network.RetrofitClient;

public class NetworkChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (NetworkUtil.isInternetAvailable(context)) {

            android.util.Log.d("SYNC", "Internet restored → triggering sync");

            ApiService apiService =
                    RetrofitClient.getClient().create(ApiService.class);

            RiderSyncWorker.sync(context, apiService);
        }
    }
}