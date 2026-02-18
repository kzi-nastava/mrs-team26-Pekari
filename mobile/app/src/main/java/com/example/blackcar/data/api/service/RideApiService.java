package com.example.blackcar.data.api.service;

import com.example.blackcar.data.api.model.ActiveRideResponse;
import com.example.blackcar.data.api.model.AdminRideDetailResponse;
import com.example.blackcar.data.api.model.AdminRideHistoryFilter;
import com.example.blackcar.data.api.model.AdminRideHistoryResponse;
import com.example.blackcar.data.api.model.DriverRideHistoryResponse;
import com.example.blackcar.data.api.model.EstimateRideRequest;
import com.example.blackcar.data.api.model.MessageResponse;
import com.example.blackcar.data.api.model.OrderRideRequest;
import com.example.blackcar.data.api.model.OrderRideResponse;
import com.example.blackcar.data.api.model.PaginatedResponse;
import com.example.blackcar.data.api.model.PassengerRideDetailResponse;
import com.example.blackcar.data.api.model.PassengerRideHistoryResponse;
import com.example.blackcar.data.api.model.CancelRideRequest;
import com.example.blackcar.data.api.model.RideEstimateResponse;
import com.example.blackcar.data.api.model.RideHistoryFilterRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface RideApiService {

    @POST("rides/estimate")
    Call<RideEstimateResponse> estimateRide(@Body EstimateRideRequest request);

    @POST("rides/order")
    Call<OrderRideResponse> orderRide(@Body OrderRideRequest request);

    @GET("rides/active/passenger")
    Call<ActiveRideResponse> getActiveRideForPassenger();

    @POST("rides/{rideId}/cancel")
    Call<MessageResponse> cancelRide(@Path("rideId") Long rideId, @Body CancelRideRequest request);

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

    // Admin endpoints
    @POST("rides/history/admin/all")
    Call<PaginatedResponse<AdminRideHistoryResponse>> getAdminRideHistory(
            @Body AdminRideHistoryFilter filter,
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("rides/admin/{id}")
    Call<AdminRideDetailResponse> getAdminRideDetail(
            @Path("id") Long rideId
    );
}
