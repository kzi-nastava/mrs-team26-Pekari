package com.example.blackcar.data.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * Request body for FCM token registration.
 */
public class RegisterFcmTokenRequest {

    @SerializedName("fcmToken")
    private String fcmToken;

    public RegisterFcmTokenRequest(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }
}
