package com.pekara.service;

import com.pekara.constant.RideStatus;
import com.pekara.dto.RouteDto;
import com.pekara.dto.common.LocationPointDto;
import com.pekara.dto.common.RideLocationDto;
import com.pekara.dto.request.RideLocationUpdateRequest;
import com.pekara.dto.response.RideTrackingResponse;
import com.pekara.model.Ride;
import com.pekara.model.RideStop;
import com.pekara.repository.RideRepository;
import com.pekara.tracking.RideLocationCacheEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RideTrackingServiceImpl implements RideTrackingService {

    private static final Duration LOCATION_TTL = Duration.ofHours(6);

    private final RideRepository rideRepository;
    private final RoutingService routingService;
    private final RedisTemplate<String, RideLocationCacheEntry> rideLocationRedisTemplate;

    @Override
    @Transactional
    public void updateLocation(Long rideId, String driverEmail, RideLocationUpdateRequest request) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found"));

        validateDriver(ride, driverEmail);

        if (!isActiveForTracking(ride.getStatus())) {
            throw new IllegalStateException("Ride is not active for tracking");
        }

        validateCoordinates(request.getLatitude(), request.getLongitude());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime recordedAt = request.getRecordedAt() != null ? request.getRecordedAt() : now;

        LocationPointDto destination = finalDestination(ride);
        RouteDto route = null;
        if (destination != null) {
            try {
                route = routingService.calculateRoute(List.of(
                        toLocationPoint(request.getLatitude(), request.getLongitude(), null),
                        destination
                ));
            } catch (Exception ex) {
                log.warn("Failed to calculate ETA for ride {}: {}", rideId, ex.getMessage());
            }
        }

        Integer etaSeconds = route != null && route.getDurationMinutes() != null
                ? route.getDurationMinutes() * 60
                : null;

        Double distanceToDestinationKm = route != null && route.getDistanceKm() != null
                ? round(route.getDistanceKm())
                : null;

        RideLocationCacheEntry cacheEntry = RideLocationCacheEntry.builder()
                .rideId(rideId)
                .rideStatus(ride.getStatus())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .heading(request.getHeading())
                .speed(request.getSpeed())
                .etaSeconds(etaSeconds)
                .distanceToDestinationKm(distanceToDestinationKm)
                .recordedAt(recordedAt)
                .updatedAt(now)
                .driverId(ride.getDriver() != null ? ride.getDriver().getId() : null)
                .driverLicensePlate(ride.getDriver() != null ? ride.getDriver().getLicensePlate() : null)
                .vehicleType(ride.getVehicleType())
                .nextStopAddress(destination != null ? destination.getAddress() : null)
                .build();

        rideLocationRedisTemplate.opsForValue().set(cacheKey(rideId), cacheEntry, LOCATION_TTL);
        log.debug("Ride {} tracking updated in Redis by {}", rideId, driverEmail);
    }

    @Override
    @Transactional(readOnly = true)
    public RideTrackingResponse getTracking(Long rideId, String requesterEmail) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found"));

        if (!isActiveForTracking(ride.getStatus())) {
            throw new IllegalStateException("Ride is not active for tracking");
        }

        if (!isParticipant(ride, requesterEmail)) {
            throw new IllegalArgumentException("You are not authorized to view tracking for this ride");
        }

        RideLocationCacheEntry cacheEntry = rideLocationRedisTemplate.opsForValue().get(cacheKey(rideId));

        if (cacheEntry == null) {
            return RideTrackingResponse.builder()
                    .rideId(ride.getId())
                    .rideStatus(ride.getStatus())
                    .driverId(ride.getDriver() != null ? ride.getDriver().getId() : null)
                    .driverLicensePlate(ride.getDriver() != null ? ride.getDriver().getLicensePlate() : null)
                    .vehicleType(ride.getVehicleType())
                    .nextStopAddress(lastStopAddress(ride))
                    .build();
        }

        return RideTrackingResponse.builder()
                .rideId(cacheEntry.getRideId())
                .rideStatus(ride.getStatus())
                .lastLocation(RideLocationDto.builder()
                        .latitude(cacheEntry.getLatitude())
                        .longitude(cacheEntry.getLongitude())
                        .heading(cacheEntry.getHeading())
                        .speed(cacheEntry.getSpeed())
                        .recordedAt(cacheEntry.getRecordedAt())
                        .build())
                .etaSeconds(cacheEntry.getEtaSeconds())
                .distanceToDestinationKm(cacheEntry.getDistanceToDestinationKm())
                .updatedAt(cacheEntry.getUpdatedAt())
                .driverId(cacheEntry.getDriverId())
                .driverLicensePlate(cacheEntry.getDriverLicensePlate())
                .vehicleType(cacheEntry.getVehicleType())
                .nextStopAddress(cacheEntry.getNextStopAddress())
                .build();
    }

    private boolean isActiveForTracking(RideStatus status) {
        return status == RideStatus.ACCEPTED || status == RideStatus.IN_PROGRESS || status == RideStatus.SCHEDULED || status == RideStatus.STOP_REQUESTED;
    }

    private void validateDriver(Ride ride, String driverEmail) {
        if (ride.getDriver() == null || !ride.getDriver().getEmail().equals(driverEmail)) {
            throw new IllegalArgumentException("You are not the assigned driver for this ride");
        }
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("Latitude and longitude are required");
        }
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Invalid coordinate values");
        }
    }

    private boolean isParticipant(Ride ride, String requesterEmail) {
        boolean isDriver = ride.getDriver() != null && ride.getDriver().getEmail().equals(requesterEmail);
        boolean isCreator = ride.getCreator() != null && ride.getCreator().getEmail().equals(requesterEmail);
        boolean isPassenger = ride.getPassengers().stream()
                .anyMatch(p -> p.getEmail().equals(requesterEmail));
        return isDriver || isCreator || isPassenger;
    }

    private LocationPointDto finalDestination(Ride ride) {
        List<RideStop> stops = ride.getStops();
        if (stops == null || stops.isEmpty()) {
            return null;
        }
        RideStop last = stops.get(stops.size() - 1);
        return toLocationPoint(last.getLatitude(), last.getLongitude(), last.getAddress());
    }

    private String lastStopAddress(Ride ride) {
        List<RideStop> stops = ride.getStops();
        if (stops == null || stops.isEmpty()) {
            return null;
        }
        return stops.get(stops.size() - 1).getAddress();
    }

    private LocationPointDto toLocationPoint(Double lat, Double lon, String address) {
        return LocationPointDto.builder()
                .latitude(lat)
                .longitude(lon)
                .address(address)
                .build();
    }

    private String cacheKey(Long rideId) {
        return "ride:%d:location".formatted(rideId);
    }

    private Double round(Double value) {
        if (value == null) {
            return null;
        }
        return BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP).doubleValue();
    }
}
