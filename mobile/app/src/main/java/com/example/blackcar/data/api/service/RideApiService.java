package com.example.blackcar.data.api.service;

import com.example.blackcar.data.api.model.DriverRideHistoryResponse;
import com.example.blackcar.data.api.model.PaginatedResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface RideApiService {

    @GET("rides/history/driver")
    Call<PaginatedResponse<DriverRideHistoryResponse>> getDriverRideHistory(
            @Query("startDate") String startDate,
            @Query("endDate") String endDate,
            @Query("page") int page,
            @Query("size") int size
    );
}
