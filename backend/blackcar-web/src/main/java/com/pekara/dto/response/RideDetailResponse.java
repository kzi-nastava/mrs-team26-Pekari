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
public class RideDetailResponse {

    private Long id;
    private String route;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String pickupLocation;
    private String dropoffLocation;
    private List<String> stops;
    private Boolean cancelled;
    private String cancelledBy;
    private String cancellationReason;
    private BigDecimal price;
    private Boolean panicActivated;
    private String status;

    private DriverInfo driver;
    private List<PassengerInfo> passengers;
    private List<InconsistencyReport> inconsistencyReports;
    private RideRating rating;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DriverInfo {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
        private String phoneNumber;
        private String profileImage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PassengerInfo {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InconsistencyReport {
        private Long id;
        private String reportedBy;
        private String description;
        private LocalDateTime reportedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RideRating {
        private Integer vehicleRating;
        private Integer driverRating;
        private String comment;
        private LocalDateTime ratedAt;
    }
}
