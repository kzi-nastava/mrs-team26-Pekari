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
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
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
    private final EntityManager entityManager;

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
        LocalDateTime now = LocalDateTime.now();
        
        // Validate schedule time
        if (request.getScheduledAt() != null) {
            if (request.getScheduledAt().isBefore(now)) {
                throw new InvalidScheduleTimeException("Scheduled time must be in the future");
            }
            if (request.getScheduledAt().isAfter(now.plusHours(5))) {
                throw new InvalidScheduleTimeException("Ride can be scheduled at most 5 hours in advance");
            }
        }

        // Validate locations
        validateLocation(request.getPickup(), "pickup");
        validateLocation(request.getDropoff(), "dropoff");
        if (request.getStops() != null) {
            for (LocationPointDto stop : request.getStops()) {
                validateLocation(stop, "stop");
            }
        }

        // Calculate route
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

        // Select driver candidate (returns ID only to avoid locking conflicts)
        Long candidateDriverId = selectDriverIdForRide(request, now);

        if (candidateDriverId == null) {
            // Send rejection notification
            sendRejectionNotificationSafely(creatorEmail, "No active drivers available");
            throw new NoDriversAvailableException("Currently there are no active drivers available");
        }

        // Clear persistence context to avoid StaleObjectStateException when acquiring pessimistic lock
        entityManager.flush();
        entityManager.clear();

        // Fetch fresh managed entities after clear
        User creator = userRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Lock and assign driver
        DriverState lockedState = driverStateRepository.findByDriverIdForUpdate(candidateDriverId)
                .orElseThrow(() -> new NoDriversAvailableException("Driver became unavailable"));

        // Re-validate after lock (driver might have been taken by another concurrent request)
        if (!Boolean.TRUE.equals(lockedState.getOnline())) {
            sendRejectionNotificationSafely(creator.getEmail(), "Driver became unavailable");
            throw new NoDriversAvailableException("Driver became unavailable");
        }

        if (Boolean.TRUE.equals(lockedState.getBusy()) && request.getScheduledAt() == null) {
            // Double-check: if driver is busy and we need immediate ride, verify they're finishing soon
            if (lockedState.getCurrentRideEndsAt() == null || 
                lockedState.getCurrentRideEndsAt().isAfter(now.plusMinutes(10))) {
                sendRejectionNotificationSafely(creator.getEmail(), "Driver became unavailable");
                throw new NoDriversAvailableException("Driver became unavailable");
            }
        }

        Driver driver = lockedState.getDriver();

        // Build the Ride entity with fresh managed entities
        Ride ride = Ride.builder()
                .creator(creator)
                .driver(driver)
                .status(request.getScheduledAt() != null ? RideStatus.SCHEDULED : RideStatus.ACCEPTED)
                .vehicleType(request.getVehicleType())
                .babyTransport(Boolean.TRUE.equals(request.getBabyTransport()))
                .petTransport(Boolean.TRUE.equals(request.getPetTransport()))
                .scheduledAt(request.getScheduledAt())
                .estimatedPrice(estimatedPrice)
                .distanceKm(roundKm(distanceKm))
                .estimatedDurationMinutes(estimatedDurationMinutes)
                .build();

        // Add passengers: creator + linked passengers (if they exist)
        ride.getPassengers().add(creator);
        if (request.getPassengerEmails() != null) {
            for (String email : request.getPassengerEmails()) {
                if (email == null || email.isBlank()) {
                    continue;
                }
                userRepository.findByEmail(email).ifPresent(ride.getPassengers()::add);
            }
        }

        // Add stops: pickup + intermediate stops + dropoff
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

        // Update driver state
        if (request.getScheduledAt() != null) {
            lockedState.setNextScheduledRideAt(request.getScheduledAt());
        } else {
            lockedState.setBusy(true);
            lockedState.setCurrentRideEndsAt(now.plusMinutes(estimatedDurationMinutes));
            lockedState.setCurrentRideEndLatitude(request.getDropoff().getLatitude());
            lockedState.setCurrentRideEndLongitude(request.getDropoff().getLongitude());
        }

        // Persist
        Ride saved = rideRepository.save(ride);
        driverStateRepository.save(lockedState);

        // Create work log entry for immediate rides (not for scheduled rides - those get logged when started)
        // The work log is created with completed=false; it will be marked completed when the ride finishes
        if (request.getScheduledAt() == null) {
            driverWorkLogRepository.save(DriverWorkLog.builder()
                .driver(driver)
                .ride(saved)
                .startedAt(now)
                .endedAt(null)  // Will be set when ride completes
                .completed(false)  // Will be set to true when ride completes
                .build());
        }

        // Email notifications (non-blocking - failures are logged, not thrown)
        sendNotificationsSafely(driver.getEmail(), creator.getEmail(), saved, request.getPassengerEmails());

        return OrderRideResponse.builder()
                .rideId(saved.getId())
                .status(saved.getStatus().name())
                .message(saved.getStatus() == RideStatus.SCHEDULED ? "Ride scheduled successfully." : "Ride ordered successfully.")
                .estimatedPrice(saved.getEstimatedPrice())
                .scheduledAt(saved.getScheduledAt())
                .assignedDriverEmail(driver.getEmail())
                .build();
    }

    /**
     * Send all ride notifications in a non-blocking way (failures are logged, not thrown).
     */
    private void sendNotificationsSafely(String driverEmail, String creatorEmail, Ride saved, List<String> passengerEmails) {
        try {
            mailService.sendRideAssignedToDriver(driverEmail, saved.getId(), saved.getScheduledAt());
        } catch (Exception e) {
            log.warn("Failed to send ride assignment email to driver {}: {}", driverEmail, e.getMessage());
        }

        try {
            mailService.sendRideOrderAccepted(creatorEmail, saved.getId(), saved.getStatus().name());
        } catch (Exception e) {
            log.warn("Failed to send ride accepted email to creator {}: {}", creatorEmail, e.getMessage());
        }

        if (passengerEmails != null) {
            for (String email : passengerEmails) {
                if (email == null || email.isBlank()) {
                    continue;
                }
                try {
                    mailService.sendRideDetailsShared(email, saved.getId(), creatorEmail);
                } catch (Exception e) {
                    log.warn("Failed to send ride details email to passenger {}: {}", email, e.getMessage());
                }
            }
        }
    }

    /**
     * Send rejection notification safely (failures are logged, not thrown).
     */
    private void sendRejectionNotificationSafely(String email, String reason) {
        try {
            mailService.sendRideOrderRejected(email, reason);
        } catch (Exception e) {
            log.warn("Failed to send rejection email to {}: {}", email, e.getMessage());
        }
    }

    /**
     * Selects the best driver ID for the ride without loading entities into persistence context.
     * Returns null if no suitable driver is found.
     * 
     * Selection criteria (per spec 2.4.1):
     * - Driver must be online
     * - Driver must not have a scheduled ride blocking them
     * - Driver must not have exceeded 8 hours of work in last 24 hours
     * - Driver's vehicle type must match the request
     * - If baby/pet transport is requested, driver must support it
     * - Priority: free drivers closest to pickup, then busy drivers finishing within 10 minutes
     */
    private Long selectDriverIdForRide(OrderRideRequest request, LocalDateTime now) {
        List<DriverState> onlineDrivers = driverStateRepository.findAllOnlineDrivers();
        if (onlineDrivers.isEmpty()) {
            throw new NoActiveDriversException("Currently there are no active drivers");
        }

        String reqType = request.getVehicleType();
        boolean needsBaby = Boolean.TRUE.equals(request.getBabyTransport());
        boolean needsPet = Boolean.TRUE.equals(request.getPetTransport());

        // Filter eligible drivers
        List<DriverState> eligible = onlineDrivers.stream()
                .filter(ds -> ds.getDriver() != null)
                // Exclude drivers with already scheduled rides
                .filter(ds -> ds.getNextScheduledRideAt() == null)
                // Exclude drivers who exceeded 8-hour work limit
                .filter(ds -> !hasExceededWorkLimit(ds.getDriver().getId(), now))
                // Match vehicle type
                .filter(ds -> {
                    if (reqType == null) return true;
                    String driverType = ds.getDriver().getVehicleType();
                    return driverType != null && driverType.equalsIgnoreCase(reqType);
                })
                // Match baby transport requirement
                .filter(ds -> !needsBaby || Boolean.TRUE.equals(ds.getDriver().getBabyFriendly()))
                // Match pet transport requirement
                .filter(ds -> !needsPet || Boolean.TRUE.equals(ds.getDriver().getPetFriendly()))
                .toList();

        if (eligible.isEmpty()) {
            return null;
        }

        // Priority 1: Free drivers - select the closest to pickup
        List<DriverState> free = eligible.stream()
                .filter(ds -> !Boolean.TRUE.equals(ds.getBusy()))
                .toList();

        if (!free.isEmpty()) {
            return free.stream()
                    .min(Comparator.comparingDouble(ds -> distanceToPickup(ds, request.getPickup())))
                    .map(ds -> ds.getDriver().getId())
                    .orElse(null);
        }

        // Priority 2: Busy drivers finishing within 10 minutes - select closest to pickup from their end location
        LocalDateTime limit = now.plusMinutes(10);
        return eligible.stream()
                .filter(ds -> Boolean.TRUE.equals(ds.getBusy()))
                .filter(ds -> ds.getCurrentRideEndsAt() != null && !ds.getCurrentRideEndsAt().isAfter(limit))
                .min(Comparator.comparingDouble(ds -> distanceToPickupFromEnd(ds, request.getPickup())))
                .map(ds -> ds.getDriver().getId())
                .orElse(null);
    }

    /**
     * Checks if a driver has exceeded the 8-hour work limit in the last 24 hours.
     * Only completed rides count towards this limit (cancelled/pending rides don't count).
     */
    private boolean hasExceededWorkLimit(Long driverId, LocalDateTime now) {
        LocalDateTime since = now.minusHours(24);
        long workedMinutes = driverWorkLogRepository.findCompletedSince(driverId, since).stream()
                .filter(w -> w.getEndedAt() != null)  // Safety check
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

    @Override
    @Transactional
    public void startRide(Long rideId, String driverEmail) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found"));

        if (ride.getDriver() == null || !ride.getDriver().getEmail().equals(driverEmail)) {
            throw new IllegalArgumentException("You are not the assigned driver for this ride");
        }

        if (ride.getStatus() != RideStatus.ACCEPTED && ride.getStatus() != RideStatus.SCHEDULED) {
            throw new IllegalArgumentException("Ride cannot be started in current status: " + ride.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        ride.setStatus(RideStatus.IN_PROGRESS);
        ride.setStartedAt(now);
        rideRepository.save(ride);

        // Create or update work log with actual start time
        DriverWorkLog workLog = driverWorkLogRepository.findByRide(ride)
                .orElseGet(() -> DriverWorkLog.builder()
                        .driver(ride.getDriver())
                        .ride(ride)
                        .completed(false)
                        .build());

        workLog.setStartedAt(now);
        driverWorkLogRepository.save(workLog);

        log.info("Ride {} started by driver {}", rideId, driverEmail);
    }

    @Override
    @Transactional
    public void completeRide(Long rideId, String driverEmail) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found"));

        if (ride.getDriver() == null || !ride.getDriver().getEmail().equals(driverEmail)) {
            throw new IllegalArgumentException("You are not the assigned driver for this ride");
        }

        if (ride.getStatus() != RideStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Only in-progress rides can be completed");
        }

        LocalDateTime now = LocalDateTime.now();
        ride.setStatus(RideStatus.COMPLETED);
        ride.setCompletedAt(now);
        rideRepository.save(ride);

        // Update driver state - no longer busy
        DriverState driverState = driverStateRepository.findById(ride.getDriver().getId())
                .orElse(null);
        if (driverState != null) {
            driverState.setBusy(false);
            driverState.setCurrentRideEndsAt(null);
            driverState.setCurrentRideEndLatitude(null);
            driverState.setCurrentRideEndLongitude(null);
            driverStateRepository.save(driverState);
        }

        // Mark work log as completed with actual end time
        DriverWorkLog workLog = driverWorkLogRepository.findByRide(ride)
                .orElseGet(() -> DriverWorkLog.builder()
                        .driver(ride.getDriver())
                        .ride(ride)
                        .startedAt(ride.getStartedAt() != null ? ride.getStartedAt() : now)
                        .build());

        workLog.setEndedAt(now);
        workLog.setCompleted(true);  // This ride now counts towards the 8-hour limit
        driverWorkLogRepository.save(workLog);

        log.info("Ride {} completed by driver {}. Work logged: {} to {}", 
                rideId, driverEmail, workLog.getStartedAt(), workLog.getEndedAt());
    }

    @Override
    @Transactional
    public void cancelRide(Long rideId, String userEmail, String reason) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found"));

        // Check if user is allowed to cancel (creator, passenger, or driver)
        boolean isCreator = ride.getCreator().getEmail().equals(userEmail);
        boolean isDriver = ride.getDriver() != null && ride.getDriver().getEmail().equals(userEmail);
        boolean isPassenger = ride.getPassengers().stream()
                .anyMatch(p -> p.getEmail().equals(userEmail));

        if (!isCreator && !isDriver && !isPassenger) {
            throw new IllegalArgumentException("You are not authorized to cancel this ride");
        }

        if (ride.getStatus() == RideStatus.COMPLETED || ride.getStatus() == RideStatus.CANCELLED) {
            throw new IllegalArgumentException("Ride cannot be cancelled in current status: " + ride.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();

        // Validation for PASSENGER cancellations
        if (!isDriver) {
            // For scheduled rides, must be at least 10 minutes before scheduled time
            if (ride.getScheduledAt() != null) {
                if (ride.getScheduledAt().minusMinutes(10).isBefore(now)) {
                    throw new IllegalArgumentException("Cannot cancel scheduled ride less than 10 minutes before start time");
                }
            } else {
                // For immediate rides, cannot cancel if already in progress
                if (ride.getStatus() == RideStatus.IN_PROGRESS) {
                    throw new IllegalArgumentException("Cannot cancel ride that is already in progress");
                }
            }
        }

        // Validation for DRIVER cancellations
        if (isDriver) {
            // Driver cannot cancel once passengers have entered the vehicle (ride is IN_PROGRESS)
            if (ride.getStatus() == RideStatus.IN_PROGRESS) {
                throw new IllegalArgumentException("Cannot cancel ride that is already in progress. Passengers are already in the vehicle.");
            }
        }

        // Set cancellation details
        ride.setStatus(RideStatus.CANCELLED);
        ride.setCancellationReason(reason);
        ride.setCancelledBy(isDriver ? "DRIVER" : "PASSENGER");
        ride.setCancelledAt(now);
        rideRepository.save(ride);

        // Update driver state - no longer busy
        if (ride.getDriver() != null) {
            DriverState driverState = driverStateRepository.findById(ride.getDriver().getId())
                    .orElse(null);
            if (driverState != null) {
                driverState.setBusy(false);
                driverState.setCurrentRideEndsAt(null);
                driverState.setCurrentRideEndLatitude(null);
                driverState.setCurrentRideEndLongitude(null);
                driverState.setNextScheduledRideAt(null);
                driverStateRepository.save(driverState);
            }

            // Delete or mark work log as not completed (won't count towards 8-hour limit)
            driverWorkLogRepository.findByRide(ride).ifPresent(workLog -> {
                workLog.setCompleted(false);
                workLog.setEndedAt(LocalDateTime.now());
                driverWorkLogRepository.save(workLog);
            });
        }

        log.info("Ride {} cancelled by {}. Reason: {}", rideId, userEmail, reason);

        // Send cancellation notifications
        try {
            if (ride.getDriver() != null && !isDriver) {
                mailService.sendRideOrderRejected(ride.getDriver().getEmail(), 
                        "Ride cancelled by " + (isCreator ? "passenger" : "user") + ": " + reason);
            }
            if (!isCreator) {
                mailService.sendRideOrderRejected(ride.getCreator().getEmail(), 
                        "Ride cancelled" + (isDriver ? " by driver" : "") + ": " + reason);
            }
        } catch (Exception e) {
            log.warn("Failed to send cancellation notification: {}", e.getMessage());
        }
    }
}
