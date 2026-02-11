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
 * Web response DTO for detailed ride information including route coordinates,
 * ratings, and inconsistency reports. Used for admin detailed view with map.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebAdminRideDetailResponse {

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
    private String panickedBy;

    // Vehicle info
    private String vehicleType;
    private Boolean babyTransport;
    private Boolean petTransport;

    // Driver details
    private DriverDetailInfo driver;

    // Passengers details
    private List<PassengerDetailInfo> passengers;

    // Ratings
    private List<RideRatingInfo> ratings;

    // Inconsistency reports
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
        private String licenseNumber;
        private String vehicleModel;
        private String licensePlate;
        private Double averageRating;
        private Integer totalRides;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PassengerDetailInfo {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
        private String phoneNumber;
        private String profilePicture;
        private Integer totalRides;
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
