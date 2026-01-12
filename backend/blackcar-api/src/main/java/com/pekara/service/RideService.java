package com.pekara.service;

import com.pekara.dto.request.EstimateRideRequest;
import com.pekara.dto.request.OrderRideRequest;
import com.pekara.dto.response.OrderRideResponse;
import com.pekara.dto.response.RideEstimateResponse;

public interface RideService {
    RideEstimateResponse estimateRide(EstimateRideRequest request);

    OrderRideResponse orderRide(String creatorEmail, OrderRideRequest request);
}
