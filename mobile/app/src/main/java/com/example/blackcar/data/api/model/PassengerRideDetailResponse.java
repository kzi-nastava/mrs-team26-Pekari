package com.example.blackcar.data.api.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO for detailed ride information for passengers.
 * Includes route coordinates for map display, driver details, ratings, and inconsistency reports.
 */
public class PassengerRideDetailResponse {

    private Long id;
    private String status;

    // Dates
    private String createdAt;
    private String scheduledAt;
    private String startedAt;
    private String completedAt;

    // Locations
    private String pickupAddress;
    private String dropoffAddress;
    private LocationPoint pickup;
    private LocationPoint dropoff;
    private List<LocationPoint> stops;

    // Route for map display (JSON string of coordinates)
    private String routeCoordinates;

    // Cancellation info
    private Boolean cancelled;
    private String cancelledBy;
    private String cancellationReason;
    private String cancelledAt;

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

    // Driver details (for passenger view)
    private DriverDetailInfo driver;

    // Ratings for this ride
    private List<RideRatingInfo> ratings;

    // Inconsistency reports for this ride
    private List<InconsistencyReportInfo> inconsistencyReports;

    public PassengerRideDetailResponse() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(String scheduledAt) { this.scheduledAt = scheduledAt; }

    public String getStartedAt() { return startedAt; }
    public void setStartedAt(String startedAt) { this.startedAt = startedAt; }

    public String getCompletedAt() { return completedAt; }
    public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }

    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }

    public String getDropoffAddress() { return dropoffAddress; }
    public void setDropoffAddress(String dropoffAddress) { this.dropoffAddress = dropoffAddress; }

    public LocationPoint getPickup() { return pickup; }
    public void setPickup(LocationPoint pickup) { this.pickup = pickup; }

    public LocationPoint getDropoff() { return dropoff; }
    public void setDropoff(LocationPoint dropoff) { this.dropoff = dropoff; }

    public List<LocationPoint> getStops() { return stops; }
    public void setStops(List<LocationPoint> stops) { this.stops = stops; }

    public String getRouteCoordinates() { return routeCoordinates; }
    public void setRouteCoordinates(String routeCoordinates) { this.routeCoordinates = routeCoordinates; }

    public Boolean getCancelled() { return cancelled; }
    public void setCancelled(Boolean cancelled) { this.cancelled = cancelled; }

    public String getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(String cancelledBy) { this.cancelledBy = cancelledBy; }

    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }

    public String getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(String cancelledAt) { this.cancelledAt = cancelledAt; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }

    public Integer getEstimatedDurationMinutes() { return estimatedDurationMinutes; }
    public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) { this.estimatedDurationMinutes = estimatedDurationMinutes; }

    public Boolean getPanicActivated() { return panicActivated; }
    public void setPanicActivated(Boolean panicActivated) { this.panicActivated = panicActivated; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public Boolean getBabyTransport() { return babyTransport; }
    public void setBabyTransport(Boolean babyTransport) { this.babyTransport = babyTransport; }

    public Boolean getPetTransport() { return petTransport; }
    public void setPetTransport(Boolean petTransport) { this.petTransport = petTransport; }

    public DriverDetailInfo getDriver() { return driver; }
    public void setDriver(DriverDetailInfo driver) { this.driver = driver; }

    public List<RideRatingInfo> getRatings() { return ratings; }
    public void setRatings(List<RideRatingInfo> ratings) { this.ratings = ratings; }

    public List<InconsistencyReportInfo> getInconsistencyReports() { return inconsistencyReports; }
    public void setInconsistencyReports(List<InconsistencyReportInfo> inconsistencyReports) { this.inconsistencyReports = inconsistencyReports; }

    public static class DriverDetailInfo {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
        private String phoneNumber;
        private String profilePicture;

        public DriverDetailInfo() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

        public String getProfilePicture() { return profilePicture; }
        public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }
    }

    public static class RideRatingInfo {
        private Long id;
        private Integer vehicleRating;
        private Integer driverRating;
        private String comment;
        private String ratedAt;

        public RideRatingInfo() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public Integer getVehicleRating() { return vehicleRating; }
        public void setVehicleRating(Integer vehicleRating) { this.vehicleRating = vehicleRating; }

        public Integer getDriverRating() { return driverRating; }
        public void setDriverRating(Integer driverRating) { this.driverRating = driverRating; }

        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }

        public String getRatedAt() { return ratedAt; }
        public void setRatedAt(String ratedAt) { this.ratedAt = ratedAt; }
    }

    public static class InconsistencyReportInfo {
        private Long id;
        private Long reportedByUserId;
        private String reportedByName;
        private String description;
        private String reportedAt;

        public InconsistencyReportInfo() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public Long getReportedByUserId() { return reportedByUserId; }
        public void setReportedByUserId(Long reportedByUserId) { this.reportedByUserId = reportedByUserId; }

        public String getReportedByName() { return reportedByName; }
        public void setReportedByName(String reportedByName) { this.reportedByName = reportedByName; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getReportedAt() { return reportedAt; }
        public void setReportedAt(String reportedAt) { this.reportedAt = reportedAt; }
    }
}
