package com.fanconn.rideOS.telemetry;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.util.Locale;

public class GpsLocationManager {

    private static final String TAG = "GpsLocationManager";

    private static GpsLocationManager instance;

    private Context context;
    private LocationManager locationManager;

    private GpsLocationManager() {
    }

    public static synchronized GpsLocationManager getInstance() {

        if (instance == null) {
            instance = new GpsLocationManager();
        }

        return instance;
    }

    public synchronized void init(Context context) {

        if (this.context != null) {
            return;
        }

        this.context = context.getApplicationContext();

        locationManager =
                (LocationManager) this.context.getSystemService(
                        Context.LOCATION_SERVICE
                );
    }

    public void start() {

        if (context == null || locationManager == null) {
           // Log.e(TAG, "GPS manager not initialized");
            return;
        }

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {

           // Log.e(TAG, "ACCESS_FINE_LOCATION permission not granted");
            return;
        }

        try {

            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000,
                    0,
                    locationListener,
                    Looper.getMainLooper()
            );

           // Log.d(TAG, "GPS location updates started");

        } catch (Exception e) {
          //  Log.e(TAG, "Failed to start GPS updates", e);
        }
    }

    public void stop() {

        if (locationManager == null) {
            return;
        }

        try {

            locationManager.removeUpdates(locationListener);

            //Log.d(TAG, "GPS location updates stopped");

        } catch (Exception e) {
           // Log.e(TAG, "Failed to stop GPS updates", e);
        }
    }

    private final LocationListener locationListener =
            new LocationListener() {

                @Override
                public void onLocationChanged(Location location) {

                    if (location == null) {
                        return;
                    }

                    String latLong = String.format(
                            Locale.US,
                            "%.6f, %.6f",
                            location.getLatitude(),
                            location.getLongitude()
                    );

                    VehicleTelemetryManager.getInstance()
                            .setLatLong(latLong);

                    //Log.d(TAG, "Location: " + latLong);
                }

                @Override
                public void onProviderEnabled(String provider) {
                    //Log.d(TAG, "GPS provider enabled: " + provider);
                }

                @Override
                public void onProviderDisabled(String provider) {
                   // Log.d(TAG, "GPS provider disabled: " + provider);
                }
            };
}