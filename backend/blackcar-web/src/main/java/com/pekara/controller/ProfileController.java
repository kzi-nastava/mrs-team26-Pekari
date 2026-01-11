package com.pekara.controller;

import com.pekara.dto.response.WebDriverProfileResponse;
import com.pekara.dto.response.WebFavouriteRouteResponse;
import com.pekara.dto.response.WebPaginatedResponse;
import com.pekara.dto.response.WebPassengerProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/profile")
@Tag(name = "Profile", description = "Profile endpoints")
public class ProfileController {

    @Operation(summary = "Get driver profile", description = "Get the currently authenticated driver's profile information")
    @PreAuthorize("hasRole('DRIVER')")
    @GetMapping("/driver")
    public ResponseEntity<WebDriverProfileResponse> getDriverProfile() {
        log.debug("Driver profile requested");

        // TODO: Implement profile retrieval via ProfileService
        // - Identify current authenticated user from security context
        // - Verify user has driver role
        // - Fetch driver info with driver-specific fields
        // - Return DriverProfileResponse

        WebDriverProfileResponse response = new WebDriverProfileResponse(
                "1",
                "john@example.com",
                "johndoe",
                "John",
                "Doe",
                "+381 64 000 000",
                "123 Main Street, City",
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                "DL123456789",
                "2026-12-31",
                "ABC123",
                4.8,
                156,
                true
        );

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get passenger profile", description = "Get the currently authenticated passenger's profile information")
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/passenger")
    public ResponseEntity<WebPassengerProfileResponse> getPassengerProfile() {
        log.debug("Passenger profile requested");

        // TODO: Implement profile retrieval via ProfileService
        // - Identify current authenticated user from security context
        // - Verify user has passenger role
        // - Fetch passenger info with passenger-specific fields
        // - Return PassengerProfileResponse

        WebPassengerProfileResponse response = new WebPassengerProfileResponse(
                "2",
                "jane@example.com",
                "janedoe",
                "Jane",
                "Doe",
                "+381 64 000 001",
                "456 Oak Street, City",
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                42,
                4.9
        );

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get favourite routes", description = "Fetch the current user's favourite/saved routes")
    @GetMapping("/favourite-routes")
    public ResponseEntity<WebPaginatedResponse<WebFavouriteRouteResponse>> getFavouriteRoutes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Favourite routes requested (page: {}, size: {})", page, size);

        // TODO: Implement favourite routes retrieval via ProfileService / RouteService
        // - Identify current authenticated user from security context
        // - Fetch stored favourite routes for the user
        // - Apply pagination (page, size)
        // - Each route should preserve ordered stops

        List<WebFavouriteRouteResponse> routes = List.of(
                new WebFavouriteRouteResponse(
                        1L,
                        "Home  Airport",
                        "Bulevar Osloboenja 1",
                        List.of("Trg slobode"),
                        "Aerodrom",
                        "STANDARD",
                        false,
                        false
                )
        );

        WebPaginatedResponse<WebFavouriteRouteResponse> response = new WebPaginatedResponse<>(routes, page, size, 1L);
        return ResponseEntity.ok(response);
    }
}
