package com.example.blackcar.data.api.model;

import java.math.BigDecimal;
import java.util.List;

public class DriverRideHistoryResponse {

    private Long id;
    private String startTime;
    private String endTime;
    private String pickupLocation;
    private String dropoffLocation;
    private Boolean cancelled;
    private String cancelledBy;
    private BigDecimal price;
    private Boolean panicActivated;
    private String panickedBy;
    private String status;
    private List<PassengerInfo> passengers;

    public DriverRideHistoryResponse() {}

    public DriverRideHistoryResponse(Long id, String startTime, String endTime, String pickupLocation,
                                    String dropoffLocation, Boolean cancelled, String cancelledBy,
                                    BigDecimal price, Boolean panicActivated, String panickedBy,
                                    String status, List<PassengerInfo> passengers) {
        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
        this.pickupLocation = pickupLocation;
        this.dropoffLocation = dropoffLocation;
        this.cancelled = cancelled;
        this.cancelledBy = cancelledBy;
        this.price = price;
        this.panicActivated = panicActivated;
        this.panickedBy = panickedBy;
        this.status = status;
        this.passengers = passengers;
    }

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

    public Boolean getCancelled() { return cancelled; }
    public void setCancelled(Boolean cancelled) { this.cancelled = cancelled; }

    public String getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(String cancelledBy) { this.cancelledBy = cancelledBy; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Boolean getPanicActivated() { return panicActivated; }
    public void setPanicActivated(Boolean panicActivated) { this.panicActivated = panicActivated; }

    public String getPanickedBy() { return panickedBy; }
    public void setPanickedBy(String panickedBy) { this.panickedBy = panickedBy; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<PassengerInfo> getPassengers() { return passengers; }
    public void setPassengers(List<PassengerInfo> passengers) { this.passengers = passengers; }

    public static class PassengerInfo {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;

        public PassengerInfo() {}

        public PassengerInfo(Long id, String firstName, String lastName, String email) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
}
