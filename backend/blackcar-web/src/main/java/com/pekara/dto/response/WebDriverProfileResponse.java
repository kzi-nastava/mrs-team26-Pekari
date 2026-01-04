package com.pekara.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebDriverProfileResponse {

    private String id;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String address;
    private String profilePicture;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Driver-specific fields
    private String licenseNumber;
    private String licenseExpiry;
    private String vehicleRegistration;
    private Double averageRating;
    private Integer totalRides;
    private Boolean isActive;
}
