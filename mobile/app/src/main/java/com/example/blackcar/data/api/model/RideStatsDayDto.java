package com.example.blackcar.data.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * Daily ride statistics for a single date.
 * Matches backend WebRideStatsDayDto / RideStatsDayDto.
 */
public class RideStatsDayDto {
    private String date;  // ISO date string e.g. "2024-01-15"
    @SerializedName("rideCount")
    private long rideCount;
    @SerializedName("distanceKm")
    private double distanceKm;
    private double amount;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public long getRideCount() {
        return rideCount;
    }

    public void setRideCount(long rideCount) {
        this.rideCount = rideCount;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
