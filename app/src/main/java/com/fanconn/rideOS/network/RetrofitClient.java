//package com.fanconn.rideOS.network;
//
//import retrofit2.Retrofit;
//import retrofit2.converter.gson.GsonConverterFactory;
//
//public class RetrofitClient {
//    private static Retrofit retrofit;
//    public static Retrofit getClient() {
//        if (retrofit == null) {
//            retrofit = new Retrofit.Builder()
////                    .baseUrl("http://192.168.1.100:8080/api/vehicle/")
////                    .baseUrl("http://192.168.1.107:9090/api/vehicle/")
////                    .baseUrl("http://103.174.148.224:8081/")//original
//                    .baseUrl("http://sb.rivotmotors.com:8081/")
////                    .baseUrl("http://192.168.1.130:8081/")
////                    .baseUrl("http://103.104.231.197:9090/api/vehicle/")//server
//                    .addConverterFactory(GsonConverterFactory.create())
//                    .build();
//        }
//        return retrofit;
//    }
//}



package com.fanconn.rideOS.network;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static Retrofit retrofit;

    public static Retrofit getClient() {

        if (retrofit == null) {

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .build();

            retrofit = new Retrofit.Builder()

//                    .baseUrl("http://192.168.1.100:8080/api/vehicle/")
//                    .baseUrl("http://192.168.1.107:9090/api/vehicle/")
//                    .baseUrl("http://103.174.148.224:8081/")
//                    .baseUrl("http://192.168.1.134:8081/")
//                    .baseUrl("http://103.104.231.197:9090/api/vehicle/")

                    .baseUrl("http://sb.rivotmotors.com:8081/")
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }

        return retrofit;
    }
}