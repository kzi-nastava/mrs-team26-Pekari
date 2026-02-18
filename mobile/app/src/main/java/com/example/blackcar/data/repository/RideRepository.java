package com.example.blackcar.data.repository;

import androidx.annotation.NonNull;

import com.example.blackcar.data.api.ApiClient;
import com.example.blackcar.data.api.model.ActiveRideResponse;
import com.example.blackcar.data.api.model.AdminRideDetailResponse;
import com.example.blackcar.data.api.model.AdminRideHistoryFilter;
import com.example.blackcar.data.api.model.AdminRideHistoryResponse;
import com.example.blackcar.data.api.model.CancelRideRequest;
import com.example.blackcar.data.api.model.DriverRideHistoryResponse;
import com.example.blackcar.data.api.model.EstimateRideRequest;
import com.example.blackcar.data.api.model.MessageResponse;
import com.example.blackcar.data.api.model.OrderRideRequest;
import com.example.blackcar.data.api.model.OrderRideResponse;
import com.example.blackcar.data.api.model.PaginatedResponse;
import com.example.blackcar.data.api.model.PassengerRideDetailResponse;
import com.example.blackcar.data.api.model.PassengerRideHistoryResponse;
import com.example.blackcar.data.api.model.RideEstimateResponse;
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

    public void estimateRide(EstimateRideRequest request, RepoCallback<RideEstimateResponse> callback) {
        api.estimateRide(request).enqueue(
                new Callback<RideEstimateResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<RideEstimateResponse> call,
                                         @NonNull Response<RideEstimateResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            callback.onError(parseErrorMessage(response, "Estimate failed"));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<RideEstimateResponse> call,
                                        @NonNull Throwable t) {
                        callback.onError("Network error. Please check your internet connection.");
                    }
                });
    }

    public void orderRide(OrderRideRequest request, RepoCallback<OrderRideResponse> callback) {
        api.orderRide(request).enqueue(
                new Callback<OrderRideResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<OrderRideResponse> call,
                                         @NonNull Response<OrderRideResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            String msg = parseErrorMessage(response, "Order failed");
                            if (response.code() == 409) {
                                msg = msg.contains("driver") ? msg : "No active drivers available. Please try again later.";
                            } else if (response.code() == 403) {
                                msg = msg.contains("blocked") ? msg : "You have been blocked and cannot order rides.";
                            } else if (response.code() == 401) {
                                msg = "Please log in to request a ride.";
                            }
                            callback.onError(msg);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<OrderRideResponse> call,
                                        @NonNull Throwable t) {
                        callback.onError("Network error. Please check your internet connection.");
                    }
                });
    }

    public void getActiveRideForPassenger(RepoCallback<ActiveRideResponse> callback) {
        api.getActiveRideForPassenger().enqueue(
                new Callback<ActiveRideResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ActiveRideResponse> call,
                                         @NonNull Response<ActiveRideResponse> response) {
                        if (response.isSuccessful()) {
                            callback.onSuccess(response.body());
                        } else {
                            if (response.code() == 404) {
                                callback.onSuccess(null);
                            } else {
                                callback.onError(parseErrorMessage(response, "Failed to load active ride"));
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ActiveRideResponse> call,
                                        @NonNull Throwable t) {
                        callback.onError("Network error. Please check your internet connection.");
                    }
                });
    }

    public void getActiveRideForDriver(RepoCallback<ActiveRideResponse> callback) {
        api.getActiveRideForDriver().enqueue(
                new Callback<ActiveRideResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ActiveRideResponse> call,
                                         @NonNull Response<ActiveRideResponse> response) {
                        if (response.isSuccessful()) {
                            callback.onSuccess(response.body());
                        } else {
                            if (response.code() == 404 || response.code() == 204) {
                                callback.onSuccess(null);
                            } else {
                                callback.onError(parseErrorMessage(response, "Failed to load active ride"));
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ActiveRideResponse> call,
                                        @NonNull Throwable t) {
                        callback.onError("Network error. Please check your internet connection.");
                    }
                });
    }

    public void cancelRide(Long rideId, String reason, RepoCallback<MessageResponse> callback) {
        api.cancelRide(rideId, new CancelRideRequest(reason)).enqueue(
                new Callback<MessageResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<MessageResponse> call,
                                         @NonNull Response<MessageResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            callback.onError(parseErrorMessage(response, "Failed to cancel ride"));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<MessageResponse> call,
                                        @NonNull Throwable t) {
                        callback.onError("Network error. Please check your internet connection.");
                    }
                });
    }

    private String parseErrorMessage(Response<?> response, String fallback) {
        if (response.errorBody() == null) return fallback;
        try {
            String body = response.errorBody().string();
            if (body == null || body.isEmpty()) return fallback;
            org.json.JSONObject json = new org.json.JSONObject(body);
            if (json.has("message")) {
                String msg = json.optString("message", fallback);
                return msg.isEmpty() ? fallback : msg;
            }
        } catch (Exception ignored) {}
        return fallback;
    }
}
