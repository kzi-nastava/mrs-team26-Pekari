package com.pekara.controller;

import com.pekara.dto.request.CancelRideRequest;
import com.pekara.dto.request.EstimateRideRequest;
import com.pekara.dto.request.InconsistencyReportRequest;
import com.pekara.dto.request.OrderRideRequest;
import com.pekara.dto.request.RideHistoryFilterRequest;
import com.pekara.dto.request.RideRatingRequest;
import com.pekara.dto.response.DriverRideHistoryResponse;
import com.pekara.dto.response.MessageResponse;
import com.pekara.dto.response.OrderRideResponse;
import com.pekara.dto.response.PassengerRideDetailResponse;
import com.pekara.dto.response.PassengerRideHistoryResponse;
import com.pekara.dto.response.RideDetailResponse;
import com.pekara.dto.response.RideEstimateResponse;
import com.pekara.dto.response.RideTrackingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Operation(summary = "Track ride", description = "Get real-time tracking information for an active ride - Protected endpoint")
    @PreAuthorize("hasAnyRole('PASSENGER', 'DRIVER')")
    @GetMapping("/{rideId}/track")
    public ResponseEntity<RideTrackingResponse> trackRide(@PathVariable Long rideId) {
        log.debug("Ride tracking requested for rideId: {}", rideId);

        // TODO: Implement ride tracking via RideService
        // - Verify user is a passenger or driver on this ride
        // - Fetch current vehicle location
        // - Calculate distance to next stop and final destination
        // - Calculate estimated time to destination (updates as vehicle moves)
        // - Return real-time tracking information

        RideTrackingResponse response = new RideTrackingResponse(
                rideId,
                45.2671,
                19.8335,
                12,
                4.5,
                "IN_PROGRESS",
                "Trg Slobode",
                5,
                new RideTrackingResponse.VehicleInfo(1L, "STANDARD", "NS-123-AB")
        );

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Report inconsistency", description = "Report driver inconsistency during an active ride - Protected endpoint (Passengers only)")
    @PreAuthorize("hasRole('PASSENGER')")
    @PostMapping("/{rideId}/report-inconsistency")
    public ResponseEntity<MessageResponse> reportInconsistency(
            @PathVariable Long rideId,
            @Valid @RequestBody InconsistencyReportRequest request) {

        log.debug("Inconsistency report for rideId: {} - {}", rideId, request.getDescription());

        // TODO: Implement inconsistency reporting via RideService
        // - Verify user is a passenger on this ride
        // - Verify ride is currently IN_PROGRESS
        // - Create inconsistency report with passenger info and description
        // - Store report linked to the ride
        // - Reports will be shown in ride history and admin reports
        // - Notify admin about the report

        log.info("Inconsistency reported for ride {}", rideId);
        return ResponseEntity.ok(new MessageResponse("Inconsistency reported successfully."));
    }

    @Operation(summary = "Rate ride", description = "Rate a completed ride (vehicle and driver) - Protected endpoint (Passengers only)")
    @PreAuthorize("hasRole('PASSENGER')")
    @PostMapping("/{rideId}/rate")
    public ResponseEntity<MessageResponse> rateRide(
            @PathVariable Long rideId,
            @Valid @RequestBody RideRatingRequest request) {

        log.debug("Rating ride {} - Vehicle: {}/5, Driver: {}/5", rideId, request.getVehicleRating(), request.getDriverRating());

        // TODO: Implement ride rating via RideService
        // - Verify user is a passenger on this ride
        // - Verify ride is COMPLETED
        // - Verify ride was completed within last 3 days (rating deadline)
        // - Verify user hasn't already rated this ride
        // - Store rating (vehicle rating, driver rating, comment)
        // - Update driver's average rating
        // - Send email/notification to passenger confirming rating submission

        log.info("Ride {} rated successfully", rideId);
        return ResponseEntity.ok(new MessageResponse("Ride rated successfully."));
    }

    @Operation(summary = "Get driver ride history", description = "View driver's ride history with date filtering - Protected endpoint (Drivers only)")
    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping("/history/driver")
    public ResponseEntity<List<DriverRideHistoryResponse>> getDriverRideHistory(
            @Valid @RequestBody RideHistoryFilterRequest filterRequest,
            @AuthenticationPrincipal UserDetails currentUser) {

        log.debug("Driver ride history requested with filters: {}", filterRequest);

        // TODO: Implement driver ride history retrieval via RideService
        // - Get current driver ID from UserDetails
        // - Fetch all rides where user was the driver
        // - Filter by date range (startDate to endDate)
        // - For each ride include:
        //   * Start time and end time
        //   * Pickup and dropoff locations
        //   * Cancelled status and who cancelled (passenger name or "driver")
        //   * Price
        //   * Panic button activation status
        //   * All passengers information
        // - Sort by date (newest first by default)

        List<DriverRideHistoryResponse> history = new ArrayList<>();

        log.debug("Retrieved {} rides for driver", history.size());
        return ResponseEntity.ok(history);
    }

    @Operation(summary = "Get passenger ride history", description = "View passenger's ride history with date filtering - Protected endpoint (Passengers only)")
    @PreAuthorize("hasRole('PASSENGER')")
    @PostMapping("/history/passenger")
    public ResponseEntity<List<PassengerRideHistoryResponse>> getPassengerRideHistory(
            @Valid @RequestBody RideHistoryFilterRequest filterRequest,
            @AuthenticationPrincipal UserDetails currentUser) {

        log.debug("Passenger ride history requested with filters: {}", filterRequest);

        // TODO: Implement passenger ride history retrieval via RideService
        // - Get current passenger ID from UserDetails
        // - Fetch all rides where user was a passenger
        // - Filter by date range (startDate to endDate)
        // - For each ride include:
        //   * Start time and end time
        //   * Pickup and dropoff locations
        //   * Cancelled status and who cancelled ("passenger" or "driver")
        //   * Price
        //   * Panic button activation status
        //   * Driver basic information (NOT other passengers)
        // - Sort by date (newest first by default)

        List<PassengerRideHistoryResponse> history = new ArrayList<>();

        log.debug("Retrieved {} rides for passenger", history.size());
        return ResponseEntity.ok(history);
    }

    @Operation(summary = "Get ride details (Driver/Admin)", description = "View detailed ride information with all passengers - Protected endpoint (Drivers/Admins only)")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    @GetMapping("/{rideId}/details")
    public ResponseEntity<RideDetailResponse> getRideDetailsForDriver(
            @PathVariable Long rideId,
            @AuthenticationPrincipal UserDetails currentUser) {

        log.debug("Requesting driver/admin details for rideId: {}", rideId);

        // TODO: Implement ride detail retrieval via RideService
        // - Get current user's ID and roles from UserDetails
        // - Fetch ride information
        // - Verify permissions:
        //   * If currentUser is the driver on this ride -> allow
        //   * If currentUser has ADMIN role -> allow
        //   * Otherwise -> throw 403 Forbidden
        // - Return complete ride information including:
        //   * Route with all stops
        //   * Driver information
        //   * All passengers information (with full details)
        //   * Inconsistency reports
        //   * Ratings and comments
        //   * Panic button activation status
        //   * Cancellation details if applicable

        RideDetailResponse rideDetails = new RideDetailResponse();

        log.debug("Retrieved driver/admin details for rideId: {}", rideId);
        return ResponseEntity.ok(rideDetails);
    }

    @Operation(summary = "Get ride details (Passenger)", description = "View simplified ride information - Protected endpoint (Passengers only)")
    @PreAuthorize("hasRole('PASSENGER')")
    @GetMapping("/{rideId}")
    public ResponseEntity<PassengerRideDetailResponse> getRideDetailsForPassenger(
            @PathVariable Long rideId,
            @AuthenticationPrincipal UserDetails currentUser) {

        log.debug("Requesting passenger details for rideId: {}", rideId);

        // TODO: Implement ride detail retrieval via RideService
        // - Get current user's ID from UserDetails
        // - Fetch ride information
        // - Verify user is a passenger on this ride, otherwise -> throw 403 Forbidden
        // - Return simplified ride information including:
        //   * Start/end times
        //   * Pickup/dropoff locations and stops
        //   * Price and status
        //   * Driver basic info (name, phone)
        //   * Passenger's own rating if exists

        PassengerRideDetailResponse rideDetails = new PassengerRideDetailResponse();

        log.debug("Retrieved passenger details for rideId: {}", rideId);
        return ResponseEntity.ok(rideDetails);
    }
}
