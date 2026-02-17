package com.example.blackcar.data.repository;

import androidx.annotation.NonNull;

import com.example.blackcar.data.api.ApiClient;
import com.example.blackcar.data.api.model.AdminRideDetailResponse;
import com.example.blackcar.data.api.model.AdminRideHistoryFilter;
import com.example.blackcar.data.api.model.AdminRideHistoryResponse;
import com.example.blackcar.data.api.model.DriverRideHistoryResponse;
import com.example.blackcar.data.api.model.PaginatedResponse;
import com.example.blackcar.data.api.model.PassengerRideDetailResponse;
import com.example.blackcar.data.api.model.PassengerRideHistoryResponse;
import com.example.blackcar.data.api.model.RideHistoryFilterRequest;
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

    public void getPassengerRideHistory(RideHistoryFilterRequest filter, int page, int size,
                                        RepoCallback<PaginatedResponse<PassengerRideHistoryResponse>> callback) {
        api.getPassengerRideHistory(filter, page, size).enqueue(
                new Callback<PaginatedResponse<PassengerRideHistoryResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<PaginatedResponse<PassengerRideHistoryResponse>> call,
                                         @NonNull Response<PaginatedResponse<PassengerRideHistoryResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            String message = "Failed to load ride history";
                            if (response.code() == 401) {
                                message = "Unauthorized. Please login again.";
                            } else if (response.code() == 403) {
                                message = "Access denied. Passenger role required.";
                            } else if (response.code() == 404) {
                                message = "No rides found";
                            }
                            callback.onError(message);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<PaginatedResponse<PassengerRideHistoryResponse>> call,
                                        @NonNull Throwable t) {
                        callback.onError("Network error. Please check your internet connection.");
                    }
                });
    }

    public void getPassengerRideDetail(Long rideId, RepoCallback<PassengerRideDetailResponse> callback) {
        api.getPassengerRideDetail(rideId).enqueue(
                new Callback<PassengerRideDetailResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<PassengerRideDetailResponse> call,
                                         @NonNull Response<PassengerRideDetailResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            String message = "Failed to load ride details";
                            if (response.code() == 401) {
                                message = "Unauthorized. Please login again.";
                            } else if (response.code() == 403) {
                                message = "Access denied.";
                            } else if (response.code() == 404) {
                                message = "Ride not found";
                            }
                            callback.onError(message);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<PassengerRideDetailResponse> call,
                                        @NonNull Throwable t) {
                        callback.onError("Network error. Please check your internet connection.");
                    }
                });
    }

    // Admin methods
    public void getAdminRideHistory(AdminRideHistoryFilter filter, int page, int size,
                                    RepoCallback<PaginatedResponse<AdminRideHistoryResponse>> callback) {
        api.getAdminRideHistory(filter, page, size).enqueue(
                new Callback<PaginatedResponse<AdminRideHistoryResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<PaginatedResponse<AdminRideHistoryResponse>> call,
                                         @NonNull Response<PaginatedResponse<AdminRideHistoryResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            String message = "Failed to load ride history";
                            if (response.code() == 401) {
                                message = "Unauthorized. Please login again.";
                            } else if (response.code() == 403) {
                                message = "Access denied. Admin role required.";
                            } else if (response.code() == 404) {
                                message = "No rides found";
                            }
                            callback.onError(message);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<PaginatedResponse<AdminRideHistoryResponse>> call,
                                        @NonNull Throwable t) {
                        callback.onError("Network error. Please check your internet connection.");
                    }
                });
    }

    public void getAdminRideDetail(Long rideId, RepoCallback<AdminRideDetailResponse> callback) {
        api.getAdminRideDetail(rideId).enqueue(
                new Callback<AdminRideDetailResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<AdminRideDetailResponse> call,
                                         @NonNull Response<AdminRideDetailResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            String message = "Failed to load ride details";
                            if (response.code() == 401) {
                                message = "Unauthorized. Please login again.";
                            } else if (response.code() == 403) {
                                message = "Access denied. Admin role required.";
                            } else if (response.code() == 404) {
                                message = "Ride not found";
                            }
                            callback.onError(message);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<AdminRideDetailResponse> call,
                                        @NonNull Throwable t) {
                        callback.onError("Network error. Please check your internet connection.");
                    }
                });
    }
}
