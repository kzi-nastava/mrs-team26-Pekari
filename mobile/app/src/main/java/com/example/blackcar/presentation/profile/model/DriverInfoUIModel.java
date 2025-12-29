package com.example.blackcar.presentation.profile.model;

public class DriverInfoUIModel {
    public final double hoursActiveLast24h;
    public final VehicleUIModel vehicle;

    public DriverInfoUIModel(double hoursActiveLast24h, VehicleUIModel vehicle) {
        this.hoursActiveLast24h = hoursActiveLast24h;
        this.vehicle = vehicle;
    }
}
