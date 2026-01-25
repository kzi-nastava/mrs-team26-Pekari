package com.pekara.controller;

import com.pekara.dto.request.WebUpdateProfileRequest;
import com.pekara.dto.response.WebDriverProfileResponse;
import com.pekara.dto.response.WebFavouriteRouteResponse;
import com.pekara.dto.response.WebMessageResponse;
import com.pekara.dto.response.WebPaginatedResponse;
import com.pekara.dto.response.WebPassengerProfileResponse;
import com.pekara.model.Driver;
import com.pekara.model.User;
import com.pekara.repository.DriverRepository;
import com.pekara.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/profile")
@Tag(name = "Profile", description = "Profile endpoints")
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository userRepository;
    private final DriverRepository driverRepository;

    @Operation(summary = "Get driver profile", description = "Get the currently authenticated driver's profile information")
    @PreAuthorize("hasRole('DRIVER')")
    @GetMapping("/driver")
    public ResponseEntity<WebDriverProfileResponse> getDriverProfile(@AuthenticationPrincipal String email) {
        log.debug("Driver profile requested for: {}", email);

        Driver driver = driverRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Driver not found: " + email));

        WebDriverProfileResponse response = new WebDriverProfileResponse(
                driver.getId().toString(),
                driver.getEmail(),
                driver.getUsername(),
                driver.getFirstName(),
                driver.getLastName(),
                driver.getPhoneNumber(),
                driver.getAddress(),
                driver.getProfilePicture(),
                driver.getCreatedAt(),
                driver.getUpdatedAt(),
                driver.getLicenseNumber(),
                driver.getLicenseExpiry(),
                driver.getLicensePlate(),
                driver.getVehicleModel(),
                driver.getVehicleType(),
                driver.getLicensePlate(),
                driver.getNumberOfSeats(),
                driver.getBabyFriendly(),
                driver.getPetFriendly(),
                driver.getAverageRating(),
                driver.getTotalRides(),
                driver.getIsActive()
        );

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get passenger profile", description = "Get the currently authenticated passenger's profile information")
    @PreAuthorize("hasRole('PASSENGER')")
    @GetMapping("/passenger")
    public ResponseEntity<WebPassengerProfileResponse> getPassengerProfile(@AuthenticationPrincipal String email) {
        log.debug("Passenger profile requested for: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        WebPassengerProfileResponse response = new WebPassengerProfileResponse(
                user.getId().toString(),
                user.getEmail(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getAddress(),
                user.getProfilePicture(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getTotalRides(),
                user.getAverageRating()
        );

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update driver profile", description = "Update the currently authenticated driver's profile information")
    @PreAuthorize("hasRole('DRIVER')")
    @PutMapping("/driver")
    public ResponseEntity<WebMessageResponse> updateDriverProfile(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody WebUpdateProfileRequest request) {
        log.debug("Driver profile update requested for: {}", email);

        Driver driver = driverRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Driver not found: " + email));

        driver.setFirstName(request.getFirstName());
        driver.setLastName(request.getLastName());
        driver.setPhoneNumber(request.getPhoneNumber());
        driver.setAddress(request.getAddress());
        if (request.getProfilePicture() != null) {
            driver.setProfilePicture(request.getProfilePicture());
        }

        driverRepository.save(driver);

        return ResponseEntity.ok(new WebMessageResponse("Profile updated successfully"));
    }

    @Operation(summary = "Update passenger profile", description = "Update the currently authenticated passenger's profile information")
    @PreAuthorize("hasRole('PASSENGER')")
    @PutMapping("/passenger")
    public ResponseEntity<WebMessageResponse> updatePassengerProfile(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody WebUpdateProfileRequest request) {
        log.debug("Passenger profile update requested for: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setAddress(request.getAddress());
        if (request.getProfilePicture() != null) {
            user.setProfilePicture(request.getProfilePicture());
        }

        userRepository.save(user);

        return ResponseEntity.ok(new WebMessageResponse("Profile updated successfully"));
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
