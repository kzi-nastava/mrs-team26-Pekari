package com.pekara.controller;

import com.pekara.dto.request.UpdateDriverLocationRequest;
import com.pekara.dto.request.UpdateDriverOnlineStatusRequest;
import com.pekara.dto.response.DriverStateResponse;
import com.pekara.service.DriverStateService;
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
@RequestMapping("/api/v1/drivers")
@RequiredArgsConstructor
@Tag(name = "Drivers", description = "Driver state endpoints")
public class DriverStateController {

    private final DriverStateService driverStateService;

    @Operation(summary = "Set driver online/offline")
    @PreAuthorize("hasRole('DRIVER')")
    @PutMapping("/me/online")
    public ResponseEntity<DriverStateResponse> updateOnline(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody UpdateDriverOnlineStatusRequest request) {
        log.debug("Driver {} online status update", email);
        return ResponseEntity.ok(driverStateService.updateOnlineStatus(email, request));
    }

    @Operation(summary = "Update driver location")
    @PreAuthorize("hasRole('DRIVER')")
    @PutMapping("/me/location")
    public ResponseEntity<DriverStateResponse> updateLocation(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody UpdateDriverLocationRequest request) {
        log.debug("Driver {} location update", email);
        return ResponseEntity.ok(driverStateService.updateLocation(email, request));
    }

    @Operation(summary = "Get my driver state")
    @PreAuthorize("hasRole('DRIVER')")
    @GetMapping("/me/state")
    public ResponseEntity<DriverStateResponse> getMyState(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(driverStateService.getMyState(email));
    }

    @Operation(summary = "List online drivers")
    @GetMapping("/online")
    public ResponseEntity<List<DriverStateResponse>> getOnlineDrivers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(driverStateService.getOnlineDrivers(page, size));
    }
}
