package com.fanconn.rideOS.network;

import com.fanconn.rideOS.RealTimeData;
import com.fanconn.rideOS.UIUpdater;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

// Use the correct class name that holds your real-time fields
public interface ApiService {
    @POST("api/vehicle/save")
    Call<ResponseBody> saveVehicleData(@Body VehicleData data);

    // Change RealTimeData to VehicleData (or your class name)
    @POST("api/realtimedata/save")
    Call<ResponseBody> saveRealTimeData(@Body RealTimeData data);
}
