package com.pekara.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RideHistoryResponse {

    private Long id;
    private String route;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String pickupLocation;
    private String dropoffLocation;
    private Boolean cancelled;
    private String cancelledBy;
    private BigDecimal price;
    private Boolean panicActivated;
    private String status;
}
