package com.pekara.controller;

import com.pekara.dto.response.DriverBasicDto;
import com.pekara.dto.response.PassengerBasicDto;
import com.pekara.dto.response.WebDriverBasicDto;
import com.pekara.dto.response.WebPassengerBasicDto;
import com.pekara.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin", description = "Admin management endpoints")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "List all drivers", description = "Get list of all drivers for admin dropdown - Protected endpoint (Admins only)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/drivers")
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

    @Operation(summary = "List all passengers", description = "Get list of all passengers for admin dropdown - Protected endpoint (Admins only)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/passengers")
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
}
