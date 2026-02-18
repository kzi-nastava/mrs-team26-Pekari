package com.example.blackcar.data.api.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response from POST /rides/estimate
 */
public class RideEstimateResponse {

    private BigDecimal estimatedPrice;
    private Integer estimatedDurationMinutes;
    private Double distanceKm;
    private String vehicleType;
    private List<LocationPoint> routePoints;

    public RideEstimateResponse() {}

    public BigDecimal getEstimatedPrice() { return estimatedPrice; }
    public void setEstimatedPrice(BigDecimal estimatedPrice) { this.estimatedPrice = estimatedPrice; }

    public Integer getEstimatedDurationMinutes() { return estimatedDurationMinutes; }
    public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) {
        this.estimatedDurationMinutes = estimatedDurationMinutes;
    }

    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public List<LocationPoint> getRoutePoints() { return routePoints; }
    public void setRoutePoints(List<LocationPoint> routePoints) { this.routePoints = routePoints; }
}
