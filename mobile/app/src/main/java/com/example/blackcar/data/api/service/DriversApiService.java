package com.example.blackcar.data.api.service;

import com.example.blackcar.data.api.model.OnlineDriverWithVehicleResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface DriversApiService {

    @GET("drivers/online-with-vehicles")
    Call<List<OnlineDriverWithVehicleResponse>> getOnlineDriversWithVehicles(
            @Query("page") int page,
            @Query("size") int size
    );
}
