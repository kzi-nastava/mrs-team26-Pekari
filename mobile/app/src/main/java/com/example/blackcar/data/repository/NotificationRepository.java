package com.example.blackcar.data.repository;

import android.util.Log;

import com.example.blackcar.data.api.ApiClient;
import com.example.blackcar.data.api.model.RegisterFcmTokenRequest;
import com.example.blackcar.data.api.service.NotificationApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository for FCM token management operations.
 */
public class NotificationRepository {

    private static final String TAG = "NotificationRepository";

    private final NotificationApiService apiService;

    public NotificationRepository() {
        this.apiService = ApiClient.getNotificationService();
    }

    /**
     * Register FCM token with backend.
     * Backend will subscribe the token to user topic and admins topic (if admin).
     */
    public void registerToken(String fcmToken, RegistrationCallback callback) {
        if (fcmToken == null || fcmToken.isEmpty()) {
            Log.w(TAG, "FCM token is null or empty, skipping registration");
            if (callback != null) {
                callback.onFailure("FCM token is empty");
            }
            return;
        }

        RegisterFcmTokenRequest request = new RegisterFcmTokenRequest(fcmToken);
        apiService.registerToken(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.i(TAG, "FCM token registered successfully");
                    if (callback != null) {
                        callback.onSuccess();
                    }
                } else {
                    Log.e(TAG, "Failed to register FCM token: " + response.code());
                    if (callback != null) {
                        callback.onFailure("Registration failed: " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "FCM token registration error", t);
                if (callback != null) {
                    callback.onFailure(t.getMessage());
                }
            }
        });
    }

    /**
     * Unsubscribe FCM token from admins topic (called on admin logout).
     */
    public void unsubscribeAdmin(String fcmToken, RegistrationCallback callback) {
        if (fcmToken == null || fcmToken.isEmpty()) {
            Log.w(TAG, "FCM token is null or empty, skipping unsubscribe");
            if (callback != null) {
                callback.onSuccess(); // Not a failure, just nothing to do
            }
            return;
        }

        RegisterFcmTokenRequest request = new RegisterFcmTokenRequest(fcmToken);
        apiService.unsubscribeAdmin(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.i(TAG, "Unsubscribed from admins topic successfully");
                    if (callback != null) {
                        callback.onSuccess();
                    }
                } else {
                    Log.e(TAG, "Failed to unsubscribe from admins topic: " + response.code());
                    if (callback != null) {
                        callback.onFailure("Unsubscribe failed: " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Admin unsubscribe error", t);
                if (callback != null) {
                    callback.onFailure(t.getMessage());
                }
            }
        });
    }

    /**
     * Callback interface for registration operations.
     */
    public interface RegistrationCallback {
        void onSuccess();
        void onFailure(String error);
    }
}
