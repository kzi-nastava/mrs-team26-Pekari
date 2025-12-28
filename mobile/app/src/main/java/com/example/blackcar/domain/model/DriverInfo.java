package com.example.blackcar.domain.model;

import androidx.annotation.NonNull;

public final class DriverInfo {
    private final double hoursActiveLast24h;
    @NonNull
    private final VehicleInfo vehicle;

    public DriverInfo(double hoursActiveLast24h, @NonNull VehicleInfo vehicle) {
        this.hoursActiveLast24h = hoursActiveLast24h;
        this.vehicle = vehicle;
    }

    public double getHoursActiveLast24h() {
        return hoursActiveLast24h;
    }

    @NonNull
    public VehicleInfo getVehicle() {
        return vehicle;
    }
}
