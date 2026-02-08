package com.example.blackcar.data.api.model;

public class UpdateProfileRequest {
    private final String firstName;
    private final String lastName;
    private final String phoneNumber;
    private final String address;
    private final String profilePicture;

    public UpdateProfileRequest(String firstName, String lastName, String phoneNumber, String address, String profilePicture) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.profilePicture = profilePicture;
    }
}
