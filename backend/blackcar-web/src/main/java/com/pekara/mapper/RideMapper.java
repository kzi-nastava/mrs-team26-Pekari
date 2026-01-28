package com.pekara.mapper;

import com.pekara.dto.common.LocationPointDto;
import com.pekara.dto.common.RideLocationDto;
import com.pekara.dto.common.WebLocationPoint;
import com.pekara.dto.request.EstimateRideRequest;
import com.pekara.dto.request.OrderRideRequest;
import com.pekara.dto.request.RideLocationUpdateRequest;
import com.pekara.dto.request.RideRatingRequest;
import com.pekara.dto.request.StopRideEarlyRequest;
import com.pekara.dto.request.WebEstimateRideRequest;
import com.pekara.dto.request.WebOrderRideRequest;
import com.pekara.dto.request.WebRideLocationUpdateRequest;
import com.pekara.dto.request.WebRideRatingRequest;
import com.pekara.dto.request.WebStopRideEarlyRequest;
import com.pekara.dto.response.ActiveRideResponse;
import com.pekara.dto.response.DriverRideHistoryResponse;
import com.pekara.dto.response.RideTrackingResponse;
import com.pekara.dto.response.WebActiveRideResponse;
import com.pekara.dto.response.WebDriverRideHistoryResponse;
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
}
