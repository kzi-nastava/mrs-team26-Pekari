package com.pekara.service;

import com.pekara.dto.common.LocationPointDto;
import com.pekara.dto.request.EstimateRideRequest;
import com.pekara.dto.request.InconsistencyReportRequest;
import com.pekara.dto.request.OrderRideRequest;
import com.pekara.dto.request.RideRatingRequest;
import com.pekara.dto.response.ActiveRideResponse;
import com.pekara.dto.response.DriverRideHistoryResponse;
import com.pekara.dto.response.OrderRideResponse;
import com.pekara.dto.response.PassengerRideDetailResponse;
import com.pekara.dto.response.PassengerRideHistoryResponse;
import com.pekara.dto.response.RideEstimateResponse;
import com.pekara.dto.response.RideStatsResponse;

import java.time.LocalDateTime;
import java.util.List;
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

    List<DriverRideHistoryResponse> getDriverRideHistory(String driverEmail, LocalDateTime startDate, LocalDateTime endDate);

    List<PassengerRideHistoryResponse> getPassengerRideHistory(String passengerEmail, LocalDateTime startDate, LocalDateTime endDate);

    RideStatsResponse getDriverRideStats(String driverEmail, LocalDateTime startDate, LocalDateTime endDate);

    RideStatsResponse getPassengerRideStats(String passengerEmail, LocalDateTime startDate, LocalDateTime endDate);

    void activatePanic(Long rideId, String userEmail);

    List<DriverRideHistoryResponse> getActivePanicRides();
  
    void rateRide(Long rideId, String passengerEmail, RideRatingRequest request);

    void reportInconsistency(Long rideId, String passengerEmail, InconsistencyReportRequest request);

    Optional<ActiveRideResponse> getNextScheduledRideForDriver(String driverEmail);

    PassengerRideDetailResponse getPassengerRideDetail(Long rideId, String passengerEmail);
}
