package com.example.blackcar.presentation.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.blackcar.domain.model.DriverInfo;
import com.example.blackcar.domain.model.ProfileData;
import com.example.blackcar.domain.model.UserRole;

public final class ProfileViewState {
    public final boolean isLoading;
    public final boolean isEditing;
    @Nullable
    public final ProfileData profile;
    @Nullable
    public final DriverInfo driverInfo;
    @Nullable
    public final String successMessage;
    @Nullable
    public final String errorMessage;
    @NonNull
    public final UserRole role;

    public ProfileViewState(
            boolean isLoading,
            boolean isEditing,
            @Nullable ProfileData profile,
            @Nullable DriverInfo driverInfo,
            @Nullable String successMessage,
            @Nullable String errorMessage,
            @NonNull UserRole role
    ) {
        this.isLoading = isLoading;
        this.isEditing = isEditing;
        this.profile = profile;
        this.driverInfo = driverInfo;
        this.successMessage = successMessage;
        this.errorMessage = errorMessage;
        this.role = role;
    }

    @NonNull
    public static ProfileViewState initial(@NonNull UserRole role) {
        return new ProfileViewState(false, false, null, null, null, null, role);
    }
}
