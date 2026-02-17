package com.pekara.tracking;

import com.pekara.constant.RideStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideLocationCacheEntry {
    private Long rideId;
    private RideStatus rideStatus;
    private Double latitude;
    private Double longitude;
    private Double heading;
    private Double speed;
    private Integer etaSeconds;
    private Double distanceToDestinationKm;
    private LocalDateTime recordedAt;
    private LocalDateTime updatedAt;
    private Long driverId;
    private String driverLicensePlate;
    private String vehicleType;
    private String nextStopAddress;
}
