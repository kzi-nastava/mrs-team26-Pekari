package com.pekara.controller;

import com.pekara.dto.request.CancelRideRequest;
import com.pekara.dto.request.EstimateRideRequest;
import com.pekara.dto.response.MessageResponse;
import com.pekara.dto.response.RideEstimateResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping("/api/v1/rides")
public class RideController {


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
     * 2.6.5 Stop Ride in Progress
     * Protected endpoint - drivers only
     */
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
