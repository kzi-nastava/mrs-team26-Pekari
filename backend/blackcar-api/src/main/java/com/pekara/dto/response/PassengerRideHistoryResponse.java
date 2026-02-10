package com.pekara.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PassengerRideHistoryResponse {

    private Long id;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String pickupLocation;
    private String dropoffLocation;
    private Boolean cancelled;
    private String cancelledBy;
    private BigDecimal price;
    private Boolean panicActivated;
    private String panickedBy;
    private String status;
    private DriverInfo driver;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DriverInfo {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
    }
}
