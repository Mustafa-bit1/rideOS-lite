//package com.fanconn.rideOS;
//
//import android.annotation.SuppressLint;
//import android.app.Service;
//import android.content.Context;
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.content.pm.PackageInfo;
//import android.content.pm.PackageManager;
//import android.location.Location;
//import android.location.LocationListener;
//import android.location.LocationManager;
//import android.net.ConnectivityManager;
//import android.net.Network;
//import android.net.NetworkCapabilities;
//import android.net.NetworkInfo;
//import android.os.Build;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.HandlerThread;
//import android.os.IBinder;
//import android.os.Looper;
//import android.util.Log;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.core.app.ActivityCompat;
//
//import com.fanconn.rideOS.network.EVWebSocketClient;
//import com.fanconn.rideOS.network.RetrofitClient;
//import com.fanconn.rideOS.network.VehicleData;
//import com.fanconn.rideOS.network.ApiService;
//
//import com.google.gson.Gson;
//
//import java.net.InetSocketAddress;
//import java.net.Socket;
//import java.net.URI;
//import java.text.SimpleDateFormat;
//import java.util.Date;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Locale;
//
//import okhttp3.ResponseBody;
//import retrofit2.Call;
//import retrofit2.Callback;
//import retrofit2.Response;
//
//import android.Manifest;
//import android.os.HandlerThread;
//
//public class TelemetryService extends Service {
//
//
//    private HandlerThread telemetryThread;
//    private Handler telemetryHandler;
//    private boolean isSending = false;
//    private EVWebSocketClient evClient;
//    private ApiService apiService;
//    private Context context;
//    // Telemetry flags
//    public static boolean shdtelemetrywhenunlocked;
//    public static int sendRealtimeDataOnceEvenAfterLocking = 0;
//    private SerialPortManager serialPortManager;
//    private Heartbeat1 hb1;
//    private static final String PREFS_OTA = "ota_prefs";
//    private static final String KEY_LAST_MCU_VERSION = "last_mcu_version";
//
//    @Override
//    public void onCreate() {
//        super.onCreate();
//        telemetryThread = new HandlerThread("TelemetryThread");
//        telemetryThread.start();
//        telemetryHandler = new Handler(telemetryThread.getLooper());
//    }
//
//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        String vinNumber = intent != null ? intent.getStringExtra("VIN_NUMBER") : null;
//        Log.d("xfcgvhbjnm","TELEMETRY SERVICE STARTED");
//        this.context = this;
//        startTelemetry(vinNumber != null ? vinNumber : "UNKNOWN");
//        return START_STICKY;
//    }
//
//    private void startTelemetry(String VINNUMBER) {
//        evClient = new EVWebSocketClient(VINNUMBER, URI.create("ws://sb.rivotmotors.com:8081/ws/ev"));
////        evClient = new EVWebSocketClient(VINNUMBER, URI.create("ws://103.174.148.224:8081/ws/ev"));
////        evClient = new EVWebSocketClient(VINNUMBER, URI.create("ws://192.168.1.106.224:8081/ws/ev"));
//
//        evClient.connect();
//
//        apiService = RetrofitClient.getClient().create(ApiService.class);
//        isSending = true;
//        sendDataContinuously(VINNUMBER);
//        startLocationUpdates(this);
//
//
//        new Thread(() -> {
//            while (evClient == null || !evClient.isOpen()) {
//                try {
//                    Thread.sleep(500);
//                } catch (InterruptedException e) {
//                    Log.e("Telemetry", "Thread interrupted", e);
//                    return;
//                }
//            }
//            serialPortManager = serialApplication.getSerialPortManager();
//            if (serialPortManager != null) {
//                serialPortManager.sendContinuousSensorData();
//            } else {
//                Log.e("Telemetry", "serialPortManager is null!");
//            }
//        }).start();
//    }
//    private void sendDataContinuously(String vin) {
//        telemetryHandler.removeCallbacksAndMessages(null); // prevent duplicate loops
//        telemetryHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                if (isSending) {
//
////                    Log.d("TelemetryService", "Loop latlong = "
////                            + TelemetryDataManager.getInstance().getTeleLatLong());
//
//                    if(serialApplication.getSerialPortManager().getHb1()!=null)
//                    {
//                        hb1= serialApplication.getSerialPortManager().getHb1();
//                    }
//                    if(hb1!=null) {
//                        TelemetryDataManager.getInstance().setTeleHandleLockState(hb1.getDragonLock());
//                        TelemetryDataManager.getInstance().setTeleSeatLockState(hb1.getSeatLock());
//                    }
//
//                    sendVehicleData(context, (float) roundTo2Decimal(TelemetryDataManager.getInstance().getTeleODO()), TelemetryDataManager.getInstance().getTeleChargingState(), TelemetryDataManager.getInstance().getTeleHandleLockState(),
//                            TelemetryDataManager.getInstance().getTeleSeatLockState(), TelemetryDataManager.getInstance().getTeleBMSMosStates()+"-"+UptimeManager.getUptime(), TelemetryDataManager.getInstance().getTeleCurrentRider(),
//                            TelemetryDataManager.getInstance().getTeleBmsLifeCycles(), TelemetryDataManager.getInstance().getTeleLatLong());
//                        sendRealTimeData(vin);
//
////                    if (shdtelemetrywhenunlocked) {
////                        sendRealTimeData(vin);
////                        sendRealtimeDataOnceEvenAfterLocking = 0;
////                    } else if (sendRealtimeDataOnceEvenAfterLocking == 0) {
////                        sendRealTimeData(vin);
////                        sendRealtimeDataOnceEvenAfterLocking++;
////                    }
//
//                    telemetryHandler.postDelayed(this, 1000);
//                }
//            }
//        }, 1000);
//    }
//
//    private void sendVehicleData(Context context, float odo, int chargingState,
//                                 int handleLockState, int seatLockState, String bmsMosStates,
//                                 String riders, int bmslifecycles, String lat_long) {
//
//        MetadataManager metadata = MetadataManager.get();
//        String vinNumber = metadata.getVin();
//
//        if (vinNumber.contains("UNKNOWN")) {
//            Log.w("Telemetry", "VIN unknown — skipping sendVehicleData");
//            return;
//        }
//        if (lat_long == null || lat_long.isEmpty()) {
//            lat_long = "0.0,0.0";   // fallback value if GPS not ready
//        }
//
//        String ownerName = metadata.getOwner();
//        String phoneNumber = metadata.getPhone();
//        String controllerID = metadata.getControllerId();
//        String motorID = metadata.getMotorId();
//        String bmsID = metadata.getBmsId();
//        String chargerID = metadata.getCharger();
//        String smartKeyID = metadata.getSmartKeyId();
//        String model = metadata.getModel();
//
//        String rideOSversion;
//        try {
//            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
//            int mcuVersion = getEffectiveMcuVersion();
//            rideOSversion = "rideOS " + pInfo.versionName+"_GA"+",AOSPv="+Build.DISPLAY+"_GA"+",MCUv="+mcuVersion;
//        } catch (PackageManager.NameNotFoundException e) {
//            rideOSversion = "rideOS N/A";
//        }
//
//        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
//        SharedPreferences prefs = context.getSharedPreferences("last_uploaded_metadata", Context.MODE_PRIVATE);
//
//        boolean anyChanged =
//                !vinNumber.equals(prefs.getString("vin", "")) ||
//                        !ownerName.equals(prefs.getString("owner", "")) ||
//                        !phoneNumber.equals(prefs.getString("phone", "")) ||
//                        !controllerID.equals(prefs.getString("controller", "")) ||
//                        !motorID.equals(prefs.getString("motor", "")) ||
//                        !bmsID.equals(prefs.getString("bms", "")) ||
//                        !chargerID.equals(prefs.getString("charger", "")) ||
//                        !smartKeyID.equals(prefs.getString("smartKey", "")) ||
//                        !model.equals(prefs.getString("model", ""));
//
//        VehicleData data = anyChanged ?
//                new VehicleData(vinNumber, model, ownerName, phoneNumber, controllerID,
//                        motorID, bmsID, chargerID, rideOSversion, smartKeyID,
//                        odo, chargingState, handleLockState, seatLockState,
//                        bmsMosStates, riders, bmslifecycles, lat_long, time)
//
//                :
//                new VehicleData(vinNumber, null, null, null, null,
//                        null, null, null, rideOSversion, null,
//                        odo, chargingState, handleLockState, seatLockState,
//                        bmsMosStates, riders, bmslifecycles, lat_long, time);
//        //Log.d("TelemetryService", "Sending telemetry: " + TelemetryDataManager.getInstance().getTeleLatLong());
//
//        if (anyChanged) {
//            prefs.edit()
//                    .putString("owner", ownerName)
//                    .putString("phone", phoneNumber)
//                    .putString("controller", controllerID)
//                    .putString("motor", motorID)
//                    .putString("bms", bmsID)
//                    .putString("charger", chargerID)
//                    .putString("smartKey", smartKeyID)
//                    .putString("model", model)
//                    .apply();
//        }
//
//        apiService.saveVehicleData(data).enqueue(new Callback<ResponseBody>() {
//            @Override
//            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) { }
//            @Override
//            public void onFailure(Call<ResponseBody> call, Throwable t) {
//                Log.e("Telemetry", "Faileddd to send vehicle data", t);
//            }
//        });
//    }
//
//    private void sendRealTimeData(String vin) {
//        RealTimeData data = collectTelemetryData(vin);
//
//        if (hasActiveInternetConnection()) {
//            // Send staged data first
//            // NEW SAFE BATCH SENDING
//            if (TelemetryStagingManager.getInstance().hasData()) {
//                new Thread(() -> {
//                    while (TelemetryStagingManager.getInstance().hasData() && hasActiveInternetConnection()) {
//
//                        List<RealTimeData> batch = TelemetryStagingManager.getInstance().pollBatch();  // Take 20 at once
//                        for (RealTimeData r : batch) {
//                            sendToServer(r);
//                            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
//                        }
//
//                        try { Thread.sleep(300); } catch (Exception ignored) {} // Small break between batches
//                    }
//                }).start();
//            }
//
//
//            // Send current data
//            //DataUsageTracker tracker = new DataUsageTracker();
//            //tracker.startTracking();
//
//            sendToServer(data);
//            //Log.e("Telemetry", "INTENET VAIL SNEDNING ");
//
//            //tracker.logUsage("Currdent Payload");
//
//        } else {
//            //Log.e("Telemetry", "NOOINTENET VAIL SNEDNING ");
//            TelemetryStagingManager.getInstance().addData(data);
//        }
//    }
//
//    private RealTimeData collectTelemetryData(String vin) {
//        RealTimeData data = new RealTimeData();
//        data.vinNumber = vin;
//        data.speed_kmph = TelemetryDataManager.getInstance().getTeleSpeed();
//        data.tripkm= roundTo2Decimal(TelemetryDataManager.getInstance().getTeleTrip());
//        data.controllerMosTemp = TelemetryDataManager.getInstance().getTeleControllerMosTemp();
//        data.motorTemp = TelemetryDataManager.getInstance().getTeleMotorTemp();
//        data.bmsMosTemp = TelemetryDataManager.getInstance().getTeleBMSMosTemp();
//        if(serialPortManager!=null){
//            data.ntc = " MOS="+TelemetryDataManager.getInstance().getTeleBMSMosStates()+",ntc="+TelemetryDataManager.getInstance().getTeleNTC()+" ICV="+TelemetryDataManager.getInstance().getteleIndividualCellVoltages()+" C="+TelemetryDataManager.getInstance().getTeleChargingState()+" CE="+TelemetryDataManager.getInstance().getTeleChargingStateError()+" ApuV="+TelemetryDataManager.getInstance().getTeleAPUBatVoltage()+" ContErr="+TelemetryDataManager.getInstance().getTeleControllerAbruptOff()+
//                    " AllCells 1to3="+serialPortManager.getHb3().getcell1TO3()+",cell4to6="+serialPortManager.getHb3().getcell4TO6()+",cell7to9="+serialPortManager.getHb3().getcell7TO9()+",cell10to12="+serialPortManager.getHb3().getcell10TO12()+",cell13to15="+serialPortManager.getHb3().getcell13TO15()+",cell16to18="+serialPortManager.getHb3().getcell16TO18()+",cell19to21="+serialPortManager.getHb3().getcell19TO21()+",cell22to23="+serialPortManager.getHb3().getcell22TO23();
//        }
//        data.currentconsumption = roundTo3Decimal(TelemetryDataManager.getInstance().getTeleCurrentConsumption());
//        data.batvoltage = roundTo1Decimal(TelemetryDataManager.getInstance().getTeleBatVoltage());
//        data.remainingCapacity_AH = roundTo3Decimal(TelemetryDataManager.getInstance().getTeleRemainingCapacityAH());
//        data.inAH = roundTo3Decimal(TelemetryDataManager.getInstance().getTeleTotalAH());
//        data.inAH_by_charger = roundTo3Decimal(TelemetryDataManager.getInstance().getTeleChargerAH());
//        data.inAH_by_regen = roundTo3Decimal(TelemetryDataManager.getInstance().getTeleRegenAH());
//        data.outAH = roundTo3Decimal(TelemetryDataManager.getInstance().getTeleDischargeAH());
//        data.ev_power_state = (shdtelemetrywhenunlocked ? "ON" : "OFF") + "," + UptimeManager.getUptime();
//        data.currentRider = shdtelemetrywhenunlocked ? TelemetryDataManager.getInstance().getTeleCurrentRider() : "Scooter Locked";
//        data.tirepressure = TelemetryDataManager.getInstance().getTeleTirePressure();
//        Log.d("sdafd","TELETIREPRESUURE2222 IS "+data.tirepressure);
//
//        data.soc = TelemetryDataManager.getInstance().getTeleSOC();
//        data.apuSoc = TelemetryDataManager.getInstance().getTeleAPUSOC();
//        data.lat_long = TelemetryDataManager.getInstance().getTeleLatLong();
//        if (data.lat_long == null || data.lat_long.isEmpty()) {
//            data.lat_long = "0.0,0.0";   // fallback if GPS not ready
//        }
//        data.time = java.time.OffsetDateTime.now().toString();
//        //int mcuVersion = getEffectiveMcuVersion();
//        data.data = "maps="+Maps.getRequestStatus(context) +",FrontTPMSVoltage="+ TelemetryDataManager.getInstance().getFTeleTirePressureVoltage()+",RearTPMSVoltage="+ TelemetryDataManager.getInstance().getRTeleTirePressureVoltage();
//
//        return data;
//    }
//
//    private void sendToServer(RealTimeData data) {
//        //Log.d("TelemetryService", "12311111111111111111111");
//
//        apiService.saveRealTimeData(data).enqueue(new Callback<ResponseBody>() {
//            @Override
//            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
//                if (response.isSuccessful()) {
//                    //Log.i("TelemetryService", "✅ RealTimeData sent successfully: " + response.code());
//                } else {
//                    Log.e("TelemetryService", "❌ RealTimeData failed with code: " + response.code());
//                    try {
//                        Log.e("TelemetryService", "Error body: " + response.errorBody().string());
//                    } catch (Exception ignored) {}
//                }
//            }
//
//            @Override
//            public void onFailure(Call<ResponseBody> call, Throwable t) {
//                // If sending fails due to network, re-stage
//                TelemetryStagingManager.getInstance().addData(data);
//                Log.e("TelemetryService", "Failed to send data, staging", t);
//            }
//        });
//    }
//
//    private boolean isInternetAvailable() {
//        try {
//            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
//            if (cm == null) return false;
//            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
//                Network n = cm.getActiveNetwork();
//                if (n == null) return false;
//                NetworkCapabilities nc = cm.getNetworkCapabilities(n);
//                return nc != null && (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
//                        nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
//                        nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
//            } else {
//                NetworkInfo ni = cm.getActiveNetworkInfo();
//                return ni != null && ni.isConnected();
//            }
//        } catch (Exception e) {
//            return false;
//        }
//    }
//
//    /** Checks if there’s any active network connection (Wi-Fi / Mobile) */
//    private boolean isNetworkConnected() {
//        try {
//            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
//            if (cm == null) return false;
//            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
//                Network n = cm.getActiveNetwork();
//                if (n == null) return false;
//                NetworkCapabilities nc = cm.getNetworkCapabilities(n);
//                return nc != null && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
//            } else {
//                NetworkInfo ni = cm.getActiveNetworkInfo();
//                return ni != null && ni.isConnected();
//            }
//        } catch (Exception e) {
//            return false;
//        }
//    }
//
//    /** Actually tests if Internet is reachable (for cloud endpoints) */
//    private boolean hasActiveInternetConnection() {
//        try {
//            Socket socket = new Socket();
//            socket.connect(new InetSocketAddress("8.8.8.8", 53), 1500);
//            socket.close();
//            return true;
//        } catch (Exception e) {
//            return false;
//        }
//    }
//
//    private double roundTo3Decimal(double value) {
//        return Math.round(value * 1000.0) / 1000.0;
//    }
//
//    private double roundTo2Decimal(double value) {
//        return Math.round(value * 100.0) / 100.0;
//    }
//    private double roundTo1Decimal(double value) {
//        return Math.round(value * 10.0) / 10.0;
//    }
//
//    @SuppressLint("MissingPermission")
//    private void startLocationUpdates(Context context) {
//
//        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
//
//        if (locationManager == null) {
//           // Log.e("TelemetryService", "LocationManager is null");
//            return;
//        }
//
//        // Request location updates every 1 second or 1 meter
//        locationManager.requestLocationUpdates(
//                LocationManager.GPS_PROVIDER,
//                1000,   // 1000 ms = 1 second
//                1,      // 1 meter
//                new LocationListener() {
//                    @Override
//                    public void onLocationChanged(@NonNull Location location) {
//                        String latlong = String.format(Locale.US, "%.6f, %.6f",
//                                location.getLatitude(), location.getLongitude());
//
//                        // Save in your TelemetryDataManager so telemetry loop can send it
//                        TelemetryDataManager.getInstance().setTeleLatLong(latlong);
//                        //Log.d("TelemetryService", "Updated location = " + latlong);
//                    }
//
//                    @Override
//                    public void onProviderDisabled(@NonNull String provider) {
//                        //Log.w("TelemetryService", "Provider disabled: " + provider);
//                    }
//
//                    @Override
//                    public void onProviderEnabled(@NonNull String provider) {
//                        //Log.d("TelemetryService", "Provider enabled: " + provider);
//                    }
//
//                    @Override
//                    public void onStatusChanged(String provider, int status, Bundle extras) {
//                        //Log.d("TelemetryService", "Status changed: " + provider + " status=" + status);
//                    }
//                }
//        );
//    }
//
//    @Override
//    public void onDestroy() {
//        isSending = false;
//        if (telemetryHandler != null) {
//            telemetryHandler.removeCallbacksAndMessages(null);
//        }
//        if (telemetryThread != null) {
//            telemetryThread.quitSafely();
//        }
//        super.onDestroy();
//    }
//
//    private int getLastSavedMcuVersion() {
//        return getSharedPreferences(PREFS_OTA, MODE_PRIVATE)
//                .getInt(KEY_LAST_MCU_VERSION, 0);
//    }
//
//    private int getEffectiveMcuVersion() {
//        int liveVersion = 0;
//
//        try {
//            if (serialPortManager != null && serialPortManager.mad != null) {
//                liveVersion = serialPortManager.mad.getMcuVersions();
//            }
//        } catch (Exception e) {
//            Log.e("OTA", "MCU version read failed", e);
//        }
//
//        if (liveVersion > 0) {
//            // optional: store again if you want
//            getSharedPreferences(PREFS_OTA, MODE_PRIVATE)
//                    .edit()
//                    .putInt(KEY_LAST_MCU_VERSION, liveVersion)
//                    .apply();
//
//            Log.d("OTA", "Telemetry using LIVE MCU version: " + liveVersion);
//            return liveVersion;
//        }
//
//        int stored = getLastSavedMcuVersion();
//        Log.w("OTA", "Telemetry using STORED MCU version: " + stored);
//        return stored;
//    }
//
//    @Nullable
//    @Override
//    public IBinder onBind(Intent intent) { return null; }
//}


//Below is for latest RIVOT one for datat sharing using websocket and above is for legacy logic

// BELOW IS FOR SENDING TELEMTRY DATA USING WEBSOCKET

package com.fanconn.rideOS;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fanconn.rideOS.network.EVWebSocketClient;
import com.fanconn.rideOS.network.RetrofitClient;
import com.fanconn.rideOS.network.VehicleData;
import com.fanconn.rideOS.network.ApiService;

import com.fanconn.rideOS.util.NumberUtils;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.fanconn.rideOS.network.RideOSTelemetrySender;
import org.json.JSONObject;

public class TelemetryService extends Service {


    private HandlerThread telemetryThread;
    private Handler telemetryHandler;
    private boolean isSending = false;
    private EVWebSocketClient evClient;
    private ApiService apiService;
    private Context context;
    // Telemetry flags
    public static boolean shdtelemetrywhenunlocked;
    public static int sendRealtimeDataOnceEvenAfterLocking = 0;
    private SerialPortManager serialPortManager;
    private Heartbeat1 hb1;
    private static final String PREFS_OTA = "ota_prefs";
    private static final String KEY_LAST_MCU_VERSION = "last_mcu_version";
    private SharedPreferences vehiclePrefs;
    private SharedPreferences realtimePrefs;
    @Override
    public void onCreate() {
        super.onCreate();
        telemetryThread = new HandlerThread("TelemetryThread");
        telemetryThread.start();
        telemetryHandler = new Handler(telemetryThread.getLooper());

//        ////Stagging controller>
//        StagedTelemetryController
//                .getInstance(getApplicationContext())
//                .initialize(stagedUploadRunnable);

        vehiclePrefs =
                getSharedPreferences("vehicle_delta", MODE_PRIVATE);

        realtimePrefs =
                getSharedPreferences("realtime_delta", MODE_PRIVATE);
    }
    private final Runnable stagedUploadRunnable = new Runnable() {

        @Override
        public void run() {

            StagedTelemetryController controller =
                    StagedTelemetryController.getInstance(getApplicationContext());

            if (!controller.shouldContinue()) {
                controller.finished();
                return;
            }

            if (!hasActiveInternetConnection()) {
                telemetryHandler.postDelayed(this, 3000);
                return;
            }

            TelemetryStagingManager manager =
                    TelemetryStagingManager.getInstance(getApplicationContext());

            if (!manager.hasData()) {
                controller.finished();
                return;
            }

            java.util.List<RealTimeData> batch = manager.pollBatch();

            for (RealTimeData data : batch) {

                sendToServer(data);

                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
            }
//            Log.d("STAGE COUNT","REALTIME_JSON STAGE COUNT = "+ StagedTelemetryController.getInstance(context).getPendingPayloadCount());
            telemetryHandler.postDelayed(this, 300);
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String vinNumber = intent != null ? intent.getStringExtra("VIN_NUMBER") : null;
        this.context = this;
        startTelemetry(vinNumber != null ? vinNumber : "UNKNOWN");
        return START_STICKY;
    }

    private void startTelemetry(String VINNUMBER) {
        evClient = new EVWebSocketClient(VINNUMBER, URI.create("ws://sb.rivotmotors.com:8081/ws/ev"));
//        evClient = new EVWebSocketClient(VINNUMBER, URI.create("ws://103.174.148.224:8081/ws/ev"));
//        evClient = new EVWebSocketClient(VINNUMBER, URI.create("ws://192.168.1.119:8081/ws/ev"));

        evClient.connect();

        apiService = RetrofitClient.getClient().create(ApiService.class);
        isSending = true;
        sendDataContinuously(VINNUMBER);
        startLocationUpdates(this);


        new Thread(() -> {
            while (evClient == null || !evClient.isOpen()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    //Log.e("Telemetry", "Thread interrupted", e);
                    return;
                }
            }
            serialPortManager = serialApplication.getSerialPortManager();
            if (serialPortManager != null) {
                serialPortManager.sendContinuousSensorData();
            } else {
                //Log.e("Telemetry", "serialPortManager is null!");
            }
        }).start();
    }
    private boolean firstVehicleUpload = true;
    private void sendDataContinuously(String vin) {
        String rideOsVersion = buildRideOSVersion(context);

        telemetryHandler.removeCallbacksAndMessages(null); // prevent duplicate loops
        telemetryHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isSending) {

//                    Log.d("TelemetryService", "Loop latlong = "
//                            + TelemetryDataManager.getInstance().getTeleLatLong());

                    if(serialApplication.getSerialPortManager().getHb1()!=null)
                    {
                        hb1= serialApplication.getSerialPortManager().getHb1();
                    }
                    if(hb1!=null) {
                        TelemetryDataManager.getInstance().setTeleHandleLockState(hb1.getDragonLock());
                        TelemetryDataManager.getInstance().setTeleSeatLockState(hb1.getSeatLock());
                    }

                    sendVehicleData(context,  NumberUtils.round(TelemetryDataManager.getInstance().getTeleODO(),3), TelemetryDataManager.getInstance().getTeleChargingState(), TelemetryDataManager.getInstance().getTeleHandleLockState(),
                            TelemetryDataManager.getInstance().getTeleSeatLockState(), TelemetryDataManager.getInstance().getTeleBMSMosStates(), TelemetryDataManager.getInstance().getTeleCurrentRider(),
                            22, TelemetryDataManager.getInstance().getTeleLatLong());
                    TelemetryDataManager.getInstance()
                            .setTeleEvPowerState(shdtelemetrywhenunlocked ? 1 : 0);
//                    RideOSTelemetrySender.send(vin);

              //This start sending RIVOT One app data seperately
               //     RideOSTelemetrySender.send(vin, rideOsVersion);

                    Log.d("STAGE COUNT","REALTIME_JSON STAGE COUNT SEND CONTINOS = "+ StagedTelemetryController.getInstance(context).getPendingPayloadCount());
                    TelemetryDataManager.getInstance().setTeleStaggingCount(StagedTelemetryController.getInstance(context).getPendingPayloadCount());

                    sendRealTimeData(vin);
//                    if (shdtelemetrywhenunlocked) {
//                        sendRealTimeData(vin);
//                        sendRealtimeDataOnceEvenAfterLocking = 0;
//                    } else if (sendRealtimeDataOnceEvenAfterLocking == 0) {
//                        sendRealTimeData(vin);
//                        sendRealtimeDataOnceEvenAfterLocking++;
//                    }
                    telemetryHandler.postDelayed(this, 3000);
                }
            }
        }, 3000);
    }

    private void sendVehicleData(Context context, double odo, int chargingState,
                                 int handleLockState, int seatLockState, String bmsMosStates,
                                 String riders, int bmslifecycles, String lat_long) {

        MetadataManager metadata = MetadataManager.get();
        String vinNumber = metadata.getVin();

        if (vinNumber.contains("UNKNOWN")) {
//            Log.w("Telemetry", "VIN unknown — skipping sendVehicleData");
            return;
        }
        if (lat_long == null || lat_long.isEmpty()) {
            lat_long = "0.0,0.0";   // fallback value if GPS not ready
        }

        String ownerName = metadata.getOwner();
        String phoneNumber = metadata.getPhone();
        String controllerID = metadata.getControllerId();
        String motorID = metadata.getMotorId();
        String bmsID = metadata.getBmsId();
        String chargerID = metadata.getCharger();
        String smartKeyID = metadata.getSmartKeyId();
        String model = metadata.getModel();

        String simInfo;
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            simInfo = telephonyManager.getSimOperatorName()+"-"+telephonyManager.getSimSerialNumber();
        }
        else {
            simInfo=null;
        }

        String rideOSversion;
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            int mcuVersion = getEffectiveMcuVersion();
            rideOSversion = pInfo.versionName+";"+Build.DISPLAY+";"+mcuVersion+";"+simInfo;
        } catch (PackageManager.NameNotFoundException e) {
            rideOSversion = "rideOS N/A";
        }

        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        SharedPreferences prefs = context.getSharedPreferences("last_uploaded_metadata", Context.MODE_PRIVATE);

        boolean anyChanged =
                !vinNumber.equals(prefs.getString("vin", "")) ||
                        !ownerName.equals(prefs.getString("owner", "")) ||
                        !phoneNumber.equals(prefs.getString("phone", "")) ||
                        !controllerID.equals(prefs.getString("controller", "")) ||
                        !motorID.equals(prefs.getString("motor", "")) ||
                        !bmsID.equals(prefs.getString("bms", "")) ||
                        !chargerID.equals(prefs.getString("charger", "")) ||
                        !smartKeyID.equals(prefs.getString("smartKey", "")) ||
                        !model.equals(prefs.getString("model", ""));

//        VehicleData data = anyChanged ?
//                new VehicleData(vinNumber, model, ownerName, phoneNumber, controllerID,
//                        motorID, bmsID, chargerID, rideOSversion, smartKeyID,
//                        odo, chargingState, handleLockState, seatLockState,
//                        bmsMosStates, riders, bmslifecycles, lat_long, time)
//
//                :
//                new VehicleData(vinNumber, null, null, null, null,
//                        null, null, null, rideOSversion, null,
//                        odo, chargingState, handleLockState, seatLockState,
//                        bmsMosStates, riders, bmslifecycles, lat_long, time);

        //Always sned complete data in first payload
        RealtimeDeltaManager delta =
                RealtimeDeltaManager.getInstance();

        VehicleData data;

        if (firstVehicleUpload) {

            firstVehicleUpload = false;

            data = new VehicleData(
                    vinNumber,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    rideOSversion,
                    null,
                    odo,
                    chargingState,
                    handleLockState,
                    seatLockState,
                    bmsMosStates,
                    riders,
                    bmslifecycles,
                    lat_long,
                    time
            );
        }
        else {

            data = new VehicleData(

                    vinNumber,

                    getChangedString(vehiclePrefs,
                            "model",
                            model),

                    getChangedString(vehiclePrefs,
                            "owner",
                            ownerName),

                    getChangedString(vehiclePrefs,
                            "phone",
                            phoneNumber),

                    getChangedString(vehiclePrefs,
                            "controller",
                            controllerID),

                    getChangedString(vehiclePrefs,
                            "motor",
                            motorID),

                    getChangedString(vehiclePrefs,
                            "bms",
                            bmsID),

                    getChangedString(vehiclePrefs,
                            "charger",
                            chargerID),

                    getChangedString(vehiclePrefs,
                            "rideos",
                            rideOSversion),

                    getChangedString(vehiclePrefs,
                            "smartkey",
                            smartKeyID),

                    delta.changedDouble(
                            "vehicle_odo",
                            odo
                    ),

                    delta.changedInt(
                            "vehicle_charging_state",
                            chargingState
                    ),

                    delta.changedInt(
                            "vehicle_handle_lock",
                            handleLockState
                    ),

                    delta.changedInt(
                            "vehicle_seat_lock",
                            seatLockState
                    ),

                    delta.changed(
                            "vehicle_bmsmos",
                            bmsMosStates
                    ),

                    delta.changed(
                            "vehicle_rider",
                            riders
                    ),

                    delta.changedInt(
                            "vehicle_bms_cycles",
                            bmslifecycles
                    ),

                    delta.changed(
                            "vehicle_latlong",
                            lat_long
                    ),

                    time
            );
        }

        //Dont send complete data in frist payload
//        RealtimeDeltaManager delta =
//                RealtimeDeltaManager.getInstance();
//
//        VehicleData data =
//                new VehicleData(
//
//                        vinNumber,
//
//                        getChangedString(vehiclePrefs,
//                                "model",
//                                model),
//
//                        getChangedString(vehiclePrefs,
//                                "owner",
//                                ownerName),
//
//                        getChangedString(vehiclePrefs,
//                                "phone",
//                                phoneNumber),
//
//                        getChangedString(vehiclePrefs,
//                                "controller",
//                                controllerID),
//
//                        getChangedString(vehiclePrefs,
//                                "motor",
//                                motorID),
//
//                        getChangedString(vehiclePrefs,
//                                "bms",
//                                bmsID),
//
//                        getChangedString(vehiclePrefs,
//                                "charger",
//                                chargerID),
//
//                        getChangedString(vehiclePrefs,
//                                "rideos",
//                                rideOSversion),
//
//                        getChangedString(vehiclePrefs,
//                                "smartkey",
//                                smartKeyID),
//
//                        delta.changedDouble(
//                                "vehicle_odo",
//                                odo
//                        ),
//
//                        delta.changedInt(
//                                "vehicle_charging_state",
//                                chargingState
//                        ),
//
//                        delta.changedInt(
//                                "vehicle_handle_lock",
//                                handleLockState
//                        ),
//
//                        delta.changedInt(
//                                "vehicle_seat_lock",
//                                seatLockState
//                        ),
//
//                        delta.changed(
//                                "vehicle_bmsmos",
//                                bmsMosStates
//                        ),
//
//                        delta.changed(
//                                "vehicle_rider",
//                                riders
//                        ),
//
//                        delta.changedInt(
//                                "vehicle_bms_cycles",
//                                bmslifecycles
//                        ),
//
//                        delta.changed(
//                                "vehicle_latlong",
//                                lat_long
//                        ),
//
//                        time
//                );
        //Log.d("TelemetryService", "Sending telemetry: " + TelemetryDataManager.getInstance().getTeleLatLong());

        if (anyChanged) {
            prefs.edit()
                    .putString("owner", ownerName)
                    .putString("phone", phoneNumber)
                    .putString("controller", controllerID)
                    .putString("motor", motorID)
                    .putString("bms", bmsID)
                    .putString("charger", chargerID)
                    .putString("smartKey", smartKeyID)
                    .putString("model", model)
                    .apply();
        }
//        String json = new Gson().toJson(data);
//        //Log.e("VEHICLE_JSON", json);
//        apiService.saveVehicleData(data).enqueue(new Callback<ResponseBody>() {
//            @Override
//            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) { }
//            @Override
//            public void onFailure(Call<ResponseBody> call, Throwable t) {
////                Log.e("Telemetry", "Faileddd to send vehicle data", t);
//            }
//        });

        try {

            JSONObject msg = new JSONObject();

            msg.put("type", "vehicle");
            msg.put("vin", vinNumber);

            // VehicleData object becomes payload
            msg.put("payload",
                    new JSONObject(new Gson().toJson(data)));

            if (evClient != null && evClient.isConnected()) {

                evClient.send(msg.toString());

                Log.d("WS", "Vehicle data sent via WebSocket");

            } else {

                Log.e("WS", "WebSocket not connected");

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private String getChangedString(
            SharedPreferences prefs,
            String key,
            String newValue) {

        String old =
                prefs.getString(key, null);

        if (Objects.equals(old, newValue)) {
            return null;
        }

        prefs.edit()
                .putString(key, newValue)
                .apply();
//        Log.e("DELTA",
//                key +
//                        " old=" + old +
//                        " new=" + newValue);
        return newValue;
    }
    private void sendRealTimeData(String vin) {
        RealTimeData data = collectTelemetryData(vin);

        if (evClient != null && evClient.isConnected()) {
            // Send staged data first
            // NEW SAFE BATCH SENDING
//            if (TelemetryStagingManager.getInstance().hasData()) {
//                new Thread(() -> {
//                    while (TelemetryStagingManager.getInstance().hasData() && hasActiveInternetConnection()) {
//
//                        List<RealTimeData> batch = TelemetryStagingManager.getInstance().pollBatch();  // Take 20 at once
//                        for (RealTimeData r : batch) {
//                            sendToServer(r);
//                            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
//                        }
//
//                        try { Thread.sleep(300); } catch (Exception ignored) {} // Small break between batches
//                    }
//                }).start();
//            }

            //START SENDNG STAGGED DATA AS SOON AS INTERNET IS AVALBALE
            if (TelemetryStagingManager.getInstance(getApplicationContext()).hasData()) {
                flushStagedData();
            }
            //==end========START SENDNG STAGGED DATA AS SOON AS INTERNET IS AVALBALE

            // Send current data
            //DataUsageTracker tracker = new DataUsageTracker();
            //tracker.startTracking();

            sendToServer(data);
            //Log.e("Telemetry", "INTENET VAIL SNEDNING ");

            //tracker.logUsage("Currdent Payload");

        } else {
//            Log.e("Telemetry", "NOOINTENET VAIL SNEDNING ");
            TelemetryStagingManager.getInstance(getApplicationContext()).addData(data);
        }
    }
    private boolean isFlushing = false;
    private void flushStagedData() {
        if (isFlushing) return;
        isFlushing = true;

        telemetryHandler.post(() -> {
            try {
                if (evClient == null || !evClient.isConnected()) {
                    return;
                }

                List<RealTimeData> batch =
                        TelemetryStagingManager
                                .getInstance(getApplicationContext())
                                .pollBatch();

                for (RealTimeData r : batch) {
                    sendToServer(r);
                    try {
                        Thread.sleep(100); // smooth throttle
                    } catch (Exception ignored) {}
                }

            } finally {
                isFlushing = false;
            }
        });
    }

    private RealTimeData collectTelemetryData(String vin) {
        RealTimeData data = new RealTimeData();
        data.vinNumber = vin;
        data.speed_kmph = TelemetryDataManager.getInstance().getTeleSpeed();
        data.tripkm= roundTo2Decimal(TelemetryDataManager.getInstance().getTeleTrip());
        data.controllerMosTemp = TelemetryDataManager.getInstance().getTeleControllerMosTemp();
        data.motorTemp = TelemetryDataManager.getInstance().getTeleMotorTemp();
        data.bmsMosTemp = TelemetryDataManager.getInstance().getTeleBMSMosTemp();
        if(SerialPortManager.getInstance().getHb3()!=null){
            data.ntc = TelemetryDataManager.getInstance().getTeleNTC();
     //   Log.d("dsafgs","ddddddddddddddd "+data.ntc);
        }else{
            //Log.d("dsafgs","ddddddddddddddddeeeeeeeeeeeeeeeeeeeeeedddddddd "+data.ntc);
        }

        data.currentconsumption = roundTo3Decimal(TelemetryDataManager.getInstance().getTeleCurrentConsumption());
        data.batvoltage = roundTo1Decimal(TelemetryDataManager.getInstance().getTeleBatVoltage());
        data.remainingCapacity_AH = roundTo3Decimal(TelemetryDataManager.getInstance().getTeleRemainingCapacityAH());
        data.inAH = roundTo3Decimal(TelemetryDataManager.getInstance().getTeleTotalAH());
        data.inAH_by_charger = roundTo3Decimal(TelemetryDataManager.getInstance().getTeleChargerAH());
        data.inAH_by_regen = roundTo3Decimal(TelemetryDataManager.getInstance().getTeleRegenAH());
        data.outAH = roundTo3Decimal(TelemetryDataManager.getInstance().getTeleDischargeAH());
        data.ev_power_state = (shdtelemetrywhenunlocked ? "ON" : "OFF");
        data.currentRider = shdtelemetrywhenunlocked ? TelemetryDataManager.getInstance().getTeleCurrentRider() : "Scooter Locked";
        data.tirepressure = TelemetryDataManager.getInstance().getTeleTirePressure();

        data.soc = NumberUtils.truncate1(TelemetryDataManager.getInstance().getTeleSOC());
        data.apuSoc = NumberUtils.truncate1(TelemetryDataManager.getInstance().getTeleAPUSOC());
        data.lat_long = TelemetryDataManager.getInstance().getTeleLatLong();
        if (data.lat_long == null || data.lat_long.isEmpty()) {
            data.lat_long = "0.0,0.0";   // fallback if GPS not ready
        }
        data.time = java.time.OffsetDateTime.now().toString();
        //int mcuVersion = getEffectiveMcuVersion();
        int temps = getCriticalTemps();
        UIUpdater uiUpdater = UIUpdater.getInstance();
        data.data = (TelemetryDataManager.getInstance().isGpsTelemetryEnabled() ? 1 : 0) +","+TelemetryDataManager.getInstance().getTeleStaggingCount();

//        if (uiUpdater!=null) {
//            data.data = TelemetryDataManager.getInstance().getTeleRange()+";"+TelemetryDataManager.getInstance().getBMSTeleSOC()+";"+TelemetryDataManager.getInstance().getBMSTeleRemainingCapacityAH()+
//                    ";"+uiUpdater.getrecoEngineParams()+";"+TelemetryDataManager.getInstance().getMax_voltage_value()+";"+TelemetryDataManager.getInstance().getAlowable_charging_current_value();
//        }else{
//            data.data = TelemetryDataManager.getInstance().getTeleRange()+";"+TelemetryDataManager.getInstance().getBMSTeleSOC()+";"+TelemetryDataManager.getInstance().getBMSTeleRemainingCapacityAH()+";"+temps
//                    +";N/A"+";"+TelemetryDataManager.getInstance().getMax_voltage_value()+";"+TelemetryDataManager.getInstance().getAlowable_charging_current_value();
//        }

        //===============

        data.bmsmos =
                TelemetryDataManager.getInstance().getTeleBMSMosStates();

        data.chargingstatus =
                TelemetryDataManager.getInstance().getTeleChargingState()+","+TelemetryDataManager.getInstance().getTeleChargingCurrentValue()+","+ETADataManager.getInstance().getEtaTime();

        data.singleminmaxvoltage =
                TelemetryDataManager.getInstance().getteleIndividualCellVoltages();

        data.apuvoltage =
                (double) TelemetryDataManager.getInstance().getTeleAPUBatVoltage();

        data.uptime =
                UptimeManager.getUptime();

        data.gear =
                String.valueOf(MainActivity.currentgear);

        data.range =
                NumberUtils.truncate1(TelemetryDataManager.getInstance().getTeleRange());

        data.bmssoc =
                NumberUtils.truncate1( TelemetryDataManager.getInstance().getBMSTeleSOC());

        data.bmsremainingcap =
                NumberUtils.truncate1( TelemetryDataManager.getInstance().getBMSTeleRemainingCapacityAH());

        try {
            data.vcutemp = (double)getCriticalTemps();
        } catch (NumberFormatException e) {
            data.vcutemp = 0.0; // or some default value
        }

        if (uiUpdater!=null) {
            data.recoengine =
                    uiUpdater.getrecoEngineParams();
        }
        data.maxtotalvoltage =
                (double) TelemetryDataManager.getInstance().getMax_voltage_value();

        data.allowablechargingcurrent =
                (double) TelemetryDataManager.getInstance().getAlowable_charging_current_value();

        data.cell1to3 =
                SerialPortManager.getInstance().getHb3().getcell1TO3();

        data.cell4to6 =
                SerialPortManager.getInstance().getHb3().getcell4TO6();

        data.cell7to9 =
                SerialPortManager.getInstance().getHb3().getcell7TO9();

        data.cell10to12 =
                SerialPortManager.getInstance().getHb3().getcell10TO12();

        data.cell13to15 =
                SerialPortManager.getInstance().getHb3().getcell13TO15();

        data.cell16to18 =
                SerialPortManager.getInstance().getHb3().getcell16TO18();

        data.cell19to21 =
                SerialPortManager.getInstance().getHb3().getcell19TO21();

        data.cell22to23 =
                SerialPortManager.getInstance().getHb3().getcell22TO23();
        //Log.d("QUALCOMTHERMAL  ", temps);
        return data;
    }

//    private void sendToServer(RealTimeData data) {
//
//        RealTimeDataApi apiData = new RealTimeDataApi();
//
//        apiData.vinNumber = data.vinNumber;
//        apiData.speed_kmph = data.speed_kmph;
//        apiData.tripkm = data.tripkm;
//        apiData.controllerMosTemp = data.controllerMosTemp;
//        apiData.motorTemp = data.motorTemp;
//        apiData.bmsMosTemp = data.bmsMosTemp;
//        apiData.ntc = data.ntc;
//        apiData.currentconsumption = data.currentconsumption;
//        apiData.batvoltage = data.batvoltage;
//        apiData.remainingCapacity_AH = data.remainingCapacity_AH;
//        apiData.inAH = data.inAH;
//        apiData.inAH_by_charger = data.inAH_by_charger;
//        apiData.inAH_by_regen = data.inAH_by_regen;
//        apiData.outAH = data.outAH;
//        apiData.ev_power_state = data.ev_power_state;
//        apiData.currentRider = data.currentRider;
//        apiData.tirepressure = data.tirepressure;
//        apiData.soc = data.soc;
//        apiData.apuSoc = data.apuSoc;
//        apiData.lat_long = data.lat_long;
//        apiData.time = data.time;
//        apiData.data = data.data;
//
//        apiService.saveRealTimeData(apiData).enqueue(new Callback<ResponseBody>() {
//            @Override
//            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
//                if (!response.isSuccessful()) {
//                   // Log.e("TelemetryService", "❌ RealTimeData failed: " + response.code());
//                }
//            }
//
//            @Override
//            public void onFailure(Call<ResponseBody> call, Throwable t) {
//                TelemetryStagingManager
//                        .getInstance(getApplicationContext())
//                        .addData(data);
//
//            //    Log.e("TelemetryService", "Failed, re-staged", t);
//            }
//        });
//    }
private void sendToServer(RealTimeData data) {

    RealTimeDataApi apiData = new RealTimeDataApi();

    RealtimeDeltaManager delta =
            RealtimeDeltaManager.getInstance();

    // Always send these
    apiData.vinNumber =  data.vinNumber;
    apiData.time = data.time;
    apiData.ev_power_state =  delta.changed("ev_power_state",data.ev_power_state);

    // Delta fields
    apiData.speed_kmph =
            delta.changedDouble(
                    "speed_kmph",
                    data.speed_kmph);

    apiData.tripkm =
            delta.changedDouble(
                    "tripkm",
                    data.tripkm);

    apiData.controllerMosTemp =
            delta.changedDouble(
                    "controllerMosTemp",
                    data.controllerMosTemp);

    apiData.motorTemp =
            delta.changedDouble(
                    "motorTemp",
                    data.motorTemp);

    apiData.bmsMosTemp =
            delta.changedDouble(
                    "bmsMosTemp",
                    data.bmsMosTemp);

    apiData.ntc =
            delta.changed(
                    "ntc",
                    data.ntc);

    apiData.currentconsumption =
            delta.changedDouble(
                    "currentconsumption",
                    data.currentconsumption);

    apiData.batvoltage =
            delta.changedDouble(
                    "batvoltage",
                    data.batvoltage);

    apiData.remainingCapacity_AH =
            delta.changedDouble(
                    "remainingCapacity_AH",
                    data.remainingCapacity_AH);

    apiData.inAH =
            delta.changedDouble(
                    "inAH",
                    data.inAH);

    apiData.inAH_by_charger =
            delta.changedDouble(
                    "inAH_by_charger",
                    data.inAH_by_charger);

    apiData.inAH_by_regen =
            delta.changedDouble(
                    "inAH_by_regen",
                    data.inAH_by_regen);

    apiData.outAH =
            delta.changedDouble(
                    "outAH",
                    data.outAH);

    apiData.currentRider =
            delta.changed(
                    "currentRider",
                    data.currentRider);

    apiData.tirepressure =
            delta.changed(
                    "tirepressure",
                    data.tirepressure);

    apiData.soc =
            delta.changedDouble(
                    "soc",
                    data.soc);

    apiData.apuSoc =
            delta.changedDouble(
                    "apuSoc",
                    data.apuSoc);

    apiData.lat_long =
            delta.changed(
                    "lat_long",
                    data.lat_long);

    apiData.data =
            delta.changed(
                    "data",
                    data.data);

    apiData.bmsmos =
            delta.changed("bmsmos", data.bmsmos);

    apiData.chargingstatus =
            delta.changed("chargingstatus", data.chargingstatus);

    apiData.singleminmaxvoltage =
            delta.changed("singleminmaxvoltage",
                    data.singleminmaxvoltage);

    apiData.apuvoltage =
            delta.changedDouble("apuvoltage",
                    data.apuvoltage);

    apiData.uptime =
            delta.changed("uptime", data.uptime);

    apiData.gear =
            delta.changed("gear", data.gear);

    apiData.range =
            delta.changedDouble("range",
                    data.range);

    apiData.bmssoc =
            delta.changedDouble("bmssoc",
                    data.bmssoc);

    apiData.bmsremainingcap =
            delta.changedDouble("bmsremainingcap",
                    data.bmsremainingcap);

    apiData.vcutemp =
            delta.changedDouble("vcutemp",
                    data.vcutemp);

    apiData.recoengine =
            delta.changed("recoengine",
                    data.recoengine);

    apiData.maxtotalvoltage =
            delta.changedDouble("maxtotalvoltage",
                    data.maxtotalvoltage);

    apiData.allowablechargingcurrent =
            delta.changedDouble("allowablechargingcurrent",
                    data.allowablechargingcurrent);

    apiData.cell1to3 =
            delta.changed("cell1to3",
                    data.cell1to3);

    apiData.cell4to6 =
            delta.changed("cell4to6",
                    data.cell4to6);

    apiData.cell7to9 =
            delta.changed("cell7to9",
                    data.cell7to9);

    apiData.cell10to12 =
            delta.changed("cell10to12",
                    data.cell10to12);

    apiData.cell13to15 =
            delta.changed("cell13to15",
                    data.cell13to15);

    apiData.cell16to18 =
            delta.changed("cell16to18",
                    data.cell16to18);

    apiData.cell19to21 =
            delta.changed("cell19to21",
                    data.cell19to21);

    apiData.cell22to23 =
            delta.changed("cell22to23",
                    data.cell22to23);

//    Log.e(
//            "REALTIME_JSON",
//            new Gson().toJson(apiData)
//    );
//    apiService.saveRealTimeData(apiData)
//            .enqueue(new Callback<ResponseBody>() {
//
//                @Override
//                public void onResponse(
//                        Call<ResponseBody> call,
//                        Response<ResponseBody> response) {
//
//                    if (!response.isSuccessful()) {
//                        // Log.e("TelemetryService",
//                        //      "❌ RealTimeData failed: "
//                        //      + response.code());
//                    }
//                }
//
//                @Override
//                public void onFailure(
//                        Call<ResponseBody> call,
//                        Throwable t) {
//
//                    TelemetryStagingManager
//                            .getInstance(getApplicationContext())
//                            .addData(data);
//
//                    // Log.e("TelemetryService",
//                    //      "Failed, re-staged", t);
//                }
//            });

    boolean sent = evClient != null &&
            evClient.sendTelemetry(apiData);

    if (!sent) {

        TelemetryStagingManager
                .getInstance(getApplicationContext())
                .addData(data);
    }
    }

    private boolean isInternetAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                Network n = cm.getActiveNetwork();
                if (n == null) return false;
                NetworkCapabilities nc = cm.getNetworkCapabilities(n);
                return nc != null && (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
            } else {
                NetworkInfo ni = cm.getActiveNetworkInfo();
                return ni != null && ni.isConnected();
            }
        } catch (Exception e) {
            return false;
        }
    }

    /** Checks if there’s any active network connection (Wi-Fi / Mobile) */
    private boolean isNetworkConnected() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                Network n = cm.getActiveNetwork();
                if (n == null) return false;
                NetworkCapabilities nc = cm.getNetworkCapabilities(n);
                return nc != null && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            } else {
                NetworkInfo ni = cm.getActiveNetworkInfo();
                return ni != null && ni.isConnected();
            }
        } catch (Exception e) {
            return false;
        }
    }

    /** Actually tests if Internet is reachable (for cloud endpoints) */
    private boolean hasActiveInternetConnection() {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress("8.8.8.8", 53), 1500);
            socket.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private double roundTo3Decimal(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private double roundTo2Decimal(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
    private double roundTo1Decimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates(Context context) {

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        if (locationManager == null) {
            // Log.e("TelemetryService", "LocationManager is null");
            return;
        }

        // Request location updates every 1 second or 1 meter
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000,   // 1000 ms = 1 second
                1,      // 1 meter
                new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        String latlong = String.format(Locale.US, "%.6f, %.6f",
                                location.getLatitude(), location.getLongitude());

                        // Save in your TelemetryDataManager so telemetry loop can send it
                        TelemetryDataManager.getInstance().setTeleLatLong(latlong);
                     //   Log.d("TelemetryService", "Updated location = " + latlong);
                    }

                    @Override
                    public void onProviderDisabled(@NonNull String provider) {
                      //  Log.w("TelemetryService", "Provider disabled: " + provider);
                    }

                    @Override
                    public void onProviderEnabled(@NonNull String provider) {
                      //  Log.d("TelemetryService", "Provider enabled: " + provider);
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {
                      //  Log.d("TelemetryService", "Status changed: " + provider + " status=" + status);
                    }
                }
        );
    }

    @Override
    public void onDestroy() {
        isSending = false;
        if (telemetryHandler != null) {
            telemetryHandler.removeCallbacksAndMessages(null);
        }
        if (telemetryThread != null) {
            telemetryThread.quitSafely();
        }
        super.onDestroy();
    }

    private int getLastSavedMcuVersion() {
        return getSharedPreferences(PREFS_OTA, MODE_PRIVATE)
                .getInt(KEY_LAST_MCU_VERSION, 0);
    }

    private int getEffectiveMcuVersion() {
        int liveVersion = 0;
        serialPortManager = serialApplication.getSerialPortManager();

        try {
            if (serialPortManager != null && serialPortManager.mad != null) {
                liveVersion = serialPortManager.mad.getMcuVersions();
             //   Log.d("asfdg","IFOFNULLSEERIAL ");

            }
            //else Log.d("asfdg","ELSEOFNULLSEERIAL ");
        } catch (Exception e) {
            //Log.e("OTA", "MCU version read failed", e);
        }

        if (liveVersion > 0) {
            // optional: store again if you want
            getSharedPreferences(PREFS_OTA, MODE_PRIVATE)
                    .edit()
                    .putInt(KEY_LAST_MCU_VERSION, liveVersion)
                    .apply();

           // Log.d("OTA", "Telemetry using LIVE MCU version: " + liveVersion);
            return liveVersion;
        }

        int stored = getLastSavedMcuVersion();
//        Log.w("OTA", "Telemetry using STORED MCU version: " + stored);
        return stored;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }


//    private TelemetryApiPayload buildTelemetryApiPayload(String vin) {
//
//        TelemetryDataManager t = TelemetryDataManager.getInstance();
//        TelemetryApiPayload d = new TelemetryApiPayload();
//
//        d.vinNumber = vin;
//        d.time = java.time.OffsetDateTime.now().toString();
//
//        d.speedKmph = Math.round(t.getTeleSpeed());
//        d.tripKm = t.getTeleTrip();
//
//        d.soc = Math.round(t.getTeleSOC());
//        d.apuSoc = Math.round(t.getTeleAPUSOC());
//
//        d.batVoltage = t.getTeleBatVoltage();
//        d.currentConsumption = t.getTeleCurrentConsumption();
//        d.remainingCapacityAh = t.getTeleRemainingCapacityAH();
//
//        d.inAh = t.getTeleTotalAH();
//        d.inAhByCharger = t.getTeleChargerAH();
//        d.inAhByRegen = t.getTeleRegenAH();
//        d.outAh = t.getTeleDischargeAH();
//
//        d.motorTemp = t.getTeleMotorTemp();
//        d.controllerMosTemp = t.getTeleControllerMosTemp();
//        d.bmsMosTemp = t.getTeleBMSMosTemp();
//
//        d.tirePressure = t.getTeleTirePressure();
//        d.ntc = t.getTeleNTC();
//
//        // ✅ CORRECT power state (single source of truth)
//        d.evPowerState = (t.getTeleEvPowerState() == 1) ? "ON" : "OFF";
//
//        d.currentRider = t.getTeleCurrentRider();
//        d.latLong = t.getTeleLatLong();
//        if (d.latLong == null || d.latLong.isEmpty()) {
//            d.latLong = "0.0,0.0";
//        }
//
//        return d;
//    }
//    private void sendTelemetryToBackend(String vin) {
//
//        RealTimeData data = collectTelemetryData(vin);
//
//        // 🔥 LOG EXACT PAYLOAD
//        Log.e("TELEMETRY_API_SEND", new Gson().toJson(data));
//
//        apiService.sendTelemetry(data)
//                .enqueue(new Callback<ResponseBody>() {
//
//                    @Override
//                    public void onResponse(Call<ResponseBody> call,
//                                           Response<ResponseBody> response) {
//
//                        Log.e("TELEMETRY_API",
//                                "✅ SENT | code=" + response.code());
//                    }
//
//                    @Override
//                    public void onFailure(Call<ResponseBody> call,
//                                          Throwable t) {
//
//                        Log.e("TELEMETRY_API",
//                                "❌ FAILED", t);
//                    }
//                });
//    }

    private String buildRideOSVersion(Context context) {
        try {
            PackageInfo pInfo =
                    context.getPackageManager()
                            .getPackageInfo(context.getPackageName(), 0);

            int mcuVersion = getEffectiveMcuVersion();

            return "rideOS " + pInfo.versionName + "_GA"
                    + ",AOSPv=" + Build.DISPLAY + "_GA"
                    + ",MCUv=" + mcuVersion;

        } catch (Exception e) {
            return "rideOS N/A";
        }
    }

    public int getCriticalTemps() {


        int cpu1=0;


        try {
            File thermalDir = new File("/sys/class/thermal/");
            File[] zones = thermalDir.listFiles();

            if (zones != null) {
                for (File zone : zones) {

                    if (zone.getName().startsWith("thermal_zone")) {

                        File typeFile = new File(zone, "type");
                        File tempFile = new File(zone, "temp");

                        if (typeFile.exists() && tempFile.exists()) {

                            String type = readLine(typeFile);
                            String tempRaw = readLine(tempFile);

                            if (type == null || tempRaw == null) continue;

                            float tempC = Float.parseFloat(tempRaw) / 1000f;
                                cpu1 = (int)tempC;
                            }
                        }
                    }
                }


        } catch (Exception e) {
            e.printStackTrace();
        }

        return
                cpu1;
    }

    private String readLine(File file) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            return br.readLine();
        } catch (Exception e) {
            return null;
        }
    }

}




// BELOW IS TELEMETRY USING HTTPS REQUEST JUST UNCOMMENT IT >
//
//package com.fanconn.rideOS;
//
//import android.annotation.SuppressLint;
//import android.app.Service;
//import android.content.Context;
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.content.pm.PackageInfo;
//import android.content.pm.PackageManager;
//import android.location.Location;
//import android.location.LocationListener;
//import android.location.LocationManager;
//import android.net.ConnectivityManager;
//import android.net.Network;
//import android.net.NetworkCapabilities;
//import android.net.NetworkInfo;
//import android.os.Build;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.HandlerThread;
//import android.os.IBinder;
//import android.telephony.TelephonyManager;
//import android.util.Log;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//
//import com.fanconn.rideOS.network.EVWebSocketClient;
//import com.fanconn.rideOS.network.RetrofitClient;
//import com.fanconn.rideOS.network.VehicleData;
//import com.fanconn.rideOS.network.ApiService;
//
//import com.fanconn.rideOS.util.NumberUtils;
//import com.google.gson.Gson;
//
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileReader;
//import java.net.InetSocketAddress;
//import java.net.Socket;
//import java.net.URI;
//import java.text.SimpleDateFormat;
//import java.util.Date;
//import java.util.List;
//import java.util.Locale;
//import java.util.Objects;
//
//import okhttp3.ResponseBody;
//import retrofit2.Call;
//import retrofit2.Callback;
//import retrofit2.Response;
//
//import com.fanconn.rideOS.network.RideOSTelemetrySender;
//
//
//public class TelemetryService extends Service {
//
//
//    private HandlerThread telemetryThread;
//    private Handler telemetryHandler;
//    private boolean isSending = false;
//    private EVWebSocketClient evClient;
//    private ApiService apiService;
//    private Context context;
//    // Telemetry flags
//    public static boolean shdtelemetrywhenunlocked;
//    public static int sendRealtimeDataOnceEvenAfterLocking = 0;
//    private SerialPortManager serialPortManager;
//    private Heartbeat1 hb1;
//    private static final String PREFS_OTA = "ota_prefs";
//    private static final String KEY_LAST_MCU_VERSION = "last_mcu_version";
//    private SharedPreferences vehiclePrefs;
//    private SharedPreferences realtimePrefs;
//    @Override
//    public void onCreate() {
//        super.onCreate();
//        telemetryThread = new HandlerThread("TelemetryThread");
//        telemetryThread.start();
//        telemetryHandler = new Handler(telemetryThread.getLooper());
//
////        ////Stagging controller>
////        StagedTelemetryController
////                .getInstance(getApplicationContext())
////                .initialize(stagedUploadRunnable);
//
//        vehiclePrefs =
//                getSharedPreferences("vehicle_delta", MODE_PRIVATE);
//
//        realtimePrefs =
//                getSharedPreferences("realtime_delta", MODE_PRIVATE);
//    }
//    private final Runnable stagedUploadRunnable = new Runnable() {
//
//        @Override
//        public void run() {
//
//            StagedTelemetryController controller =
//                    StagedTelemetryController.getInstance(getApplicationContext());
//
//            if (!controller.shouldContinue()) {
//                controller.finished();
//                return;
//            }
//
//            if (!hasActiveInternetConnection()) {
//                telemetryHandler.postDelayed(this, 3000);
//                return;
//            }
//
//            TelemetryStagingManager manager =
//                    TelemetryStagingManager.getInstance(getApplicationContext());
//
//            if (!manager.hasData()) {
//                controller.finished();
//                return;
//            }
//
//            java.util.List<RealTimeData> batch = manager.pollBatch();
//
//            for (RealTimeData data : batch) {
//
//                sendToServer(data);
//
//                try {
//                    Thread.sleep(100);
//                } catch (InterruptedException ignored) {
//                }
//            }
////            Log.d("STAGE COUNT","REALTIME_JSON STAGE COUNT = "+ StagedTelemetryController.getInstance(context).getPendingPayloadCount());
//            telemetryHandler.postDelayed(this, 300);
//        }
//    };
//
//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        String vinNumber = intent != null ? intent.getStringExtra("VIN_NUMBER") : null;
//        this.context = this;
//        startTelemetry(vinNumber != null ? vinNumber : "UNKNOWN");
//        return START_STICKY;
//    }
//
//    private void startTelemetry(String VINNUMBER) {
//        evClient = new EVWebSocketClient(VINNUMBER, URI.create("ws://sb.rivotmotors.com:8081/ws/ev"));
////        evClient = new EVWebSocketClient(VINNUMBER, URI.create("ws://103.174.148.224:8081/ws/ev"));
////        evClient = new EVWebSocketClient(VINNUMBER, URI.create("ws://192.168.1.116:8081/ws/ev"));
//
//        evClient.connect();
//
//        apiService = RetrofitClient.getClient().create(ApiService.class);
//        isSending = true;
//        sendDataContinuously(VINNUMBER);
//        startLocationUpdates(this);
//
//
//        new Thread(() -> {
//            while (evClient == null || !evClient.isOpen()) {
//                try {
//                    Thread.sleep(500);
//                } catch (InterruptedException e) {
//                    //Log.e("Telemetry", "Thread interrupted", e);
//                    return;
//                }
//            }
//            serialPortManager = serialApplication.getSerialPortManager();
//            if (serialPortManager != null) {
//                serialPortManager.sendContinuousSensorData();
//            } else {
//                //Log.e("Telemetry", "serialPortManager is null!");
//            }
//        }).start();
//    }
//    private boolean firstVehicleUpload = true;
//    private void sendDataContinuously(String vin) {
//        String rideOsVersion = buildRideOSVersion(context);
//
//        telemetryHandler.removeCallbacksAndMessages(null); // prevent duplicate loops
//        telemetryHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                if (isSending) {
//
////                    Log.d("TelemetryService", "Loop latlong = "
////                            + TelemetryDataManager.getInstance().getTeleLatLong());
//
//                    if(serialApplication.getSerialPortManager().getHb1()!=null)
//                    {
//                        hb1= serialApplication.getSerialPortManager().getHb1();
//                    }
//                    if(hb1!=null) {
//                        TelemetryDataManager.getInstance().setTeleHandleLockState(hb1.getDragonLock());
//                        TelemetryDataManager.getInstance().setTeleSeatLockState(hb1.getSeatLock());
//                    }
//
//                    sendVehicleData(context,  NumberUtils.round(TelemetryDataManager.getInstance().getTeleODO(),3), TelemetryDataManager.getInstance().getTeleChargingState(), TelemetryDataManager.getInstance().getTeleHandleLockState(),
//                            TelemetryDataManager.getInstance().getTeleSeatLockState(), TelemetryDataManager.getInstance().getTeleBMSMosStates(), TelemetryDataManager.getInstance().getTeleCurrentRider(),
//                            22, TelemetryDataManager.getInstance().getTeleLatLong());
//                    TelemetryDataManager.getInstance()
//                            .setTeleEvPowerState(shdtelemetrywhenunlocked ? 1 : 0);
////                    RideOSTelemetrySender.send(vin);
//
//              //This start sending RIVOT One app data seperately
//               //     RideOSTelemetrySender.send(vin, rideOsVersion);
//
////                    Log.d("STAGE COUNT","REALTIME_JSON STAGE COUNT SEND CONTINOS = "+ StagedTelemetryController.getInstance(context).getPendingPayloadCount());
//                    TelemetryDataManager.getInstance().setTeleStaggingCount(StagedTelemetryController.getInstance(context).getPendingPayloadCount());
//
//                    sendRealTimeData(vin);
////                    if (shdtelemetrywhenunlocked) {
////                        sendRealTimeData(vin);
////                        sendRealtimeDataOnceEvenAfterLocking = 0;
////                    } else if (sendRealtimeDataOnceEvenAfterLocking == 0) {
////                        sendRealTimeData(vin);
////                        sendRealtimeDataOnceEvenAfterLocking++;
////                    }
//                    telemetryHandler.postDelayed(this, 3000);
//                }
//            }
//        }, 3000);
//    }
//
//    private void sendVehicleData(Context context, double odo, int chargingState,
//                                 int handleLockState, int seatLockState, String bmsMosStates,
//                                 String riders, int bmslifecycles, String lat_long) {
//
//        MetadataManager metadata = MetadataManager.get();
//        String vinNumber = metadata.getVin();
//
//        if (vinNumber.contains("UNKNOWN")) {
////            Log.w("Telemetry", "VIN unknown — skipping sendVehicleData");
//            return;
//        }
//        if (lat_long == null || lat_long.isEmpty()) {
//            lat_long = "0.0,0.0";   // fallback value if GPS not ready
//        }
//
//        String ownerName = metadata.getOwner();
//        String phoneNumber = metadata.getPhone();
//        String controllerID = metadata.getControllerId();
//        String motorID = metadata.getMotorId();
//        String bmsID = metadata.getBmsId();
//        String chargerID = metadata.getCharger();
//        String smartKeyID = metadata.getSmartKeyId();
//        String model = metadata.getModel();
//
//        String simInfo;
//        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
//        if (telephonyManager != null) {
//            simInfo = telephonyManager.getSimOperatorName()+"-"+telephonyManager.getSimSerialNumber();
//        }
//        else {
//            simInfo=null;
//        }
//
//        String rideOSversion;
//        try {
//            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
//            int mcuVersion = getEffectiveMcuVersion();
//            rideOSversion = pInfo.versionName+";"+Build.DISPLAY+";"+mcuVersion+";"+simInfo;
//        } catch (PackageManager.NameNotFoundException e) {
//            rideOSversion = "rideOS N/A";
//        }
//
//        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
//        SharedPreferences prefs = context.getSharedPreferences("last_uploaded_metadata", Context.MODE_PRIVATE);
//
//        boolean anyChanged =
//                !vinNumber.equals(prefs.getString("vin", "")) ||
//                        !ownerName.equals(prefs.getString("owner", "")) ||
//                        !phoneNumber.equals(prefs.getString("phone", "")) ||
//                        !controllerID.equals(prefs.getString("controller", "")) ||
//                        !motorID.equals(prefs.getString("motor", "")) ||
//                        !bmsID.equals(prefs.getString("bms", "")) ||
//                        !chargerID.equals(prefs.getString("charger", "")) ||
//                        !smartKeyID.equals(prefs.getString("smartKey", "")) ||
//                        !model.equals(prefs.getString("model", ""));
//
////        VehicleData data = anyChanged ?
////                new VehicleData(vinNumber, model, ownerName, phoneNumber, controllerID,
////                        motorID, bmsID, chargerID, rideOSversion, smartKeyID,
////                        odo, chargingState, handleLockState, seatLockState,
////                        bmsMosStates, riders, bmslifecycles, lat_long, time)
////
////                :
////                new VehicleData(vinNumber, null, null, null, null,
////                        null, null, null, rideOSversion, null,
////                        odo, chargingState, handleLockState, seatLockState,
////                        bmsMosStates, riders, bmslifecycles, lat_long, time);
//
//        //Always sned complete data in first payload
//        RealtimeDeltaManager delta =
//                RealtimeDeltaManager.getInstance();
//
//        VehicleData data;
//
//        if (firstVehicleUpload) {
//
//            firstVehicleUpload = false;
//
//            data = new VehicleData(
//                    vinNumber,
//                    null,
//                    null,
//                    null,
//                    null,
//                    null,
//                    null,
//                    null,
//                    rideOSversion,
//                    null,
//                    odo,
//                    chargingState,
//                    handleLockState,
//                    seatLockState,
//                    bmsMosStates,
//                    riders,
//                    bmslifecycles,
//                    lat_long,
//                    time
//            );
//        }
//        else {
//
//            data = new VehicleData(
//
//                    vinNumber,
//
//                    getChangedString(vehiclePrefs,
//                            "model",
//                            model),
//
//                    getChangedString(vehiclePrefs,
//                            "owner",
//                            ownerName),
//
//                    getChangedString(vehiclePrefs,
//                            "phone",
//                            phoneNumber),
//
//                    getChangedString(vehiclePrefs,
//                            "controller",
//                            controllerID),
//
//                    getChangedString(vehiclePrefs,
//                            "motor",
//                            motorID),
//
//                    getChangedString(vehiclePrefs,
//                            "bms",
//                            bmsID),
//
//                    getChangedString(vehiclePrefs,
//                            "charger",
//                            chargerID),
//
//                    getChangedString(vehiclePrefs,
//                            "rideos",
//                            rideOSversion),
//
//                    getChangedString(vehiclePrefs,
//                            "smartkey",
//                            smartKeyID),
//
//                    delta.changedDouble(
//                            "vehicle_odo",
//                            odo
//                    ),
//
//                    delta.changedInt(
//                            "vehicle_charging_state",
//                            chargingState
//                    ),
//
//                    delta.changedInt(
//                            "vehicle_handle_lock",
//                            handleLockState
//                    ),
//
//                    delta.changedInt(
//                            "vehicle_seat_lock",
//                            seatLockState
//                    ),
//
//                    delta.changed(
//                            "vehicle_bmsmos",
//                            bmsMosStates
//                    ),
//
//                    delta.changed(
//                            "vehicle_rider",
//                            riders
//                    ),
//
//                    delta.changedInt(
//                            "vehicle_bms_cycles",
//                            bmslifecycles
//                    ),
//
//                    delta.changed(
//                            "vehicle_latlong",
//                            lat_long
//                    ),
//
//                    time
//            );
//        }
//
//        //Dont send complete data in frist payload
////        RealtimeDeltaManager delta =
////                RealtimeDeltaManager.getInstance();
////
////        VehicleData data =
////                new VehicleData(
////
////                        vinNumber,
////
////                        getChangedString(vehiclePrefs,
////                                "model",
////                                model),
////
////                        getChangedString(vehiclePrefs,
////                                "owner",
////                                ownerName),
////
////                        getChangedString(vehiclePrefs,
////                                "phone",
////                                phoneNumber),
////
////                        getChangedString(vehiclePrefs,
////                                "controller",
////                                controllerID),
////
////                        getChangedString(vehiclePrefs,
////                                "motor",
////                                motorID),
////
////                        getChangedString(vehiclePrefs,
////                                "bms",
////                                bmsID),
////
////                        getChangedString(vehiclePrefs,
////                                "charger",
////                                chargerID),
////
////                        getChangedString(vehiclePrefs,
////                                "rideos",
////                                rideOSversion),
////
////                        getChangedString(vehiclePrefs,
////                                "smartkey",
////                                smartKeyID),
////
////                        delta.changedDouble(
////                                "vehicle_odo",
////                                odo
////                        ),
////
////                        delta.changedInt(
////                                "vehicle_charging_state",
////                                chargingState
////                        ),
////
////                        delta.changedInt(
////                                "vehicle_handle_lock",
////                                handleLockState
////                        ),
////
////                        delta.changedInt(
////                                "vehicle_seat_lock",
////                                seatLockState
////                        ),
////
////                        delta.changed(
////                                "vehicle_bmsmos",
////                                bmsMosStates
////                        ),
////
////                        delta.changed(
////                                "vehicle_rider",
////                                riders
////                        ),
////
////                        delta.changedInt(
////                                "vehicle_bms_cycles",
////                                bmslifecycles
////                        ),
////
////                        delta.changed(
////                                "vehicle_latlong",
////                                lat_long
////                        ),
////
////                        time
////                );
//        //Log.d("TelemetryService", "Sending telemetry: " + TelemetryDataManager.getInstance().getTeleLatLong());
//
//        if (anyChanged) {
//            prefs.edit()
//                    .putString("owner", ownerName)
//                    .putString("phone", phoneNumber)
//                    .putString("controller", controllerID)
//                    .putString("motor", motorID)
//                    .putString("bms", bmsID)
//                    .putString("charger", chargerID)
//                    .putString("smartKey", smartKeyID)
//                    .putString("model", model)
//                    .apply();
//        }
//        String json = new Gson().toJson(data);
//        //Log.e("VEHICLE_JSON", json);
//        apiService.saveVehicleData(data).enqueue(new Callback<ResponseBody>() {
//            @Override
//            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) { }
//            @Override
//            public void onFailure(Call<ResponseBody> call, Throwable t) {
////                Log.e("Telemetry", "Faileddd to send vehicle data", t);
//            }
//        });
//    }
//    private String getChangedString(
//            SharedPreferences prefs,
//            String key,
//            String newValue) {
//
//        String old =
//                prefs.getString(key, null);
//
//        if (Objects.equals(old, newValue)) {
//            return null;
//        }
//
//        prefs.edit()
//                .putString(key, newValue)
//                .apply();
////        Log.e("DELTA",
////                key +
////                        " old=" + old +
////                        " new=" + newValue);
//        return newValue;
//    }
//    private void sendRealTimeData(String vin) {
//        RealTimeData data = collectTelemetryData(vin);
//
//        if (hasActiveInternetConnection()) {
//            // Send staged data first
//            // NEW SAFE BATCH SENDING
////            if (TelemetryStagingManager.getInstance().hasData()) {
////                new Thread(() -> {
////                    while (TelemetryStagingManager.getInstance().hasData() && hasActiveInternetConnection()) {
////
////                        List<RealTimeData> batch = TelemetryStagingManager.getInstance().pollBatch();  // Take 20 at once
////                        for (RealTimeData r : batch) {
////                            sendToServer(r);
////                            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
////                        }
////
////                        try { Thread.sleep(300); } catch (Exception ignored) {} // Small break between batches
////                    }
////                }).start();
////            }
//
//            //START SENDNG STAGGED DATA AS SOON AS INTERNET IS AVALBALE
//            if (TelemetryStagingManager.getInstance(getApplicationContext()).hasData()) {
//                flushStagedData();
//            }
//            //==end========START SENDNG STAGGED DATA AS SOON AS INTERNET IS AVALBALE
//
//            // Send current data
//            //DataUsageTracker tracker = new DataUsageTracker();
//            //tracker.startTracking();
//
//            sendToServer(data);
//            //Log.e("Telemetry", "INTENET VAIL SNEDNING ");
//
//            //tracker.logUsage("Currdent Payload");
//
//        } else {
////            Log.e("Telemetry", "NOOINTENET VAIL SNEDNING ");
//            TelemetryStagingManager.getInstance(getApplicationContext()).addData(data);
//        }
//    }
//    private boolean isFlushing = false;
//    private void flushStagedData() {
//        if (isFlushing) return;
//        isFlushing = true;
//
//        telemetryHandler.post(() -> {
//            try {
//                if (!hasActiveInternetConnection()) return;
//
//                List<RealTimeData> batch =
//                        TelemetryStagingManager
//                                .getInstance(getApplicationContext())
//                                .pollBatch();
//
//                for (RealTimeData r : batch) {
//                    sendToServer(r);
//                    try {
//                        Thread.sleep(100); // smooth throttle
//                    } catch (Exception ignored) {}
//                }
//
//            } finally {
//                isFlushing = false;
//            }
//        });
//    }
//
//    private RealTimeData collectTelemetryData(String vin) {
//        RealTimeData data = new RealTimeData();
//        data.vinNumber = vin;
//        data.speed_kmph = TelemetryDataManager.getInstance().getTeleSpeed();
//        data.tripkm= roundTo2Decimal(TelemetryDataManager.getInstance().getTeleTrip());
//        data.controllerMosTemp = TelemetryDataManager.getInstance().getTeleControllerMosTemp();
//        data.motorTemp = TelemetryDataManager.getInstance().getTeleMotorTemp();
//        data.bmsMosTemp = TelemetryDataManager.getInstance().getTeleBMSMosTemp();
//        if(SerialPortManager.getInstance().getHb3()!=null){
//            data.ntc = TelemetryDataManager.getInstance().getTeleNTC();
//     //   Log.d("dsafgs","ddddddddddddddd "+data.ntc);
//        }else{
//            //Log.d("dsafgs","ddddddddddddddddeeeeeeeeeeeeeeeeeeeeeedddddddd "+data.ntc);
//        }
//
//        data.currentconsumption = roundTo3Decimal(TelemetryDataManager.getInstance().getTeleCurrentConsumption());
//        data.batvoltage = roundTo1Decimal(TelemetryDataManager.getInstance().getTeleBatVoltage());
//        data.remainingCapacity_AH = roundTo3Decimal(TelemetryDataManager.getInstance().getTeleRemainingCapacityAH());
//        data.inAH = roundTo3Decimal(TelemetryDataManager.getInstance().getTeleTotalAH());
//        data.inAH_by_charger = roundTo3Decimal(TelemetryDataManager.getInstance().getTeleChargerAH());
//        data.inAH_by_regen = roundTo3Decimal(TelemetryDataManager.getInstance().getTeleRegenAH());
//        data.outAH = roundTo3Decimal(TelemetryDataManager.getInstance().getTeleDischargeAH());
//        data.ev_power_state = (shdtelemetrywhenunlocked ? "ON" : "OFF");
//        data.currentRider = shdtelemetrywhenunlocked ? TelemetryDataManager.getInstance().getTeleCurrentRider() : "Scooter Locked";
//        data.tirepressure = TelemetryDataManager.getInstance().getTeleTirePressure();
//
//        data.soc = NumberUtils.truncate1(TelemetryDataManager.getInstance().getTeleSOC());
//        data.apuSoc = NumberUtils.truncate1(TelemetryDataManager.getInstance().getTeleAPUSOC());
//        data.lat_long = TelemetryDataManager.getInstance().getTeleLatLong();
//        if (data.lat_long == null || data.lat_long.isEmpty()) {
//            data.lat_long = "0.0,0.0";   // fallback if GPS not ready
//        }
//        data.time = java.time.OffsetDateTime.now().toString();
//        //int mcuVersion = getEffectiveMcuVersion();
//        int temps = getCriticalTemps();
//        UIUpdater uiUpdater = UIUpdater.getInstance();
//        data.data = (TelemetryDataManager.getInstance().isGpsTelemetryEnabled() ? 1 : 0) +","+TelemetryDataManager.getInstance().getTeleStaggingCount();
//
////        if (uiUpdater!=null) {
////            data.data = TelemetryDataManager.getInstance().getTeleRange()+";"+TelemetryDataManager.getInstance().getBMSTeleSOC()+";"+TelemetryDataManager.getInstance().getBMSTeleRemainingCapacityAH()+
////                    ";"+uiUpdater.getrecoEngineParams()+";"+TelemetryDataManager.getInstance().getMax_voltage_value()+";"+TelemetryDataManager.getInstance().getAlowable_charging_current_value();
////        }else{
////            data.data = TelemetryDataManager.getInstance().getTeleRange()+";"+TelemetryDataManager.getInstance().getBMSTeleSOC()+";"+TelemetryDataManager.getInstance().getBMSTeleRemainingCapacityAH()+";"+temps
////                    +";N/A"+";"+TelemetryDataManager.getInstance().getMax_voltage_value()+";"+TelemetryDataManager.getInstance().getAlowable_charging_current_value();
////        }
//
//        //===============
//
//        data.bmsmos =
//                TelemetryDataManager.getInstance().getTeleBMSMosStates();
//
//        data.chargingstatus =
//                TelemetryDataManager.getInstance().getTeleChargingState()+","+TelemetryDataManager.getInstance().getTeleChargingCurrentValue()+","+ETADataManager.getInstance().getEtaTime();
//
//        data.singleminmaxvoltage =
//                TelemetryDataManager.getInstance().getteleIndividualCellVoltages();
//
//        data.apuvoltage =
//                (double) TelemetryDataManager.getInstance().getTeleAPUBatVoltage();
//
//        data.uptime =
//                UptimeManager.getUptime();
//
//        data.gear =
//                String.valueOf(MainActivity.currentgear);
//
//        data.range =
//                NumberUtils.truncate1(TelemetryDataManager.getInstance().getTeleRange());
//
//        data.bmssoc =
//                NumberUtils.truncate1( TelemetryDataManager.getInstance().getBMSTeleSOC());
//
//        data.bmsremainingcap =
//                NumberUtils.truncate1( TelemetryDataManager.getInstance().getBMSTeleRemainingCapacityAH());
//
//        try {
//            data.vcutemp = (double)getCriticalTemps();
//        } catch (NumberFormatException e) {
//            data.vcutemp = 0.0; // or some default value
//        }
//
//        if (uiUpdater!=null) {
//            data.recoengine =
//                    uiUpdater.getrecoEngineParams();
//        }
//        data.maxtotalvoltage =
//                (double) TelemetryDataManager.getInstance().getMax_voltage_value();
//
//        data.allowablechargingcurrent =
//                (double) TelemetryDataManager.getInstance().getAlowable_charging_current_value();
//
//        data.cell1to3 =
//                SerialPortManager.getInstance().getHb3().getcell1TO3();
//
//        data.cell4to6 =
//                SerialPortManager.getInstance().getHb3().getcell4TO6();
//
//        data.cell7to9 =
//                SerialPortManager.getInstance().getHb3().getcell7TO9();
//
//        data.cell10to12 =
//                SerialPortManager.getInstance().getHb3().getcell10TO12();
//
//        data.cell13to15 =
//                SerialPortManager.getInstance().getHb3().getcell13TO15();
//
//        data.cell16to18 =
//                SerialPortManager.getInstance().getHb3().getcell16TO18();
//
//        data.cell19to21 =
//                SerialPortManager.getInstance().getHb3().getcell19TO21();
//
//        data.cell22to23 =
//                SerialPortManager.getInstance().getHb3().getcell22TO23();
//        //Log.d("QUALCOMTHERMAL  ", temps);
//        return data;
//    }
//
////    private void sendToServer(RealTimeData data) {
////
////        RealTimeDataApi apiData = new RealTimeDataApi();
////
////        apiData.vinNumber = data.vinNumber;
////        apiData.speed_kmph = data.speed_kmph;
////        apiData.tripkm = data.tripkm;
////        apiData.controllerMosTemp = data.controllerMosTemp;
////        apiData.motorTemp = data.motorTemp;
////        apiData.bmsMosTemp = data.bmsMosTemp;
////        apiData.ntc = data.ntc;
////        apiData.currentconsumption = data.currentconsumption;
////        apiData.batvoltage = data.batvoltage;
////        apiData.remainingCapacity_AH = data.remainingCapacity_AH;
////        apiData.inAH = data.inAH;
////        apiData.inAH_by_charger = data.inAH_by_charger;
////        apiData.inAH_by_regen = data.inAH_by_regen;
////        apiData.outAH = data.outAH;
////        apiData.ev_power_state = data.ev_power_state;
////        apiData.currentRider = data.currentRider;
////        apiData.tirepressure = data.tirepressure;
////        apiData.soc = data.soc;
////        apiData.apuSoc = data.apuSoc;
////        apiData.lat_long = data.lat_long;
////        apiData.time = data.time;
////        apiData.data = data.data;
////
////        apiService.saveRealTimeData(apiData).enqueue(new Callback<ResponseBody>() {
////            @Override
////            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
////                if (!response.isSuccessful()) {
////                   // Log.e("TelemetryService", "❌ RealTimeData failed: " + response.code());
////                }
////            }
////
////            @Override
////            public void onFailure(Call<ResponseBody> call, Throwable t) {
////                TelemetryStagingManager
////                        .getInstance(getApplicationContext())
////                        .addData(data);
////
////            //    Log.e("TelemetryService", "Failed, re-staged", t);
////            }
////        });
////    }
//private void sendToServer(RealTimeData data) {
//
//    RealTimeDataApi apiData = new RealTimeDataApi();
//
//    RealtimeDeltaManager delta =
//            RealtimeDeltaManager.getInstance();
//
//    // Always send these
//    apiData.vinNumber =  data.vinNumber;
//    apiData.time = data.time;
//    apiData.ev_power_state =  delta.changed("ev_power_state",data.ev_power_state);
//
//    // Delta fields
//    apiData.speed_kmph =
//            delta.changedDouble(
//                    "speed_kmph",
//                    data.speed_kmph);
//
//    apiData.tripkm =
//            delta.changedDouble(
//                    "tripkm",
//                    data.tripkm);
//
//    apiData.controllerMosTemp =
//            delta.changedDouble(
//                    "controllerMosTemp",
//                    data.controllerMosTemp);
//
//    apiData.motorTemp =
//            delta.changedDouble(
//                    "motorTemp",
//                    data.motorTemp);
//
//    apiData.bmsMosTemp =
//            delta.changedDouble(
//                    "bmsMosTemp",
//                    data.bmsMosTemp);
//
//    apiData.ntc =
//            delta.changed(
//                    "ntc",
//                    data.ntc);
//
//    apiData.currentconsumption =
//            delta.changedDouble(
//                    "currentconsumption",
//                    data.currentconsumption);
//
//    apiData.batvoltage =
//            delta.changedDouble(
//                    "batvoltage",
//                    data.batvoltage);
//
//    apiData.remainingCapacity_AH =
//            delta.changedDouble(
//                    "remainingCapacity_AH",
//                    data.remainingCapacity_AH);
//
//    apiData.inAH =
//            delta.changedDouble(
//                    "inAH",
//                    data.inAH);
//
//    apiData.inAH_by_charger =
//            delta.changedDouble(
//                    "inAH_by_charger",
//                    data.inAH_by_charger);
//
//    apiData.inAH_by_regen =
//            delta.changedDouble(
//                    "inAH_by_regen",
//                    data.inAH_by_regen);
//
//    apiData.outAH =
//            delta.changedDouble(
//                    "outAH",
//                    data.outAH);
//
//    apiData.currentRider =
//            delta.changed(
//                    "currentRider",
//                    data.currentRider);
//
//    apiData.tirepressure =
//            delta.changed(
//                    "tirepressure",
//                    data.tirepressure);
//
//    apiData.soc =
//            delta.changedDouble(
//                    "soc",
//                    data.soc);
//
//    apiData.apuSoc =
//            delta.changedDouble(
//                    "apuSoc",
//                    data.apuSoc);
//
//    apiData.lat_long =
//            delta.changed(
//                    "lat_long",
//                    data.lat_long);
//
//    apiData.data =
//            delta.changed(
//                    "data",
//                    data.data);
//
//    apiData.bmsmos =
//            delta.changed("bmsmos", data.bmsmos);
//
//    apiData.chargingstatus =
//            delta.changed("chargingstatus", data.chargingstatus);
//
//    apiData.singleminmaxvoltage =
//            delta.changed("singleminmaxvoltage",
//                    data.singleminmaxvoltage);
//
//    apiData.apuvoltage =
//            delta.changedDouble("apuvoltage",
//                    data.apuvoltage);
//
//    apiData.uptime =
//            delta.changed("uptime", data.uptime);
//
//    apiData.gear =
//            delta.changed("gear", data.gear);
//
//    apiData.range =
//            delta.changedDouble("range",
//                    data.range);
//
//    apiData.bmssoc =
//            delta.changedDouble("bmssoc",
//                    data.bmssoc);
//
//    apiData.bmsremainingcap =
//            delta.changedDouble("bmsremainingcap",
//                    data.bmsremainingcap);
//
//    apiData.vcutemp =
//            delta.changedDouble("vcutemp",
//                    data.vcutemp);
//
//    apiData.recoengine =
//            delta.changed("recoengine",
//                    data.recoengine);
//
//    apiData.maxtotalvoltage =
//            delta.changedDouble("maxtotalvoltage",
//                    data.maxtotalvoltage);
//
//    apiData.allowablechargingcurrent =
//            delta.changedDouble("allowablechargingcurrent",
//                    data.allowablechargingcurrent);
//
//    apiData.cell1to3 =
//            delta.changed("cell1to3",
//                    data.cell1to3);
//
//    apiData.cell4to6 =
//            delta.changed("cell4to6",
//                    data.cell4to6);
//
//    apiData.cell7to9 =
//            delta.changed("cell7to9",
//                    data.cell7to9);
//
//    apiData.cell10to12 =
//            delta.changed("cell10to12",
//                    data.cell10to12);
//
//    apiData.cell13to15 =
//            delta.changed("cell13to15",
//                    data.cell13to15);
//
//    apiData.cell16to18 =
//            delta.changed("cell16to18",
//                    data.cell16to18);
//
//    apiData.cell19to21 =
//            delta.changed("cell19to21",
//                    data.cell19to21);
//
//    apiData.cell22to23 =
//            delta.changed("cell22to23",
//                    data.cell22to23);
//
////    Log.e(
////            "REALTIME_JSON",
////            new Gson().toJson(apiData)
////    );
//    apiService.saveRealTimeData(apiData)
//            .enqueue(new Callback<ResponseBody>() {
//
//                @Override
//                public void onResponse(
//                        Call<ResponseBody> call,
//                        Response<ResponseBody> response) {
//
//                    if (!response.isSuccessful()) {
//                        // Log.e("TelemetryService",
//                        //      "❌ RealTimeData failed: "
//                        //      + response.code());
//                    }
//                }
//
//                @Override
//                public void onFailure(
//                        Call<ResponseBody> call,
//                        Throwable t) {
//
//                    TelemetryStagingManager
//                            .getInstance(getApplicationContext())
//                            .addData(data);
//
//                    // Log.e("TelemetryService",
//                    //      "Failed, re-staged", t);
//                }
//            });
//    }
//
//    private boolean isInternetAvailable() {
//        try {
//            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
//            if (cm == null) return false;
//            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
//                Network n = cm.getActiveNetwork();
//                if (n == null) return false;
//                NetworkCapabilities nc = cm.getNetworkCapabilities(n);
//                return nc != null && (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
//                        nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
//                        nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
//            } else {
//                NetworkInfo ni = cm.getActiveNetworkInfo();
//                return ni != null && ni.isConnected();
//            }
//        } catch (Exception e) {
//            return false;
//        }
//    }
//
//    /** Checks if there’s any active network connection (Wi-Fi / Mobile) */
//    private boolean isNetworkConnected() {
//        try {
//            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
//            if (cm == null) return false;
//            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
//                Network n = cm.getActiveNetwork();
//                if (n == null) return false;
//                NetworkCapabilities nc = cm.getNetworkCapabilities(n);
//                return nc != null && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
//            } else {
//                NetworkInfo ni = cm.getActiveNetworkInfo();
//                return ni != null && ni.isConnected();
//            }
//        } catch (Exception e) {
//            return false;
//        }
//    }
//
//    /** Actually tests if Internet is reachable (for cloud endpoints) */
//    private boolean hasActiveInternetConnection() {
//        try {
//            Socket socket = new Socket();
//            socket.connect(new InetSocketAddress("8.8.8.8", 53), 1500);
//            socket.close();
//            return true;
//        } catch (Exception e) {
//            return false;
//        }
//    }
//
//    private double roundTo3Decimal(double value) {
//        return Math.round(value * 1000.0) / 1000.0;
//    }
//
//    private double roundTo2Decimal(double value) {
//        return Math.round(value * 100.0) / 100.0;
//    }
//    private double roundTo1Decimal(double value) {
//        return Math.round(value * 10.0) / 10.0;
//    }
//
//    @SuppressLint("MissingPermission")
//    private void startLocationUpdates(Context context) {
//
//        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
//
//        if (locationManager == null) {
//            // Log.e("TelemetryService", "LocationManager is null");
//            return;
//        }
//
//        // Request location updates every 1 second or 1 meter
//        locationManager.requestLocationUpdates(
//                LocationManager.GPS_PROVIDER,
//                1000,   // 1000 ms = 1 second
//                1,      // 1 meter
//                new LocationListener() {
//                    @Override
//                    public void onLocationChanged(@NonNull Location location) {
//                        String latlong = String.format(Locale.US, "%.6f, %.6f",
//                                location.getLatitude(), location.getLongitude());
//
//                        // Save in your TelemetryDataManager so telemetry loop can send it
//                        TelemetryDataManager.getInstance().setTeleLatLong(latlong);
//                     //   Log.d("TelemetryService", "Updated location = " + latlong);
//                    }
//
//                    @Override
//                    public void onProviderDisabled(@NonNull String provider) {
//                      //  Log.w("TelemetryService", "Provider disabled: " + provider);
//                    }
//
//                    @Override
//                    public void onProviderEnabled(@NonNull String provider) {
//                      //  Log.d("TelemetryService", "Provider enabled: " + provider);
//                    }
//
//                    @Override
//                    public void onStatusChanged(String provider, int status, Bundle extras) {
//                      //  Log.d("TelemetryService", "Status changed: " + provider + " status=" + status);
//                    }
//                }
//        );
//    }
//
//    @Override
//    public void onDestroy() {
//        isSending = false;
//        if (telemetryHandler != null) {
//            telemetryHandler.removeCallbacksAndMessages(null);
//        }
//        if (telemetryThread != null) {
//            telemetryThread.quitSafely();
//        }
//        super.onDestroy();
//    }
//
//    private int getLastSavedMcuVersion() {
//        return getSharedPreferences(PREFS_OTA, MODE_PRIVATE)
//                .getInt(KEY_LAST_MCU_VERSION, 0);
//    }
//
//    private int getEffectiveMcuVersion() {
//        int liveVersion = 0;
//        serialPortManager = serialApplication.getSerialPortManager();
//
//        try {
//            if (serialPortManager != null && serialPortManager.mad != null) {
//                liveVersion = serialPortManager.mad.getMcuVersions();
//             //   Log.d("asfdg","IFOFNULLSEERIAL ");
//
//            }
//            //else Log.d("asfdg","ELSEOFNULLSEERIAL ");
//        } catch (Exception e) {
//            //Log.e("OTA", "MCU version read failed", e);
//        }
//
//        if (liveVersion > 0) {
//            // optional: store again if you want
//            getSharedPreferences(PREFS_OTA, MODE_PRIVATE)
//                    .edit()
//                    .putInt(KEY_LAST_MCU_VERSION, liveVersion)
//                    .apply();
//
//           // Log.d("OTA", "Telemetry using LIVE MCU version: " + liveVersion);
//            return liveVersion;
//        }
//
//        int stored = getLastSavedMcuVersion();
////        Log.w("OTA", "Telemetry using STORED MCU version: " + stored);
//        return stored;
//    }
//
//    @Nullable
//    @Override
//    public IBinder onBind(Intent intent) { return null; }
//
//
////    private TelemetryApiPayload buildTelemetryApiPayload(String vin) {
////
////        TelemetryDataManager t = TelemetryDataManager.getInstance();
////        TelemetryApiPayload d = new TelemetryApiPayload();
////
////        d.vinNumber = vin;
////        d.time = java.time.OffsetDateTime.now().toString();
////
////        d.speedKmph = Math.round(t.getTeleSpeed());
////        d.tripKm = t.getTeleTrip();
////
////        d.soc = Math.round(t.getTeleSOC());
////        d.apuSoc = Math.round(t.getTeleAPUSOC());
////
////        d.batVoltage = t.getTeleBatVoltage();
////        d.currentConsumption = t.getTeleCurrentConsumption();
////        d.remainingCapacityAh = t.getTeleRemainingCapacityAH();
////
////        d.inAh = t.getTeleTotalAH();
////        d.inAhByCharger = t.getTeleChargerAH();
////        d.inAhByRegen = t.getTeleRegenAH();
////        d.outAh = t.getTeleDischargeAH();
////
////        d.motorTemp = t.getTeleMotorTemp();
////        d.controllerMosTemp = t.getTeleControllerMosTemp();
////        d.bmsMosTemp = t.getTeleBMSMosTemp();
////
////        d.tirePressure = t.getTeleTirePressure();
////        d.ntc = t.getTeleNTC();
////
////        // ✅ CORRECT power state (single source of truth)
////        d.evPowerState = (t.getTeleEvPowerState() == 1) ? "ON" : "OFF";
////
////        d.currentRider = t.getTeleCurrentRider();
////        d.latLong = t.getTeleLatLong();
////        if (d.latLong == null || d.latLong.isEmpty()) {
////            d.latLong = "0.0,0.0";
////        }
////
////        return d;
////    }
////    private void sendTelemetryToBackend(String vin) {
////
////        RealTimeData data = collectTelemetryData(vin);
////
////        // 🔥 LOG EXACT PAYLOAD
////        Log.e("TELEMETRY_API_SEND", new Gson().toJson(data));
////
////        apiService.sendTelemetry(data)
////                .enqueue(new Callback<ResponseBody>() {
////
////                    @Override
////                    public void onResponse(Call<ResponseBody> call,
////                                           Response<ResponseBody> response) {
////
////                        Log.e("TELEMETRY_API",
////                                "✅ SENT | code=" + response.code());
////                    }
////
////                    @Override
////                    public void onFailure(Call<ResponseBody> call,
////                                          Throwable t) {
////
////                        Log.e("TELEMETRY_API",
////                                "❌ FAILED", t);
////                    }
////                });
////    }
//
//    private String buildRideOSVersion(Context context) {
//        try {
//            PackageInfo pInfo =
//                    context.getPackageManager()
//                            .getPackageInfo(context.getPackageName(), 0);
//
//            int mcuVersion = getEffectiveMcuVersion();
//
//            return "rideOS " + pInfo.versionName + "_GA"
//                    + ",AOSPv=" + Build.DISPLAY + "_GA"
//                    + ",MCUv=" + mcuVersion;
//
//        } catch (Exception e) {
//            return "rideOS N/A";
//        }
//    }
//
//    public int getCriticalTemps() {
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
//                                cpu1 = (int)tempC;
//                            }
//                        }
//                    }
//                }
//
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return
//                cpu1;
//    }
//
//    private String readLine(File file) {
//        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
//            return br.readLine();
//        } catch (Exception e) {
//            return null;
//        }
//    }
//
//}