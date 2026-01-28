package com.pekara.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebRideTrackingResponse {

    private Long rideId;
    private Double vehicleLatitude;
    private Double vehicleLongitude;
    private Integer estimatedTimeToDestinationMinutes;
    private Double distanceToDestinationKm;
    private String status; // IN_PROGRESS, APPROACHING, ARRIVING
    private String nextStopName;
    private Integer nextStopEta; // minutes
    private LocalDateTime updatedAt;
    private LocalDateTime recordedAt;

    private VehicleInfo vehicle;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleInfo {
        private Long id;
        private String type;
        private String licensePlate;
    }
}
