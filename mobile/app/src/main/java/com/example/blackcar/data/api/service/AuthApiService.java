package com.example.blackcar.data.api.service;

import com.example.blackcar.data.api.model.LoginRequest;
import com.example.blackcar.data.api.model.LoginResponse;
import com.example.blackcar.data.api.model.RegisterDriverResponse;
import com.example.blackcar.data.api.model.RegisterResponse;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface AuthApiService {

    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @Multipart
    @POST("auth/register/user")
    Call<RegisterResponse> registerUser(
            @Part("email") RequestBody email,
            @Part("username") RequestBody username,
            @Part("password") RequestBody password,
            @Part("firstName") RequestBody firstName,
            @Part("lastName") RequestBody lastName,
            @Part("address") RequestBody address,
            @Part("phoneNumber") RequestBody phoneNumber
    );

    @Multipart
    @POST("auth/register/driver")
    Call<RegisterDriverResponse> registerDriver(
            @Part("email") RequestBody email,
            @Part("firstName") RequestBody firstName,
            @Part("lastName") RequestBody lastName,
            @Part("address") RequestBody address,
            @Part("phoneNumber") RequestBody phoneNumber,
            @Part("vehicle.model") RequestBody vehicleModel,
            @Part("vehicle.type") RequestBody vehicleType,
            @Part("vehicle.licensePlate") RequestBody licensePlate,
            @Part("vehicle.numberOfSeats") RequestBody numberOfSeats,
            @Part("vehicle.babyFriendly") RequestBody babyFriendly,
            @Part("vehicle.petFriendly") RequestBody petFriendly
    );

    @POST("auth/logout")
    Call<Void> logout();
}
