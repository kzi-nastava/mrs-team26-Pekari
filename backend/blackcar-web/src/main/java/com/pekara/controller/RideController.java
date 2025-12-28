package com.pekara.controller;

import com.pekara.dto.request.CancelRideRequest;
import com.pekara.dto.request.EstimateRideRequest;
import com.pekara.dto.request.OrderRideRequest;
import com.pekara.dto.response.MessageResponse;
import com.pekara.dto.response.OrderRideResponse;
import com.pekara.dto.response.RideEstimateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/v1/rides")
@Tag(name = "Rides", description = "Ride management endpoints")
public class RideController {

    @Operation(summary = "Estimate ride", description = "Calculate ride estimation (price, duration, distance) - Public endpoint")
    @PostMapping("/estimate")
    public ResponseEntity<RideEstimateResponse> estimateRide(@Valid @RequestBody EstimateRideRequest request) {
        log.debug("Ride estimation requested from {} to {}", request.getPickupLocation(), request.getDropoffLocation());

        // TODO: Implement ride estimation logic via RideService
        // - Calculate distance between pickup and dropoff locations
        // - Calculate estimated duration based on traffic
        // - Calculate price based on vehicle type, distance, baby/pet transport
        // - Filter available vehicles by baby/pet transport requirements
        // - Return estimation details

        RideEstimateResponse response = new RideEstimateResponse(
                new BigDecimal("250.00"),
                15,
                5.5,
                request.getVehicleType()
        );

        log.debug("Ride estimate calculated: {} RSD, {} minutes", response.getEstimatedPrice(), response.getEstimatedDurationMinutes());
        return ResponseEntity.ok(response);
    }


    @Operation(summary = "Order ride", description = "Order a ride now or schedule it up to 5 hours ahead - Protected endpoint")
    @PostMapping("/order")
    public ResponseEntity<OrderRideResponse> orderRide(@Valid @RequestBody OrderRideRequest request) {
        log.debug("Ride order requested from {} to {}", request.getPickupLocation(), request.getDropoffLocation());

        if (request.getScheduledAt() != null) {
            LocalDateTime now = LocalDateTime.now();
            if (request.getScheduledAt().isBefore(now)) {
                throw new IllegalArgumentException("Scheduled time must be in the future");
            }
            if (request.getScheduledAt().isAfter(now.plusHours(5))) {
                throw new IllegalArgumentException("Ride can be scheduled at most 5 hours in advance");
            }
        }

        // TODO: Implement ride ordering logic via RideService
        // - Build ordered route: pickup -> stops (ordered) -> dropoff
        // - Validate linked passengers exist by email; creator pays the ride
        // - Calculate route distance (km) and price:
        //     price = basePriceByVehicleType + distanceKm * 120
        // - Find available drivers:
        //   * Reject if no active/logged-in drivers
        //   * Reject if all drivers are busy AND already have a future scheduled ride
        //   * If there are free drivers, pick the nearest to pickup
        //   * If all are busy, pick one closest to pickup AND close to finishing current ride (<= 10 min remaining)
        //   * Exclude drivers with > 8 working hours in last 24h
        // - If scheduled ride: prioritize pre-scheduled rides in assignment
        // - Send notifications:
        //   * Driver: new ride assigned
        //   * Creator: accepted/rejected
        //   * Linked passengers: ride details available
        // - Create reminder notifications: 15 minutes before, then every 5 minutes until start

        boolean isScheduled = request.getScheduledAt() != null;
        OrderRideResponse response = new OrderRideResponse(
                1L,
                isScheduled ? "SCHEDULED" : "ACCEPTED",
                isScheduled ? "Ride scheduled successfully." : "Ride ordered successfully.",
                new BigDecimal("250.00"),
                request.getScheduledAt(),
                null
        );

        return ResponseEntity.status(201).body(response);
    }


    @Operation(summary = "Cancel ride", description = "Cancel a scheduled or active ride - Protected endpoint")
    @PostMapping("/{rideId}/cancel")
    public ResponseEntity<MessageResponse> cancelRide(
            @PathVariable Long rideId,
            @Valid @RequestBody CancelRideRequest request) {

        log.debug("Ride cancellation requested for rideId: {} with reason: {}", rideId, request.getReason());

        // TODO: Implement ride cancellation logic via RideService
        // - Verify user has permission to cancel this ride
        // - Check if ride can be cancelled (not completed, not already cancelled)
        // - For passengers: can cancel up to 10 minutes before ride start
        // - For drivers: can cancel before passengers enter vehicle
        // - Update ride status to CANCELLED
        // - Notify driver/passengers about cancellation
        // - Apply cancellation fee if applicable
        // - Return confirmation

        log.info("Ride {} cancelled successfully", rideId);
        return ResponseEntity.ok(new MessageResponse("Ride cancelled successfully."));
    }

    /**
     * 2.6.1 Start Ride
     * Protected endpoint - drivers only
     */
    @Operation(summary = "Start ride", description = "Mark that the ride has started - Protected endpoint (Drivers only)")
    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping("/{rideId}/start")
    public ResponseEntity<MessageResponse> startRide(@PathVariable Long rideId) {
        log.debug("Start ride requested for rideId: {}", rideId);

        // TODO: Implement start ride logic via RideService
        // - Verify current user is the assigned driver for this ride
        // - Verify ride is in a state that can be started (e.g., ACCEPTED / SCHEDULED)
        // - Verify all passengers have entered the vehicle (business rule / confirmation)
        // - Update ride status to IN_PROGRESS and set start timestamp
        // - Ensure passengers on this ride cannot order new rides until completion
        // - Notify passengers that the ride has started

        log.info("Ride {} started successfully", rideId);
        return ResponseEntity.ok(new MessageResponse("Ride started successfully."));
    }

    /**
     * 2.6.5 Stop Ride in Progress
     * Protected endpoint - drivers only
     */
    @Operation(summary = "Stop ride", description = "Stop a ride in progress - Protected endpoint (Drivers only)")
    @PostMapping("/{rideId}/stop")
    public ResponseEntity<MessageResponse> stopRide(@PathVariable Long rideId) {
        log.debug("Stop ride requested for rideId: {}", rideId);

        // TODO: Implement stop ride logic via RideService
        // - Verify user is the driver of this ride
        // - Check if ride is currently IN_PROGRESS
        // - Update ride status to COMPLETED
        // - Capture current location as new dropoff point
        // - Calculate final price based on actual distance/time
        // - Process payment
        // - Notify passengers about ride completion
        // - Return completion details

        log.info("Ride {} stopped and completed successfully", rideId);
        return ResponseEntity.ok(new MessageResponse("Ride completed successfully."));
    }
}
