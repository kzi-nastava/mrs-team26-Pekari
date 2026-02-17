package com.example.blackcar.data.api.service;

import com.example.blackcar.data.api.model.DriverRideHistoryResponse;
import com.example.blackcar.data.api.model.PaginatedResponse;
import com.example.blackcar.data.api.model.PassengerRideDetailResponse;
import com.example.blackcar.data.api.model.PassengerRideHistoryResponse;
import com.example.blackcar.data.api.model.RideHistoryFilterRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface RideApiService {

    @GET("rides/history/driver")
    Call<PaginatedResponse<DriverRideHistoryResponse>> getDriverRideHistory(
            @Query("startDate") String startDate,
            @Query("endDate") String endDate,
            @Query("page") int page,
            @Query("size") int size
    );

    @POST("rides/history/passenger")
    Call<PaginatedResponse<PassengerRideHistoryResponse>> getPassengerRideHistory(
            @Body RideHistoryFilterRequest filter,
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("rides/{id}")
    Call<PassengerRideDetailResponse> getPassengerRideDetail(
            @Path("id") Long rideId
    );
}
