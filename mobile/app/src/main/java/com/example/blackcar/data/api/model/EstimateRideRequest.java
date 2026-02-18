package com.example.blackcar.data.api.model;

import java.util.List;

/**
 * Request body for POST /rides/estimate
 */
public class EstimateRideRequest {

    private LocationPoint pickup;
    private List<LocationPoint> stops;
    private LocationPoint dropoff;
    private String vehicleType;

    public EstimateRideRequest() {}

    public EstimateRideRequest(LocationPoint pickup, List<LocationPoint> stops,
                              LocationPoint dropoff, String vehicleType) {
        this.pickup = pickup;
        this.stops = stops;
        this.dropoff = dropoff;
        this.vehicleType = vehicleType;
    }

    public LocationPoint getPickup() { return pickup; }
    public void setPickup(LocationPoint pickup) { this.pickup = pickup; }

    public List<LocationPoint> getStops() { return stops; }
    public void setStops(List<LocationPoint> stops) { this.stops = stops; }

    public LocationPoint getDropoff() { return dropoff; }
    public void setDropoff(LocationPoint dropoff) { this.dropoff = dropoff; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
}
