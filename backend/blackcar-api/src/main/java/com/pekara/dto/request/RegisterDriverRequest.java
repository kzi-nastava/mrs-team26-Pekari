package com.pekara.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterDriverRequest {
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String address;
    private byte[] profileImage;
    private String profileImageFileName;

    // Vehicle information
    private String vehicleModel;
    private String vehicleType;
    private String licensePlate;
    private Integer numberOfSeats;
    private Boolean babyFriendly;
    private Boolean petFriendly;
}
