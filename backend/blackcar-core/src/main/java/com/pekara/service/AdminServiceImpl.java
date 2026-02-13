package com.pekara.service;

import com.pekara.constant.RideStatsScope;
import com.pekara.constant.RideStatus;
import com.pekara.dto.common.LocationPointDto;
import com.pekara.dto.response.AdminRideDetailResponse;
import com.pekara.dto.response.AdminRideHistoryResponse;
import com.pekara.dto.response.DriverBasicDto;
import com.pekara.dto.response.PassengerBasicDto;
import com.pekara.dto.response.RideStatsDayDto;
import com.pekara.dto.response.RideStatsResponse;
import com.pekara.model.Driver;
import com.pekara.model.InconsistencyReport;
import com.pekara.model.Ride;
import com.pekara.model.RideRating;
import com.pekara.model.RideStop;
import com.pekara.model.User;
import com.pekara.model.UserRole;
import com.pekara.repository.DriverRepository;
import com.pekara.repository.InconsistencyReportRepository;
import com.pekara.repository.RideRatingRepository;
import com.pekara.repository.RideRepository;
import com.pekara.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final RideRepository rideRepository;
    private final RideRatingRepository rideRatingRepository;
    private final InconsistencyReportRepository inconsistencyReportRepository;
    private final DriverRepository driverRepository;
    private final UserRepository userRepository;

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
    public AdminRideDetailResponse getRideDetail(Long rideId) {
        log.debug("Fetching ride detail for rideId: {}", rideId);

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found: " + rideId));

        List<RideRating> ratings = rideRatingRepository.findAllByRideId(rideId);
        List<InconsistencyReport> reports = inconsistencyReportRepository.findAllByRideId(rideId);

        return mapToDetailResponse(ride, ratings, reports);
    }

    @Override
    @Transactional(readOnly = true)
    public RideStatsResponse getRideStatsAdmin(LocalDateTime startDate, LocalDateTime endDate, RideStatsScope scope, Long userId) {
        log.debug("Admin ride stats requested: scope={}, userId={}, range {} to {}", scope, userId, startDate, endDate);

        List<Ride> rides = rideRepository.findAllRidesHistory(startDate, endDate);
        List<Ride> filteredRides = filterRidesByScope(rides, scope, userId);
        return buildRideStatsResponse(filteredRides, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DriverBasicDto> listDriversForAdmin() {
        return driverRepository.findAll().stream()
                .map(d -> DriverBasicDto.builder()
                        .id(d.getId())
                        .firstName(d.getFirstName())
                        .lastName(d.getLastName())
                        .email(d.getEmail())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PassengerBasicDto> listPassengersForAdmin() {
        return userRepository.findByRole(UserRole.PASSENGER).stream()
                .map(u -> PassengerBasicDto.builder()
                        .id(u.getId())
                        .firstName(u.getFirstName())
                        .lastName(u.getLastName())
                        .email(u.getEmail())
                        .build())
                .collect(Collectors.toList());
    }

    private List<Ride> filterRidesByScope(List<Ride> rides, RideStatsScope scope, Long userId) {
        List<Ride> completed = rides.stream()
                .filter(r -> r.getStatus() == RideStatus.COMPLETED)
                .collect(Collectors.toList());

        return switch (scope) {
            case ALL_DRIVERS -> completed.stream()
                    .filter(r -> r.getDriver() != null)
                    .collect(Collectors.toList());
            case ALL_PASSENGERS -> completed.stream()
                    .filter(r -> r.getPassengers() != null && !r.getPassengers().isEmpty())
                    .collect(Collectors.toList());
            case DRIVER -> {
                if (userId == null) throw new IllegalArgumentException("userId required for DRIVER scope");
                yield completed.stream()
                        .filter(r -> r.getDriver() != null && r.getDriver().getId().equals(userId))
                        .collect(Collectors.toList());
            }
            case PASSENGER -> {
                if (userId == null) throw new IllegalArgumentException("userId required for PASSENGER scope");
                yield completed.stream()
                        .filter(r -> r.getPassengers().stream().anyMatch(p -> p.getId().equals(userId)))
                        .collect(Collectors.toList());
            }
        };
    }

    private RideStatsResponse buildRideStatsResponse(List<Ride> rides, LocalDateTime startDate, LocalDateTime endDate) {
        LocalDate start = startDate.toLocalDate();
        LocalDate end = endDate.toLocalDate();
        long daysInRange = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;

        Map<LocalDate, List<Ride>> ridesByDate = rides.stream()
                .collect(Collectors.groupingBy(r -> {
                    LocalDateTime dt = r.getCompletedAt() != null ? r.getCompletedAt()
                            : (r.getStartedAt() != null ? r.getStartedAt() : r.getCreatedAt());
                    return dt.toLocalDate();
                }));

        List<RideStatsDayDto> dailyData = new ArrayList<>();
        long totalRides = 0;
        double totalDistanceKm = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            List<Ride> dayRides = ridesByDate.getOrDefault(date, List.of());
            long count = dayRides.size();
            double distance = dayRides.stream()
                    .mapToDouble(r -> r.getDistanceKm() != null ? r.getDistanceKm() : 0.0)
                    .sum();
            BigDecimal amount = dayRides.stream()
                    .map(r -> r.getEstimatedPrice() != null ? r.getEstimatedPrice() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            dailyData.add(RideStatsDayDto.builder()
                    .date(date)
                    .rideCount(count)
                    .distanceKm(distance)
                    .amount(amount)
                    .build());

            totalRides += count;
            totalDistanceKm += distance;
            totalAmount = totalAmount.add(amount);
        }

        double avgRidesPerDay = daysInRange > 0 ? (double) totalRides / daysInRange : 0;
        double avgDistancePerDay = daysInRange > 0 ? totalDistanceKm / daysInRange : 0;
        BigDecimal avgAmountPerDay = daysInRange > 0 ? totalAmount.divide(BigDecimal.valueOf(daysInRange), 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;

        return RideStatsResponse.builder()
                .dailyData(dailyData)
                .totalRides(totalRides)
                .totalDistanceKm(totalDistanceKm)
                .totalAmount(totalAmount)
                .avgRidesPerDay(avgRidesPerDay)
                .avgDistancePerDay(avgDistancePerDay)
                .avgAmountPerDay(avgAmountPerDay)
                .build();
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
