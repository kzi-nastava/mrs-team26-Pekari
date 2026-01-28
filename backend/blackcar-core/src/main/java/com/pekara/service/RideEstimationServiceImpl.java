package com.pekara.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pekara.dto.RouteDto;
import com.pekara.dto.common.LocationPointDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RideEstimationServiceImpl implements RideEstimationService {

    private static final BigDecimal PRICE_PER_KM = new BigDecimal("120");

    private final RoutingService routingService;
    private final ObjectMapper objectMapper;

    @Override
    public RouteData calculateRouteWithStops(LocationPointDto pickup, LocationPointDto dropoff, List<LocationPointDto> stops) {
        validateLocation(pickup, "pickup");
        validateLocation(dropoff, "dropoff");

        if (stops != null) {
            for (LocationPointDto stop : stops) {
                validateLocation(stop, "stop");
            }
        }

        List<LocationPointDto> waypoints = new ArrayList<>();
        waypoints.add(pickup);
        if (stops != null) {
            waypoints.addAll(stops);
        }
        waypoints.add(dropoff);

        RouteDto routeDto = routingService.calculateRoute(waypoints);
        return new RideEstimationService.RouteData(routeDto.getDistanceKm(), routeDto.getDurationMinutes(), routeDto.getRoutePoints());
    }

    @Override
    public BigDecimal calculatePrice(String vehicleType, double distanceKm) {
        BigDecimal base = basePrice(vehicleType);
        BigDecimal kmPart = PRICE_PER_KM.multiply(BigDecimal.valueOf(distanceKm));
        return base.add(kmPart).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public Double roundKm(double km) {
        return BigDecimal.valueOf(km).setScale(3, RoundingMode.HALF_UP).doubleValue();
    }

    @Override
    public String serializeRouteCoordinates(List<LocationPointDto> routePoints) {
        if (routePoints == null || routePoints.isEmpty()) {
            return null;
        }
        List<double[]> coordinates = routePoints.stream()
                .map(point -> new double[] {point.getLatitude(), point.getLongitude()})
                .toList();
        try {
            return objectMapper.writeValueAsString(coordinates);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize route coordinates", e);
            return null;
        }
    }

    @Override
    public void validateLocation(LocationPointDto point, String name) {
        if (point == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        if (point.getAddress() == null || point.getAddress().isBlank()) {
            throw new IllegalArgumentException(name + " address is required");
        }
    }

    private BigDecimal basePrice(String vehicleType) {
        if (vehicleType == null) {
            return new BigDecimal("0");
        }
        return switch (vehicleType.toUpperCase()) {
            case "STANDARD" -> new BigDecimal("200");
            case "VAN" -> new BigDecimal("300");
            case "LUX" -> new BigDecimal("500");
            default -> new BigDecimal("200");
        };
    }
}
