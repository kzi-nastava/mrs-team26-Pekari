package com.pekara.controller;

import com.pekara.service.RideNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification registration and utilities")
public class NotificationController {

    private final RideNotificationService rideNotificationService;

    @Data
    public static class RegisterTokenRequest {
        @NotBlank
        private String fcmToken;
    }

    @Operation(summary = "Register FCM token for the current user and subscribe it to per-user topic")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/register-token")
    public ResponseEntity<Void> registerToken(@AuthenticationPrincipal String email,
                                              @Valid @RequestBody RegisterTokenRequest request) {
        log.info("[DEBUG_LOG] registerToken endpoint hit for {}", email);
        rideNotificationService.registerClientToken(email, request.getFcmToken());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Unsubscribe FCM token from admin topic (called on logout)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/unsubscribe-admin")
    public ResponseEntity<Void> unsubscribeAdminToken(@Valid @RequestBody RegisterTokenRequest request) {
        log.debug("Unsubscribing token from admin topic");
        rideNotificationService.unsubscribeFromAdminTopic(request.getFcmToken());
        return ResponseEntity.ok().build();
    }
}
