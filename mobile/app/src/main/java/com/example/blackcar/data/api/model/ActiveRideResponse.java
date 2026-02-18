package com.example.blackcar.data.api.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response from GET /rides/active/passenger (or driver)
 * Matches backend WebActiveRideResponse
 */
public class ActiveRideResponse {

    private Long rideId;
    private String status;
    private String vehicleType;
    private Boolean babyTransport;
    private Boolean petTransport;
    private String scheduledAt;
    private BigDecimal estimatedPrice;
    private Double distanceKm;
    private Integer estimatedDurationMinutes;
    private String startedAt;
    private String routeCoordinates;
    private LocationPoint pickup;
    private LocationPoint dropoff;
    private List<LocationPoint> stops;
    private List<PassengerInfo> passengers;
    private DriverInfo driver;

    public ActiveRideResponse() {}

    public Long getRideId() { return rideId; }
    public void setRideId(Long rideId) { this.rideId = rideId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public Boolean getBabyTransport() { return babyTransport; }
    public void setBabyTransport(Boolean babyTransport) { this.babyTransport = babyTransport; }

    public Boolean getPetTransport() { return petTransport; }
    public void setPetTransport(Boolean petTransport) { this.petTransport = petTransport; }

    public String getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(String scheduledAt) { this.scheduledAt = scheduledAt; }

    public BigDecimal getEstimatedPrice() { return estimatedPrice; }
    public void setEstimatedPrice(BigDecimal estimatedPrice) { this.estimatedPrice = estimatedPrice; }

    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }

    public Integer getEstimatedDurationMinutes() { return estimatedDurationMinutes; }
    public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) {
        this.estimatedDurationMinutes = estimatedDurationMinutes;
    }

    public String getStartedAt() { return startedAt; }
    public void setStartedAt(String startedAt) { this.startedAt = startedAt; }

    public String getRouteCoordinates() { return routeCoordinates; }
    public void setRouteCoordinates(String routeCoordinates) { this.routeCoordinates = routeCoordinates; }

    public LocationPoint getPickup() { return pickup; }
    public void setPickup(LocationPoint pickup) { this.pickup = pickup; }

    public LocationPoint getDropoff() { return dropoff; }
    public void setDropoff(LocationPoint dropoff) { this.dropoff = dropoff; }

    public List<LocationPoint> getStops() { return stops; }
    public void setStops(List<LocationPoint> stops) { this.stops = stops; }

    public List<PassengerInfo> getPassengers() { return passengers; }
    public void setPassengers(List<PassengerInfo> passengers) { this.passengers = passengers; }

    public DriverInfo getDriver() { return driver; }
    public void setDriver(DriverInfo driver) { this.driver = driver; }

    public static class PassengerInfo {
        private Long id;
        private String name;
        private String email;
        private String phoneNumber;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    }

    public static class DriverInfo {
        private Long id;
        private String name;
        private String email;
        private String phoneNumber;
        private String vehicleType;
        private String licensePlate;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public String getVehicleType() { return vehicleType; }
        public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
        public String getLicensePlate() { return licensePlate; }
        public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }
    }
}
