package com.pekara.dto.response;

import com.pekara.dto.common.WebLocationPoint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Web response DTO for admin ride history list.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebAdminRideHistoryResponse {

    private Long id;
    private String status;

    // Dates
    private LocalDateTime createdAt;
    private LocalDateTime scheduledAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    // Locations
    private String pickupAddress;
    private String dropoffAddress;
    private WebLocationPoint pickup;
    private WebLocationPoint dropoff;
    private List<WebLocationPoint> stops;

    // Cancellation info
    private Boolean cancelled;
    private String cancelledBy;
    private String cancellationReason;
    private LocalDateTime cancelledAt;

    // Pricing
    private BigDecimal price;
    private Double distanceKm;
    private Integer estimatedDurationMinutes;

    // Panic info
    private Boolean panicActivated;
    private String panickedBy;

    // Vehicle info
    private String vehicleType;
    private Boolean babyTransport;
    private Boolean petTransport;

    // Participants
    private DriverBasicInfo driver;
    private List<PassengerBasicInfo> passengers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DriverBasicInfo {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
        private String phoneNumber;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PassengerBasicInfo {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
    }
}
