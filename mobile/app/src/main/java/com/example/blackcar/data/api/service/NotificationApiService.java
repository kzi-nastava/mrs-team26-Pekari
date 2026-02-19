package com.example.blackcar.data.api.service;

import com.example.blackcar.data.api.model.RegisterFcmTokenRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * API service for FCM token registration and management.
 */
public interface NotificationApiService {

    /**
     * Register FCM token for the current authenticated user.
     * Backend subscribes the token to user-{email} topic.
     * If user is ADMIN, also subscribes to 'admins' topic for panic notifications.
     */
    @POST("notifications/register-token")
    Call<Void> registerToken(@Body RegisterFcmTokenRequest request);

    /**
     * Unsubscribe FCM token from admins topic (called on admin logout).
     */
    @POST("notifications/unsubscribe-admin")
    Call<Void> unsubscribeAdmin(@Body RegisterFcmTokenRequest request);
}
