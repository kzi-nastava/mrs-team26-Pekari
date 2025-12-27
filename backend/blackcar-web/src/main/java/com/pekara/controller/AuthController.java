package com.pekara.controller;

import com.pekara.dto.request.LoginRequest;
import com.pekara.dto.request.NewPasswordRequest;
import com.pekara.dto.request.RegisterRequest;
import com.pekara.dto.request.ResetPasswordRequest;
import com.pekara.dto.response.AuthResponse;
import com.pekara.dto.response.MessageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {


    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        // TODO: Implement login logic via AuthService
        // - Validate email and password
        // - Check if account is activated
        // - Generate JWT token
        // - Return AuthResponse with token, email, and role
        return ResponseEntity.ok(new AuthResponse("dummy-token", request.getEmail(), "USER"));
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@RequestBody RegisterRequest request) {
        // TODO: Implement registration logic via AuthService
        // - Validate all required fields
        // - Check if passwords match
        // - Check if email is already registered
        // - Create user account (inactive)
        // - Generate activation token (24h validity)
        // - Send activation email with link
        // - Set default profile image if not provided
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MessageResponse("Registration successful. Please check your email to activate your account."));
    }

    @GetMapping("/activate")
    public ResponseEntity<MessageResponse> activateAccount(@RequestParam("token") String token) {
        // TODO: Implement account activation logic via AuthService
        // - Validate activation token
        // - Check if token is expired (24h)
        // - Activate user account
        return ResponseEntity.ok(new MessageResponse("Account activated successfully. You can now login."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
        // TODO: Implement password reset request logic via AuthService
        // - Validate email exists
        // - Generate password reset token
        // - Send email with reset link
        return ResponseEntity.ok(new MessageResponse("Password reset link has been sent to your email."));
    }

    @PostMapping("/new-password")
    public ResponseEntity<MessageResponse> setNewPassword(@RequestBody NewPasswordRequest request) {
        // TODO: Implement new password logic via AuthService
        // - Validate reset token
        // - Check if token is expired
        // - Update user password
        return ResponseEntity.ok(new MessageResponse("Password has been reset successfully."));
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout() {
        // TODO: Implement logout logic
        // - For drivers: check if they have active ride
        // - If driver is marked inactive during ride, become inactive after ride ends
        // - Clear session/token
        return ResponseEntity.ok(new MessageResponse("Logged out successfully."));
    }
}
