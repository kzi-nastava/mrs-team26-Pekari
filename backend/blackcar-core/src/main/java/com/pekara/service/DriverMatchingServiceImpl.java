package com.pekara.service;

import com.pekara.dto.common.LocationPointDto;
import com.pekara.dto.request.OrderRideRequest;
import com.pekara.exception.NoActiveDriversException;
import com.pekara.model.DriverState;
import com.pekara.repository.DriverStateRepository;
import com.pekara.repository.DriverWorkLogRepository;
import com.pekara.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DriverMatchingServiceImpl implements DriverMatchingService {

    private final DriverStateRepository driverStateRepository;
    private final DriverWorkLogRepository driverWorkLogRepository;

    @Override
    public Long selectDriverIdForRide(OrderRideRequest request, LocalDateTime now) {
        List<DriverState> onlineDrivers = driverStateRepository.findAllOnlineDrivers();
        if (onlineDrivers.isEmpty()) {
            throw new NoActiveDriversException("Currently there are no active drivers");
        }

        String reqType = request.getVehicleType();
        boolean needsBaby = Boolean.TRUE.equals(request.getBabyTransport());
        boolean needsPet = Boolean.TRUE.equals(request.getPetTransport());

        List<DriverState> eligible = onlineDrivers.stream()
                .filter(ds -> ds.getDriver() != null)
                .filter(ds -> ds.getNextScheduledRideAt() == null)
                .filter(ds -> !hasExceededWorkLimit(ds.getDriver().getId(), now))
                .filter(ds -> {
                    if (reqType == null) return true;
                    String driverType = ds.getDriver().getVehicleType();
                    return driverType != null && driverType.equalsIgnoreCase(reqType);
                })
                .filter(ds -> !needsBaby || Boolean.TRUE.equals(ds.getDriver().getBabyFriendly()))
                .filter(ds -> !needsPet || Boolean.TRUE.equals(ds.getDriver().getPetFriendly()))
                .toList();

        if (eligible.isEmpty()) {
            return null;
        }

        List<DriverState> free = eligible.stream()
                .filter(ds -> !Boolean.TRUE.equals(ds.getBusy()))
                .toList();

        if (!free.isEmpty()) {
            return free.stream()
                    .min(Comparator.comparingDouble(ds -> distanceToPickup(ds, request.getPickup())))
                    .map(ds -> ds.getDriver().getId())
                    .orElse(null);
        }

        LocalDateTime limit = now.plusMinutes(10);
        return eligible.stream()
                .filter(ds -> Boolean.TRUE.equals(ds.getBusy()))
                .filter(ds -> ds.getCurrentRideEndsAt() != null && !ds.getCurrentRideEndsAt().isAfter(limit))
                .min(Comparator.comparingDouble(ds -> distanceToPickupFromEnd(ds, request.getPickup())))
                .map(ds -> ds.getDriver().getId())
                .orElse(null);
    }

    @Override
    public boolean hasExceededWorkLimit(Long driverId, LocalDateTime now) {
        LocalDateTime since = now.minusHours(24);
        long workedMinutes = driverWorkLogRepository.findCompletedSince(driverId, since).stream()
                .filter(w -> w.getEndedAt() != null)
                .mapToLong(w -> java.time.Duration.between(w.getStartedAt(), w.getEndedAt()).toMinutes())
                .sum();
        return workedMinutes > 8L * 60L;
    }

    private double distanceToPickup(DriverState ds, LocationPointDto pickup) {
        if (ds.getLatitude() == null || ds.getLongitude() == null) {
            return Double.MAX_VALUE;
        }
        return GeoUtils.haversineKm(ds.getLatitude(), ds.getLongitude(), pickup.getLatitude(), pickup.getLongitude());
    }

    private double distanceToPickupFromEnd(DriverState ds, LocationPointDto pickup) {
        if (ds.getCurrentRideEndLatitude() != null && ds.getCurrentRideEndLongitude() != null) {
            return GeoUtils.haversineKm(ds.getCurrentRideEndLatitude(), ds.getCurrentRideEndLongitude(), pickup.getLatitude(), pickup.getLongitude());
        }
        return distanceToPickup(ds, pickup);
    }
}
