package com.example.blackcar.data.repository;

import androidx.annotation.NonNull;

import com.example.blackcar.data.api.ApiClient;
import com.example.blackcar.data.api.model.BlockUserRequest;
import com.example.blackcar.data.api.model.DriverBasicInfo;
import com.example.blackcar.data.api.model.MessageResponse;
import com.example.blackcar.data.api.model.PassengerBasicInfo;
import com.example.blackcar.data.api.model.UserListItemResponse;
import com.example.blackcar.data.api.service.AdminApiService;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminRepository {

    public interface RepoCallback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    private final AdminApiService api = ApiClient.getAdminService();

    public void getDrivers(RepoCallback<List<UserListItemResponse>> callback) {
        api.getDrivers().enqueue(new Callback<List<UserListItemResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<UserListItemResponse>> call,
                                  @NonNull Response<List<UserListItemResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(mapError(response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<UserListItemResponse>> call, @NonNull Throwable t) {
                callback.onError("Network error. Please check your internet connection.");
            }
        });
    }

    public void getPassengers(RepoCallback<List<UserListItemResponse>> callback) {
        api.getPassengers().enqueue(new Callback<List<UserListItemResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<UserListItemResponse>> call,
                                  @NonNull Response<List<UserListItemResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(mapError(response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<UserListItemResponse>> call, @NonNull Throwable t) {
                callback.onError("Network error. Please check your internet connection.");
            }
        });
    }

    public void getDriversBasic(RepoCallback<List<DriverBasicInfo>> callback) {
        api.getDriversBasic().enqueue(new Callback<List<DriverBasicInfo>>() {
            @Override
            public void onResponse(@NonNull Call<List<DriverBasicInfo>> call,
                                  @NonNull Response<List<DriverBasicInfo>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(mapError(response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<DriverBasicInfo>> call, @NonNull Throwable t) {
                callback.onError("Network error. Please check your internet connection.");
            }
        });
    }

    public void getPassengersBasic(RepoCallback<List<PassengerBasicInfo>> callback) {
        api.getPassengersBasic().enqueue(new Callback<List<PassengerBasicInfo>>() {
            @Override
            public void onResponse(@NonNull Call<List<PassengerBasicInfo>> call,
                                  @NonNull Response<List<PassengerBasicInfo>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(mapError(response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<PassengerBasicInfo>> call, @NonNull Throwable t) {
                callback.onError("Network error. Please check your internet connection.");
            }
        });
    }

    public void setUserBlock(String userId, boolean blocked, String blockedNote, RepoCallback<String> callback) {
        Long id;
        try {
            id = Long.parseLong(userId);
        } catch (NumberFormatException e) {
            callback.onError("Invalid user ID");
            return;
        }

        BlockUserRequest request = new BlockUserRequest(blocked, blocked ? blockedNote : null);
        api.setUserBlock(id, request).enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(@NonNull Call<MessageResponse> call, @NonNull Response<MessageResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body().getMessage());
                } else {
                    callback.onError(parseErrorMessage(response, mapError(response.code())));
                }
            }

            @Override
            public void onFailure(@NonNull Call<MessageResponse> call, @NonNull Throwable t) {
                callback.onError("Network error. Please check your internet connection.");
            }
        });
    }

    private static String parseErrorMessage(Response<?> response, String fallback) {
        if (response.errorBody() == null) return fallback;
        try {
            String body = response.errorBody().string();
            if (body == null || body.isEmpty()) return fallback;
            org.json.JSONObject json = new org.json.JSONObject(body);
            if (json.has("message")) {
                String msg = json.optString("message", fallback);
                return msg.isEmpty() ? fallback : msg;
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private static String mapError(int code) {
        if (code == 401 || code == 403) {
            return "Session expired or access denied. Please log in again.";
        }
        if (code == 400) {
            return "Invalid request.";
        }
        return "Operation failed. Please try again.";
    }
}
