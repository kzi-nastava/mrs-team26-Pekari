package com.pekara.mapper;

import com.pekara.dto.common.LocationPointDto;
import com.pekara.dto.common.RideLocationDto;
import com.pekara.dto.common.WebLocationPoint;
import com.pekara.dto.request.EstimateRideRequest;
import com.pekara.dto.request.InconsistencyReportRequest;
import com.pekara.dto.request.OrderRideRequest;
import com.pekara.dto.request.RideLocationUpdateRequest;
import com.pekara.dto.request.RideRatingRequest;
import com.pekara.dto.request.StopRideEarlyRequest;
import com.pekara.dto.request.WebEstimateRideRequest;
import com.pekara.dto.request.WebInconsistencyReportRequest;
import com.pekara.dto.request.WebOrderRideRequest;
import com.pekara.dto.request.WebRideLocationUpdateRequest;
import com.pekara.dto.request.WebRideRatingRequest;
import com.pekara.dto.request.WebStopRideEarlyRequest;
import com.pekara.dto.response.ActiveRideResponse;
import com.pekara.dto.response.AdminRideDetailResponse;
import com.pekara.dto.response.AdminRideHistoryResponse;
import com.pekara.dto.response.DriverRideHistoryResponse;
import com.pekara.dto.response.PassengerRideDetailResponse;
import com.pekara.dto.response.PassengerRideHistoryResponse;
import com.pekara.dto.response.RideTrackingResponse;
import com.pekara.dto.response.WebActiveRideResponse;
import com.pekara.dto.response.WebAdminRideDetailResponse;
import com.pekara.dto.response.WebAdminRideHistoryResponse;
import com.pekara.dto.response.WebDriverRideHistoryResponse;
import com.pekara.dto.response.WebPassengerRideDetailResponse;
import com.pekara.dto.response.WebPassengerRideHistoryResponse;
import com.pekara.dto.response.WebRideTrackingResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RideMapper {

    public EstimateRideRequest toServiceEstimateRideRequest(WebEstimateRideRequest web) {
        return EstimateRideRequest.builder()
                .pickup(toLocation(web.getPickup()))
                .stops(web.getStops() == null ? null : web.getStops().stream().map(this::toLocation).toList())
                .dropoff(toLocation(web.getDropoff()))
                .vehicleType(web.getVehicleType())
                .build();
    }

    public OrderRideRequest toServiceOrderRideRequest(WebOrderRideRequest web) {
        return OrderRideRequest.builder()
                .pickup(toLocation(web.getPickup()))
                .stops(web.getStops() == null ? null : web.getStops().stream().map(this::toLocation).toList())
                .dropoff(toLocation(web.getDropoff()))
                .passengerEmails(web.getPassengerEmails())
                .vehicleType(web.getVehicleType())
                .babyTransport(web.getBabyTransport())
                .petTransport(web.getPetTransport())
                .scheduledAt(web.getScheduledAt())
                .build();
    }

    public RideLocationUpdateRequest toServiceRideLocationUpdateRequest(WebRideLocationUpdateRequest web) {
        return RideLocationUpdateRequest.builder()
                .latitude(web.getLatitude())
                .longitude(web.getLongitude())
                .heading(web.getHeading())
                .speed(web.getSpeed())
                .recordedAt(web.getRecordedAt())
                .build();
    }

    public StopRideEarlyRequest toServiceStopRideEarlyRequest(WebStopRideEarlyRequest web) {
        if (web == null) {
            return null;
        }
        return StopRideEarlyRequest.builder()
                .stopLocation(toLocation(web.getStopLocation()))
                .build();
    }

    public InconsistencyReportRequest toServiceInconsistencyReportRequest(WebInconsistencyReportRequest web) {
        if (web == null) {
            return null;
        }
        return InconsistencyReportRequest.builder()
                .description(web.getDescription())
                .build();
    }

    public WebRideTrackingResponse toWebRideTrackingResponse(RideTrackingResponse response) {
        if (response == null) {
            return null;
        }

        RideLocationDto location = response.getLastLocation();
        Integer etaMinutes = response.getEtaSeconds() != null
                ? (int) Math.ceil(response.getEtaSeconds() / 60.0)
                : null;

        return WebRideTrackingResponse.builder()
                .rideId(response.getRideId())
                .vehicleLatitude(location != null ? location.getLatitude() : null)
                .vehicleLongitude(location != null ? location.getLongitude() : null)
                .estimatedTimeToDestinationMinutes(etaMinutes)
                .distanceToDestinationKm(response.getDistanceToDestinationKm())
                .status(response.getRideStatus() != null ? response.getRideStatus().name() : null)
                .rideStatus(response.getRideStatus() != null ? response.getRideStatus().name() : null)
                .nextStopName(response.getNextStopAddress())
                .nextStopEta(etaMinutes)
                .updatedAt(response.getUpdatedAt())
                .recordedAt(location != null ? location.getRecordedAt() : null)
                .vehicle(new WebRideTrackingResponse.VehicleInfo(
                        response.getDriverId(),
                        response.getVehicleType(),
                        response.getDriverLicensePlate()
                ))
                .build();
    }

    private LocationPointDto toLocation(WebLocationPoint web) {
        if (web == null) {
            return null;
        }
        return LocationPointDto.builder()
                .address(web.getAddress())
                .latitude(web.getLatitude())
                .longitude(web.getLongitude())
                .build();
    }

    public WebLocationPoint toWebLocation(LocationPointDto dto) {
        if (dto == null) {
            return null;
        }
        return WebLocationPoint.builder()
                .address(dto.getAddress())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .build();
    }

    public WebActiveRideResponse toWebActiveRideResponse(ActiveRideResponse response) {
        if (response == null) {
            return null;
        }

        return WebActiveRideResponse.builder()
                .rideId(response.getRideId())
                .status(response.getStatus() != null ? response.getStatus().name() : null)
                .vehicleType(response.getVehicleType())
                .babyTransport(response.getBabyTransport())
                .petTransport(response.getPetTransport())
                .scheduledAt(response.getScheduledAt())
                .estimatedPrice(response.getEstimatedPrice())
                .distanceKm(response.getDistanceKm())
                .estimatedDurationMinutes(response.getEstimatedDurationMinutes())
                .startedAt(response.getStartedAt())
                .routeCoordinates(response.getRouteCoordinates())
                .pickup(toWebLocationFromDto(response.getPickup()))
                .dropoff(toWebLocationFromDto(response.getDropoff()))
                .stops(response.getStops() == null ? null : 
                       response.getStops().stream().map(this::toWebLocationFromDto).collect(Collectors.toList()))
                .passengers(response.getPassengers() == null ? null :
                           response.getPassengers().stream().map(this::toWebPassengerInfo).collect(Collectors.toList()))
                .driver(toWebDriverInfo(response.getDriver()))
                .build();
    }

    private WebActiveRideResponse.LocationPoint toWebLocationFromDto(LocationPointDto dto) {
        if (dto == null) {
            return null;
        }
        return WebActiveRideResponse.LocationPoint.builder()
                .address(dto.getAddress())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .build();
    }

    private WebActiveRideResponse.PassengerInfo toWebPassengerInfo(ActiveRideResponse.PassengerInfo passenger) {
        if (passenger == null) {
            return null;
        }
        return WebActiveRideResponse.PassengerInfo.builder()
                .id(passenger.getId())
                .name(passenger.getName())
                .email(passenger.getEmail())
                .phoneNumber(passenger.getPhoneNumber())
                .build();
    }

    private WebActiveRideResponse.DriverInfo toWebDriverInfo(ActiveRideResponse.DriverInfo driver) {
        if (driver == null) {
            return null;
        }
        return WebActiveRideResponse.DriverInfo.builder()
                .id(driver.getId())
                .name(driver.getName())
                .email(driver.getEmail())
                .phoneNumber(driver.getPhoneNumber())
                .vehicleType(driver.getVehicleType())
                .licensePlate(driver.getLicensePlate())
                .build();
    }

    public WebDriverRideHistoryResponse toWebDriverRideHistoryResponse(DriverRideHistoryResponse response) {
        if (response == null) {
            return null;
        }

        List<WebDriverRideHistoryResponse.PassengerInfo> passengers = response.getPassengers() == null ? null :
                response.getPassengers().stream()
                        .map(p -> new WebDriverRideHistoryResponse.PassengerInfo(
                                p.getId(),
                                p.getFirstName(),
                                p.getLastName(),
                                p.getEmail()))
                        .collect(Collectors.toList());

        return new WebDriverRideHistoryResponse(
                response.getId(),
                response.getStartTime(),
                response.getEndTime(),
                response.getPickupLocation(),
                response.getDropoffLocation(),
                response.getCancelled(),
                response.getCancelledBy(),
                response.getPrice(),
                response.getPanicActivated(),
                response.getPanickedBy(),
                response.getStatus(),
                passengers
        );
    }

    public WebPassengerRideHistoryResponse toWebPassengerRideHistoryResponse(PassengerRideHistoryResponse response) {
        if (response == null) {
            return null;
        }

        WebPassengerRideHistoryResponse.DriverInfo driver = response.getDriver() == null ? null :
                new WebPassengerRideHistoryResponse.DriverInfo(
                        response.getDriver().getId(),
                        response.getDriver().getFirstName(),
                        response.getDriver().getLastName());

        List<WebLocationPoint> stops = response.getStops() == null ? null :
                response.getStops().stream()
                        .map(this::toWebLocation)
                        .collect(Collectors.toList());

        return new WebPassengerRideHistoryResponse(
                response.getId(),
                response.getStartTime(),
                response.getEndTime(),
                response.getPickupLocation(),
                response.getDropoffLocation(),
                toWebLocation(response.getPickup()),
                toWebLocation(response.getDropoff()),
                stops,
                response.getCancelled(),
                response.getCancelledBy(),
                response.getPrice(),
                response.getPanicActivated(),
                response.getStatus(),
                response.getVehicleType(),
                response.getBabyTransport(),
                response.getPetTransport(),
                response.getDistanceKm(),
                driver
        );
    }

    public RideRatingRequest toServiceRideRatingRequest(WebRideRatingRequest web) {
        if (web == null) {
            return null;
        }
        return RideRatingRequest.builder()
                .vehicleRating(web.getVehicleRating())
                .driverRating(web.getDriverRating())
                .comment(web.getComment())
                .build();
    }

    // Admin ride history mapping
    public WebAdminRideHistoryResponse toWebAdminRideHistoryResponse(AdminRideHistoryResponse response) {
        if (response == null) {
            return null;
        }

        return WebAdminRideHistoryResponse.builder()
                .id(response.getId())
                .status(response.getStatus())
                .createdAt(response.getCreatedAt())
                .scheduledAt(response.getScheduledAt())
                .startedAt(response.getStartedAt())
                .completedAt(response.getCompletedAt())
                .pickupAddress(response.getPickupAddress())
                .dropoffAddress(response.getDropoffAddress())
                .pickup(toWebLocation(response.getPickup()))
                .dropoff(toWebLocation(response.getDropoff()))
                .stops(response.getStops() == null ? null :
                        response.getStops().stream().map(this::toWebLocation).collect(Collectors.toList()))
                .cancelled(response.getCancelled())
                .cancelledBy(response.getCancelledBy())
                .cancellationReason(response.getCancellationReason())
                .cancelledAt(response.getCancelledAt())
                .price(response.getPrice())
                .distanceKm(response.getDistanceKm())
                .estimatedDurationMinutes(response.getEstimatedDurationMinutes())
                .panicActivated(response.getPanicActivated())
                .panickedBy(response.getPanickedBy())
                .vehicleType(response.getVehicleType())
                .babyTransport(response.getBabyTransport())
                .petTransport(response.getPetTransport())
                .driver(mapAdminDriverBasicInfo(response.getDriver()))
                .passengers(response.getPassengers() == null ? null :
                        response.getPassengers().stream().map(this::mapAdminPassengerBasicInfo).collect(Collectors.toList()))
                .build();
    }

    private WebAdminRideHistoryResponse.DriverBasicInfo mapAdminDriverBasicInfo(AdminRideHistoryResponse.DriverBasicInfo driver) {
        if (driver == null) {
            return null;
        }
        return WebAdminRideHistoryResponse.DriverBasicInfo.builder()
                .id(driver.getId())
                .firstName(driver.getFirstName())
                .lastName(driver.getLastName())
                .email(driver.getEmail())
                .phoneNumber(driver.getPhoneNumber())
                .build();
    }

    private WebAdminRideHistoryResponse.PassengerBasicInfo mapAdminPassengerBasicInfo(AdminRideHistoryResponse.PassengerBasicInfo passenger) {
        if (passenger == null) {
            return null;
        }
        return WebAdminRideHistoryResponse.PassengerBasicInfo.builder()
                .id(passenger.getId())
                .firstName(passenger.getFirstName())
                .lastName(passenger.getLastName())
                .email(passenger.getEmail())
                .build();
    }

    // Admin ride detail mapping
    public WebAdminRideDetailResponse toWebAdminRideDetailResponse(AdminRideDetailResponse response) {
        if (response == null) {
            return null;
        }

        return WebAdminRideDetailResponse.builder()
                .id(response.getId())
                .status(response.getStatus())
                .createdAt(response.getCreatedAt())
                .scheduledAt(response.getScheduledAt())
                .startedAt(response.getStartedAt())
                .completedAt(response.getCompletedAt())
                .pickupAddress(response.getPickupAddress())
                .dropoffAddress(response.getDropoffAddress())
                .pickup(toWebLocation(response.getPickup()))
                .dropoff(toWebLocation(response.getDropoff()))
                .stops(response.getStops() == null ? null :
                        response.getStops().stream().map(this::toWebLocation).collect(Collectors.toList()))
                .routeCoordinates(response.getRouteCoordinates())
                .cancelled(response.getCancelled())
                .cancelledBy(response.getCancelledBy())
                .cancellationReason(response.getCancellationReason())
                .cancelledAt(response.getCancelledAt())
                .price(response.getPrice())
                .distanceKm(response.getDistanceKm())
                .estimatedDurationMinutes(response.getEstimatedDurationMinutes())
                .panicActivated(response.getPanicActivated())
                .panickedBy(response.getPanickedBy())
                .vehicleType(response.getVehicleType())
                .babyTransport(response.getBabyTransport())
                .petTransport(response.getPetTransport())
                .driver(mapAdminDriverDetailInfo(response.getDriver()))
                .passengers(response.getPassengers() == null ? null :
                        response.getPassengers().stream().map(this::mapAdminPassengerDetailInfo).collect(Collectors.toList()))
                .ratings(response.getRatings() == null ? null :
                        response.getRatings().stream().map(this::mapAdminRideRatingInfo).collect(Collectors.toList()))
                .inconsistencyReports(response.getInconsistencyReports() == null ? null :
                        response.getInconsistencyReports().stream().map(this::mapAdminInconsistencyReportInfo).collect(Collectors.toList()))
                .build();
    }

    private WebAdminRideDetailResponse.DriverDetailInfo mapAdminDriverDetailInfo(AdminRideDetailResponse.DriverDetailInfo driver) {
        if (driver == null) {
            return null;
        }
        return WebAdminRideDetailResponse.DriverDetailInfo.builder()
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

    private WebAdminRideDetailResponse.PassengerDetailInfo mapAdminPassengerDetailInfo(AdminRideDetailResponse.PassengerDetailInfo passenger) {
        if (passenger == null) {
            return null;
        }
        return WebAdminRideDetailResponse.PassengerDetailInfo.builder()
                .id(passenger.getId())
                .firstName(passenger.getFirstName())
                .lastName(passenger.getLastName())
                .email(passenger.getEmail())
                .phoneNumber(passenger.getPhoneNumber())
                .profilePicture(passenger.getProfilePicture())
                .totalRides(passenger.getTotalRides())
                .averageRating(passenger.getAverageRating())
                .build();
    }

    private WebAdminRideDetailResponse.RideRatingInfo mapAdminRideRatingInfo(AdminRideDetailResponse.RideRatingInfo rating) {
        if (rating == null) {
            return null;
        }
        return WebAdminRideDetailResponse.RideRatingInfo.builder()
                .id(rating.getId())
                .passengerId(rating.getPassengerId())
                .passengerName(rating.getPassengerName())
                .vehicleRating(rating.getVehicleRating())
                .driverRating(rating.getDriverRating())
                .comment(rating.getComment())
                .ratedAt(rating.getRatedAt())
                .build();
    }

    private WebAdminRideDetailResponse.InconsistencyReportInfo mapAdminInconsistencyReportInfo(AdminRideDetailResponse.InconsistencyReportInfo report) {
        if (report == null) {
            return null;
        }
        return WebAdminRideDetailResponse.InconsistencyReportInfo.builder()
                .id(report.getId())
                .reportedByUserId(report.getReportedByUserId())
                .reportedByName(report.getReportedByName())
                .description(report.getDescription())
                .reportedAt(report.getReportedAt())
                .build();
    }

    // Passenger ride detail mapping
    public WebPassengerRideDetailResponse toWebPassengerRideDetailResponse(PassengerRideDetailResponse response) {
        if (response == null) {
            return null;
        }

        return WebPassengerRideDetailResponse.builder()
                .id(response.getId())
                .status(response.getStatus())
                .createdAt(response.getCreatedAt())
                .scheduledAt(response.getScheduledAt())
                .startedAt(response.getStartedAt())
                .completedAt(response.getCompletedAt())
                .pickupAddress(response.getPickupAddress())
                .dropoffAddress(response.getDropoffAddress())
                .pickup(toWebLocation(response.getPickup()))
                .dropoff(toWebLocation(response.getDropoff()))
                .stops(response.getStops() == null ? null :
                        response.getStops().stream().map(this::toWebLocation).collect(Collectors.toList()))
                .routeCoordinates(response.getRouteCoordinates())
                .cancelled(response.getCancelled())
                .cancelledBy(response.getCancelledBy())
                .cancellationReason(response.getCancellationReason())
                .cancelledAt(response.getCancelledAt())
                .price(response.getPrice())
                .distanceKm(response.getDistanceKm())
                .estimatedDurationMinutes(response.getEstimatedDurationMinutes())
                .panicActivated(response.getPanicActivated())
                .vehicleType(response.getVehicleType())
                .babyTransport(response.getBabyTransport())
                .petTransport(response.getPetTransport())
                .driver(mapPassengerDriverDetailInfo(response.getDriver()))
                .ratings(response.getRatings() == null ? null :
                        response.getRatings().stream().map(this::mapPassengerRideRatingInfo).collect(Collectors.toList()))
                .inconsistencyReports(response.getInconsistencyReports() == null ? null :
                        response.getInconsistencyReports().stream().map(this::mapPassengerInconsistencyReportInfo).collect(Collectors.toList()))
                .build();
    }

    private WebPassengerRideDetailResponse.DriverDetailInfo mapPassengerDriverDetailInfo(PassengerRideDetailResponse.DriverDetailInfo driver) {
        if (driver == null) {
            return null;
        }
        return WebPassengerRideDetailResponse.DriverDetailInfo.builder()
                .id(driver.getId())
                .firstName(driver.getFirstName())
                .lastName(driver.getLastName())
                .email(driver.getEmail())
                .phoneNumber(driver.getPhoneNumber())
                .profilePicture(driver.getProfilePicture())
                .vehicleModel(driver.getVehicleModel())
                .licensePlate(driver.getLicensePlate())
                .averageRating(driver.getAverageRating())
                .build();
    }

    private WebPassengerRideDetailResponse.RideRatingInfo mapPassengerRideRatingInfo(PassengerRideDetailResponse.RideRatingInfo rating) {
        if (rating == null) {
            return null;
        }
        return WebPassengerRideDetailResponse.RideRatingInfo.builder()
                .id(rating.getId())
                .passengerId(rating.getPassengerId())
                .passengerName(rating.getPassengerName())
                .vehicleRating(rating.getVehicleRating())
                .driverRating(rating.getDriverRating())
                .comment(rating.getComment())
                .ratedAt(rating.getRatedAt())
                .build();
    }

    private WebPassengerRideDetailResponse.InconsistencyReportInfo mapPassengerInconsistencyReportInfo(PassengerRideDetailResponse.InconsistencyReportInfo report) {
        if (report == null) {
            return null;
        }
        return WebPassengerRideDetailResponse.InconsistencyReportInfo.builder()
                .id(report.getId())
                .reportedByUserId(report.getReportedByUserId())
                .reportedByName(report.getReportedByName())
                .description(report.getDescription())
                .reportedAt(report.getReportedAt())
                .build();
    }
}
