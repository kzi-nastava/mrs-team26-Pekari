package com.example.blackcar.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class ProfileData {
    @NonNull
    private final String id;
    @NonNull
    private final String email;
    @NonNull
    private final String username;
    @NonNull
    private final String firstName;
    @NonNull
    private final String lastName;
    @NonNull
    private final String phoneNumber;
    @NonNull
    private final String address;
    @NonNull
    private final UserRole role;
    @Nullable
    private final String profilePicture;

    public ProfileData(
            @NonNull String id,
            @NonNull String email,
            @NonNull String username,
            @NonNull String firstName,
            @NonNull String lastName,
            @NonNull String phoneNumber,
            @NonNull String address,
            @NonNull UserRole role,
            @Nullable String profilePicture
    ) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.role = role;
        this.profilePicture = profilePicture;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getEmail() {
        return email;
    }

    @NonNull
    public String getUsername() {
        return username;
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

    @NonNull
    public UserRole getRole() {
        return role;
    }

    @Nullable
    public String getProfilePicture() {
        return profilePicture;
    }

    @NonNull
    public ProfileData withUpdated(
            @NonNull String firstName,
            @NonNull String lastName,
            @NonNull String phoneNumber,
            @NonNull String address,
            @Nullable String profilePicture
    ) {
        return new ProfileData(id, email, username, firstName, lastName, phoneNumber, address, role, profilePicture);
    }
}
