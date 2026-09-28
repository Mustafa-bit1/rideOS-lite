////package com.fanconn.rideOS.network;
////
////import android.util.Log;
////
////import com.fanconn.rideOS.TelemetryDataManager;
////import com.google.gson.Gson;
////
////import java.io.IOException;
////
////import okhttp3.*;
////
////public class RideOSTelemetrySender {
////
////    private static final String TAG = "RIDEOS_API";
////    private static final String API_URL =
////            "http://103.174.148.224:9090/api/ev/telemetry";
////
////    private static final OkHttpClient client = new OkHttpClient();
////    private static final Gson gson = new Gson();
////
////    public static void send(String vin) {
////
////        TelemetryDataManager t = TelemetryDataManager.getInstance();
////        TelemetryApiPayload data = new TelemetryApiPayload();
////
////        data.vinNumber = vin;
////        data.time = java.time.OffsetDateTime.now().toString();
////
////        data.speedKmph = Math.round(t.getTeleSpeed());
////        data.tripKm = t.getTeleTrip();
////
////        data.soc = Math.round(t.getTeleSOC());
////        data.apuSoc = Math.round(t.getTeleAPUSOC());
////
////        data.batVoltage = t.getTeleBatVoltage();
////        data.currentConsumption = t.getTeleCurrentConsumption();
////        data.remainingCapacityAh = t.getTeleRemainingCapacityAH();
////
////        data.inAh = t.getTeleTotalAH();
////        data.inAhByCharger = t.getTeleChargerAH();
////        data.inAhByRegen = t.getTeleRegenAH();
////        data.outAh = t.getTeleDischargeAH();
////
////        data.motorTemp = t.getTeleMotorTemp();
////        data.controllerMosTemp = t.getTeleControllerMosTemp();
////        data.bmsMosTemp = t.getTeleBMSMosTemp();
////
////        data.tirePressure = t.getTeleTirePressure();
////        data.ntc = t.getTeleNTC();
////
////        // ✅ SINGLE SOURCE OF TRUTH
////        data.evPowerState =
////                (t.getTeleEvPowerState() == 1) ? "ON" : "OFF";
////
////        data.currentRider = t.getTeleCurrentRider();
////        data.latLong = t.getTeleLatLong();
////
////        if (data.latLong == null || data.latLong.isEmpty()) {
////            data.latLong = "0.0,0.0";
////        }
////
////        // 🔥 FULL JSON LOG
////        Log.e("TELEMETRY_API_SEND", gson.toJson(data));
////
//////        RequestBody body = RequestBody.create(
//////                gson.toJson(data),
//////                MediaType.parse("application/json")
//////        );
////        RequestBody body = RequestBody.create(
////                MediaType.parse("application/json"),
////                gson.toJson(data)
////        );
////
////
////        Request request = new Request.Builder()
////                .url(API_URL)
////                .post(body)
////                .build();
////
////        client.newCall(request).enqueue(new Callback() {
////
////            @Override
////            public void onFailure(Call call, IOException e) {
////                Log.e(TAG, "❌ Telemetry API send failed", e);
////            }
////
////            @Override
////            public void onResponse(Call call, Response response) {
////                Log.e(TAG, "✅ Telemetry API sent | code=" + response.code());
////                response.close();
////            }
////        });
////    }
////}
//package com.fanconn.rideOS.network;
//
//import android.content.Context;
//import android.content.pm.PackageInfo;
//import android.os.Build;
//import android.util.Log;
//
//import com.fanconn.rideOS.ETADataManager;
//import com.fanconn.rideOS.UIUpdater;
//import com.google.gson.Gson;
//
//import java.io.File;
//import java.io.IOException;
//
//import okhttp3.Call;
//import okhttp3.Callback;
//import okhttp3.MediaType;
//import okhttp3.OkHttpClient;
//import okhttp3.Request;
//import okhttp3.RequestBody;
//import okhttp3.Response;
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
//    //    public static void send(String vin) {
////
////        TelemetryDataManager t = TelemetryDataManager.getInstance();
////        TelemetryApiPayload data = new TelemetryApiPayload();
////
////        // =============================
////        // BASIC
////        // =============================
////        data.vinNumber = vin;
////        data.time = java.time.OffsetDateTime.now().toString();
////
////        // =============================
////        // MOTION
////        // =============================
////        data.speedKmph = Math.round(t.getTeleSpeed());
////        data.tripKm = t.getTeleTrip();
////
////        // =============================
////        // BATTERY
////        // =============================
////        data.soc = Math.round(t.getTeleSOC());
////        data.apuSoc = Math.round(t.getTeleAPUSOC());
////        data.batVoltage = t.getTeleBatVoltage();
////        data.currentConsumption = t.getTeleCurrentConsumption();
////        data.remainingCapacityAh = t.getTeleRemainingCapacityAH();
////
////        // =============================
////        // ENERGY
////        // =============================
////        data.inAh = t.getTeleTotalAH();
////        data.inAhByCharger = t.getTeleChargerAH();
////        data.inAhByRegen = t.getTeleRegenAH();
////        data.outAh = t.getTeleDischargeAH();
////
////        // =============================
////        // TEMPERATURES
////        // =============================
////        data.motorTemp = t.getTeleMotorTemp();
////        data.controllerMosTemp = t.getTeleControllerMosTemp();
////        data.bmsMosTemp = t.getTeleBMSMosTemp();
////
////        // =============================
////        // MISC
////        // =============================
////        data.tirePressure = t.getTeleTirePressure();
////        data.ntc = t.getTeleNTC();
////        data.currentRider = t.getTeleCurrentRider();
////
////        // =============================
////        // 🔥 RAW STATES (NO STRING CONVERSION)
////        // =============================
////        data.evPowerState = t.getTeleEvPowerState();      // 0 / 1
////        data.chargingState = t.getTeleChargingState();   // 0 / 1
////
////        // =============================
////        // LOCATION
////        // =============================
////        data.latLong = t.getTeleLatLong();
////        if (data.latLong == null || data.latLong.isEmpty()) {
////            data.latLong = "0.0,0.0";
////        }
////
////        // =============================
////        // LOG (SOURCE OF TRUTH)
////        // =============================
////        Log.e("TELEMETRY_API_SEND", gson.toJson(data));
////        Log.e("TELEMETRY_STATE_CHECK",
////                "evPower=" + data.evPowerState +
////                        ", charging=" + data.chargingState);
////
////        // =============================
////        // SEND
////        // =============================
////        RequestBody body = RequestBody.create(
////                MediaType.parse("application/json"),
////                gson.toJson(data)
////        );
////
////        Request request = new Request.Builder()
////                .url(API_URL)
////                .post(body)
////                .build();
////
////        client.newCall(request).enqueue(new Callback() {
////
////            @Override
////            public void onFailure(Call call, IOException e) {
////                Log.e(TAG, "❌ Telemetry API send failed", e);
////            }
////
////            @Override
////            public void onResponse(Call call, Response response) {
////                Log.e(TAG, "✅ Telemetry API sent | code=" + response.code());
////                response.close();
////            }
////        });
////    }
//    public static void send(String vin, String rideOsVersion) {
//
//        TelemetryDataManager t = TelemetryDataManager.getInstance();
//        TelemetryDeltaManager delta =
//                TelemetryDeltaManager.getInstance();
//        CompactTelemetryPayload data =
//                new CompactTelemetryPayload();
//
//        // =============================
//        // BASIC
//        // =============================
//        data.vinNumber = vin;
//        data.time = java.time.OffsetDateTime.now().toString();
//
//
//        data.rideOsVersion =
//                delta.changed(
//                        "rideos",
//                        rideOsVersion);
//
//        // =============================
//        // MOTION (always valid)
//        // =============================
//        data.speedKmph =
//                delta.changedInt(
//                        "speed",
//                        Math.round(t.getTeleSpeed()));
//        data.tripKm =
//                delta.changedFloat(
//                        "trip",
//                        t.getTeleTrip());
//        data.odo =
//                delta.changedFloat(
//                        "odo",
//                        t.getTeleODO());
//        //Log.e("ODOFORRO ","ODOFORRO "+data.odo);
//
//
//        // =============================
//        // BATTERY (guarded)
//        // =============================
//        data.soc =
//                delta.changedInt(
//                        "soc",
//                        Math.round(t.getTeleSOC()));
//
//        data.range =
//                delta.changedFloat(
//                        "range",
//                        TelemetryDataManager.getInstance()
//                                .getTeleRange());
//
//        String reco = "";
//
//        if (UIUpdater.getInstance() != null) {
//            reco = UIUpdater.getInstance()
//                    .getrecoEngineParams();
//
//            if (reco == null)
//                reco = "";
//        }
//
//        data.recoEngine =
//                delta.changed(
//                        "reco_engine",
//                        reco);
//        // =============================
//        // TEMPERATURES (guarded)
//        // =============================
//        data.controllerTemp =
//                delta.changedFloat(
//                        "ctrl",
//                        t.getTeleControllerMosTemp());
//
//        data.motorTemp =
//                delta.changedFloat(
//                        "motor",
//                        t.getTeleMotorTemp());
//
//        data.vcuTemp =
//                delta.changedFloat(
//                        "vcu",
//                        getCriticalTemps());
//
//        data.bmsTemp =
//                delta.changedFloat(
//                        "bms",
//                        t.getTeleBMSMosTemp());
//
//        // =============================
//        // MISC
//        // =============================
//        String front = "";
//        String rear = "";
//
//        String tpms =
//                t.getTeleTirePressure();
//
//        if (tpms != null) {
//
//            String[] arr =
//                    tpms.split(";");
//
//            if (arr.length > 0)
//                front = arr[0];
//
//            if (arr.length > 1)
//                rear = arr[1];
//        }
//
//        data.frontTpms =
//                delta.changed(
//                        "front_tpms",
//                        front);
//
//        data.rearTpms =
//                delta.changed(
//                        "rear_tpms",
//                        rear);
//
//
////        if (t.getTeleNTC() != null) {
//            //data.ntc = t.getTeleNTC()+";"+ ETADataManager.getInstance().getEtaTime()+";"+TelemetryDataManager.getInstance().getTeleChargingCurrentValue()+";"+TelemetryDataManager.getInstance().getTeleChargingState()+";"+getCpu1Temp()+";"+TelemetryDataManager.getInstance().getTeleRange()+";"+ UIUpdater.getInstance().getrecoEngineParams();
////        }
//        String ntc =
//                t.getTeleNTC();
//
//        if (ntc == null)
//            ntc = "";
//
//        data.ntc =
//                delta.changed(
//                        "ntc",
//                        ntc);
//
//        String rider =
//                t.getTeleCurrentRider();
//
//        if (rider == null)
//            rider = "";
//
//        data.rider =
//                delta.changed(
//                        "rider",
//                        rider);
//
//        // =============================
//        // RAW STATES (ALWAYS SEND)
//        // =============================
////    data.evPowerState = (TelemetryService.shdtelemetrywhenunlocked ? "ON" : "OFF");
//        String state =
//                (t.getTeleEvPowerState() == 1)
//                        ? "ON"
//                        : "OFF";
//        data.scooterState =
//                delta.changed(
//                        "state",
//                        state);
//
//
//
//        data.chargingState =
//                delta.changedInt(
//                        "charging_state",
//                        t.getTeleChargingState());
//
//        data.chargingCurrent =
//                delta.changedFloat(
//                        "charging_current",
//                        TelemetryDataManager.getInstance()
//                                .getTeleChargingCurrentValue());
//
//        float eta =
//                ETADataManager.getInstance()
//                        .getEtaTime();
//
//        data.estChargingTime =
//                delta.changedFloat(
//                        "eta",
//                        eta);
//        // =============================
//        // LOCATION
//        // =============================
//        String location =
//                t.getTeleLatLong();
//
//        if (location == null)
//            location = "0.0,0.0";
//
//        data.location =
//                delta.changed(
//                        "location",
//                        location);
//
//        // =============================
//        // LOG
//        // =============================
//       // Log.e("TELEMETRY_API_SEND", new Gson().toJson(data));
//
//        // =============================
//        // SEND
//        // =============================
//
//        String json = gson.toJson(data);
//
//        Log.e("COMPACT_TELEMETRY", json);
//
//        RequestBody body = RequestBody.create(
//                MediaType.parse("application/json"),
//                json
//        );
//
//        Request request = new Request.Builder()
//                .url(API_URL)
//                .post(body)
//                .build();
//
//        client.newCall(request).enqueue(new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//             //   Log.e(TAG, "❌ Telemetry API send failed", e);
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) {
//               // Log.e(TAG, "✅ Telemetry API sent | code=" + response.code());
//                response.close();
//            }
//        });
//    }
//
//
//    private static int getCriticalTemps() {
//
//
//        int cpu1=0;
//
//
//        try {
//            File thermalDir = new File("/sys/class/thermal/");
//            File[] zones = thermalDir.listFiles();
//
//            if (zones != null) {
//                for (File zone : zones) {
//
//                    if (zone.getName().startsWith("thermal_zone")) {
//
//                        File typeFile = new File(zone, "type");
//                        File tempFile = new File(zone, "temp");
//
//                        if (typeFile.exists() && tempFile.exists()) {
//
//                            String type = readLine(typeFile);
//                            String tempRaw = readLine(tempFile);
//
//                            if (type == null || tempRaw == null) continue;
//
//                            float tempC = Float.parseFloat(tempRaw) / 1000f;
//                            cpu1 = (int)tempC;
//                        }
//                    }
//                }
//            }
//
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return
//                cpu1;
//    }
//    private static String readLine(File file) {
//
//        try (java.io.BufferedReader br =
//                     new java.io.BufferedReader(
//                             new java.io.FileReader(file))) {
//
//            return br.readLine();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return null;
//    }
////    private static String getRideOSVersion(Context context) {
////        try {
////            PackageInfo pInfo =
////                    context.getPackageManager()
////                            .getPackageInfo(context.getPackageName(), 0);
////
////            int mcuVersion = TelemetryService.getEffectiveMcuVersionStatic();
////
////            return "rideOS " + pInfo.versionName + "_GA"
////                    + ",AOSPv=" + Build.DISPLAY + "_GA"
////                    + ",MCUv=" + mcuVersion;
////
////        } catch (Exception e) {
////            return "rideOS N/A";
////        }
////    }
//
//
//}