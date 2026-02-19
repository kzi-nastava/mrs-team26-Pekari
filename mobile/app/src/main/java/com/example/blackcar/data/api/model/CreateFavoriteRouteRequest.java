package com.example.blackcar.data.api.model;

import java.util.List;

/**
 * Request model for creating a favorite route.
 * Matches WebCreateFavoriteRouteRequest from backend.
 */
public class CreateFavoriteRouteRequest {

    private String name;
    private LocationPoint pickup;
    private List<LocationPoint> stops;
    private LocationPoint dropoff;
    private String vehicleType;
    private Boolean babyTransport;
    private Boolean petTransport;

    public CreateFavoriteRouteRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocationPoint getPickup() { return pickup; }
    public void setPickup(LocationPoint pickup) { this.pickup = pickup; }

    public List<LocationPoint> getStops() { return stops; }
    public void setStops(List<LocationPoint> stops) { this.stops = stops; }

    public LocationPoint getDropoff() { return dropoff; }
    public void setDropoff(LocationPoint dropoff) { this.dropoff = dropoff; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public Boolean getBabyTransport() { return babyTransport; }
    public void setBabyTransport(Boolean babyTransport) { this.babyTransport = babyTransport; }

    public Boolean getPetTransport() { return petTransport; }
    public void setPetTransport(Boolean petTransport) { this.petTransport = petTransport; }
}
