package com.pekara.service;

import com.pekara.dto.request.EstimateRideRequest;
import com.pekara.dto.request.OrderRideRequest;
import com.pekara.dto.response.ActiveRideResponse;
import com.pekara.dto.response.OrderRideResponse;
import com.pekara.dto.response.RideEstimateResponse;

import java.util.Optional;

public interface RideService {
    RideEstimateResponse estimateRide(EstimateRideRequest request);

    OrderRideResponse orderRide(String creatorEmail, OrderRideRequest request);

    /**
     * Start a ride - called when driver begins the ride.
     * Creates/updates work log with actual start time.
     */
    void startRide(Long rideId, String driverEmail);

    /**
     * Complete a ride - called when driver finishes the ride.
     * Marks work log as completed with actual end time.
     */
    void completeRide(Long rideId, String driverEmail);

    /**
     * Cancel a ride - marks work log as not completed (won't count towards 8-hour limit).
     */
    void cancelRide(Long rideId, String userEmail, String reason);

    /**
     * Get the active ride for a driver (ACCEPTED, SCHEDULED, or IN_PROGRESS).
     */
    Optional<ActiveRideResponse> getActiveRideForDriver(String driverEmail);

    /**
     * Get the active ride for a passenger (ACCEPTED, SCHEDULED, or IN_PROGRESS).
     */
    Optional<ActiveRideResponse> getActiveRideForPassenger(String passengerEmail);
}
