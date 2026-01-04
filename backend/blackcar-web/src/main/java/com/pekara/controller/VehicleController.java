package com.pekara.controller;

import com.pekara.dto.response.WebActiveVehicleResponse;
import com.pekara.dto.response.WebPaginatedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/vehicles")
@Tag(name = "Vehicles", description = "Vehicle management endpoints")
public class VehicleController {

    @Operation(summary = "Get active vehicles", description = "Get all currently active vehicles with their positions on the map - Public endpoint")
    @GetMapping("/active")
    public ResponseEntity<WebPaginatedResponse<WebActiveVehicleResponse>> getActiveVehicles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        log.debug("Active vehicles requested (page: {}, size: {})", page, size);

        // TODO: Implement active vehicles retrieval via VehicleService
        // - Fetch all vehicles that are currently active (driver logged in and available)
        // - Apply pagination (page, size)
        // - Include current location (latitude, longitude)
        // - Include status: FREE (available) or BUSY (in ride)
        // - Return paginated list of active vehicles

        List<WebActiveVehicleResponse> vehicles = List.of(
                new WebActiveVehicleResponse(
                        1L,
                        "STANDARD",
                        "NS-123-AB",
                        45.2671,
                        19.8335,
                        false,
                        "FREE",
                        new WebActiveVehicleResponse.DriverBasicInfo(1L, "John", "Doe")
                ),
                new WebActiveVehicleResponse(
                        2L,
                        "VAN",
                        "NS-456-CD",
                        45.2550,
                        19.8450,
                        true,
                        "BUSY",
                        new WebActiveVehicleResponse.DriverBasicInfo(2L, "Jane", "Smith")
                )
        );

        WebPaginatedResponse<WebActiveVehicleResponse> response = new WebPaginatedResponse<>(vehicles, page, size, 2L);
        return ResponseEntity.ok(response);
    }
}
