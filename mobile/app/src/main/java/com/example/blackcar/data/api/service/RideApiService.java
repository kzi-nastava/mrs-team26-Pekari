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
import com.example.blackcar.data.api.model.RideStatsResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

import com.example.blackcar.data.api.model.InconsistencyReportRequest;
import com.example.blackcar.data.api.model.WebRideTrackingResponse;
import com.example.blackcar.data.api.model.StopRideEarlyRequest;
import com.example.blackcar.data.api.model.RideLocationUpdateRequest;

public interface RideApiService {

    @POST("rides/estimate")
    Call<RideEstimateResponse> estimateRide(@Body EstimateRideRequest request);

    @POST("rides/order")
    Call<OrderRideResponse> orderRide(@Body OrderRideRequest request);

    @GET("rides/active/passenger")
    Call<ActiveRideResponse> getActiveRideForPassenger();

    @GET("rides/active/driver")
    Call<ActiveRideResponse> getActiveRideForDriver();

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

    @GET("rides/active/all")
    Call<java.util.List<AdminRideHistoryResponse>> getAllActiveRides();

    // Driver ride management endpoints
    @POST("rides/{rideId}/start")
    Call<MessageResponse> startRide(@Path("rideId") Long rideId);

    @POST("rides/{rideId}/stop")
    Call<MessageResponse> completeRide(@Path("rideId") Long rideId);

    @POST("rides/{rideId}/stop")
    Call<MessageResponse> stopRideEarly(@Path("rideId") Long rideId, @Body StopRideEarlyRequest request);

    @POST("rides/{rideId}/panic")
    Call<MessageResponse> activatePanic(@Path("rideId") Long rideId);

    @POST("rides/{rideId}/location")
    Call<MessageResponse> updateRideLocation(@Path("rideId") Long rideId, @Body RideLocationUpdateRequest request);

    @GET("rides/{rideId}/track")
    Call<WebRideTrackingResponse> trackRide(@Path("rideId") Long rideId);

    @POST("rides/{rideId}/report-inconsistency")
    Call<MessageResponse> reportInconsistency(@Path("rideId") Long rideId, @Body InconsistencyReportRequest request);

    // Passenger ride management endpoints
    @POST("rides/{rideId}/request-stop")
    Call<MessageResponse> requestStopRide(@Path("rideId") Long rideId);

    // Admin panic mode endpoints
    @GET("rides/panic/active")
    Call<java.util.List<DriverRideHistoryResponse>> getActivePanicRides();

    // Ride stats endpoints
    @GET("rides/stats/driver")
    Call<RideStatsResponse> getDriverRideStats(
            @Query("startDate") String startDate,
            @Query("endDate") String endDate
    );

    @GET("rides/stats/passenger")
    Call<RideStatsResponse> getPassengerRideStats(
            @Query("startDate") String startDate,
            @Query("endDate") String endDate
    );

    @GET("rides/stats/admin")
    Call<RideStatsResponse> getAdminRideStats(
            @Query("startDate") String startDate,
            @Query("endDate") String endDate,
            @Query("scope") String scope,
            @Query("userId") Long userId
    );
}
