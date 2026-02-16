//package com.fanconn.rideOS.network;
//
//import android.util.Log;
//
//import com.fanconn.rideOS.TelemetryDataManager;
//import com.google.gson.Gson;
//
//import java.io.IOException;
//
//import okhttp3.*;
//
//public class RideOSTelemetrySender {
//
//    private static final String TAG = "RIDEOS_API";
//    private static final String API_URL =
//            "http://103.174.148.224:9090/api/ev/telemetry";
//
//    private static final OkHttpClient client = new OkHttpClient();
//    private static final Gson gson = new Gson();
//
//    public static void send(String vin) {
//
//        TelemetryDataManager t = TelemetryDataManager.getInstance();
//        TelemetryApiPayload data = new TelemetryApiPayload();
//
//        data.vinNumber = vin;
//        data.time = java.time.OffsetDateTime.now().toString();
//
//        data.speedKmph = Math.round(t.getTeleSpeed());
//        data.tripKm = t.getTeleTrip();
//
//        data.soc = Math.round(t.getTeleSOC());
//        data.apuSoc = Math.round(t.getTeleAPUSOC());
//
//        data.batVoltage = t.getTeleBatVoltage();
//        data.currentConsumption = t.getTeleCurrentConsumption();
//        data.remainingCapacityAh = t.getTeleRemainingCapacityAH();
//
//        data.inAh = t.getTeleTotalAH();
//        data.inAhByCharger = t.getTeleChargerAH();
//        data.inAhByRegen = t.getTeleRegenAH();
//        data.outAh = t.getTeleDischargeAH();
//
//        data.motorTemp = t.getTeleMotorTemp();
//        data.controllerMosTemp = t.getTeleControllerMosTemp();
//        data.bmsMosTemp = t.getTeleBMSMosTemp();
//
//        data.tirePressure = t.getTeleTirePressure();
//        data.ntc = t.getTeleNTC();
//
//        // ✅ SINGLE SOURCE OF TRUTH
//        data.evPowerState =
//                (t.getTeleEvPowerState() == 1) ? "ON" : "OFF";
//
//        data.currentRider = t.getTeleCurrentRider();
//        data.latLong = t.getTeleLatLong();
//
//        if (data.latLong == null || data.latLong.isEmpty()) {
//            data.latLong = "0.0,0.0";
//        }
//
//        // 🔥 FULL JSON LOG
//        Log.e("TELEMETRY_API_SEND", gson.toJson(data));
//
////        RequestBody body = RequestBody.create(
////                gson.toJson(data),
////                MediaType.parse("application/json")
////        );
//        RequestBody body = RequestBody.create(
//                MediaType.parse("application/json"),
//                gson.toJson(data)
//        );
//
//
//        Request request = new Request.Builder()
//                .url(API_URL)
//                .post(body)
//                .build();
//
//        client.newCall(request).enqueue(new Callback() {
//
//            @Override
//            public void onFailure(Call call, IOException e) {
//                Log.e(TAG, "❌ Telemetry API send failed", e);
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) {
//                Log.e(TAG, "✅ Telemetry API sent | code=" + response.code());
//                response.close();
//            }
//        });
//    }
//}
package com.fanconn.rideOS.network;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.util.Log;

import com.fanconn.rideOS.TelemetryDataManager;
import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RideOSTelemetrySender {

    private static final String TAG = "RIDEOS_API";
    private static final String API_URL =
            "http://103.174.148.224:9090/api/ev/telemetry";

    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new Gson();

    //    public static void send(String vin) {
//
//        TelemetryDataManager t = TelemetryDataManager.getInstance();
//        TelemetryApiPayload data = new TelemetryApiPayload();
//
//        // =============================
//        // BASIC
//        // =============================
//        data.vinNumber = vin;
//        data.time = java.time.OffsetDateTime.now().toString();
//
//        // =============================
//        // MOTION
//        // =============================
//        data.speedKmph = Math.round(t.getTeleSpeed());
//        data.tripKm = t.getTeleTrip();
//
//        // =============================
//        // BATTERY
//        // =============================
//        data.soc = Math.round(t.getTeleSOC());
//        data.apuSoc = Math.round(t.getTeleAPUSOC());
//        data.batVoltage = t.getTeleBatVoltage();
//        data.currentConsumption = t.getTeleCurrentConsumption();
//        data.remainingCapacityAh = t.getTeleRemainingCapacityAH();
//
//        // =============================
//        // ENERGY
//        // =============================
//        data.inAh = t.getTeleTotalAH();
//        data.inAhByCharger = t.getTeleChargerAH();
//        data.inAhByRegen = t.getTeleRegenAH();
//        data.outAh = t.getTeleDischargeAH();
//
//        // =============================
//        // TEMPERATURES
//        // =============================
//        data.motorTemp = t.getTeleMotorTemp();
//        data.controllerMosTemp = t.getTeleControllerMosTemp();
//        data.bmsMosTemp = t.getTeleBMSMosTemp();
//
//        // =============================
//        // MISC
//        // =============================
//        data.tirePressure = t.getTeleTirePressure();
//        data.ntc = t.getTeleNTC();
//        data.currentRider = t.getTeleCurrentRider();
//
//        // =============================
//        // 🔥 RAW STATES (NO STRING CONVERSION)
//        // =============================
//        data.evPowerState = t.getTeleEvPowerState();      // 0 / 1
//        data.chargingState = t.getTeleChargingState();   // 0 / 1
//
//        // =============================
//        // LOCATION
//        // =============================
//        data.latLong = t.getTeleLatLong();
//        if (data.latLong == null || data.latLong.isEmpty()) {
//            data.latLong = "0.0,0.0";
//        }
//
//        // =============================
//        // LOG (SOURCE OF TRUTH)
//        // =============================
//        Log.e("TELEMETRY_API_SEND", gson.toJson(data));
//        Log.e("TELEMETRY_STATE_CHECK",
//                "evPower=" + data.evPowerState +
//                        ", charging=" + data.chargingState);
//
//        // =============================
//        // SEND
//        // =============================
//        RequestBody body = RequestBody.create(
//                MediaType.parse("application/json"),
//                gson.toJson(data)
//        );
//
//        Request request = new Request.Builder()
//                .url(API_URL)
//                .post(body)
//                .build();
//
//        client.newCall(request).enqueue(new Callback() {
//
//            @Override
//            public void onFailure(Call call, IOException e) {
//                Log.e(TAG, "❌ Telemetry API send failed", e);
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) {
//                Log.e(TAG, "✅ Telemetry API sent | code=" + response.code());
//                response.close();
//            }
//        });
//    }
    public static void send(String vin, String rideOsVersion) {

        TelemetryDataManager t = TelemetryDataManager.getInstance();
        TelemetryApiPayload data = new TelemetryApiPayload();

        // =============================
        // BASIC
        // =============================
        data.vinNumber = vin;
        data.time = java.time.OffsetDateTime.now().toString();


        data.rideOsVersion = rideOsVersion;

        Log.e("TELEMETRY_API_SEND", new Gson().toJson(data));

        // =============================
        // MOTION (always valid)
        // =============================
        data.speedKmph = Math.round(t.getTeleSpeed());
        data.tripKm = t.getTeleTrip();
        data.odo = t.getTeleODO();


        // =============================
        // BATTERY (guarded)
        // =============================
        if (t.getTeleSOC() > 0) {
            data.soc = Math.round(t.getTeleSOC());
        }

        if (t.getTeleAPUSOC() > 0) {
            data.apuSoc = Math.round(t.getTeleAPUSOC());
        }


        if (t.getTeleBatVoltage() > 0) {
            data.batVoltage = t.getTeleBatVoltage();
        }

        if (t.getTeleCurrentConsumption() != 0) {
            data.currentConsumption = t.getTeleCurrentConsumption();
        }

        if (t.getTeleRemainingCapacityAH() > 0) {
            data.remainingCapacityAh = t.getTeleRemainingCapacityAH();
        }

        // =============================
        // ENERGY (event-based)
        // =============================
        if (t.getTeleTotalAH() > 0) {
            data.inAh = t.getTeleTotalAH();
        }

        if (t.getTeleChargerAH() > 0) {
            data.inAhByCharger = t.getTeleChargerAH();
        }

        if (t.getTeleRegenAH() > 0) {
            data.inAhByRegen = t.getTeleRegenAH();
        }

        if (t.getTeleDischargeAH() > 0) {
            data.outAh = t.getTeleDischargeAH();
        }

        // =============================
        // TEMPERATURES (guarded)
        // =============================
        if (t.getTeleMotorTemp() > 0) {
            data.motorTemp = t.getTeleMotorTemp();
        }

        if (t.getTeleControllerMosTemp() > 0) {
            data.controllerMosTemp = t.getTeleControllerMosTemp();
        }

        if (t.getTeleBMSMosTemp() > 0) {
            data.bmsMosTemp = t.getTeleBMSMosTemp();
        }

        // =============================
        // MISC
        // =============================
        if (t.getTeleTirePressure() != null) {
            data.tirePressure = t.getTeleTirePressure();
        }

        if (t.getTeleNTC() != null) {
            data.ntc = t.getTeleNTC();
        }

        // ✅ CURRENT RIDER (CRITICAL FIX)
        if (t.getTeleCurrentRider() != null && !t.getTeleCurrentRider().isEmpty()) {
            data.currentRider = t.getTeleCurrentRider();
        }

        // =============================
        // RAW STATES (ALWAYS SEND)
        // =============================
//    data.evPowerState = (TelemetryService.shdtelemetrywhenunlocked ? "ON" : "OFF");
        data.evPowerState =
                (t.getTeleEvPowerState() == 1) ? "ON" : "OFF";



        data.chargingState = t.getTeleChargingState();

        // =============================
        // LOCATION
        // =============================
        String latLong = t.getTeleLatLong();
        data.latLong = (latLong == null || latLong.isEmpty())
                ? "0.0,0.0"
                : latLong;

        // =============================
        // LOG
        // =============================
        Log.e("TELEMETRY_API_SEND", new Gson().toJson(data));

        // =============================
        // SEND
        // =============================
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"),
                new Gson().toJson(data)
        );

        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "❌ Telemetry API send failed", e);
            }

            @Override
            public void onResponse(Call call, Response response) {
                Log.e(TAG, "✅ Telemetry API sent | code=" + response.code());
                response.close();
            }
        });
    }

//    private static String getRideOSVersion(Context context) {
//        try {
//            PackageInfo pInfo =
//                    context.getPackageManager()
//                            .getPackageInfo(context.getPackageName(), 0);
//
//            int mcuVersion = TelemetryService.getEffectiveMcuVersionStatic();
//
//            return "rideOS " + pInfo.versionName + "_GA"
//                    + ",AOSPv=" + Build.DISPLAY + "_GA"
//                    + ",MCUv=" + mcuVersion;
//
//        } catch (Exception e) {
//            return "rideOS N/A";
//        }
//    }


}