package com.example.blackcar.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class ProfileUpdateRequest {
    @NonNull
    private final String firstName;
    @NonNull
    private final String lastName;
    @NonNull
    private final String phoneNumber;
    @NonNull
    private final String address;
    @Nullable
    private final String profilePicture;

    public ProfileUpdateRequest(
            @NonNull String firstName,
            @NonNull String lastName,
            @NonNull String phoneNumber,
            @NonNull String address,
            @Nullable String profilePicture
    ) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.profilePicture = profilePicture;
    }

    @NonNull
    public String getFirstName() {
        return firstName;
    }

    @NonNull
    public String getLastName() {
        return lastName;
    }

    @NonNull
    public String getPhoneNumber() {
        return phoneNumber;
    }

    @NonNull
    public String getAddress() {
        return address;
    }

    @Nullable
    public String getProfilePicture() {
        return profilePicture;
    }
}
