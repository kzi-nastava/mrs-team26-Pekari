package com.pekara.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PassengerRideHistoryResponse {

    private Long id;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String pickupLocation;
    private String dropoffLocation;
    private Boolean cancelled;
    private String cancelledBy; // "passenger" or "driver"
    private BigDecimal price;
    private Boolean panicActivated;
    private String status;

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
