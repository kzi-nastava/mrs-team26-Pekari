package com.example.blackcar.data.api.service;

import com.example.blackcar.data.api.model.BlockUserRequest;
import com.example.blackcar.data.api.model.MessageResponse;
import com.example.blackcar.data.api.model.UserListItemResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.Path;

public interface AdminApiService {

    @GET("admin/drivers")
    Call<List<UserListItemResponse>> getDrivers();

    @GET("admin/passengers")
    Call<List<UserListItemResponse>> getPassengers();

    @PATCH("admin/users/{id}")
    Call<MessageResponse> setUserBlock(
            @Path("id") Long userId,
            @Body BlockUserRequest request
    );
}
