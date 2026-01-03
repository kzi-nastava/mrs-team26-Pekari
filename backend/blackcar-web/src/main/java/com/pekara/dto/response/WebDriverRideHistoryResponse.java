package com.pekara.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebDriverRideHistoryResponse {

    private Long id;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String pickupLocation;
    private String dropoffLocation;
    private Boolean cancelled;
    private String cancelledBy; // passenger name or "driver"
    private BigDecimal price;
    private Boolean panicActivated;
    private String status;

    // Driver sees all passengers in history
    private List<PassengerInfo> passengers;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PassengerInfo {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
    }
}
