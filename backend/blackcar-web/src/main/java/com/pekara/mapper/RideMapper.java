package com.pekara.mapper;

import com.pekara.dto.common.LocationPointDto;
import com.pekara.dto.common.WebLocationPoint;
import com.pekara.dto.request.EstimateRideRequest;
import com.pekara.dto.request.OrderRideRequest;
import com.pekara.dto.request.WebEstimateRideRequest;
import com.pekara.dto.request.WebOrderRideRequest;
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
