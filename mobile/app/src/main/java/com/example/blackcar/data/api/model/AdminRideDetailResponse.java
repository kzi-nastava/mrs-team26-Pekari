package com.example.blackcar.data.api.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO for detailed admin ride information.
 * Includes route coordinates for map display, detailed driver/passenger info, ratings, and inconsistency reports.
 */
public class AdminRideDetailResponse {

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
    private String panickedBy;

    // Vehicle info
    private String vehicleType;
    private Boolean babyTransport;
    private Boolean petTransport;

    // Detailed people info
    private AdminDriverDetailInfo driver;
    private List<AdminPassengerDetailInfo> passengers;

    // Ratings and reports
    private List<AdminRideRatingInfo> ratings;
    private List<AdminInconsistencyReportInfo> inconsistencyReports;

    public AdminRideDetailResponse() {}

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

    public String getPanickedBy() { return panickedBy; }
    public void setPanickedBy(String panickedBy) { this.panickedBy = panickedBy; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public Boolean getBabyTransport() { return babyTransport; }
    public void setBabyTransport(Boolean babyTransport) { this.babyTransport = babyTransport; }

    public Boolean getPetTransport() { return petTransport; }
    public void setPetTransport(Boolean petTransport) { this.petTransport = petTransport; }

    public AdminDriverDetailInfo getDriver() { return driver; }
    public void setDriver(AdminDriverDetailInfo driver) { this.driver = driver; }

    public List<AdminPassengerDetailInfo> getPassengers() { return passengers; }
    public void setPassengers(List<AdminPassengerDetailInfo> passengers) { this.passengers = passengers; }

    public List<AdminRideRatingInfo> getRatings() { return ratings; }
    public void setRatings(List<AdminRideRatingInfo> ratings) { this.ratings = ratings; }

    public List<AdminInconsistencyReportInfo> getInconsistencyReports() { return inconsistencyReports; }
    public void setInconsistencyReports(List<AdminInconsistencyReportInfo> inconsistencyReports) { this.inconsistencyReports = inconsistencyReports; }

    // Inner classes
    public static class AdminDriverDetailInfo {
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

        public AdminDriverDetailInfo() {}

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

        public String getLicenseNumber() { return licenseNumber; }
        public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

        public String getVehicleModel() { return vehicleModel; }
        public void setVehicleModel(String vehicleModel) { this.vehicleModel = vehicleModel; }

        public String getLicensePlate() { return licensePlate; }
        public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

        public Double getAverageRating() { return averageRating; }
        public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }

        public Integer getTotalRides() { return totalRides; }
        public void setTotalRides(Integer totalRides) { this.totalRides = totalRides; }
    }

    public static class AdminPassengerDetailInfo {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
        private String phoneNumber;
        private String profilePicture;
        private Integer totalRides;
        private Double averageRating;

        public AdminPassengerDetailInfo() {}

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

        public Integer getTotalRides() { return totalRides; }
        public void setTotalRides(Integer totalRides) { this.totalRides = totalRides; }

        public Double getAverageRating() { return averageRating; }
        public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }
    }

    public static class AdminRideRatingInfo {
        private Long id;
        private Long passengerId;
        private String passengerName;
        private Integer vehicleRating;
        private Integer driverRating;
        private String comment;
        private String ratedAt;

        public AdminRideRatingInfo() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public Long getPassengerId() { return passengerId; }
        public void setPassengerId(Long passengerId) { this.passengerId = passengerId; }

        public String getPassengerName() { return passengerName; }
        public void setPassengerName(String passengerName) { this.passengerName = passengerName; }

        public Integer getVehicleRating() { return vehicleRating; }
        public void setVehicleRating(Integer vehicleRating) { this.vehicleRating = vehicleRating; }

        public Integer getDriverRating() { return driverRating; }
        public void setDriverRating(Integer driverRating) { this.driverRating = driverRating; }

        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }

        public String getRatedAt() { return ratedAt; }
        public void setRatedAt(String ratedAt) { this.ratedAt = ratedAt; }
    }

    public static class AdminInconsistencyReportInfo {
        private Long id;
        private Long reportedByUserId;
        private String reportedByName;
        private String description;
        private String reportedAt;

        public AdminInconsistencyReportInfo() {}

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
