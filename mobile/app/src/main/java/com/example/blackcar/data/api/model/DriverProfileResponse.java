package com.example.blackcar.data.api.model;

public class DriverProfileResponse {
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

    private String licenseNumber;
    private String licenseExpiry;
    private String vehicleRegistration;
    private String vehicleModel;
    private String vehicleType;
    private String licensePlate;
    private Integer numberOfSeats;
    private Boolean babyFriendly;
    private Boolean petFriendly;
    private Double averageRating;
    private Integer totalRides;
    private Boolean isActive;

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

    public String getLicenseNumber() { return licenseNumber; }
    public String getLicenseExpiry() { return licenseExpiry; }
    public String getVehicleRegistration() { return vehicleRegistration; }
    public String getVehicleModel() { return vehicleModel; }
    public String getVehicleType() { return vehicleType; }
    public String getLicensePlate() { return licensePlate; }
    public Integer getNumberOfSeats() { return numberOfSeats; }
    public Boolean getBabyFriendly() { return babyFriendly; }
    public Boolean getPetFriendly() { return petFriendly; }
    public Double getAverageRating() { return averageRating; }
    public Integer getTotalRides() { return totalRides; }
    public Boolean getIsActive() { return isActive; }
}
