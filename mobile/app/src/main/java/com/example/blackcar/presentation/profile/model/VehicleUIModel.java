package com.example.blackcar.presentation.profile.model;

public class VehicleUIModel {
    public final String id;
    public final String make;
    public final String model;
    public final int year;
    public final String licensePlate;
    public final String vin;

    public VehicleUIModel(String id, String make, String model, int year, String licensePlate, String vin) {
        this.id = id;
        this.make = make;
        this.model = model;
        this.year = year;
        this.licensePlate = licensePlate;
        this.vin = vin;
    }
}
