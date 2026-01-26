package com.pekara.controller;

import com.pekara.dto.request.WebCancelRideRequest;
import com.pekara.dto.request.WebEstimateRideRequest;
import com.pekara.dto.request.WebInconsistencyReportRequest;
import com.pekara.dto.request.WebOrderRideRequest;
import com.pekara.dto.request.WebRideHistoryFilterRequest;
import com.pekara.dto.request.WebRideLocationUpdateRequest;
import com.pekara.dto.request.WebRideRatingRequest;
import com.pekara.dto.response.WebDriverRideHistoryResponse;
import com.pekara.dto.response.WebMessageResponse;
import com.pekara.dto.response.WebOrderRideResponse;
import com.pekara.dto.response.WebPaginatedResponse;
import com.pekara.dto.response.WebPassengerRideDetailResponse;
import com.pekara.dto.response.WebPassengerRideHistoryResponse;
import com.pekara.dto.response.WebRideDetailResponse;
import com.pekara.dto.response.WebRideEstimateResponse;
import com.pekara.dto.response.WebRideTrackingResponse;
import com.pekara.mapper.RideMapper;
import com.pekara.service.RideService;
import com.pekara.service.RideTrackingService;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/rides")
@Tag(name = "Rides", description = "Ride management endpoints")
@RequiredArgsConstructor
public class RideController {

    private final RideService rideService;
    private final RideTrackingService rideTrackingService;
    private final RideMapper rideMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Operation(summary = "Estimate ride", description = "Calculate ride estimation (price, duration, distance) - Public endpoint")
    @PostMapping("/estimate")
    public ResponseEntity<WebRideEstimateResponse> estimateRide(@Valid @RequestBody WebEstimateRideRequest request) {
        log.debug("Ride estimation requested");

        var serviceResponse = rideService.estimateRide(rideMapper.toServiceEstimateRideRequest(request));
        WebRideEstimateResponse response = new WebRideEstimateResponse(
            serviceResponse.getEstimatedPrice(),
            serviceResponse.getEstimatedDurationMinutes(),
            serviceResponse.getDistanceKm(),
            serviceResponse.getVehicleType(),
            serviceResponse.getRoutePoints() == null ? null :
                serviceResponse.getRoutePoints().stream().map(rideMapper::toWebLocation).toList()
        );

        return ResponseEntity.ok(response);
    }


    @Operation(summary = "Order ride", description = "Order a ride now or schedule it up to 5 hours ahead - Protected endpoint")
    @PreAuthorize("hasRole('PASSENGER')")
    @PostMapping("/order")
    public ResponseEntity<WebOrderRideResponse> orderRide(
            @Valid @RequestBody WebOrderRideRequest request,
            @AuthenticationPrincipal String currentUserEmail) {
        log.debug("Ride order requested");

        var serviceResponse = rideService.orderRide(currentUserEmail, rideMapper.toServiceOrderRideRequest(request));

        WebOrderRideResponse response = new WebOrderRideResponse(
                serviceResponse.getRideId(),
                serviceResponse.getStatus(),
                serviceResponse.getMessage(),
                serviceResponse.getEstimatedPrice(),
                serviceResponse.getScheduledAt(),
                serviceResponse.getAssignedDriverEmail()
        );

        return ResponseEntity.status(201).body(response);
    }


    @Operation(summary = "Cancel ride", description = "Cancel a scheduled or active ride - Protected endpoint")
    @PreAuthorize("hasAnyRole('PASSENGER', 'DRIVER')")
    @PostMapping("/{rideId}/cancel")
    public ResponseEntity<WebMessageResponse> cancelRide(
            @PathVariable Long rideId,
            @Valid @RequestBody WebCancelRideRequest request,
            @AuthenticationPrincipal String currentUserEmail) {

        log.debug("Ride cancellation requested for rideId: {} with reason: {}", rideId, request.getReason());

        rideService.cancelRide(rideId, currentUserEmail, request.getReason());

        log.info("Ride {} cancelled successfully", rideId);
        return ResponseEntity.ok(new WebMessageResponse("Ride cancelled successfully."));
    }

    /**
     * 2.6.1 Start Ride
     * Protected endpoint - drivers only
     */
    @Operation(summary = "Start ride", description = "Mark that the ride has started - Protected endpoint (Drivers only)")
    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping("/{rideId}/start")
    public ResponseEntity<WebMessageResponse> startRide(
            @PathVariable Long rideId,
            @AuthenticationPrincipal String currentUserEmail) {
        log.debug("Start ride requested for rideId: {}", rideId);

        rideService.startRide(rideId, currentUserEmail);

        log.info("Ride {} started successfully", rideId);
        return ResponseEntity.ok(new WebMessageResponse("Ride started successfully."));
    }

    /**
     * 2.6.5 Stop Ride in Progress (Complete the ride)
     * Protected endpoint - drivers only
     */
    @Operation(summary = "Complete ride", description = "Complete a ride in progress - Protected endpoint (Drivers only)")
    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping("/{rideId}/stop")
    public ResponseEntity<WebMessageResponse> stopRide(
            @PathVariable Long rideId,
            @AuthenticationPrincipal String currentUserEmail) {
        log.debug("Stop ride requested for rideId: {}", rideId);

        rideService.completeRide(rideId, currentUserEmail);

        log.info("Ride {} stopped and completed successfully", rideId);
        return ResponseEntity.ok(new WebMessageResponse("Ride completed successfully."));
    }

    @Operation(summary = "Track ride", description = "Get real-time tracking information for an active ride - Protected endpoint")
    @PreAuthorize("hasAnyRole('PASSENGER', 'DRIVER')")
    @GetMapping("/{rideId}/track")
    public ResponseEntity<WebRideTrackingResponse> trackRide(
            @PathVariable Long rideId,
            @AuthenticationPrincipal String currentUserEmail) {
        log.debug("Ride tracking requested for rideId: {}", rideId);

        var tracking = rideTrackingService.getTracking(rideId, currentUserEmail);
        WebRideTrackingResponse response = rideMapper.toWebRideTrackingResponse(tracking);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update ride location", description = "Driver location ping for active ride - Protected endpoint (Drivers only)")
    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping("/{rideId}/location")
    public ResponseEntity<WebMessageResponse> updateRideLocation(
            @PathVariable Long rideId,
            @Valid @RequestBody WebRideLocationUpdateRequest request,
            @AuthenticationPrincipal String currentUserEmail) {

        log.debug("Ride {} location update by driver {}", rideId, currentUserEmail);
        rideTrackingService.updateLocation(rideId, currentUserEmail, rideMapper.toServiceRideLocationUpdateRequest(request));
        var tracking = rideTrackingService.getTracking(rideId, currentUserEmail);
        WebRideTrackingResponse payload = rideMapper.toWebRideTrackingResponse(tracking);
        messagingTemplate.convertAndSend("/topic/rides/" + rideId + "/tracking", payload);
        return ResponseEntity.ok(new WebMessageResponse("Location updated."));
    }

    @Operation(summary = "Report inconsistency", description = "Report driver inconsistency during an active ride - Protected endpoint (Passengers only)")
    @PreAuthorize("hasRole('PASSENGER')")
    @PostMapping("/{rideId}/report-inconsistency")
    public ResponseEntity<WebMessageResponse> reportInconsistency(
            @PathVariable Long rideId,
            @Valid @RequestBody WebInconsistencyReportRequest request) {

        log.debug("Inconsistency report for rideId: {} - {}", rideId, request.getDescription());

        // TODO: Implement inconsistency reporting via RideService
        // - Verify user is a passenger on this ride
        // - Verify ride is currently IN_PROGRESS
        // - Create inconsistency report with passenger info and description
        // - Store report linked to the ride
        // - Reports will be shown in ride history and admin reports
        // - Notify admin about the report

        log.info("Inconsistency reported for ride {}", rideId);
        return ResponseEntity.ok(new WebMessageResponse("Inconsistency reported successfully."));
    }

    @Operation(summary = "Rate ride", description = "Rate a completed ride (vehicle and driver) - Protected endpoint (Passengers only)")
    @PreAuthorize("hasRole('PASSENGER')")
    @PostMapping("/{rideId}/rate")
    public ResponseEntity<WebMessageResponse> rateRide(
            @PathVariable Long rideId,
            @Valid @RequestBody WebRideRatingRequest request) {

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
        return ResponseEntity.ok(new WebMessageResponse("Ride rated successfully."));
    }

    @Operation(summary = "Get driver ride history", description = "View driver's ride history with date filtering - Protected endpoint (Drivers only)")
    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping("/history/driver")
    public ResponseEntity<WebPaginatedResponse<WebDriverRideHistoryResponse>> getDriverRideHistory(
            @Valid @RequestBody WebRideHistoryFilterRequest filterRequest,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails currentUser) {

        log.debug("Driver ride history requested with filters: {} (page: {}, size: {})", filterRequest, page, size);

        // TODO: Implement driver ride history retrieval via RideService
        // - Get current driver ID from UserDetails
        // - Fetch all rides where user was the driver
        // - Filter by date range (startDate to endDate)
        // - Apply pagination (page, size)
        // - For each ride include:
        //   * Start time and end time
        //   * Pickup and dropoff locations
        //   * Cancelled status and who cancelled (passenger name or "driver")
        //   * Price
        //   * Panic button activation status
        //   * All passengers information
        // - Sort by date (newest first by default)

        List<WebDriverRideHistoryResponse> history = new ArrayList<>();
        WebPaginatedResponse<WebDriverRideHistoryResponse> response = new WebPaginatedResponse<>(history, page, size, 0L);

        log.debug("Retrieved {} rides for driver", history.size());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get passenger ride history", description = "View passenger's ride history with date filtering - Protected endpoint (Passengers only)")
    @PreAuthorize("hasRole('PASSENGER')")
    @PostMapping("/history/passenger")
    public ResponseEntity<WebPaginatedResponse<WebPassengerRideHistoryResponse>> getPassengerRideHistory(
            @Valid @RequestBody WebRideHistoryFilterRequest filterRequest,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails currentUser) {

        log.debug("Passenger ride history requested with filters: {} (page: {}, size: {})", filterRequest, page, size);

        // TODO: Implement passenger ride history retrieval via RideService
        // - Get current passenger ID from UserDetails
        // - Fetch all rides where user was a passenger
        // - Filter by date range (startDate to endDate)
        // - Apply pagination (page, size)
        // - For each ride include:
        //   * Start time and end time
        //   * Pickup and dropoff locations
        //   * Cancelled status and who cancelled ("passenger" or "driver")
        //   * Price
        //   * Panic button activation status
        //   * Driver basic information (NOT other passengers)
        // - Sort by date (newest first by default)

        List<WebPassengerRideHistoryResponse> history = new ArrayList<>();
        WebPaginatedResponse<WebPassengerRideHistoryResponse> response = new WebPaginatedResponse<>(history, page, size, 0L);

        log.debug("Retrieved {} rides for passenger", history.size());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all rides history (Admin)", description = "View complete ride history for all drivers and passengers with filtering - Protected endpoint (Admins only)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/history/admin/all")
    public ResponseEntity<WebPaginatedResponse<WebDriverRideHistoryResponse>> getAllRidesHistory(
            @Valid @RequestBody WebRideHistoryFilterRequest filterRequest,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Admin all rides history requested with filters: {} (page: {}, size: {})", filterRequest, page, size);

        // TODO: Implement admin ride history retrieval via RideService
        // - Fetch ALL rides in the system (no user filtering)
        // - Filter by date range (startDate to endDate)
        // - Apply pagination (page, size)
        // - For each ride include:
        //   * Start time and end time
        //   * Pickup and dropoff locations
        //   * Cancelled status and who cancelled (passenger name or "driver")
        //   * Price
        //   * Panic button activation status
        //   * All passengers information
        //   * Driver information
        //   * Inconsistency reports if any
        //   * Ratings if any
        // - Sort by date (newest first by default)

        List<WebDriverRideHistoryResponse> history = new ArrayList<>();
        WebPaginatedResponse<WebDriverRideHistoryResponse> response = new WebPaginatedResponse<>(history, page, size, 0L);

        log.debug("Retrieved {} total rides for admin", history.size());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get ride details (Driver/Admin)", description = "View detailed ride information with all passengers - Protected endpoint (Drivers/Admins only)")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    @GetMapping("/{rideId}/details")
    public ResponseEntity<WebRideDetailResponse> getRideDetailsForDriver(
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

        WebRideDetailResponse rideDetails = new WebRideDetailResponse();

        log.debug("Retrieved driver/admin details for rideId: {}", rideId);
        return ResponseEntity.ok(rideDetails);
    }

    @Operation(summary = "Get ride details (Passenger)", description = "View simplified ride information - Protected endpoint (Passengers only)")
    @PreAuthorize("hasRole('PASSENGER')")
    @GetMapping("/{rideId}")
    public ResponseEntity<WebPassengerRideDetailResponse> getRideDetailsForPassenger(
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

        WebPassengerRideDetailResponse rideDetails = new WebPassengerRideDetailResponse();

        log.debug("Retrieved passenger details for rideId: {}", rideId);
        return ResponseEntity.ok(rideDetails);
    }
}
