package com.pekara.dto.response;

import com.pekara.constant.RideStatus;
import com.pekara.dto.common.RideLocationDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideTrackingResponse {
    private Long rideId;
    private RideStatus rideStatus;
    private RideLocationDto lastLocation;
    private Integer etaSeconds;
    private Double distanceToDestinationKm;
    private LocalDateTime updatedAt;
    private Long driverId;
    private String driverLicensePlate;
    private String vehicleType;
    private String nextStopAddress;
}
