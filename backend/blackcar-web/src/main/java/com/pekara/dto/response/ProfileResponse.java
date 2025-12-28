package com.pekara.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {

    private String id;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String address;

    /**
     * Expected values based on web app: admin | passenger | driver
     */
    private String role;

    /**
     * Optional profile image (URL or Base64 string).
     */
    private String profilePicture;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
