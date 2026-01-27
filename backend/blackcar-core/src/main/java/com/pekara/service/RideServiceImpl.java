package com.pekara.service;

import com.pekara.constant.RideStatus;
import com.pekara.dto.common.LocationPointDto;
import com.pekara.dto.request.EstimateRideRequest;
import com.pekara.dto.request.OrderRideRequest;
import com.pekara.dto.response.ActiveRideResponse;
import com.pekara.dto.response.OrderRideResponse;
import com.pekara.dto.response.RideEstimateResponse;
import com.pekara.exception.ActiveRideConflictException;
import com.pekara.exception.InvalidScheduleTimeException;
import com.pekara.exception.NoDriversAvailableException;
import com.pekara.model.Driver;
import com.pekara.model.DriverState;
import com.pekara.model.Ride;
import com.pekara.model.RideStop;
import com.pekara.model.User;
import com.pekara.repository.DriverStateRepository;
import com.pekara.repository.RideRepository;
import com.pekara.repository.UserRepository;
import com.pekara.util.GeoUtils;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RideServiceImpl implements RideService {

    private final UserRepository userRepository;
    private final RideRepository rideRepository;
    private final DriverStateRepository driverStateRepository;
    private final EntityManager entityManager;

    private final RideEstimationService rideEstimationService;
    private final DriverMatchingService driverMatchingService;
    private final DriverStateManagementService driverStateManagementService;
    private final RideWorkLogService rideWorkLogService;
    private final RideNotificationService rideNotificationService;
    private final RoutingService routingService;

    @Override
    @Transactional(readOnly = true)
    public RideEstimateResponse estimateRide(EstimateRideRequest request) {
        RideEstimationService.RouteData route = rideEstimationService.calculateRouteWithStops(
                request.getPickup(), request.getDropoff(), request.getStops());

        BigDecimal estimatedPrice = rideEstimationService.calculatePrice(request.getVehicleType(), route.getDistanceKm());

        return RideEstimateResponse.builder()
                .estimatedPrice(estimatedPrice)
                .estimatedDurationMinutes(route.getDurationMinutes())
                .distanceKm(rideEstimationService.roundKm(route.getDistanceKm()))
                .vehicleType(request.getVehicleType())
                .routePoints(route.getRoutePoints())
                .build();
    }

    @Override
    @Transactional
    public OrderRideResponse orderRide(String creatorEmail, OrderRideRequest request) {
        LocalDateTime now = LocalDateTime.now();

        User creator = userRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        entityManager.flush();

        validateNoActiveRides(creator, creatorEmail);
        validateScheduleTime(request.getScheduledAt(), now);

        RideEstimationService.RouteData routeData = rideEstimationService.calculateRouteWithStops(
                request.getPickup(), request.getDropoff(), request.getStops());

        double distanceKm = routeData.getDistanceKm();
        int estimatedDurationMinutes = routeData.getDurationMinutes();
        BigDecimal estimatedPrice = rideEstimationService.calculatePrice(request.getVehicleType(), distanceKm);
        String routeCoordinates = rideEstimationService.serializeRouteCoordinates(routeData.getRoutePoints());

        Long candidateDriverId = driverMatchingService.selectDriverIdForRide(request, now);

        if (candidateDriverId == null) {
            rideNotificationService.sendRejectionNotification(creatorEmail, "No active drivers available");
            throw new NoDriversAvailableException("Currently there are no active drivers available");
        }

        entityManager.flush();
        entityManager.clear();

        creator = userRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        DriverState lockedState = driverStateRepository.findByDriverIdForUpdate(candidateDriverId)
                .orElseThrow(() -> new NoDriversAvailableException("Driver became unavailable"));

        validateDriverAvailability(lockedState, creator.getEmail(), request.getScheduledAt(), now);

        Driver driver = lockedState.getDriver();

        Ride ride = buildRide(creator, driver, request, estimatedPrice, distanceKm, estimatedDurationMinutes, routeCoordinates);

        addPassengersToRide(ride, creator, request.getPassengerEmails());
        addStopsToRide(ride, request);

        if (request.getScheduledAt() != null) {
            driverStateManagementService.setNextScheduledRide(candidateDriverId, request.getScheduledAt());
        } else {
            driverStateManagementService.markDriverBusy(candidateDriverId, estimatedDurationMinutes,
                    request.getDropoff().getLatitude(), request.getDropoff().getLongitude());
        }

        Ride saved = rideRepository.save(ride);

        if (request.getScheduledAt() == null) {
            rideWorkLogService.createWorkLogForRide(saved.getId(), driver.getId(), now);
        }

        rideNotificationService.sendRideOrderNotifications(driver.getEmail(), creator.getEmail(),
                saved.getId(), saved.getStatus().name(), saved.getScheduledAt(), request.getPassengerEmails());

        return OrderRideResponse.builder()
                .rideId(saved.getId())
                .status(saved.getStatus().name())
                .message(saved.getStatus() == RideStatus.SCHEDULED ? "Ride scheduled successfully." : "Ride ordered successfully.")
                .estimatedPrice(saved.getEstimatedPrice())
                .scheduledAt(saved.getScheduledAt())
                .assignedDriverEmail(driver.getEmail())
                .build();
    }

    private void validateNoActiveRides(User creator, String creatorEmail) {
        List<RideStatus> activeStatuses = List.of(RideStatus.ACCEPTED, RideStatus.SCHEDULED, RideStatus.IN_PROGRESS);
        List<Ride> activeRides = rideRepository.findPassengerActiveRides(creator.getId(), activeStatuses);

        if (!activeRides.isEmpty()) {
            Ride conflictingRide = activeRides.get(0);
            log.warn("User {} has {} active ride(s). Ride IDs: {}, Statuses: {}",
                    creatorEmail,
                    activeRides.size(),
                    activeRides.stream().map(Ride::getId).toList(),
                    activeRides.stream().map(r -> r.getStatus().name()).toList());

            String errorMessage = String.format(
                    "You cannot order a new ride while you have an active ride (ID: %d, Status: %s). Please complete or cancel your current ride first.",
                    conflictingRide.getId(),
                    conflictingRide.getStatus().name()
            );
            throw new ActiveRideConflictException(errorMessage);
        }
    }

    private void validateScheduleTime(LocalDateTime scheduledAt, LocalDateTime now) {
        if (scheduledAt != null) {
            if (scheduledAt.isBefore(now)) {
                throw new InvalidScheduleTimeException("Scheduled time must be in the future");
            }
            if (scheduledAt.isAfter(now.plusHours(5))) {
                throw new InvalidScheduleTimeException("Ride can be scheduled at most 5 hours in advance");
            }
        }
    }

    private void validateDriverAvailability(DriverState lockedState, String creatorEmail, LocalDateTime scheduledAt, LocalDateTime now) {
        if (!Boolean.TRUE.equals(lockedState.getOnline())) {
            rideNotificationService.sendRejectionNotification(creatorEmail, "Driver became unavailable");
            throw new NoDriversAvailableException("Driver became unavailable");
        }

        if (Boolean.TRUE.equals(lockedState.getBusy()) && scheduledAt == null) {
            if (lockedState.getCurrentRideEndsAt() == null ||
                    lockedState.getCurrentRideEndsAt().isAfter(now.plusMinutes(10))) {
                rideNotificationService.sendRejectionNotification(creatorEmail, "Driver became unavailable");
                throw new NoDriversAvailableException("Driver became unavailable");
            }
        }
    }

    private Ride buildRide(User creator, Driver driver, OrderRideRequest request, BigDecimal estimatedPrice, double distanceKm, int estimatedDurationMinutes, String routeCoordinates) {
        return Ride.builder()
                .creator(creator)
                .driver(driver)
                .status(request.getScheduledAt() != null ? RideStatus.SCHEDULED : RideStatus.ACCEPTED)
                .vehicleType(request.getVehicleType())
                .babyTransport(Boolean.TRUE.equals(request.getBabyTransport()))
                .petTransport(Boolean.TRUE.equals(request.getPetTransport()))
                .scheduledAt(request.getScheduledAt())
                .estimatedPrice(estimatedPrice)
                .distanceKm(rideEstimationService.roundKm(distanceKm))
                .estimatedDurationMinutes(estimatedDurationMinutes)
                .routeCoordinates(routeCoordinates)
                .build();
    }

    private void addPassengersToRide(Ride ride, User creator, List<String> passengerEmails) {
        ride.getPassengers().add(creator);
        if (passengerEmails != null) {
            for (String email : passengerEmails) {
                if (email == null || email.isBlank()) {
                    continue;
                }
                userRepository.findByEmail(email).ifPresent(ride.getPassengers()::add);
            }
        }
    }

    private void addStopsToRide(Ride ride, OrderRideRequest request) {
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
    }

    @Override
    @Transactional
    public void startRide(Long rideId, String driverEmail) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found"));

        validateDriver(ride, driverEmail);
        validateRideStatusIn(ride,
                List.of(RideStatus.ACCEPTED, RideStatus.SCHEDULED),
                "Ride cannot be started in current status: " + ride.getStatus());

        LocalDateTime now = LocalDateTime.now();
        ride.setStatus(RideStatus.IN_PROGRESS);
        ride.setStartedAt(now);
        rideRepository.save(ride);

        rideWorkLogService.startWorkLog(rideId, now);

        log.info("Ride {} started by driver {}", rideId, driverEmail);
    }

    @Override
    @Transactional
    public void completeRide(Long rideId, String driverEmail) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found"));

        validateDriver(ride, driverEmail);
        validateRideStatus(ride, RideStatus.IN_PROGRESS, "Only in-progress rides can be completed");

        LocalDateTime now = LocalDateTime.now();
        ride.setStatus(RideStatus.COMPLETED);
        ride.setCompletedAt(now);
        rideRepository.save(ride);

        driverStateManagementService.releaseDriverAfterRide(ride.getDriver().getId());
        rideWorkLogService.completeWorkLog(rideId, now);

        log.info("Ride {} completed by driver {}", rideId, driverEmail);
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

        if (!isDriver) {
            if (ride.getScheduledAt() != null) {
                if (ride.getScheduledAt().minusMinutes(10).isBefore(now)) {
                    throw new IllegalArgumentException("Cannot cancel scheduled ride less than 10 minutes before start time");
                }
            } else {
                if (ride.getStatus() == RideStatus.IN_PROGRESS) {
                    throw new IllegalArgumentException("Cannot cancel ride that is already in progress");
                }
            }
        }

        if (isDriver) {
            if (ride.getStatus() == RideStatus.IN_PROGRESS) {
                throw new IllegalArgumentException("Cannot cancel ride that is already in progress. Passengers are already in the vehicle.");
            }
        }

        ride.setStatus(RideStatus.CANCELLED);
        ride.setCancellationReason(reason);
        ride.setCancelledBy(isDriver ? "DRIVER" : "PASSENGER");
        ride.setCancelledAt(now);
        rideRepository.save(ride);

        if (ride.getDriver() != null) {
            driverStateManagementService.releaseDriverAndClearSchedule(ride.getDriver().getId());
            rideWorkLogService.cancelWorkLog(rideId, now);
        }

        log.info("Ride {} cancelled by {}. Reason: {}", rideId, userEmail, reason);

        rideNotificationService.sendCancellationNotifications(
                ride.getDriver() != null ? ride.getDriver().getEmail() : null,
                ride.getCreator().getEmail(),
                isDriver, isCreator, reason);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ActiveRideResponse> getActiveRideForDriver(String driverEmail) {
        User driver = userRepository.findByEmail(driverEmail)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found"));

        List<RideStatus> activeStatuses = List.of(RideStatus.ACCEPTED, RideStatus.SCHEDULED, RideStatus.IN_PROGRESS, RideStatus.STOP_REQUESTED);
        List<Ride> activeRides = rideRepository.findDriverActiveRides(driver.getId(), activeStatuses);

        if (activeRides.isEmpty()) {
            return Optional.empty();
        }

        Ride ride = activeRides.get(0);
        return Optional.of(mapToActiveRideResponse(ride));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ActiveRideResponse> getActiveRideForPassenger(String passengerEmail) {
        User passenger = userRepository.findByEmail(passengerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Passenger not found"));

        List<RideStatus> activeStatuses = List.of(RideStatus.ACCEPTED, RideStatus.SCHEDULED, RideStatus.IN_PROGRESS, RideStatus.STOP_REQUESTED);
        List<Ride> activeRides = rideRepository.findPassengerActiveRides(passenger.getId(), activeStatuses);

        if (activeRides.isEmpty()) {
            return Optional.empty();
        }

        Ride ride = activeRides.get(0);
        return Optional.of(mapToActiveRideResponse(ride));
    }

    private ActiveRideResponse mapToActiveRideResponse(Ride ride) {
        List<RideStop> stops = ride.getStops();
        LocationPointDto pickup = null;
        LocationPointDto dropoff = null;
        List<LocationPointDto> intermediateStops = new ArrayList<>();

        if (!stops.isEmpty()) {
            RideStop firstStop = stops.get(0);
            pickup = LocationPointDto.builder()
                    .address(firstStop.getAddress())
                    .latitude(firstStop.getLatitude())
                    .longitude(firstStop.getLongitude())
                    .build();

            RideStop lastStop = stops.get(stops.size() - 1);
            dropoff = LocationPointDto.builder()
                    .address(lastStop.getAddress())
                    .latitude(lastStop.getLatitude())
                    .longitude(lastStop.getLongitude())
                    .build();

            if (stops.size() > 2) {
                intermediateStops = stops.subList(1, stops.size() - 1).stream()
                        .map(stop -> LocationPointDto.builder()
                                .address(stop.getAddress())
                                .latitude(stop.getLatitude())
                                .longitude(stop.getLongitude())
                                .build())
                        .collect(Collectors.toList());
            }
        }

        List<ActiveRideResponse.PassengerInfo> passengers = ride.getPassengers().stream()
                .map(p -> ActiveRideResponse.PassengerInfo.builder()
                        .id(p.getId())
                        .name(p.getFirstName() + " " + p.getLastName())
                        .email(p.getEmail())
                        .phoneNumber(p.getPhoneNumber())
                        .build())
                .collect(Collectors.toList());

        ActiveRideResponse.DriverInfo driverInfo = null;
        if (ride.getDriver() != null) {
            Driver driver = ride.getDriver();
            driverInfo = ActiveRideResponse.DriverInfo.builder()
                    .id(driver.getId())
                    .name(driver.getFirstName() + " " + driver.getLastName())
                    .email(driver.getEmail())
                    .phoneNumber(driver.getPhoneNumber())
                    .vehicleType(driver.getVehicleType())
                    .licensePlate(driver.getLicensePlate())
                    .build();
        }

        return ActiveRideResponse.builder()
                .rideId(ride.getId())
                .status(ride.getStatus())
                .vehicleType(ride.getVehicleType())
                .babyTransport(ride.getBabyTransport())
                .petTransport(ride.getPetTransport())
                .scheduledAt(ride.getScheduledAt())
                .estimatedPrice(ride.getEstimatedPrice())
                .distanceKm(ride.getDistanceKm())
                .estimatedDurationMinutes(ride.getEstimatedDurationMinutes())
                .startedAt(ride.getStartedAt())
                .routeCoordinates(ride.getRouteCoordinates())
                .pickup(pickup)
                .dropoff(dropoff)
                .stops(intermediateStops)
                .passengers(passengers)
                .driver(driverInfo)
                .build();
    }

    @Override
    @Transactional
    public void requestStopRide(Long rideId, String passengerEmail) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found"));

        if (ride.getStatus() != RideStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Only in-progress rides can be stopped");
        }

        boolean isPassenger = ride.getPassengers().stream()
                .anyMatch(p -> p.getEmail().equals(passengerEmail));

        if (!isPassenger) {
            throw new IllegalArgumentException("You are not a passenger on this ride");
        }

        ride.setStatus(RideStatus.STOP_REQUESTED);
        rideRepository.save(ride);

        log.info("Ride {} stop requested by passenger {}", rideId, passengerEmail);
    }

    @Override
    @Transactional
    public void stopRideEarly(Long rideId, String driverEmail, LocationPointDto actualStopLocation) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found"));

        validateDriver(ride, driverEmail);
        validateRideStatusIn(ride,
                List.of(RideStatus.IN_PROGRESS, RideStatus.STOP_REQUESTED),
                "Only in-progress or stop-requested rides can be stopped early");

        if (actualStopLocation == null || actualStopLocation.getAddress() == null || actualStopLocation.getAddress().isBlank()) {
            throw new IllegalArgumentException("Valid stop location is required");
        }

        LocalDateTime now = LocalDateTime.now();

        Double actualDistanceKm = routingService.calculateActualDistanceFromRoute(
                ride.getRouteCoordinates(),
                actualStopLocation
        );

        if (actualDistanceKm == null || actualDistanceKm == 0.0) {
            log.warn("Could not calculate actual distance from route, using fallback calculation");
            List<RideStop> stops = ride.getStops();
            if (!stops.isEmpty()) {
                RideStop pickup = stops.get(0);
                actualDistanceKm = GeoUtils.haversineKm(
                        pickup.getLatitude(), pickup.getLongitude(),
                        actualStopLocation.getLatitude(), actualStopLocation.getLongitude()
                );
            } else {
                actualDistanceKm = ride.getDistanceKm();
            }
        }

        BigDecimal actualPrice = rideEstimationService.calculatePrice(ride.getVehicleType(), actualDistanceKm);

        List<RideStop> stops = ride.getStops();
        if (!stops.isEmpty()) {
            RideStop lastStop = stops.get(stops.size() - 1);
            lastStop.setAddress(actualStopLocation.getAddress());
            lastStop.setLatitude(actualStopLocation.getLatitude());
            lastStop.setLongitude(actualStopLocation.getLongitude());
        }

        ride.setDistanceKm(rideEstimationService.roundKm(actualDistanceKm));
        ride.setEstimatedPrice(actualPrice);
        ride.setStatus(RideStatus.COMPLETED);
        ride.setCompletedAt(now);
        rideRepository.save(ride);

        driverStateManagementService.releaseDriverAfterRide(ride.getDriver().getId());
        rideWorkLogService.completeWorkLog(rideId, now);

        log.info("Ride {} stopped early by driver {} at new location. Actual distance: {} km, Actual price: {}",
                rideId, driverEmail, actualDistanceKm, actualPrice);
    }

    private void validateDriver(Ride ride, String driverEmail) {
        if (ride.getDriver() == null || !ride.getDriver().getEmail().equals(driverEmail)) {
            throw new IllegalArgumentException("You are not the assigned driver for this ride");
        }
    }

    private void validateRideStatus(Ride ride, RideStatus expectedStatus, String errorMessage) {
        if (ride.getStatus() != expectedStatus) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    /**
     * Validate that the ride is in one of the expected statuses.
     */
    private void validateRideStatusIn(Ride ride, List<RideStatus> expectedStatuses, String errorMessage) {
        if (!expectedStatuses.contains(ride.getStatus())) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.pekara.dto.response.DriverRideHistoryResponse> getDriverRideHistory(String driverEmail, LocalDateTime startDate, LocalDateTime endDate) {
        User driver = userRepository.findByEmail(driverEmail)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found"));

        List<Ride> rides = rideRepository.findDriverRideHistory(driver.getId(), startDate, endDate);

        return rides.stream()
                .map(this::mapToDriverRideHistoryResponse)
                .collect(Collectors.toList());
    }

    private com.pekara.dto.response.DriverRideHistoryResponse mapToDriverRideHistoryResponse(Ride ride) {
        List<RideStop> stops = ride.getStops();
        String pickupLocation = !stops.isEmpty() ? stops.get(0).getAddress() : null;
        String dropoffLocation = stops.size() > 1 ? stops.get(stops.size() - 1).getAddress() : null;

        List<com.pekara.dto.response.DriverRideHistoryResponse.PassengerInfo> passengers = ride.getPassengers().stream()
                .map(p -> com.pekara.dto.response.DriverRideHistoryResponse.PassengerInfo.builder()
                        .id(p.getId())
                        .firstName(p.getFirstName())
                        .lastName(p.getLastName())
                        .email(p.getEmail())
                        .build())
                .collect(Collectors.toList());

        return com.pekara.dto.response.DriverRideHistoryResponse.builder()
                .id(ride.getId())
                .startTime(ride.getStartedAt())
                .endTime(ride.getCompletedAt())
                .pickupLocation(pickupLocation)
                .dropoffLocation(dropoffLocation)
                .cancelled(ride.getStatus() == RideStatus.CANCELLED)
                .cancelledBy(ride.getCancelledBy())
                .price(ride.getEstimatedPrice())
                .panicActivated(false)
                .status(ride.getStatus().name())
                .passengers(passengers)
                .build();
    }
}
