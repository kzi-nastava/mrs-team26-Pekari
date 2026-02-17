package com.pekara.controller;

import com.pekara.dto.request.WebCancelRideRequest;
import com.pekara.dto.request.WebEstimateRideRequest;
import com.pekara.dto.request.WebInconsistencyReportRequest;
import com.pekara.dto.request.WebOrderRideRequest;
import com.pekara.dto.request.WebRideHistoryFilterRequest;
import com.pekara.dto.request.WebRideLocationUpdateRequest;
import com.pekara.dto.request.WebRideRatingRequest;
import com.pekara.dto.request.WebStopRideEarlyRequest;
import com.pekara.dto.response.WebActiveRideResponse;
import com.pekara.dto.response.WebAdminRideDetailResponse;
import com.pekara.dto.response.WebAdminRideHistoryResponse;
import com.pekara.dto.response.WebDriverRideHistoryResponse;
import com.pekara.dto.response.WebMessageResponse;
import com.pekara.dto.response.WebOrderRideResponse;
import com.pekara.dto.response.WebPaginatedResponse;
import com.pekara.dto.response.WebPassengerRideDetailResponse;
import com.pekara.dto.response.WebPassengerRideHistoryResponse;
import com.pekara.dto.response.WebRideDetailResponse;
import com.pekara.dto.response.WebRideEstimateResponse;
import com.pekara.dto.response.WebRideStatsResponse;
import com.pekara.dto.response.WebRideTrackingResponse;
import com.pekara.mapper.RideMapper;
import com.pekara.service.AdminService;
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

import com.pekara.constant.RideStatsScope;

import java.time.LocalDateTime;
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
    private final AdminService adminService;
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

    @Operation(summary = "Get active ride for driver", description = "Get the current active ride for the logged-in driver - Protected endpoint (Drivers only)")
    @PreAuthorize("hasRole('DRIVER')")
    @GetMapping("/active/driver")
    public ResponseEntity<WebActiveRideResponse> getActiveRideForDriver(
            @AuthenticationPrincipal String currentUserEmail) {
        log.debug("Get active ride requested for driver: {}", currentUserEmail);

        var activeRide = rideService.getActiveRideForDriver(currentUserEmail);
        
        if (activeRide.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        WebActiveRideResponse response = rideMapper.toWebActiveRideResponse(activeRide.get());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get active ride for passenger", description = "Get the current active ride for the logged-in passenger - Protected endpoint (Passengers only)")
    @PreAuthorize("hasRole('PASSENGER')")
    @GetMapping("/active/passenger")
    public ResponseEntity<WebActiveRideResponse> getActiveRideForPassenger(
            @AuthenticationPrincipal String currentUserEmail) {
        log.debug("Get active ride requested for passenger: {}", currentUserEmail);

        var activeRide = rideService.getActiveRideForPassenger(currentUserEmail);
        
        if (activeRide.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        WebActiveRideResponse response = rideMapper.toWebActiveRideResponse(activeRide.get());
        return ResponseEntity.ok(response);
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
     * Request early stop - called by passenger
     * Protected endpoint - passengers only
     */
    @Operation(summary = "Request stop", description = "Passenger requests to stop the ride early - Protected endpoint (Passengers only)")
    @PreAuthorize("hasRole('PASSENGER')")
    @PostMapping("/{rideId}/request-stop")
    public ResponseEntity<WebMessageResponse> requestStopRide(
            @PathVariable Long rideId,
            @AuthenticationPrincipal String currentUserEmail) {
        log.debug("Stop requested by passenger for rideId: {}", rideId);

        rideService.requestStopRide(rideId, currentUserEmail);

        log.info("Ride {} stop requested successfully", rideId);
        return ResponseEntity.ok(new WebMessageResponse("Stop request sent to driver."));
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
            @RequestBody(required = false) WebStopRideEarlyRequest request,
            @AuthenticationPrincipal String currentUserEmail) {
        log.debug("Stop ride requested for rideId: {}", rideId);

        // If stop location provided, handle early stop with new location
        if (request != null && request.getStopLocation() != null) {
            var serviceRequest = rideMapper.toServiceStopRideEarlyRequest(request);
            rideService.stopRideEarly(rideId, currentUserEmail, serviceRequest.getStopLocation());
            log.info("Ride {} stopped early at new location", rideId);

            // Send WebSocket update with COMPLETED status
            sendCompletionWebSocketUpdate(rideId);

            return ResponseEntity.ok(new WebMessageResponse("Ride completed at new location."));
        }

        // Otherwise, complete ride normally at original destination
        rideService.completeRide(rideId, currentUserEmail);
        log.info("Ride {} stopped and completed successfully", rideId);

        // Send WebSocket update with COMPLETED status
        sendCompletionWebSocketUpdate(rideId);

        return ResponseEntity.ok(new WebMessageResponse("Ride completed successfully."));
    }

    private void sendCompletionWebSocketUpdate(Long rideId) {
        try {
            WebRideTrackingResponse payload = WebRideTrackingResponse.builder()
                    .rideId(rideId)
                    .status("COMPLETED")
                    .rideStatus("COMPLETED")
                    .build();
            messagingTemplate.convertAndSend("/topic/rides/" + rideId + "/tracking", payload);
            log.debug("Sent COMPLETED status via WebSocket for ride {}", rideId);
        } catch (Exception e) {
            log.warn("Failed to send WebSocket completion update for ride {}: {}", rideId, e.getMessage());
        }
    }
    //fallback
    @Operation(summary = "Track ride", description = "Get real-time tracking information for an active ride - Protected endpoint")
    @PreAuthorize("hasAnyRole('PASSENGER', 'DRIVER', 'ADMIN')")
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
        try {
            messagingTemplate.convertAndSend("/topic/rides/" + rideId + "/tracking", payload);
        } catch (Exception e) {
            log.warn("Failed to send WebSocket location update for ride {}: {}", rideId, e.getMessage());
        }
        return ResponseEntity.ok(new WebMessageResponse("Location updated."));
    }

    @Operation(summary = "Report inconsistency", description = "Report driver inconsistency during an active ride - Protected endpoint (Passengers only)")
    @PreAuthorize("hasRole('PASSENGER')")
    @PostMapping("/{rideId}/report-inconsistency")
    public ResponseEntity<WebMessageResponse> reportInconsistency(
            @PathVariable Long rideId,
            @Valid @RequestBody WebInconsistencyReportRequest request,
            @AuthenticationPrincipal String currentUserEmail) {

        log.debug("Inconsistency report for rideId: {} by user: {} - {}", rideId, currentUserEmail, request.getDescription());

        rideService.reportInconsistency(rideId, currentUserEmail, rideMapper.toServiceInconsistencyReportRequest(request));

        log.info("Inconsistency reported for ride {} by {}", rideId, currentUserEmail);
        return ResponseEntity.ok(new WebMessageResponse("Inconsistency reported successfully."));
    }

    @Operation(summary = "Rate ride", description = "Rate a completed ride (vehicle and driver) - Protected endpoint (Passengers only)")
    @PreAuthorize("hasRole('PASSENGER')")
    @PostMapping("/{rideId}/rate")
    public ResponseEntity<WebMessageResponse> rateRide(
            @PathVariable Long rideId,
            @Valid @RequestBody WebRideRatingRequest request,
            @AuthenticationPrincipal String currentUserEmail) {

        log.debug("Rating ride {} - Vehicle: {}/5, Driver: {}/5", rideId, request.getVehicleRating(), request.getDriverRating());

        rideService.rateRide(rideId, currentUserEmail, rideMapper.toServiceRideRatingRequest(request));

        log.info("Ride {} rated successfully by {}", rideId, currentUserEmail);
        return ResponseEntity.ok(new WebMessageResponse("Ride rated successfully."));
    }

    @Operation(summary = "Activate panic", description = "Activate panic button during an active ride - Protected endpoint (Drivers and Passengers only)")
    @PreAuthorize("hasAnyRole('DRIVER', 'PASSENGER')")
    @PostMapping("/{rideId}/panic")
    public ResponseEntity<WebMessageResponse> activatePanic(
            @PathVariable Long rideId,
            @AuthenticationPrincipal String currentUserEmail) {

        log.warn("Panic activation requested for rideId: {} by user: {}", rideId, currentUserEmail);

        rideService.activatePanic(rideId, currentUserEmail);

        log.warn("Panic activated successfully for ride {}", rideId);
        return ResponseEntity.ok(new WebMessageResponse("Panic activated. Emergency support has been notified."));
    }

    @Operation(summary = "Get active panic rides (Admin)", description = "Get all active rides with panic activated - Protected endpoint (Admins only)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/panic/active")
    public ResponseEntity<List<WebDriverRideHistoryResponse>> getActivePanicRides() {
        log.debug("Admin requesting active panic rides");

        var serviceResponse = rideService.getActivePanicRides();
        List<WebDriverRideHistoryResponse> panicRides = serviceResponse.stream()
                .map(rideMapper::toWebDriverRideHistoryResponse)
                .toList();

        log.debug("Retrieved {} active panic rides", panicRides.size());
        return ResponseEntity.ok(panicRides);
    }

    @Operation(summary = "Get driver ride history", description = "View driver's ride history with date filtering - Protected endpoint (Drivers only)")
    @PreAuthorize("hasRole('DRIVER')")
    @GetMapping("/history/driver")
    public ResponseEntity<WebPaginatedResponse<WebDriverRideHistoryResponse>> getDriverRideHistory(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal String currentUserEmail) {

        log.debug("Driver ride history requested with filters: startDate={}, endDate={} (page: {}, size: {})", startDate, endDate, page, size);

        var serviceResponse = rideService.getDriverRideHistory(
                currentUserEmail,
                LocalDateTime.parse(startDate + "T00:00:00"),
                LocalDateTime.parse(endDate + "T23:59:59"));

        List<WebDriverRideHistoryResponse> history = serviceResponse.stream()
                .map(rideMapper::toWebDriverRideHistoryResponse)
                .toList();

        int start = page * size;
        int end = Math.min(start + size, history.size());
        List<WebDriverRideHistoryResponse> paginatedHistory = start < history.size()
                ? history.subList(start, end)
                : new ArrayList<>();

        WebPaginatedResponse<WebDriverRideHistoryResponse> response = new WebPaginatedResponse<>(
                paginatedHistory, page, size, (long) history.size());

        log.debug("Retrieved {} rides for driver", history.size());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get driver ride stats", description = "View driver's ride statistics (rides per day, distance, earnings) for date range - Protected endpoint (Drivers only)")
    @PreAuthorize("hasRole('DRIVER')")
    @GetMapping("/stats/driver")
    public ResponseEntity<WebRideStatsResponse> getDriverRideStats(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @AuthenticationPrincipal String currentUserEmail) {

        log.debug("Driver ride stats requested: startDate={}, endDate={}", startDate, endDate);

        var serviceResponse = rideService.getDriverRideStats(
                currentUserEmail,
                LocalDateTime.parse(startDate + "T00:00:00"),
                LocalDateTime.parse(endDate + "T23:59:59"));

        return ResponseEntity.ok(rideMapper.toWebRideStatsResponse(serviceResponse));
    }

    @Operation(summary = "Get passenger ride stats", description = "View passenger's ride statistics (rides per day, distance, spending) for date range - Protected endpoint (Passengers only)")
    @PreAuthorize("hasRole('PASSENGER')")
    @GetMapping("/stats/passenger")
    public ResponseEntity<WebRideStatsResponse> getPassengerRideStats(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @AuthenticationPrincipal String currentUserEmail) {

        log.debug("Passenger ride stats requested: startDate={}, endDate={}", startDate, endDate);

        var serviceResponse = rideService.getPassengerRideStats(
                currentUserEmail,
                LocalDateTime.parse(startDate + "T00:00:00"),
                LocalDateTime.parse(endDate + "T23:59:59"));

        return ResponseEntity.ok(rideMapper.toWebRideStatsResponse(serviceResponse));
    }

    @Operation(summary = "Get ride stats (Admin)", description = "View ride statistics for all drivers/passengers or a single person - Protected endpoint (Admins only)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/stats/admin")
    public ResponseEntity<WebRideStatsResponse> getAdminRideStats(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam String scope,
            @RequestParam(required = false) Long userId) {

        log.debug("Admin ride stats requested: scope={}, userId={}, startDate={}, endDate={}", scope, userId, startDate, endDate);

        RideStatsScope rideStatsScope = RideStatsScope.valueOf(scope.toUpperCase());
        var serviceResponse = adminService.getRideStatsAdmin(
                LocalDateTime.parse(startDate + "T00:00:00"),
                LocalDateTime.parse(endDate + "T23:59:59"),
                rideStatsScope,
                userId);

        return ResponseEntity.ok(rideMapper.toWebRideStatsResponse(serviceResponse));
    }

    @Operation(summary = "Get passenger ride history", description = "View passenger's ride history with date filtering - Protected endpoint (Passengers only)")
    @PreAuthorize("hasRole('PASSENGER')")
    @PostMapping("/history/passenger")
    public ResponseEntity<WebPaginatedResponse<WebPassengerRideHistoryResponse>> getPassengerRideHistory(
            @Valid @RequestBody WebRideHistoryFilterRequest filterRequest,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal String currentUserEmail) {

        log.debug("Passenger ride history requested for {} with filters: {} (page: {}, size: {})", currentUserEmail, filterRequest, page, size);

        LocalDateTime startDateTime = filterRequest.getStartDate() != null ? filterRequest.getStartDate() : LocalDateTime.now().minusYears(1);
        LocalDateTime endDateTime = filterRequest.getEndDate() != null ? filterRequest.getEndDate() : LocalDateTime.now();

        var serviceResponse = rideService.getPassengerRideHistory(
                currentUserEmail,
                startDateTime,
                endDateTime);

        List<WebPassengerRideHistoryResponse> history = serviceResponse.stream()
                .map(rideMapper::toWebPassengerRideHistoryResponse)
                .toList();

        int start = page * size;
        int end = Math.min(start + size, history.size());
        List<WebPassengerRideHistoryResponse> paginatedHistory = start < history.size()
                ? history.subList(start, end)
                : new ArrayList<>();

        WebPaginatedResponse<WebPassengerRideHistoryResponse> response = new WebPaginatedResponse<>(
                paginatedHistory, page, size, (long) history.size());

        log.debug("Retrieved {} rides for passenger {}", history.size(), currentUserEmail);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all rides history (Admin)", description = "View complete ride history for all drivers and passengers with filtering - Protected endpoint (Admins only)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/history/admin/all")
    public ResponseEntity<WebPaginatedResponse<WebAdminRideHistoryResponse>> getAllRidesHistory(
            @Valid @RequestBody WebRideHistoryFilterRequest filterRequest,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Admin all rides history requested with filters: {} (page: {}, size: {})", filterRequest, page, size);

        LocalDateTime startDateTime = filterRequest.getStartDate() != null
                ? filterRequest.getStartDate()
                : LocalDateTime.now().minusYears(1);
        LocalDateTime endDateTime = filterRequest.getEndDate() != null
                ? filterRequest.getEndDate()
                : LocalDateTime.now();

        var serviceResponse = adminService.getAllRidesHistory(startDateTime, endDateTime);

        List<WebAdminRideHistoryResponse> history = serviceResponse.stream()
                .map(rideMapper::toWebAdminRideHistoryResponse)
                .toList();

        // Apply pagination
        int start = page * size;
        int end = Math.min(start + size, history.size());
        List<WebAdminRideHistoryResponse> paginatedHistory = start < history.size()
                ? history.subList(start, end)
                : new ArrayList<>();

        WebPaginatedResponse<WebAdminRideHistoryResponse> response = new WebPaginatedResponse<>(
                paginatedHistory, page, size, (long) history.size());

        log.debug("Retrieved {} total rides for admin", history.size());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all active rides (Admin)", description = "View all currently active rides - Protected endpoint (Admins only)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/active/all")
    public ResponseEntity<List<WebAdminRideHistoryResponse>> getAllActiveRides() {
        log.debug("Admin requesting all active rides");

        var serviceResponse = adminService.getActiveRides();
        List<WebAdminRideHistoryResponse> response = serviceResponse.stream()
                .map(rideMapper::toWebAdminRideHistoryResponse)
                .toList();

        log.debug("Retrieved {} active rides for admin", response.size());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get ride details (Admin)", description = "View detailed ride information with route, ratings, and reports - Protected endpoint (Admins only)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/{rideId}")
    public ResponseEntity<WebAdminRideDetailResponse> getAdminRideDetails(
            @PathVariable Long rideId) {

        log.debug("Admin requesting ride details for rideId: {}", rideId);

        var serviceResponse = adminService.getRideDetail(rideId);
        WebAdminRideDetailResponse response = rideMapper.toWebAdminRideDetailResponse(serviceResponse);

        log.debug("Retrieved admin details for rideId: {}", rideId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get ride details (Driver)", description = "View detailed ride information with all passengers - Protected endpoint (Drivers only)")
    @PreAuthorize("hasRole('DRIVER')")
    @GetMapping("/{rideId}/details")
    public ResponseEntity<WebRideDetailResponse> getRideDetailsForDriver(
            @PathVariable Long rideId,
            @AuthenticationPrincipal UserDetails currentUser) {

        log.debug("Requesting driver details for rideId: {}", rideId);

        // TODO: Implement ride detail retrieval via RideService for drivers
        // - Verify currentUser is the driver on this ride
        // - Return ride information

        WebRideDetailResponse rideDetails = new WebRideDetailResponse();

        log.debug("Retrieved driver details for rideId: {}", rideId);
        return ResponseEntity.ok(rideDetails);
    }

    @Operation(summary = "Get ride details (Passenger)", description = "View detailed ride information with route, driver details, ratings, and inconsistency reports - Protected endpoint (Passengers only)")
    @PreAuthorize("hasRole('PASSENGER')")
    @GetMapping("/{rideId}")
    public ResponseEntity<WebPassengerRideDetailResponse> getRideDetailsForPassenger(
            @PathVariable Long rideId,
            @AuthenticationPrincipal String currentUserEmail) {

        log.debug("Requesting passenger details for rideId: {} by user: {}", rideId, currentUserEmail);

        var serviceResponse = rideService.getPassengerRideDetail(rideId, currentUserEmail);
        WebPassengerRideDetailResponse response = rideMapper.toWebPassengerRideDetailResponse(serviceResponse);

        log.debug("Retrieved passenger details for rideId: {}", rideId);
        return ResponseEntity.ok(response);
    }


    @Operation(summary = "Get next scheduled ride for driver", description = "Get the next scheduled ride for the logged-in driver - Protected endpoint (Drivers only)")
    @PreAuthorize("hasRole('DRIVER')")
    @GetMapping("/next-scheduled/driver")
    public ResponseEntity<WebActiveRideResponse> getNextScheduledRideForDriver(
            @AuthenticationPrincipal String currentUserEmail) {
        log.debug("Get next scheduled ride requested for driver: {}", currentUserEmail);

        var nextRide = rideService.getNextScheduledRideForDriver(currentUserEmail);

        if (nextRide.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        WebActiveRideResponse response = rideMapper.toWebActiveRideResponse(nextRide.get());
        return ResponseEntity.ok(response);
    }


}
