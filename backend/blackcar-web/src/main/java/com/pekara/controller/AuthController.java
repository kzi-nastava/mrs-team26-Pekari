package com.pekara.controller;

import com.pekara.dto.request.WebLoginRequest;
import com.pekara.dto.request.WebNewPasswordRequest;
import com.pekara.dto.request.WebRegisterDriverRequest;
import com.pekara.dto.request.WebRegisterUserRequest;
import com.pekara.dto.request.WebResetPasswordRequest;
import com.pekara.dto.response.WebMessageResponse;
import com.pekara.dto.response.WebRegisterDriverResponse;
import com.pekara.dto.response.WebRegisterResponse;
import com.pekara.dto.response.WebAuthResponse;
import com.pekara.mapper.AuthMapper;
import com.pekara.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication and registration endpoints")
public class AuthController {

    private final AuthService authService;
    private final AuthMapper authMapper;


    @Operation(summary = "User login", description = "Authenticate user with email and password")
    @PostMapping("/login")
    public ResponseEntity<WebAuthResponse> login(@Valid @RequestBody WebLoginRequest request) {
        log.debug("Login attempt for email: {}", request.getEmail());

        // TODO: Implement login logic via AuthService
        // - Validate email and password
        // - Check if account is activated
        // - Generate JWT token
        // - Return AuthResponse with token, email, and role

        log.info("User logged in successfully: {}", request.getEmail());
        return ResponseEntity.ok(new WebAuthResponse("dummy-token", request.getEmail(), "USER"));
    }

    @Operation(summary = "Register user", description = "Create a new user account")
    @PostMapping(value = "/register/user", consumes = "multipart/form-data")
    public ResponseEntity<WebRegisterResponse> registerUser(@Valid @ModelAttribute WebRegisterUserRequest webRequest) {
        log.debug("User registration attempt for email: {}", webRequest.getEmail());

        var serviceRequest = authMapper.toServiceRegisterUserRequest(webRequest);
        var serviceResponse = authService.registerUser(serviceRequest);

        WebRegisterResponse webResponse = authMapper.toWebRegisterResponse(serviceResponse);

        log.info("User registered successfully: {}", webRequest.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(webResponse);
    }

        @Operation(summary = "Register driver", description = "Create a new driver account (typically pending admin approval)")
        @PostMapping(value = "/register/driver", consumes = "multipart/form-data")
        public ResponseEntity<WebRegisterDriverResponse> registerDriver(@Valid @ModelAttribute WebRegisterDriverRequest request) {
        log.debug("Driver registration attempt for email: {}", request.getEmail());

        // TODO: Implement driver registration logic via AuthService
        // - Validate all required fields
        // - Check if passwords match
        // - Check if email is already registered
        // - Validate vehicle fields (make/model/year/licensePlate/vin)
        // - Create driver account (inactive / pending approval)
        // - Notify admins or create approval workflow entry
        // - Send activation email when approved or when account is created

        log.info("Driver registered successfully (pending approval): {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new WebRegisterDriverResponse(
                "Driver registration submitted successfully. Your account may require admin approval.",
                request.getEmail(),
                "PENDING_APPROVAL"
            ));
        }

    @GetMapping("/activate")
    public ResponseEntity<WebMessageResponse> activateAccount(@RequestParam("token") String token) {
        log.debug("Account activation attempt with token");

        // TODO: Implement account activation logic via AuthService
        // - Validate activation token
        // - Check if token is expired (24h)
        // - Activate user account

        log.info("Account activated successfully");
        return ResponseEntity.ok(new WebMessageResponse("Account activated successfully. You can now login."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<WebMessageResponse> resetPassword(@Valid @RequestBody WebResetPasswordRequest request) {
        log.debug("Password reset requested for email: {}", request.getEmail());

        // TODO: Implement password reset request logic via AuthService
        // - Validate email exists
        // - Generate password reset token
        // - Send email with reset link

        log.debug("Password reset email sent to: {}", request.getEmail());
        return ResponseEntity.ok(new WebMessageResponse("Password reset link has been sent to your email."));
    }

    @PostMapping("/new-password")
    public ResponseEntity<WebMessageResponse> setNewPassword(@Valid @RequestBody WebNewPasswordRequest request) {
        log.debug("New password submission attempt");

        // TODO: Implement new password logic via AuthService
        // - Validate reset token
        // - Check if token is expired
        // - Update user password

        log.info("Password changed successfully");
        return ResponseEntity.ok(new WebMessageResponse("Password has been reset successfully."));
    }

    @PostMapping("/logout")
    public ResponseEntity<WebMessageResponse> logout() {
        log.debug("Logout request received");

        // TODO: Implement logout logic
        // - For drivers: check if they have active ride
        // - If driver is marked inactive during ride, become inactive after ride ends
        // - Clear session/token

        log.debug("User logged out successfully");
        return ResponseEntity.ok(new WebMessageResponse("Logged out successfully."));
    }
}
