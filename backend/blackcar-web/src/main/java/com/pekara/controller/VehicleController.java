package com.pekara.controller;

import com.pekara.dto.response.ActiveVehicleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/vehicles")
@Tag(name = "Vehicles", description = "Vehicle management endpoints")
public class VehicleController {

    @Operation(summary = "Get active vehicles", description = "Get all currently active vehicles with their positions on the map - Public endpoint")
    @GetMapping("/active")
    public ResponseEntity<List<ActiveVehicleResponse>> getActiveVehicles() {
        log.debug("Active vehicles requested");

        // TODO: Implement active vehicles retrieval via VehicleService
        // - Fetch all vehicles that are currently active (driver logged in and available)
        // - Include current location (latitude, longitude)
        // - Include status: FREE (available) or BUSY (in ride)
        // - Return list of active vehicles

        List<ActiveVehicleResponse> vehicles = List.of(
                new ActiveVehicleResponse(
                        1L,
                        "STANDARD",
                        "NS-123-AB",
                        45.2671,
                        19.8335,
                        false,
                        "FREE",
                        new ActiveVehicleResponse.DriverBasicInfo(1L, "John", "Doe")
                ),
                new ActiveVehicleResponse(
                        2L,
                        "VAN",
                        "NS-456-CD",
                        45.2550,
                        19.8450,
                        true,
                        "BUSY",
                        new ActiveVehicleResponse.DriverBasicInfo(2L, "Jane", "Smith")
                )
        );

        return ResponseEntity.ok(vehicles);
    }
}
