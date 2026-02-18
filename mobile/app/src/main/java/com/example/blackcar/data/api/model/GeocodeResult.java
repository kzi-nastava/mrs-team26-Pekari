package com.example.blackcar.data.api.model;

/**
 * Result from Nominatim geocoding (search or reverse)
 */
public class GeocodeResult {

    private String displayName;
    private double latitude;
    private double longitude;

    public GeocodeResult() {}

    public GeocodeResult(String displayName, double latitude, double longitude) {
        this.displayName = displayName;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}
