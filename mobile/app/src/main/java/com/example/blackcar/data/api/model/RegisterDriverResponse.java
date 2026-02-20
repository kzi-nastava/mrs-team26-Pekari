package com.example.blackcar.data.api.model;

import com.google.gson.annotations.SerializedName;

public class RegisterDriverResponse {

    @SerializedName("message")
    private String message;

    @SerializedName("email")
    private String email;

    @SerializedName("status")
    private String status;

    public String getMessage() {
        return message;
    }

    public String getEmail() {
        return email;
    }

    public String getStatus() {
        return status;
    }
}
