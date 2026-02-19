package com.example.blackcar.data.api.service;

import com.example.blackcar.data.api.model.ChangePasswordRequest;
import com.example.blackcar.data.api.model.CreateFavoriteRouteRequest;
import com.example.blackcar.data.api.model.DriverProfileResponse;
import com.example.blackcar.data.api.model.FavoriteRouteResponse;
import com.example.blackcar.data.api.model.MessageResponse;
import com.example.blackcar.data.api.model.PassengerProfileResponse;
import com.example.blackcar.data.api.model.UpdateProfileRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ProfileApiService {

    @GET("profile/driver")
    Call<DriverProfileResponse> getDriverProfile();

    @GET("profile/passenger")
    Call<PassengerProfileResponse> getPassengerProfile();

    @GET("profile/admin")
    Call<PassengerProfileResponse> getAdminProfile();

    @PUT("profile/driver")
    Call<MessageResponse> updateDriverProfile(@Body UpdateProfileRequest request);

    @PUT("profile/passenger")
    Call<MessageResponse> updatePassengerProfile(@Body UpdateProfileRequest request);

    @PUT("profile/admin")
    Call<MessageResponse> updateAdminProfile(@Body UpdateProfileRequest request);

    @POST("profile/change-password")
    Call<MessageResponse> changePassword(@Body ChangePasswordRequest request);

    // Favorite routes
    @GET("profile/favourite-routes")
    Call<List<FavoriteRouteResponse>> getFavoriteRoutes();

    @POST("profile/favourite-routes")
    Call<FavoriteRouteResponse> createFavoriteRoute(@Body CreateFavoriteRouteRequest request);

    @DELETE("profile/favourite-routes/{id}")
    Call<MessageResponse> deleteFavoriteRoute(@Path("id") Long id);
}
