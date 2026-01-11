package com.pekara.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebRideTrackingResponse {

    private Long rideId;
    private Double vehicleLatitude;
    private Double vehicleLongitude;
    private Integer estimatedTimeToDestinationMinutes;
    private Double distanceToDestinationKm;
    private String status; // IN_PROGRESS, APPROACHING, ARRIVING
    private String nextStopName;
    private Integer nextStopEta; // minutes

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
