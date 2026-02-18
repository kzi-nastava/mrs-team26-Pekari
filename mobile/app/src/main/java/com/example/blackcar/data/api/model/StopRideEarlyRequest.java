package com.example.blackcar.data.api.model;

/**
 * Request body for stopping a ride early with current location.
 * Used with POST /rides/{rideId}/stop
 */
public class StopRideEarlyRequest {

    private LocationPoint stopLocation;

    public StopRideEarlyRequest() {}

    public StopRideEarlyRequest(LocationPoint stopLocation) {
        this.stopLocation = stopLocation;
    }

    public LocationPoint getStopLocation() {
        return stopLocation;
    }

    public void setStopLocation(LocationPoint stopLocation) {
        this.stopLocation = stopLocation;
    }
}
