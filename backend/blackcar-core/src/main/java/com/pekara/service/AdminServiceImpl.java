package com.pekara.service;

import com.pekara.constant.RideStatus;
import com.pekara.dto.common.LocationPointDto;
import com.pekara.dto.response.AdminRideDetailResponse;
import com.pekara.dto.response.AdminRideHistoryResponse;
import com.pekara.model.*;
import com.pekara.repository.InconsistencyReportRepository;
import com.pekara.repository.RideRatingRepository;
import com.pekara.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final RideRepository rideRepository;
    private final RideRatingRepository rideRatingRepository;
    private final InconsistencyReportRepository inconsistencyReportRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AdminRideHistoryResponse> getAllRidesHistory(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Fetching all rides history from {} to {}", startDate, endDate);
        
        List<Ride> rides = rideRepository.findAllRidesHistory(startDate, endDate);
        
        return rides.stream()
                .map(this::mapToHistoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminRideHistoryResponse> getActiveRides() {
        log.debug("Fetching all active rides for admin");
        List<RideStatus> activeStatuses = List.of(
                RideStatus.ACCEPTED,
                RideStatus.SCHEDULED,
                RideStatus.IN_PROGRESS,
                RideStatus.STOP_REQUESTED
        );
        List<Ride> activeRides = rideRepository.findAllActiveRides(activeStatuses);
        return activeRides.stream()
                .map(this::mapToHistoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminRideDetailResponse getRideDetail(Long rideId) {
        log.debug("Fetching ride detail for rideId: {}", rideId);
        
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found: " + rideId));
        
        List<RideRating> ratings = rideRatingRepository.findAllByRideId(rideId);
        List<InconsistencyReport> reports = inconsistencyReportRepository.findAllByRideId(rideId);
        
        return mapToDetailResponse(ride, ratings, reports);
    }

    private AdminRideHistoryResponse mapToHistoryResponse(Ride ride) {
        // Get pickup and dropoff from stops (first and last)
        List<RideStop> stops = ride.getStops();
        RideStop pickup = stops.isEmpty() ? null : stops.get(0);
        RideStop dropoff = stops.isEmpty() ? null : stops.get(stops.size() - 1);
        
        // Get intermediate stops (excluding first and last)
        List<LocationPointDto> intermediateStops = new ArrayList<>();
        if (stops.size() > 2) {
            for (int i = 1; i < stops.size() - 1; i++) {
                RideStop stop = stops.get(i);
                intermediateStops.add(LocationPointDto.builder()
                        .latitude(stop.getLatitude())
                        .longitude(stop.getLongitude())
                        .address(stop.getAddress())
                        .build());
            }
        }

        return AdminRideHistoryResponse.builder()
                .id(ride.getId())
                .status(ride.getStatus().name())
                // Dates
                .createdAt(ride.getCreatedAt())
                .scheduledAt(ride.getScheduledAt())
                .startedAt(ride.getStartedAt())
                .completedAt(ride.getCompletedAt())
                // Locations
                .pickupAddress(pickup != null ? pickup.getAddress() : null)
                .dropoffAddress(dropoff != null ? dropoff.getAddress() : null)
                .pickup(pickup != null ? LocationPointDto.builder()
                        .latitude(pickup.getLatitude())
                        .longitude(pickup.getLongitude())
                        .address(pickup.getAddress())
                        .build() : null)
                .dropoff(dropoff != null ? LocationPointDto.builder()
                        .latitude(dropoff.getLatitude())
                        .longitude(dropoff.getLongitude())
                        .address(dropoff.getAddress())
                        .build() : null)
                .stops(intermediateStops)
                // Cancellation
                .cancelled(ride.getCancelledBy() != null)
                .cancelledBy(ride.getCancelledBy())
                .cancellationReason(ride.getCancellationReason())
                .cancelledAt(ride.getCancelledAt())
                // Pricing
                .price(ride.getEstimatedPrice())
                .distanceKm(ride.getDistanceKm())
                .estimatedDurationMinutes(ride.getEstimatedDurationMinutes())
                // Panic
                .panicActivated(ride.getPanicActivated())
                .panickedBy(ride.getPanickedBy())
                // Vehicle
                .vehicleType(ride.getVehicleType())
                .babyTransport(ride.getBabyTransport())
                .petTransport(ride.getPetTransport())
                // Participants
                .driver(mapDriverBasicInfo(ride.getDriver()))
                .passengers(mapPassengersBasicInfo(ride.getPassengers()))
                .build();
    }

    private AdminRideDetailResponse mapToDetailResponse(Ride ride, List<RideRating> ratings, List<InconsistencyReport> reports) {
        // Get pickup and dropoff from stops (first and last)
        List<RideStop> stops = ride.getStops();
        RideStop pickup = stops.isEmpty() ? null : stops.get(0);
        RideStop dropoff = stops.isEmpty() ? null : stops.get(stops.size() - 1);
        
        // Get intermediate stops (excluding first and last)
        List<LocationPointDto> intermediateStops = new ArrayList<>();
        if (stops.size() > 2) {
            for (int i = 1; i < stops.size() - 1; i++) {
                RideStop stop = stops.get(i);
                intermediateStops.add(LocationPointDto.builder()
                        .latitude(stop.getLatitude())
                        .longitude(stop.getLongitude())
                        .address(stop.getAddress())
                        .build());
            }
        }

        return AdminRideDetailResponse.builder()
                .id(ride.getId())
                .status(ride.getStatus().name())
                // Dates
                .createdAt(ride.getCreatedAt())
                .scheduledAt(ride.getScheduledAt())
                .startedAt(ride.getStartedAt())
                .completedAt(ride.getCompletedAt())
                // Locations
                .pickupAddress(pickup != null ? pickup.getAddress() : null)
                .dropoffAddress(dropoff != null ? dropoff.getAddress() : null)
                .pickup(pickup != null ? LocationPointDto.builder()
                        .latitude(pickup.getLatitude())
                        .longitude(pickup.getLongitude())
                        .address(pickup.getAddress())
                        .build() : null)
                .dropoff(dropoff != null ? LocationPointDto.builder()
                        .latitude(dropoff.getLatitude())
                        .longitude(dropoff.getLongitude())
                        .address(dropoff.getAddress())
                        .build() : null)
                .stops(intermediateStops)
                // Route for map
                .routeCoordinates(ride.getRouteCoordinates())
                // Cancellation
                .cancelled(ride.getCancelledBy() != null)
                .cancelledBy(ride.getCancelledBy())
                .cancellationReason(ride.getCancellationReason())
                .cancelledAt(ride.getCancelledAt())
                // Pricing
                .price(ride.getEstimatedPrice())
                .distanceKm(ride.getDistanceKm())
                .estimatedDurationMinutes(ride.getEstimatedDurationMinutes())
                // Panic
                .panicActivated(ride.getPanicActivated())
                .panickedBy(ride.getPanickedBy())
                // Vehicle
                .vehicleType(ride.getVehicleType())
                .babyTransport(ride.getBabyTransport())
                .petTransport(ride.getPetTransport())
                // Driver details
                .driver(mapDriverDetailInfo(ride.getDriver()))
                // Passenger details
                .passengers(mapPassengersDetailInfo(ride.getPassengers()))
                // Ratings
                .ratings(mapRatings(ratings))
                // Inconsistency reports
                .inconsistencyReports(mapInconsistencyReports(reports))
                .build();
    }

    private AdminRideHistoryResponse.DriverBasicInfo mapDriverBasicInfo(Driver driver) {
        if (driver == null) return null;
        
        return AdminRideHistoryResponse.DriverBasicInfo.builder()
                .id(driver.getId())
                .firstName(driver.getFirstName())
                .lastName(driver.getLastName())
                .email(driver.getEmail())
                .phoneNumber(driver.getPhoneNumber())
                .build();
    }

    private List<AdminRideHistoryResponse.PassengerBasicInfo> mapPassengersBasicInfo(java.util.Set<User> passengers) {
        if (passengers == null || passengers.isEmpty()) return new ArrayList<>();
        
        return passengers.stream()
                .map(p -> AdminRideHistoryResponse.PassengerBasicInfo.builder()
                        .id(p.getId())
                        .firstName(p.getFirstName())
                        .lastName(p.getLastName())
                        .email(p.getEmail())
                        .build())
                .collect(Collectors.toList());
    }

    private AdminRideDetailResponse.DriverDetailInfo mapDriverDetailInfo(Driver driver) {
        if (driver == null) return null;
        
        return AdminRideDetailResponse.DriverDetailInfo.builder()
                .id(driver.getId())
                .firstName(driver.getFirstName())
                .lastName(driver.getLastName())
                .email(driver.getEmail())
                .phoneNumber(driver.getPhoneNumber())
                .profilePicture(driver.getProfilePicture())
                .licenseNumber(driver.getLicenseNumber())
                .vehicleModel(driver.getVehicleModel())
                .licensePlate(driver.getLicensePlate())
                .averageRating(driver.getAverageRating())
                .totalRides(driver.getTotalRides())
                .build();
    }

    private List<AdminRideDetailResponse.PassengerDetailInfo> mapPassengersDetailInfo(java.util.Set<User> passengers) {
        if (passengers == null || passengers.isEmpty()) return new ArrayList<>();
        
        return passengers.stream()
                .map(p -> AdminRideDetailResponse.PassengerDetailInfo.builder()
                        .id(p.getId())
                        .firstName(p.getFirstName())
                        .lastName(p.getLastName())
                        .email(p.getEmail())
                        .phoneNumber(p.getPhoneNumber())
                        .profilePicture(p.getProfilePicture())
                        .totalRides(p.getTotalRides())
                        .averageRating(p.getAverageRating())
                        .build())
                .collect(Collectors.toList());
    }

    private List<AdminRideDetailResponse.RideRatingInfo> mapRatings(List<RideRating> ratings) {
        if (ratings == null || ratings.isEmpty()) return new ArrayList<>();
        
        return ratings.stream()
                .map(r -> AdminRideDetailResponse.RideRatingInfo.builder()
                        .id(r.getId())
                        .passengerId(r.getPassenger().getId())
                        .passengerName(r.getPassenger().getFirstName() + " " + r.getPassenger().getLastName())
                        .vehicleRating(r.getVehicleRating())
                        .driverRating(r.getDriverRating())
                        .comment(r.getComment())
                        .ratedAt(r.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private List<AdminRideDetailResponse.InconsistencyReportInfo> mapInconsistencyReports(List<InconsistencyReport> reports) {
        if (reports == null || reports.isEmpty()) return new ArrayList<>();
        
        return reports.stream()
                .map(r -> AdminRideDetailResponse.InconsistencyReportInfo.builder()
                        .id(r.getId())
                        .reportedByUserId(r.getReportedBy().getId())
                        .reportedByName(r.getReportedBy().getFirstName() + " " + r.getReportedBy().getLastName())
                        .description(r.getDescription())
                        .reportedAt(r.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}
