package com.fanconn.rideOS.network;

import com.fanconn.rideOS.RealTimeData;
import com.fanconn.rideOS.RealTimeDataApi;
import com.fanconn.rideOS.UIUpdater;

import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

// Use the correct class name that holds your real-time fields
public interface ApiService {
    @POST("api/vehicle/save")
    Call<ResponseBody> saveVehicleData(@Body VehicleData data);

    // Change RealTimeData to VehicleData (or your class name)
    @POST("api/realtimedata/save")
    Call<ResponseBody> saveRealTimeData(@Body RealTimeDataApi data);

    @POST("api/rider/add")
    Call<ResponseBody> addRider(@Body Map<String, Object> data);

    @PUT("api/rider/update/{id}")
    Call<ResponseBody> updateRider(@Path("id") int id, @Body Map<String, Object> data);

    @DELETE("api/rider/delete/{id}")
    Call<ResponseBody> deleteRider(@Path("id") int id);
}
