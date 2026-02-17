package com.pekara.service;

import com.pekara.constant.RideStatsScope;
import com.pekara.dto.response.AdminRideDetailResponse;
import com.pekara.dto.response.AdminRideHistoryResponse;
import com.pekara.dto.response.DriverBasicDto;
import com.pekara.dto.response.PassengerBasicDto;
import com.pekara.dto.response.RideStatsResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface AdminService {

    List<AdminRideHistoryResponse> getAllRidesHistory(LocalDateTime startDate, LocalDateTime endDate);

    List<AdminRideHistoryResponse> getActiveRides();

    AdminRideDetailResponse getRideDetail(Long rideId);

    RideStatsResponse getRideStatsAdmin(LocalDateTime startDate, LocalDateTime endDate, RideStatsScope scope, Long userId);

    List<DriverBasicDto> listDriversForAdmin();

    List<PassengerBasicDto> listPassengersForAdmin();
}
