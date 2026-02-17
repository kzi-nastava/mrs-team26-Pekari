package com.example.blackcar.data.api.model;

import java.math.BigDecimal;
import java.util.List;

public class PassengerRideHistoryResponse {

    private Long id;
    private String startTime;
    private String endTime;
    private String pickupLocation;
    private String dropoffLocation;
    private LocationPoint pickup;
    private LocationPoint dropoff;
    private List<LocationPoint> stops;
    private Boolean cancelled;
    private String cancelledBy;
    private BigDecimal price;
    private Boolean panicActivated;
    private String status;
    private String vehicleType;
    private Boolean babyTransport;
    private Boolean petTransport;
    private Double distanceKm;
    private DriverHistoryInfo driver;

    public PassengerRideHistoryResponse() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getPickupLocation() { return pickupLocation; }
    public void setPickupLocation(String pickupLocation) { this.pickupLocation = pickupLocation; }

    public String getDropoffLocation() { return dropoffLocation; }
    public void setDropoffLocation(String dropoffLocation) { this.dropoffLocation = dropoffLocation; }

    public LocationPoint getPickup() { return pickup; }
    public void setPickup(LocationPoint pickup) { this.pickup = pickup; }

    public LocationPoint getDropoff() { return dropoff; }
    public void setDropoff(LocationPoint dropoff) { this.dropoff = dropoff; }

    public List<LocationPoint> getStops() { return stops; }
    public void setStops(List<LocationPoint> stops) { this.stops = stops; }

    public Boolean getCancelled() { return cancelled; }
    public void setCancelled(Boolean cancelled) { this.cancelled = cancelled; }

    public String getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(String cancelledBy) { this.cancelledBy = cancelledBy; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Boolean getPanicActivated() { return panicActivated; }
    public void setPanicActivated(Boolean panicActivated) { this.panicActivated = panicActivated; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public Boolean getBabyTransport() { return babyTransport; }
    public void setBabyTransport(Boolean babyTransport) { this.babyTransport = babyTransport; }

    public Boolean getPetTransport() { return petTransport; }
    public void setPetTransport(Boolean petTransport) { this.petTransport = petTransport; }

    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }

    public DriverHistoryInfo getDriver() { return driver; }
    public void setDriver(DriverHistoryInfo driver) { this.driver = driver; }

    public static class DriverHistoryInfo {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
        private String profilePicture;

        public DriverHistoryInfo() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getProfilePicture() { return profilePicture; }
        public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }
    }
}
