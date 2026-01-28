package com.pekara.service;

import com.pekara.dto.request.OrderRideRequest;

import java.time.LocalDateTime;

public interface DriverMatchingService {

    Long selectDriverIdForRide(OrderRideRequest request, LocalDateTime now);

    boolean hasExceededWorkLimit(Long driverId, LocalDateTime now);
}
