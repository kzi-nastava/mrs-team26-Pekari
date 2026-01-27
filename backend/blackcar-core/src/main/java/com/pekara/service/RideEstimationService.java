package com.pekara.service;

import com.pekara.dto.common.LocationPointDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

public interface RideEstimationService {

    RouteData calculateRouteWithStops(LocationPointDto pickup, LocationPointDto dropoff, List<LocationPointDto> stops);

    BigDecimal calculatePrice(String vehicleType, double distanceKm);

    Double roundKm(double km);

    String serializeRouteCoordinates(List<LocationPointDto> routePoints);

    void validateLocation(LocationPointDto point, String name);

    @Getter
    @AllArgsConstructor
    class RouteData {
        private final double distanceKm;
        private final int durationMinutes;
        private final List<LocationPointDto> routePoints;
    }
}
