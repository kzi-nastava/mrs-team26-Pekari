package com.pekara.service;

import com.pekara.dto.response.AdminRideDetailResponse;
import com.pekara.dto.response.AdminRideHistoryResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface AdminService {

    List<AdminRideHistoryResponse> getAllRidesHistory(LocalDateTime startDate, LocalDateTime endDate);

    AdminRideDetailResponse getRideDetail(Long rideId);
}
