package com.pekara.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pekara.dto.common.LocationPointDto;
import com.pekara.dto.RouteDto;
import com.pekara.util.GeoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RoutingServiceImpl implements RoutingService {

    private static final String OSRM_BASE_URL = "https://router.project-osrm.org";
    private static final int TIMEOUT_SECONDS = 10;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public RoutingServiceImpl(ObjectMapper objectMapper) {
        this.webClient = WebClient.builder()
                .baseUrl(OSRM_BASE_URL)
                .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public RouteDto calculateRoute(List<LocationPointDto> waypoints) {
        if (waypoints == null || waypoints.size() < 2) {
            throw new IllegalArgumentException("At least 2 waypoints required for routing");
        }

        try {
            String coordinates = waypoints.stream()
                    .map(wp -> wp.getLongitude() + "," + wp.getLatitude())
                    .collect(Collectors.joining(";"));

            String uri = String.format("/route/v1/driving/%s?overview=full&geometries=geojson", coordinates);

            log.debug("Calling OSRM API: {}", uri);

            Map<String, Object> response = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .block();

            if (response == null || !"Ok".equals(response.get("code"))) {
                log.warn("OSRM API returned error, falling back to Haversine calculation");
                return fallbackCalculation(waypoints);
            }

            List<Map<String, Object>> routes = (List<Map<String, Object>>) response.get("routes");
            if (routes == null || routes.isEmpty()) {
                log.warn("OSRM returned no routes, falling back to Haversine calculation");
                return fallbackCalculation(waypoints);
            }

            Map<String, Object> route = routes.get(0);
            double distanceMeters = ((Number) route.get("distance")).doubleValue();
            double durationSeconds = ((Number) route.get("duration")).doubleValue();

            Map<String, Object> geometry = (Map<String, Object>) route.get("geometry");
            List<List<Number>> coords = (List<List<Number>>) geometry.get("coordinates");

            List<LocationPointDto> routePoints = new ArrayList<>();
            if (coords != null) {
                for (List<Number> coord : coords) {
                    routePoints.add(LocationPointDto.builder()
                            .longitude(coord.get(0).doubleValue())
                            .latitude(coord.get(1).doubleValue())
                            .build());
                }
            }

            return RouteDto.builder()
                    .distanceKm(distanceMeters / 1000.0)
                    .durationMinutes((int) Math.ceil(durationSeconds / 60.0))
                    .routePoints(routePoints)
                    .build();

        } catch (Exception e) {
            log.error("Error calling OSRM API, falling back to Haversine calculation", e);
            return fallbackCalculation(waypoints);
        }
    }

    private RouteDto fallbackCalculation(List<LocationPointDto> waypoints) {
        double totalDistanceKm = 0;
        for (int i = 0; i < waypoints.size() - 1; i++) {
            LocationPointDto from = waypoints.get(i);
            LocationPointDto to = waypoints.get(i + 1);
            totalDistanceKm += GeoUtils.haversineKm(
                    from.getLatitude(), from.getLongitude(),
                    to.getLatitude(), to.getLongitude()
            );
        }

        int stopCount = waypoints.size() - 2;
        double durationMinutes = (totalDistanceKm / 40.0) * 60.0 + (stopCount * 3.0);

        return RouteDto.builder()
                .distanceKm(totalDistanceKm)
                .durationMinutes(Math.max(1, (int) Math.round(durationMinutes)))
                .routePoints(waypoints)
                .build();
    }

    @Override
    public Double calculateActualDistanceFromRoute(String routeCoordinatesJson, LocationPointDto stopLocation) {
        if (routeCoordinatesJson == null || routeCoordinatesJson.isBlank()) {
            log.warn("Route coordinates JSON is null or empty, returning 0");
            return 0.0;
        }

        if (stopLocation == null) {
            log.warn("Stop location is invalid, returning 0");
            return 0.0;
        }

        try {
            // Parse JSON: [[lon, lat], [lon, lat], ...]
            List<List<Double>> coordinates = objectMapper.readValue(
                    routeCoordinatesJson,
                    new TypeReference<>() {}
            );

            if (coordinates == null || coordinates.size() < 2) {
                log.warn("Not enough route coordinates, returning 0");
                return 0.0;
            }

            double cumulativeDistance = 0.0;
            double minDistanceToStop = Double.MAX_VALUE;
            double distanceAtClosestPoint = 0.0;

            // Iterate through route coordinates
            for (int i = 0; i < coordinates.size(); i++) {
                List<Double> coord = coordinates.get(i);
                if (coord.size() < 2) continue;

                double lon = coord.get(0);
                double lat = coord.get(1);

                // Calculate distance from current route point to stop location
                double distanceToStop = GeoUtils.haversineKm(
                        lat, lon,
                        stopLocation.getLatitude(), stopLocation.getLongitude()
                );

                // Track closest point
                if (distanceToStop < minDistanceToStop) {
                    minDistanceToStop = distanceToStop;
                    distanceAtClosestPoint = cumulativeDistance;
                }

                // Add distance to next point (if not last)
                if (i < coordinates.size() - 1) {
                    List<Double> nextCoord = coordinates.get(i + 1);
                    if (nextCoord.size() >= 2) {
                        double nextLon = nextCoord.get(0);
                        double nextLat = nextCoord.get(1);
                        cumulativeDistance += GeoUtils.haversineKm(lat, lon, nextLat, nextLon);
                    }
                }
            }

            log.debug("Calculated actual distance: {} km (closest point distance: {} km)",
                    distanceAtClosestPoint, minDistanceToStop);

            return distanceAtClosestPoint;

        } catch (Exception e) {
            log.error("Error parsing route coordinates JSON, returning 0", e);
            return 0.0;
        }
    }
}
