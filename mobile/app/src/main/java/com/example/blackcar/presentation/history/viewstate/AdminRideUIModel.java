package com.example.blackcar.presentation.history.viewstate;

import java.util.Objects;

/**
 * UI model for admin ride history list items.
 */
public class AdminRideUIModel {

    public Long id;
    public String status;
    public String createdAt;
    public String startedAt;
    public String completedAt;
    public String pickupAddress;
    public String dropoffAddress;
    public double price;
    public Double distanceKm;
    public String vehicleType;
    public boolean cancelled;
    public String cancelledBy;
    public boolean panicActivated;
    public String panickedBy;
    public String driverName;
    public String driverEmail;
    public int passengerCount;

    public AdminRideUIModel() {}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AdminRideUIModel that = (AdminRideUIModel) o;
        return Double.compare(that.price, price) == 0 &&
               cancelled == that.cancelled &&
               panicActivated == that.panicActivated &&
               passengerCount == that.passengerCount &&
               Objects.equals(id, that.id) &&
               Objects.equals(status, that.status) &&
               Objects.equals(createdAt, that.createdAt) &&
               Objects.equals(startedAt, that.startedAt) &&
               Objects.equals(completedAt, that.completedAt) &&
               Objects.equals(pickupAddress, that.pickupAddress) &&
               Objects.equals(dropoffAddress, that.dropoffAddress) &&
               Objects.equals(distanceKm, that.distanceKm) &&
               Objects.equals(vehicleType, that.vehicleType) &&
               Objects.equals(cancelledBy, that.cancelledBy) &&
               Objects.equals(panickedBy, that.panickedBy) &&
               Objects.equals(driverName, that.driverName) &&
               Objects.equals(driverEmail, that.driverEmail);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, status, createdAt, startedAt, completedAt, pickupAddress, dropoffAddress,
                          price, distanceKm, vehicleType, cancelled, cancelledBy, panicActivated,
                          panickedBy, driverName, driverEmail, passengerCount);
    }
}
