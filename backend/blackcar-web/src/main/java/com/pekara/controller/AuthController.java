package com.pekara.controller;

import com.pekara.dto.request.LoginRequest;
import com.pekara.dto.request.NewPasswordRequest;
import com.pekara.dto.request.RegisterRequest;
import com.pekara.dto.request.ResetPasswordRequest;
import com.pekara.dto.response.AuthResponse;
import com.pekara.dto.response.MessageResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {


    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.debug("Login attempt for email: {}", request.getEmail());

        // TODO: Implement login logic via AuthService
        // - Validate email and password
        // - Check if account is activated
        // - Generate JWT token
        // - Return AuthResponse with token, email, and role

        log.info("User logged in successfully: {}", request.getEmail());
        return ResponseEntity.ok(new AuthResponse("dummy-token", request.getEmail(), "USER"));
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.debug("Registration attempt for email: {}", request.getEmail());

        // TODO: Implement registration logic via AuthService
        // - Validate all required fields
        // - Check if passwords match
        // - Check if email is already registered
        // - Create user account (inactive)
        // - Generate activation token (24h validity)
        // - Send activation email with link
        // - Set default profile image if not provided

        log.info("User registered successfully: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MessageResponse("Registration successful. Please check your email to activate your account."));
    }

    @GetMapping("/activate")
    public ResponseEntity<MessageResponse> activateAccount(@RequestParam("token") String token) {
        log.debug("Account activation attempt with token");

        // TODO: Implement account activation logic via AuthService
        // - Validate activation token
        // - Check if token is expired (24h)
        // - Activate user account

        log.info("Account activated successfully");
        return ResponseEntity.ok(new MessageResponse("Account activated successfully. You can now login."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.debug("Password reset requested for email: {}", request.getEmail());

        // TODO: Implement password reset request logic via AuthService
        // - Validate email exists
        // - Generate password reset token
        // - Send email with reset link

        log.debug("Password reset email sent to: {}", request.getEmail());
        return ResponseEntity.ok(new MessageResponse("Password reset link has been sent to your email."));
    }

    @PostMapping("/new-password")
    public ResponseEntity<MessageResponse> setNewPassword(@Valid @RequestBody NewPasswordRequest request) {
        log.debug("New password submission attempt");

        // TODO: Implement new password logic via AuthService
        // - Validate reset token
        // - Check if token is expired
        // - Update user password

        log.info("Password changed successfully");
        return ResponseEntity.ok(new MessageResponse("Password has been reset successfully."));
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout() {
        log.debug("Logout request received");

        // TODO: Implement logout logic
        // - For drivers: check if they have active ride
        // - If driver is marked inactive during ride, become inactive after ride ends
        // - Clear session/token

        log.debug("User logged out successfully");
        return ResponseEntity.ok(new MessageResponse("Logged out successfully."));
    }
}
