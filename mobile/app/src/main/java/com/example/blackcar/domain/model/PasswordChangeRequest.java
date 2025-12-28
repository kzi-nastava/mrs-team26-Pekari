package com.example.blackcar.domain.model;

import androidx.annotation.NonNull;

public final class PasswordChangeRequest {
    @NonNull
    private final String currentPassword;
    @NonNull
    private final String newPassword;
    @NonNull
    private final String confirmPassword;

    public PasswordChangeRequest(
            @NonNull String currentPassword,
            @NonNull String newPassword,
            @NonNull String confirmPassword
    ) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
        this.confirmPassword = confirmPassword;
    }

    @NonNull
    public String getCurrentPassword() {
        return currentPassword;
    }

    @NonNull
    public String getNewPassword() {
        return newPassword;
    }

    @NonNull
    public String getConfirmPassword() {
        return confirmPassword;
    }
}
