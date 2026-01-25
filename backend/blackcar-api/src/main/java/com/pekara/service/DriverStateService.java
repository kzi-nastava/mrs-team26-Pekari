package com.pekara.service;

import com.pekara.dto.request.UpdateDriverLocationRequest;
import com.pekara.dto.request.UpdateDriverOnlineStatusRequest;
import com.pekara.dto.response.DriverStateResponse;
import com.pekara.dto.response.OnlineDriverWithVehicleResponse;

import java.util.List;

public interface DriverStateService {
    DriverStateResponse updateOnlineStatus(String driverEmail, UpdateDriverOnlineStatusRequest request);

    DriverStateResponse updateLocation(String driverEmail, UpdateDriverLocationRequest request);

    DriverStateResponse getMyState(String driverEmail);

    List<DriverStateResponse> getOnlineDrivers(int page, int size);

    List<OnlineDriverWithVehicleResponse> getOnlineDriversWithVehicles(int page, int size);
}
