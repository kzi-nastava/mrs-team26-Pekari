package com.pekara.mapper;

import com.pekara.dto.request.RegisterUserRequest;
import com.pekara.dto.request.WebRegisterUserRequest;
import com.pekara.dto.response.AuthResponse;
import com.pekara.dto.response.RegisterResponse;
import com.pekara.dto.response.WebAuthResponse;
import com.pekara.dto.response.WebRegisterResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Component
public class AuthMapper {

    /**
     * Maps service layer AuthResponse to presentation layer WebAuthResponse
     * Keeps presentation layer decoupled from service/domain layer
     */
    public WebAuthResponse toWebAuthResponse(AuthResponse authResponse) {
        if (authResponse == null) {
            return null;
        }

        return new WebAuthResponse(
                authResponse.getToken(),
                authResponse.getEmail(),
                authResponse.getRole()
        );
    }

    /**
     * Maps service layer RegisterResponse to presentation layer WebRegisterResponse
     */
    public WebRegisterResponse toWebRegisterResponse(RegisterResponse registerResponse) {
        if (registerResponse == null) {
            return null;
        }

        return WebRegisterResponse.builder()
                .message(registerResponse.getMessage())
                .userId(registerResponse.getUserId())
                .email(registerResponse.getEmail())
                .build();
    }

    /**
     * Maps web layer RegisterUserRequest to service layer RegisterUserRequest
     * Converts MultipartFile to byte array for service layer processing
     */
    public RegisterUserRequest toServiceRegisterUserRequest(WebRegisterUserRequest webRequest) {
        if (webRequest == null) {
            return null;
        }

        byte[] imageBytes = null;
        String imageFileName = null;

        if (webRequest.getProfileImage() != null && !webRequest.getProfileImage().isEmpty()) {
            MultipartFile file = webRequest.getProfileImage();
            try {
                imageBytes = file.getBytes();
                imageFileName = file.getOriginalFilename();
            } catch (IOException e) {
                log.error("Failed to read profile image file", e);
                // Continue without image
            }
        }

        return RegisterUserRequest.builder()
                .email(webRequest.getEmail())
                .username(webRequest.getUsername())
                .password(webRequest.getPassword())
                .firstName(webRequest.getFirstName())
                .lastName(webRequest.getLastName())
                .phoneNumber(webRequest.getPhoneNumber())
                .address(webRequest.getAddress())
                .profileImage(imageBytes)
                .profileImageFileName(imageFileName)
                .build();
    }
}
