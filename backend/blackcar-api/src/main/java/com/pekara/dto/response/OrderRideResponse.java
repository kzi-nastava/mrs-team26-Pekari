package com.pekara.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRideResponse {
    private Long rideId;
    private String status;
    private String message;
    private BigDecimal estimatedPrice;
    private LocalDateTime scheduledAt;
    private String assignedDriverEmail;
}
