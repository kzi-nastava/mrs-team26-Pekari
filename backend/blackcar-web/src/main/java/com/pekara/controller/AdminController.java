package com.pekara.controller;

import com.pekara.dto.request.WebBlockUserRequest;
import com.pekara.dto.response.WebMessageResponse;
import com.pekara.dto.response.WebUserListItemResponse;
import com.pekara.model.Driver;
import com.pekara.model.User;
import com.pekara.model.UserRole;
import com.pekara.repository.DriverRepository;
import com.pekara.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin", description = "Admin-only endpoints for user management")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final DriverRepository driverRepository;

    @Operation(summary = "List all drivers", description = "Get list of drivers for block/unblock management")
    @GetMapping("/drivers")
    public ResponseEntity<List<WebUserListItemResponse>> getDrivers() {
        log.debug("Admin requested drivers list");
        List<Driver> drivers = driverRepository.findAll();
        List<WebUserListItemResponse> response = drivers.stream()
                .map(this::toUserListItem)
                .toList();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List all passengers", description = "Get list of passengers for block/unblock management")
    @GetMapping("/passengers")
    public ResponseEntity<List<WebUserListItemResponse>> getPassengers() {
        log.debug("Admin requested passengers list");
        List<User> passengers = userRepository.findByRoleOrderByEmail(UserRole.PASSENGER);
        List<WebUserListItemResponse> response = passengers.stream()
                .map(this::toUserListItem)
                .toList();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update user block state", description = "Block or unblock a driver or passenger")
    @PatchMapping("/users/{id}")
    public ResponseEntity<WebMessageResponse> updateUserBlock(
            @PathVariable Long id,
            @Valid @RequestBody WebBlockUserRequest request) {
        log.debug("Admin requested block update for user id: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        if (user.getRole() == UserRole.ADMIN) {
            throw new IllegalArgumentException("Cannot block an administrator");
        }

        user.setBlocked(request.getBlocked());
        user.setBlockedNote(Boolean.TRUE.equals(request.getBlocked()) ? request.getBlockedNote() : null);
        userRepository.save(user);

        String message = Boolean.TRUE.equals(request.getBlocked())
                ? "User has been blocked."
                : "User has been unblocked.";
        return ResponseEntity.ok(new WebMessageResponse(message));
    }

    private WebUserListItemResponse toUserListItem(Driver driver) {
        return new WebUserListItemResponse(
                driver.getId().toString(),
                driver.getEmail(),
                driver.getFirstName(),
                driver.getLastName(),
                "driver",
                driver.getBlocked(),
                driver.getBlockedNote()
        );
    }

    private WebUserListItemResponse toUserListItem(User user) {
        return new WebUserListItemResponse(
                user.getId().toString(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name().toLowerCase(),
                user.getBlocked(),
                user.getBlockedNote()
        );
    }
}
