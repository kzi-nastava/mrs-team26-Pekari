package com.pekara.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRideResponse {

    private Long rideId;

    /**
     * Suggested values: ACCEPTED, REJECTED, SCHEDULED
     */
    private String status;

    private String message;

    private BigDecimal estimatedPrice;

    private LocalDateTime scheduledAt;

    /**
     * Optional in stubbed implementation.
     */
    private String assignedDriverEmail;
}
