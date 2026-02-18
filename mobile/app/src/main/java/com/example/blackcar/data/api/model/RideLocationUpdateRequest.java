package com.example.blackcar.data.api.model;

/**
 * Request body for updating ride location during an active ride.
 * Used with POST /rides/{rideId}/location
 */
public class RideLocationUpdateRequest {

    private Double latitude;
    private Double longitude;
    private Double heading;
    private Double speed;
    private String recordedAt;

    public RideLocationUpdateRequest() {}

    public RideLocationUpdateRequest(Double latitude, Double longitude, Double heading, Double speed, String recordedAt) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.heading = heading;
        this.speed = speed;
        this.recordedAt = recordedAt;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getHeading() {
        return heading;
    }

    public void setHeading(Double heading) {
        this.heading = heading;
    }

    public Double getSpeed() {
        return speed;
    }

    public void setSpeed(Double speed) {
        this.speed = speed;
    }

    public String getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(String recordedAt) {
        this.recordedAt = recordedAt;
    }
}
