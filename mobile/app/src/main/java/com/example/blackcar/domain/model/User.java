package com.example.blackcar.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class User {
    @NonNull
    private final String id;
    @NonNull
    private final String email;
    @NonNull
    private final String username;
    @Nullable
    private final String firstName;
    @Nullable
    private final String lastName;
    @NonNull
    private final UserRole role;

    public User(
            @NonNull String id,
            @NonNull String email,
            @NonNull String username,
            @Nullable String firstName,
            @Nullable String lastName,
            @NonNull UserRole role
    ) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
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

    @Nullable
    public String getFirstName() {
        return firstName;
    }

    @Nullable
    public String getLastName() {
        return lastName;
    }

    @NonNull
    public UserRole getRole() {
        return role;
    }
}
