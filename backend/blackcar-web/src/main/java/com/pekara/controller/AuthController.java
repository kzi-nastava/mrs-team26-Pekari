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
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
    public ResponseEntity<WebAuthResponse> login(@Valid @RequestBody WebLoginRequest request, HttpServletResponse response) {
        log.debug("Login attempt for email: {}", request.getEmail());

        var serviceResponse = authService.login(request.getEmail(), request.getPassword());

        // Set JWT as HTTP-only cookie
        Cookie jwtCookie = new Cookie("jwt", serviceResponse.getToken());
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(false);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(24 * 60 * 60); // 24 hours
        response.addCookie(jwtCookie);

        WebAuthResponse webResponse = authMapper.toWebAuthResponse(serviceResponse);

        log.info("User logged in successfully: {}", request.getEmail());
        return ResponseEntity.ok(webResponse);
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

    @Operation(summary = "Register driver", description = "Create a new driver account (admin-initiated)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/register/driver", consumes = "multipart/form-data")
    public ResponseEntity<WebRegisterDriverResponse> registerDriver(@Valid @ModelAttribute WebRegisterDriverRequest request) {
        log.debug("Driver registration attempt for email: {}", request.getEmail());

        var serviceRequest = authMapper.toServiceRegisterDriverRequest(request);
        var serviceResponse = authService.registerDriver(serviceRequest);

        WebRegisterDriverResponse webResponse = authMapper.toWebRegisterDriverResponse(serviceResponse);

        log.info("Driver registered successfully: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(webResponse);
    }

    @GetMapping("/activate")
    public ResponseEntity<WebMessageResponse> activateAccount(@RequestParam("token") String token) {
        log.debug("Account activation attempt with token");

        var serviceResponse = authService.activateAccount(token);

        WebMessageResponse webResponse = authMapper.toWebMessageResponse(serviceResponse);

        log.info("Account activated successfully for user: {}", serviceResponse.getEmail());
        return ResponseEntity.ok(webResponse);
    }

    @GetMapping("/activation-info")
    public ResponseEntity<?> getActivationInfo(@RequestParam("token") String token) {
        log.debug("Activation info requested");

        var info = authService.getActivationInfo(token);
        return ResponseEntity.ok(info);
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

        var serviceResponse = authService.setNewPassword(request.getToken(), request.getNewPassword());

        log.info("Password set successfully for user: {}", serviceResponse.getEmail());
        return ResponseEntity.ok(new WebMessageResponse(serviceResponse.getMessage()));
    }

    @Operation(summary = "Get current user session", description = "Get current authenticated user from session")
    @GetMapping("/me")
    public ResponseEntity<WebAuthResponse> getCurrentUser(Authentication authentication) {
        log.debug("Getting current user session");

        String email = authentication.getName();
        var serviceResponse = authService.getCurrentUser(email);

        WebAuthResponse webResponse = authMapper.toWebAuthResponse(serviceResponse);

        log.info("Current user session retrieved: {}", email);
        return ResponseEntity.ok(webResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<WebMessageResponse> logout(HttpServletResponse response) {
        log.debug("Logout request received");

        // Clear JWT cookie
        Cookie jwtCookie = new Cookie("jwt", null);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(false); // Set to true in production with HTTPS
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(0); // Expire immediately
        response.addCookie(jwtCookie);

        // TODO: Implement additional logout logic
        // - For drivers: check if they have active ride
        // - If driver is marked inactive during ride, become inactive after ride ends

        log.debug("User logged out successfully");
        return ResponseEntity.ok(new WebMessageResponse("Logged out successfully."));
    }
}
