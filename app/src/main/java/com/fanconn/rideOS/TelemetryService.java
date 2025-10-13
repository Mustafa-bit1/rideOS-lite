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
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.fanconn.rideOS.network.EVWebSocketClient;
import com.fanconn.rideOS.network.RetrofitClient;
import com.fanconn.rideOS.network.VehicleData;
import com.fanconn.rideOS.network.ApiService;

import com.google.gson.Gson;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.Manifest;
import android.os.HandlerThread;

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

    @Override
    public void onCreate() {
        super.onCreate();
        telemetryThread = new HandlerThread("TelemetryThread");
        telemetryThread.start();
        telemetryHandler = new Handler(telemetryThread.getLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String vinNumber = intent != null ? intent.getStringExtra("VIN_NUMBER") : null;
        Log.d("xfcgvhbjnm","TELEMETRY SERVICE STARTED");
        this.context = this;
        startTelemetry(vinNumber != null ? vinNumber : "UNKNOWN");
        return START_STICKY;
    }

    private void startTelemetry(String VINNUMBER) {
//        evClient = new EVWebSocketClient(VINNUMBER, URI.create("ws://192.168.1.110:8081/ws/ev"));
        evClient = new EVWebSocketClient(VINNUMBER, URI.create("ws://103.174.148.224:8081/ws/ev"));

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
                    Log.e("Telemetry", "Thread interrupted", e);
                    return;
                }
            }
            serialPortManager = serialApplication.getSerialPortManager();
            if (serialPortManager != null) {
                serialPortManager.sendContinuousSensorData();
            } else {
                Log.e("Telemetry", "serialPortManager is null!");
            }
        }).start();
    }
    private void sendDataContinuously(String vin) {
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

                    sendVehicleData(context, (float) roundTo2Decimal(TelemetryDataManager.getInstance().getTeleODO()), TelemetryDataManager.getInstance().getTeleChargingState(), TelemetryDataManager.getInstance().getTeleHandleLockState(),
                            TelemetryDataManager.getInstance().getTeleSeatLockState(), TelemetryDataManager.getInstance().getTeleBMSMosStates(), TelemetryDataManager.getInstance().getTeleCurrentRider(),
                            TelemetryDataManager.getInstance().getTeleBmsLifeCycles(), TelemetryDataManager.getInstance().getTeleLatLong());

                    if (shdtelemetrywhenunlocked) {
                        sendRealTimeData(vin);
                        sendRealtimeDataOnceEvenAfterLocking = 0;
                    } else if (sendRealtimeDataOnceEvenAfterLocking == 0) {
                        sendRealTimeData(vin);
                        sendRealtimeDataOnceEvenAfterLocking++;
                    }

                    telemetryHandler.postDelayed(this, 1000);
                }
            }
        }, 1000);
    }

    private void sendVehicleData(Context context, float odo, int chargingState,
                                 int handleLockState, int seatLockState, String bmsMosStates,
                                 String riders, int bmslifecycles, String lat_long) {

        MetadataManager metadata = MetadataManager.get();
        String vinNumber = metadata.getVin();

        if (vinNumber.contains("UNKNOWN")) {
            Log.w("Telemetry", "VIN unknown — skipping sendVehicleData");
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

        String rideOSversion;
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            rideOSversion = "rideOS " + pInfo.versionName+"_GA";
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

        VehicleData data = anyChanged ?
                new VehicleData(vinNumber, model, ownerName, phoneNumber, controllerID,
                        motorID, bmsID, chargerID, rideOSversion, smartKeyID,
                        odo, chargingState, handleLockState, seatLockState,
                        bmsMosStates, riders, bmslifecycles, lat_long, time)

                :
                new VehicleData(vinNumber, null, null, null, null,
                        null, null, null, rideOSversion, null,
                        odo, chargingState, handleLockState, seatLockState,
                        bmsMosStates, riders, bmslifecycles, lat_long, time);
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

        apiService.saveVehicleData(data).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) { }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("Telemetry", "Failed to send vehicle data", t);
            }
        });
    }

    private void sendRealTimeData(String vin) {
        RealTimeData data = collectTelemetryData(vin);

        if (isInternetAvailable()) {
            // Send staged data first
            if (TelemetryStagingManager.getInstance().hasData()) {
                telemetryHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        RealTimeData r;
                        while ((r = TelemetryStagingManager.getInstance().poll()) != null) {
                            DataUsageTracker tracker = new DataUsageTracker();
                            tracker.startTracking();

                            sendToServer(r);

                            tracker.logUsage("Staged Payload");

                            try {
                                Thread.sleep(30);
                            } catch (InterruptedException e) {
                                // ignore
                            }
                        }
                    }
                });
            }

            // Send current data
            //DataUsageTracker tracker = new DataUsageTracker();
            //tracker.startTracking();

            sendToServer(data);

            //tracker.logUsage("Currdent Payload");

        } else {
            // Stage the current data
            TelemetryStagingManager.getInstance().addData(data);
        }
    }

    private RealTimeData collectTelemetryData(String vin) {
        RealTimeData data = new RealTimeData();
        data.vinNumber = vin;
        data.speed_kmph = TelemetryDataManager.getInstance().getTeleSpeed();
        data.tripkm= roundTo2Decimal(TelemetryDataManager.getInstance().getTeleTrip());
        data.controllerMosTemp = TelemetryDataManager.getInstance().getTeleControllerMosTemp();
        data.motorTemp = TelemetryDataManager.getInstance().getTeleMotorTemp();
        data.bmsMosTemp = TelemetryDataManager.getInstance().getTeleBMSMosTemp();
        data.ntc = TelemetryDataManager.getInstance().getTeleNTC();
        data.currentconsumption = roundTo3Decimal(TelemetryDataManager.getInstance().getTeleCurrentConsumption());
        data.batvoltage = roundTo1Decimal(TelemetryDataManager.getInstance().getTeleBatVoltage());
        data.remainingCapacity_AH = roundTo3Decimal(TelemetryDataManager.getInstance().getTeleRemainingCapacityAH());
        data.inAH = roundTo3Decimal(TelemetryDataManager.getInstance().getTeleTotalAH());
        data.inAH_by_charger = roundTo3Decimal(TelemetryDataManager.getInstance().getTeleChargerAH());
        data.inAH_by_regen = roundTo3Decimal(TelemetryDataManager.getInstance().getTeleRegenAH());
        data.outAH = roundTo3Decimal(TelemetryDataManager.getInstance().getTeleDischargeAH());
        data.ev_power_state = shdtelemetrywhenunlocked ? "ON" : "OFF";
        data.currentRider = shdtelemetrywhenunlocked ? TelemetryDataManager.getInstance().getTeleCurrentRider() : "Scooter Locked";
        data.tirepressure = TelemetryDataManager.getInstance().getTeleTirePressure();
        data.soc = TelemetryDataManager.getInstance().getTeleSOC();
        data.apuSoc = TelemetryDataManager.getInstance().getTeleAPUSOC();
        data.lat_long = TelemetryDataManager.getInstance().getTeleLatLong();
        if (data.lat_long == null || data.lat_long.isEmpty()) {
            data.lat_long = "0.0,0.0";   // fallback if GPS not ready
        }
        data.time = java.time.OffsetDateTime.now().toString();
        return data;

    }
    private void sendToServer(RealTimeData data) {
        apiService.saveRealTimeData(data).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) { }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // If sending fails due to network, re-stage
                TelemetryStagingManager.getInstance().addData(data);
                Log.e("TelemetryService", "Failed to send data, staging", t);
            }
        });
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
                        //Log.d("TelemetryService", "Updated location = " + latlong);
                    }

                    @Override
                    public void onProviderDisabled(@NonNull String provider) {
                        //Log.w("TelemetryService", "Provider disabled: " + provider);
                    }

                    @Override
                    public void onProviderEnabled(@NonNull String provider) {
                        //Log.d("TelemetryService", "Provider enabled: " + provider);
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {
                        //Log.d("TelemetryService", "Status changed: " + provider + " status=" + status);
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


    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}
