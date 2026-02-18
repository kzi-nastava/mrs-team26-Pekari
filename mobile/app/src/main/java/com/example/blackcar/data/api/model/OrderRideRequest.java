package com.example.blackcar.data.api.model;

import java.util.List;

/**
 * Request body for POST /rides/order
 * scheduledAt: ISO-8601 string (e.g. "2025-02-18T15:00:00") or null for immediate ride
 */
public class OrderRideRequest {

    private LocationPoint pickup;
    private List<LocationPoint> stops;
    private LocationPoint dropoff;
    private List<String> passengerEmails;
    private String vehicleType;
    private Boolean babyTransport;
    private Boolean petTransport;
    private String scheduledAt;

    public OrderRideRequest() {}

    public LocationPoint getPickup() { return pickup; }
    public void setPickup(LocationPoint pickup) { this.pickup = pickup; }

    public List<LocationPoint> getStops() { return stops; }
    public void setStops(List<LocationPoint> stops) { this.stops = stops; }

    public LocationPoint getDropoff() { return dropoff; }
    public void setDropoff(LocationPoint dropoff) { this.dropoff = dropoff; }

    public List<String> getPassengerEmails() { return passengerEmails; }
    public void setPassengerEmails(List<String> passengerEmails) { this.passengerEmails = passengerEmails; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public Boolean getBabyTransport() { return babyTransport; }
    public void setBabyTransport(Boolean babyTransport) { this.babyTransport = babyTransport; }

    public Boolean getPetTransport() { return petTransport; }
    public void setPetTransport(Boolean petTransport) { this.petTransport = petTransport; }

    public String getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(String scheduledAt) { this.scheduledAt = scheduledAt; }
}
