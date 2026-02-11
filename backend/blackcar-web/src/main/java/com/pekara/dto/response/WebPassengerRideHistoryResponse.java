package com.pekara.dto.response;

import com.pekara.dto.common.WebLocationPoint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebPassengerRideHistoryResponse {

    private Long id;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String pickupLocation;
    private String dropoffLocation;
    private WebLocationPoint pickup;
    private WebLocationPoint dropoff;
    private List<WebLocationPoint> stops;
    private Boolean cancelled;
    private String cancelledBy; // "passenger" or "driver"
    private BigDecimal price;
    private Boolean panicActivated;
    private String status;
    private String vehicleType;
    private Boolean babyTransport;
    private Boolean petTransport;
    private Double distanceKm;

    // Passenger does NOT see other passengers
    private DriverInfo driver;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DriverInfo {
        private Long id;
        private String firstName;
        private String lastName;
    }
}
