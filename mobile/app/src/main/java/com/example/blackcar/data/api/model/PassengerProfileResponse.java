package com.example.blackcar.data.api.model;

public class PassengerProfileResponse {
    private String id;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String address;
    private String profilePicture;
    private String createdAt;
    private String updatedAt;
    private Integer totalRides;
    private Double averageRating;
    private Boolean blocked;
    private String blockedNote;

    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getUsername() { return username; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getAddress() { return address; }
    public String getProfilePicture() { return profilePicture; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public Integer getTotalRides() { return totalRides; }
    public Double getAverageRating() { return averageRating; }
    public Boolean getBlocked() { return blocked; }
    public String getBlockedNote() { return blockedNote; }
}
