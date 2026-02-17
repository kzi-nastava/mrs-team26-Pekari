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
 * Web response DTO for detailed ride information for passengers.
 * Includes route coordinates for map display, driver details, ratings, and inconsistency reports.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebPassengerRideDetailResponse {

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

    // Route for map display
    private String routeCoordinates;

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

    // Vehicle info
    private String vehicleType;
    private Boolean babyTransport;
    private Boolean petTransport;

    // Driver details
    private DriverDetailInfo driver;

    // Ratings for this ride
    private List<RideRatingInfo> ratings;

    // Inconsistency reports for this ride
    private List<InconsistencyReportInfo> inconsistencyReports;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DriverDetailInfo {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
        private String phoneNumber;
        private String profilePicture;
        private String vehicleModel;
        private String licensePlate;
        private Double averageRating;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RideRatingInfo {
        private Long id;
        private Long passengerId;
        private String passengerName;
        private Integer vehicleRating;
        private Integer driverRating;
        private String comment;
        private LocalDateTime ratedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InconsistencyReportInfo {
        private Long id;
        private Long reportedByUserId;
        private String reportedByName;
        private String description;
        private LocalDateTime reportedAt;
    }
}
