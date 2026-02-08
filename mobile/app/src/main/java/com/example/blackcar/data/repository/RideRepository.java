package com.example.blackcar.data.repository;

import androidx.annotation.NonNull;

import com.example.blackcar.data.api.ApiClient;
import com.example.blackcar.data.api.model.DriverRideHistoryResponse;
import com.example.blackcar.data.api.model.PaginatedResponse;
import com.example.blackcar.data.api.service.RideApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RideRepository {

    public interface RepoCallback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    private final RideApiService api = ApiClient.getRideService();

    public void getDriverRideHistory(String startDate, String endDate, int page, int size,
                                     RepoCallback<PaginatedResponse<DriverRideHistoryResponse>> callback) {
        api.getDriverRideHistory(startDate, endDate, page, size).enqueue(
                new Callback<PaginatedResponse<DriverRideHistoryResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<PaginatedResponse<DriverRideHistoryResponse>> call,
                                         @NonNull Response<PaginatedResponse<DriverRideHistoryResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            String message = "Failed to load ride history";
                            if (response.code() == 401) {
                                message = "Unauthorized. Please login again.";
                            } else if (response.code() == 403) {
                                message = "Access denied. Driver role required.";
                            } else if (response.code() == 404) {
                                message = "No rides found";
                            }
                            callback.onError(message);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<PaginatedResponse<DriverRideHistoryResponse>> call,
                                        @NonNull Throwable t) {
                        callback.onError("Network error. Please check your internet connection.");
                    }
                });
    }
}
