package com.pekara.controller;

import com.pekara.dto.request.WebChangePasswordRequest;
import com.pekara.dto.request.WebCreateFavoriteRouteRequest;
import com.pekara.dto.request.WebUpdateProfileRequest;
import com.pekara.dto.response.WebDriverProfileResponse;
import com.pekara.dto.response.WebFavouriteRouteResponse;
import com.pekara.dto.response.WebMessageResponse;
import com.pekara.dto.response.WebPassengerProfileResponse;
import com.pekara.mapper.FavoriteRouteMapper;
import com.pekara.model.Driver;
import com.pekara.model.User;
import com.pekara.repository.DriverRepository;
import com.pekara.repository.UserRepository;
import com.pekara.service.FavoriteRouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder;
    private final FavoriteRouteService favoriteRouteService;
    private final FavoriteRouteMapper favoriteRouteMapper;

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
                driver.getIsActive(),
                driver.getBlocked(),
                driver.getBlockedNote()
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
                user.getAverageRating(),
                user.getBlocked(),
                user.getBlockedNote()
        );

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get admin profile", description = "Get the currently authenticated admin's profile information")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public ResponseEntity<WebPassengerProfileResponse> getAdminProfile(@AuthenticationPrincipal String email) {
        log.debug("Admin profile requested for: {}", email);

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
                user.getAverageRating(),
                user.getBlocked(),
                user.getBlockedNote()
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

    @Operation(summary = "Update admin profile", description = "Update the currently authenticated admin's profile information")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin")
    public ResponseEntity<WebMessageResponse> updateAdminProfile(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody WebUpdateProfileRequest request) {
        log.debug("Admin profile update requested for: {}", email);

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

    @Operation(summary = "Change password", description = "Change the currently authenticated user's password")
    @PostMapping("/change-password")
    public ResponseEntity<WebMessageResponse> changePassword(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody WebChangePasswordRequest request) {
        log.debug("Password change requested for: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body(new WebMessageResponse("Current password is incorrect"));
        }

        // Verify new passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest().body(new WebMessageResponse("New passwords do not match"));
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ResponseEntity.ok(new WebMessageResponse("Password changed successfully"));
    }

    @Operation(summary = "Get favourite routes", description = "Fetch the current user's favourite/saved routes")
    @PreAuthorize("hasRole('PASSENGER')")
    @GetMapping("/favourite-routes")
    public ResponseEntity<List<WebFavouriteRouteResponse>> getFavouriteRoutes(
            @AuthenticationPrincipal String email) {

        log.debug("Favourite routes requested for user: {}", email);

        var routes = favoriteRouteService.getFavoriteRoutes(email);
        List<WebFavouriteRouteResponse> response = routes.stream()
                .map(favoriteRouteMapper::toWebResponse)
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Create favourite route", description = "Save a route as favourite")
    @PreAuthorize("hasRole('PASSENGER')")
    @PostMapping("/favourite-routes")
    public ResponseEntity<WebFavouriteRouteResponse> createFavouriteRoute(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody WebCreateFavoriteRouteRequest request) {

        log.debug("Create favourite route requested for user: {}", email);

        var route = favoriteRouteService.createFavoriteRoute(
                email,
                request.getName(),
                favoriteRouteMapper.toLocationPointDto(request.getPickup()),
                favoriteRouteMapper.toLocationPointDtoList(request.getStops()),
                favoriteRouteMapper.toLocationPointDto(request.getDropoff()),
                request.getVehicleType(),
                request.getBabyTransport(),
                request.getPetTransport()
        );

        WebFavouriteRouteResponse response = favoriteRouteMapper.toWebResponse(route);
        return ResponseEntity.status(201).body(response);
    }

    @Operation(summary = "Delete favourite route", description = "Remove a route from favourites")
    @PreAuthorize("hasRole('PASSENGER')")
    @DeleteMapping("/favourite-routes/{id}")
    public ResponseEntity<WebMessageResponse> deleteFavouriteRoute(
            @AuthenticationPrincipal String email,
            @PathVariable Long id) {

        log.debug("Delete favourite route {} requested for user: {}", id, email);

        favoriteRouteService.deleteFavoriteRoute(email, id);
        return ResponseEntity.ok(new WebMessageResponse("Favourite route deleted successfully"));
    }
}
