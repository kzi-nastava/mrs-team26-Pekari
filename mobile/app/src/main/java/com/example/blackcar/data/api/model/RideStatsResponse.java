package com.example.blackcar.data.api.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Ride statistics response for a date range.
 * Matches backend WebRideStatsResponse / RideStatsResponse.
 */
public class RideStatsResponse {
    @SerializedName("dailyData")
    private List<RideStatsDayDto> dailyData;
    @SerializedName("totalRides")
    private long totalRides;
    @SerializedName("totalDistanceKm")
    private double totalDistanceKm;
    @SerializedName("totalAmount")
    private double totalAmount;
    @SerializedName("avgRidesPerDay")
    private double avgRidesPerDay;
    @SerializedName("avgDistancePerDay")
    private double avgDistancePerDay;
    @SerializedName("avgAmountPerDay")
    private double avgAmountPerDay;

    public List<RideStatsDayDto> getDailyData() {
        return dailyData;
    }

    public void setDailyData(List<RideStatsDayDto> dailyData) {
        this.dailyData = dailyData;
    }

    public long getTotalRides() {
        return totalRides;
    }

    public void setTotalRides(long totalRides) {
        this.totalRides = totalRides;
    }

    public double getTotalDistanceKm() {
        return totalDistanceKm;
    }

    public void setTotalDistanceKm(double totalDistanceKm) {
        this.totalDistanceKm = totalDistanceKm;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public double getAvgRidesPerDay() {
        return avgRidesPerDay;
    }

    public void setAvgRidesPerDay(double avgRidesPerDay) {
        this.avgRidesPerDay = avgRidesPerDay;
    }

    public double getAvgDistancePerDay() {
        return avgDistancePerDay;
    }

    public void setAvgDistancePerDay(double avgDistancePerDay) {
        this.avgDistancePerDay = avgDistancePerDay;
    }

    public double getAvgAmountPerDay() {
        return avgAmountPerDay;
    }

    public void setAvgAmountPerDay(double avgAmountPerDay) {
        this.avgAmountPerDay = avgAmountPerDay;
    }
}
