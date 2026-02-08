package com.example.blackcar.data.api.model;

public class ChangePasswordRequest {
    private final String currentPassword;
    private final String newPassword;
    private final String confirmPassword;

    public ChangePasswordRequest(String currentPassword, String newPassword, String confirmPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
        this.confirmPassword = confirmPassword;
    }
}
