package com.pekara.service;

import com.pekara.constant.RideStatus;
import com.pekara.dto.common.LocationPointDto;
import com.pekara.dto.request.EstimateRideRequest;
import com.pekara.dto.request.OrderRideRequest;
import com.pekara.dto.response.OrderRideResponse;
import com.pekara.dto.response.RideEstimateResponse;
import com.pekara.exception.InvalidScheduleTimeException;
import com.pekara.exception.NoActiveDriversException;
import com.pekara.exception.NoDriversAvailableException;
import com.pekara.model.Driver;
import com.pekara.model.DriverState;
import com.pekara.model.DriverWorkLog;
import com.pekara.model.Ride;
import com.pekara.model.RideStop;
import com.pekara.model.User;
import com.pekara.repository.DriverStateRepository;
import com.pekara.repository.DriverWorkLogRepository;
import com.pekara.repository.RideRepository;
import com.pekara.repository.UserRepository;
import com.pekara.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RideServiceImpl implements com.pekara.service.RideService {

    private static final BigDecimal PRICE_PER_KM = new BigDecimal("120");

    private final UserRepository userRepository;
    private final RideRepository rideRepository;
    private final DriverStateRepository driverStateRepository;
    private final DriverWorkLogRepository driverWorkLogRepository;
    private final MailService mailService;
    private final RoutingService routingService;

    @Override
    @Transactional(readOnly = true)
    public RideEstimateResponse estimateRide(EstimateRideRequest request) {
        validateLocation(request.getPickup(), "pickup");
        validateLocation(request.getDropoff(), "dropoff");

        if (request.getStops() != null) {
            for (LocationPointDto stop : request.getStops()) {
                validateLocation(stop, "stop");
            }
        }

        List<LocationPointDto> waypoints = new ArrayList<>();
        waypoints.add(request.getPickup());
        if (request.getStops() != null) {
            waypoints.addAll(request.getStops());
        }
        waypoints.add(request.getDropoff());

        var route = routingService.calculateRoute(waypoints);

        BigDecimal estimatedPrice = calculatePrice(request.getVehicleType(), route.getDistanceKm());

        return RideEstimateResponse.builder()
                .estimatedPrice(estimatedPrice)
                .estimatedDurationMinutes(route.getDurationMinutes())
                .distanceKm(roundKm(route.getDistanceKm()))
                .vehicleType(request.getVehicleType())
                .routePoints(route.getRoutePoints())
                .build();
    }

    @Override
    @Transactional
    public OrderRideResponse orderRide(String creatorEmail, OrderRideRequest request) {
        User creator = userRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        LocalDateTime now = LocalDateTime.now();
        if (request.getScheduledAt() != null) {
            if (request.getScheduledAt().isBefore(now)) {
                throw new InvalidScheduleTimeException("Scheduled time must be in the future");
            }
            if (request.getScheduledAt().isAfter(now.plusHours(5))) {
                throw new InvalidScheduleTimeException("Ride can be scheduled at most 5 hours in advance");
            }
        }

        validateLocation(request.getPickup(), "pickup");
        validateLocation(request.getDropoff(), "dropoff");

        if (request.getStops() != null) {
            for (LocationPointDto stop : request.getStops()) {
                validateLocation(stop, "stop");
            }
        }

        List<LocationPointDto> waypoints = new ArrayList<>();
        waypoints.add(request.getPickup());
        if (request.getStops() != null) {
            waypoints.addAll(request.getStops());
        }
        waypoints.add(request.getDropoff());

        var routeData = routingService.calculateRoute(waypoints);
        double distanceKm = routeData.getDistanceKm();
        int estimatedDurationMinutes = routeData.getDurationMinutes();
        BigDecimal estimatedPrice = calculatePrice(request.getVehicleType(), distanceKm);

        Ride ride = Ride.builder()
                .creator(creator)
                .status(request.getScheduledAt() != null ? RideStatus.SCHEDULED : RideStatus.ACCEPTED)
                .vehicleType(request.getVehicleType())
                .babyTransport(Boolean.TRUE.equals(request.getBabyTransport()))
                .petTransport(Boolean.TRUE.equals(request.getPetTransport()))
                .scheduledAt(request.getScheduledAt())
                .estimatedPrice(estimatedPrice)
                .distanceKm(roundKm(distanceKm))
                .estimatedDurationMinutes(estimatedDurationMinutes)
                .build();

        // Passengers: include creator + linked passengers (if they exist)
        ride.getPassengers().add(creator);
        if (request.getPassengerEmails() != null) {
            for (String email : request.getPassengerEmails()) {
                if (email == null || email.isBlank()) {
                    continue;
                }
                userRepository.findByEmail(email).ifPresent(ride.getPassengers()::add);
            }
        }

        // Stops: pickup + intermediate stops + dropoff
        int seq = 0;
        ride.addStop(RideStop.builder()
                .sequenceIndex(seq++)
                .address(request.getPickup().getAddress())
                .latitude(request.getPickup().getLatitude())
                .longitude(request.getPickup().getLongitude())
                .build());

        if (request.getStops() != null) {
            for (LocationPointDto stop : request.getStops()) {
                ride.addStop(RideStop.builder()
                        .sequenceIndex(seq++)
                        .address(stop.getAddress())
                        .latitude(stop.getLatitude())
                        .longitude(stop.getLongitude())
                        .build());
            }
        }

        ride.addStop(RideStop.builder()
                .sequenceIndex(seq)
                .address(request.getDropoff().getAddress())
                .latitude(request.getDropoff().getLatitude())
                .longitude(request.getDropoff().getLongitude())
                .build());

        DriverState chosenDriver = selectDriverForRide(request, now);

        if (chosenDriver == null) {
            throw new NoDriversAvailableException("Currently there are no active drivers available");
        }

        // Lock and assign
        DriverState lockedState = driverStateRepository.findByDriverIdForUpdate(chosenDriver.getId())
                .orElseThrow(() -> new IllegalArgumentException("Driver state not found"));

        if (!Boolean.TRUE.equals(lockedState.getOnline())) {
            throw new NoDriversAvailableException("Currently there are no active drivers available");
        }

        // If already reserved/scheduled, keep them unavailable for immediate rides
        if (request.getScheduledAt() != null) {
            lockedState.setNextScheduledRideAt(request.getScheduledAt());
        } else {
            lockedState.setBusy(true);
            lockedState.setCurrentRideEndsAt(now.plusMinutes(estimatedDurationMinutes));
            lockedState.setCurrentRideEndLatitude(request.getDropoff().getLatitude());
            lockedState.setCurrentRideEndLongitude(request.getDropoff().getLongitude());
        }

        Driver driver = lockedState.getDriver();
        ride.setDriver(driver);

        Ride saved = rideRepository.save(ride);
        driverStateRepository.save(lockedState);

        if (request.getScheduledAt() == null) {
            // Best-effort approximation: until full start/stop flow is implemented,
            // treat assignment time as work start and estimated end as work end.
            driverWorkLogRepository.save(DriverWorkLog.builder()
                .driver(driver)
                .ride(saved)
                .startedAt(now)
                .endedAt(now.plusMinutes(estimatedDurationMinutes))
                .build());
        }

        // Email notifications
        mailService.sendRideAssignedToDriver(driver.getEmail(), saved.getId(), saved.getScheduledAt());
        mailService.sendRideOrderAccepted(creator.getEmail(), saved.getId(), saved.getStatus().name());

        if (request.getPassengerEmails() != null) {
            for (String email : request.getPassengerEmails()) {
                if (email == null || email.isBlank()) {
                    continue;
                }
                mailService.sendRideDetailsShared(email, saved.getId(), creator.getEmail());
            }
        }

        return OrderRideResponse.builder()
                .rideId(saved.getId())
                .status(saved.getStatus().name())
                .message(saved.getStatus() == RideStatus.SCHEDULED ? "Ride scheduled successfully." : "Ride ordered successfully.")
                .estimatedPrice(saved.getEstimatedPrice())
                .scheduledAt(saved.getScheduledAt())
                .assignedDriverEmail(driver.getEmail())
                .build();
    }

    private DriverState selectDriverForRide(OrderRideRequest request, LocalDateTime now) {
        List<DriverState> onlineDrivers = driverStateRepository.findAllOnlineDrivers();
        if (onlineDrivers.isEmpty()) {
            throw new NoActiveDriversException("Currently there are no active drivers");
        }

        // Exclude drivers that already have a reserved scheduled ride (simple rule for now)
        List<DriverState> eligible = onlineDrivers.stream()
                .filter(ds -> ds.getDriver() != null)
                .filter(ds -> ds.getNextScheduledRideAt() == null)
                .filter(ds -> !hasExceededWorkLimit(ds.getDriver().getId(), now))
                .filter(ds -> {
                    String reqType = request.getVehicleType();
                    // If request doesn't specify type, assume STANDARD or allow any?
                    // Let's assume strict matching if specified, otherwise loose (or default STANDARD).
                    if (reqType == null) return true;
                    
                    String driverType = ds.getDriver().getVehicleType();
                    return driverType != null && driverType.equalsIgnoreCase(reqType);
                })
                .toList();

        if (eligible.isEmpty()) {
            return null;
        }

        // Free drivers first
        List<DriverState> free = eligible.stream()
                .filter(ds -> !Boolean.TRUE.equals(ds.getBusy()))
                .toList();

        if (!free.isEmpty()) {
            return free.stream()
                    .min(Comparator.comparingDouble(ds -> distanceToPickup(ds, request.getPickup())))
                    .orElse(null);
        }

        // Otherwise allow busy drivers that finish within 10 minutes
        LocalDateTime limit = now.plusMinutes(10);
        return eligible.stream()
                .filter(ds -> Boolean.TRUE.equals(ds.getBusy()))
                .filter(ds -> ds.getCurrentRideEndsAt() != null && !ds.getCurrentRideEndsAt().isAfter(limit))
                .min(Comparator.comparingDouble(ds -> distanceToPickupFromEnd(ds, request.getPickup())))
                .orElse(null);
    }

    private boolean hasExceededWorkLimit(Long driverId, LocalDateTime now) {
        LocalDateTime since = now.minusHours(24);
        long workedMinutes = driverWorkLogRepository.findSince(driverId, since).stream()
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

    private void validateLocation(LocationPointDto point, String name) {
        if (point == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        if (point.getAddress() == null || point.getAddress().isBlank()) {
            throw new IllegalArgumentException(name + " address is required");
        }
    }


    private BigDecimal calculatePrice(String vehicleType, double distanceKm) {
        BigDecimal base = basePrice(vehicleType);
        BigDecimal kmPart = PRICE_PER_KM.multiply(BigDecimal.valueOf(distanceKm));
        return base.add(kmPart).setScale(2, RoundingMode.HALF_UP);
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

    private Double roundKm(double km) {
        return BigDecimal.valueOf(km).setScale(3, RoundingMode.HALF_UP).doubleValue();
    }
}
