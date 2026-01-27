package com.pekara.service;

import com.pekara.dto.common.LocationPointDto;
import com.pekara.dto.request.EstimateRideRequest;
import com.pekara.dto.request.OrderRideRequest;
import com.pekara.dto.response.ActiveRideResponse;
import com.pekara.dto.response.OrderRideResponse;
import com.pekara.dto.response.RideEstimateResponse;

import java.util.Optional;

public interface RideService {
    RideEstimateResponse estimateRide(EstimateRideRequest request);

    OrderRideResponse orderRide(String creatorEmail, OrderRideRequest request);

    void startRide(Long rideId, String driverEmail);

    void completeRide(Long rideId, String driverEmail);

    void cancelRide(Long rideId, String userEmail, String reason);

    Optional<ActiveRideResponse> getActiveRideForDriver(String driverEmail);

    Optional<ActiveRideResponse> getActiveRideForPassenger(String passengerEmail);

    void requestStopRide(Long rideId, String passengerEmail);

    void stopRideEarly(Long rideId, String driverEmail, LocationPointDto actualStopLocation);
}
