package com.pekara.mapper;

import com.pekara.dto.common.LocationPointDto;
import com.pekara.dto.common.RideLocationDto;
import com.pekara.dto.common.WebLocationPoint;
import com.pekara.dto.request.EstimateRideRequest;
import com.pekara.dto.request.OrderRideRequest;
import com.pekara.dto.request.RideLocationUpdateRequest;
import com.pekara.dto.request.WebEstimateRideRequest;
import com.pekara.dto.request.WebOrderRideRequest;
import com.pekara.dto.request.WebRideLocationUpdateRequest;
import com.pekara.dto.response.RideTrackingResponse;
import com.pekara.dto.response.WebRideTrackingResponse;
import org.springframework.stereotype.Component;

import java.util.List;

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
}
