package com.example.blackcar.data.repository;

import androidx.annotation.NonNull;

import com.example.blackcar.data.api.ApiClient;
import com.example.blackcar.data.api.model.CreateFavoriteRouteRequest;
import com.example.blackcar.data.api.model.FavoriteRouteResponse;
import com.example.blackcar.data.api.model.MessageResponse;
import com.example.blackcar.data.api.service.ProfileApiService;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FavoriteRoutesRepository {

    public interface RepoCallback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    private final ProfileApiService api = ApiClient.getProfileService();

    public void getFavoriteRoutes(RepoCallback<List<FavoriteRouteResponse>> callback) {
        api.getFavoriteRoutes().enqueue(new Callback<List<FavoriteRouteResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<FavoriteRouteResponse>> call,
                                   @NonNull Response<List<FavoriteRouteResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(parseErrorMessage(response, "Failed to load favorite routes"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<FavoriteRouteResponse>> call,
                                 @NonNull Throwable t) {
                callback.onError("Network error. Please check your internet connection.");
            }
        });
    }

    public void createFavoriteRoute(CreateFavoriteRouteRequest request,
                                    RepoCallback<FavoriteRouteResponse> callback) {
        api.createFavoriteRoute(request).enqueue(new Callback<FavoriteRouteResponse>() {
            @Override
            public void onResponse(@NonNull Call<FavoriteRouteResponse> call,
                                   @NonNull Response<FavoriteRouteResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(parseErrorMessage(response, "Failed to add to favorites"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<FavoriteRouteResponse> call,
                                 @NonNull Throwable t) {
                callback.onError("Network error. Please check your internet connection.");
            }
        });
    }

    public void deleteFavoriteRoute(Long id, RepoCallback<MessageResponse> callback) {
        api.deleteFavoriteRoute(id).enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(@NonNull Call<MessageResponse> call,
                                   @NonNull Response<MessageResponse> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(parseErrorMessage(response, "Failed to remove from favorites"));
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
