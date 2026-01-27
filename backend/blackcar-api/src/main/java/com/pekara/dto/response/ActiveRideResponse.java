package com.pekara.dto.response;

import com.pekara.constant.RideStatus;
import com.pekara.dto.common.LocationPointDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActiveRideResponse {
    private Long rideId;
    private RideStatus status;
    private String vehicleType;
    private Boolean babyTransport;
    private Boolean petTransport;
    private LocalDateTime scheduledAt;
    private BigDecimal estimatedPrice;
    private Double distanceKm;
    private Integer estimatedDurationMinutes;
    private LocalDateTime startedAt;
    private String routeCoordinates;
    private LocationPointDto pickup;
    private LocationPointDto dropoff;
    private List<LocationPointDto> stops;
    private List<PassengerInfo> passengers;
    private DriverInfo driver;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PassengerInfo {
        private Long id;
        private String name;
        private String email;
        private String phoneNumber;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DriverInfo {
        private Long id;
        private String name;
        private String email;
        private String phoneNumber;
        private String vehicleType;
        private String licensePlate;
    }
}
