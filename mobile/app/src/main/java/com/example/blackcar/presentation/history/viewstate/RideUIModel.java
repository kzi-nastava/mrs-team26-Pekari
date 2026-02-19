package com.example.blackcar.presentation.history.viewstate;

import com.example.blackcar.data.api.model.LocationPoint;

import java.util.List;
import java.util.Objects;

public class RideUIModel {

    public Long id;
    public String startTime;
    public String endTime;
    public String origin;
    public String destination;
    public String canceledBy; // null if not canceled
    public boolean panic;
    public double price;
    public List<String> passengers;
    public Double distanceKm;
    public String vehicleType;
    public String status;
    public String driverName; // For passenger history view

    /** Pickup location with coordinates (for creating favorite route) */
    public LocationPoint pickup;
    /** Dropoff location with coordinates (for creating favorite route) */
    public LocationPoint dropoff;
    /** Stops with coordinates (for creating favorite route) */
    public List<LocationPoint> stops;
    public Boolean babyTransport;
    public Boolean petTransport;
    /** ID of matching favorite route, if any */
    public Long favoriteRouteId;

    public RideUIModel() { }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RideUIModel that = (RideUIModel) o;
        return panic == that.panic &&
               Double.compare(that.price, price) == 0 &&
               Objects.equals(id, that.id) &&
               Objects.equals(startTime, that.startTime) &&
               Objects.equals(endTime, that.endTime) &&
               Objects.equals(origin, that.origin) &&
               Objects.equals(destination, that.destination) &&
               Objects.equals(canceledBy, that.canceledBy) &&
               Objects.equals(passengers, that.passengers) &&
               Objects.equals(distanceKm, that.distanceKm) &&
               Objects.equals(vehicleType, that.vehicleType) &&
               Objects.equals(status, that.status) &&
               Objects.equals(driverName, that.driverName) &&
               Objects.equals(favoriteRouteId, that.favoriteRouteId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, startTime, endTime, origin, destination,
                          canceledBy, panic, price, passengers, distanceKm, vehicleType, status, driverName, favoriteRouteId);
    }
}
