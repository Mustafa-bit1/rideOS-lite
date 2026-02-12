package com.fanconn.rideOS;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fanconn.rideOS.util.http;

public class aosp_ota_update_check {

    public interface Callback {
        void onResult(boolean updateAvailable);
        void onError();
    }

    public static void checkAospUpdate(String versionUrl, int localVersion, Callback callback) {

        new Thread(() -> {
            try {
                String json = http.doGet(versionUrl);
                if (json == null || json.isEmpty()) {
                    callback.onError();
                    return;
                }

                JSONObject jo = JSON.parseObject(json);
                JSONObject platform = jo.getJSONObject("ElectricBike");
                if (platform == null) {
                    callback.onError();
                    return;
                }

                int serverVersion = Integer.parseInt(platform.getString("version"));
                boolean updateAvailable = serverVersion > localVersion;

                callback.onResult(updateAvailable);

            } catch (Exception e) {
                e.printStackTrace();
                callback.onError();
            }
        }).start();
    }
}
