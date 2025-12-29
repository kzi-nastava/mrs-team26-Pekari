package com.pekara.controller;

import com.pekara.dto.response.FavouriteRouteResponse;
import com.pekara.dto.response.ProfileResponse;
import com.pekara.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/profile")
@Tag(name = "Profile", description = "Profile endpoints")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @Operation(summary = "Get profile", description = "Get the currently authenticated user's profile information")
    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile() {
        log.debug("Profile requested");

        ProfileResponse response = profileService.getCurrentProfile();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get favourite routes", description = "Fetch the current user's favourite/saved routes")
    @GetMapping("/favourite-routes")
    public ResponseEntity<List<FavouriteRouteResponse>> getFavouriteRoutes() {
        log.debug("Favourite routes requested");

        // TODO: Implement favourite routes retrieval via ProfileService / RouteService
        // - Identify current authenticated user from security context
        // - Fetch stored favourite routes for the user
        // - Each route should preserve ordered stops

        List<FavouriteRouteResponse> response = List.of(
                new FavouriteRouteResponse(
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

        return ResponseEntity.ok(response);
    }
}
