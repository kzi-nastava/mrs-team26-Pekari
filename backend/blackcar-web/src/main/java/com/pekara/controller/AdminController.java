package com.pekara.controller;

import com.pekara.dto.PricingDto;
import com.pekara.dto.request.WebBlockUserRequest;
import com.pekara.dto.response.DriverBasicDto;
import com.pekara.dto.response.PassengerBasicDto;
import com.pekara.dto.response.WebDriverBasicDto;
import com.pekara.dto.response.WebMessageResponse;
import com.pekara.dto.response.WebPassengerBasicDto;
import com.pekara.dto.response.WebUserListItemResponse;
import com.pekara.model.Driver;
import com.pekara.model.User;
import com.pekara.model.UserRole;
import com.pekara.repository.DriverRepository;
import com.pekara.repository.UserRepository;
import com.pekara.service.AdminService;
import com.pekara.service.PricingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin", description = "Admin-only endpoints for user management")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final PricingService pricingService;

    @Operation(summary = "Get all pricing", description = "Get list of pricing for all vehicle types")
    @GetMapping("/pricing")
    public ResponseEntity<List<PricingDto>> getAllPricing() {
        log.debug("Admin requested pricing list");
        return ResponseEntity.ok(pricingService.getAllPricing());
    }

    @Operation(summary = "Update pricing", description = "Update pricing for a vehicle type")
    @PutMapping("/pricing")
    public ResponseEntity<PricingDto> updatePricing(@Valid @RequestBody PricingDto request) {
        log.debug("Admin requested pricing update for vehicle type: {}", request.getVehicleType());
        return ResponseEntity.ok(pricingService.updatePricing(request));
    }

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

    @Operation(summary = "List all drivers (basic)", description = "Get list of all drivers for admin dropdown")
    @GetMapping("/drivers/basic")
    public ResponseEntity<List<WebDriverBasicDto>> listDrivers() {
        log.debug("Admin requested list of drivers");
        List<DriverBasicDto> drivers = adminService.listDriversForAdmin();
        List<WebDriverBasicDto> response = drivers.stream()
                .map(d -> WebDriverBasicDto.builder()
                        .id(d.getId())
                        .firstName(d.getFirstName())
                        .lastName(d.getLastName())
                        .email(d.getEmail())
                        .build())
                .collect(Collectors.toList());
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

    @Operation(summary = "List all passengers (basic)", description = "Get list of all passengers for admin dropdown")
    @GetMapping("/passengers/basic")
    public ResponseEntity<List<WebPassengerBasicDto>> listPassengers() {
        log.debug("Admin requested list of passengers");
        List<PassengerBasicDto> passengers = adminService.listPassengersForAdmin();
        List<WebPassengerBasicDto> response = passengers.stream()
                .map(p -> WebPassengerBasicDto.builder()
                        .id(p.getId())
                        .firstName(p.getFirstName())
                        .lastName(p.getLastName())
                        .email(p.getEmail())
                        .build())
                .collect(Collectors.toList());
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
