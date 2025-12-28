package com.example.blackcar.domain.model;

import androidx.annotation.NonNull;

public final class VehicleInfo {
    @NonNull
    private final String id;
    @NonNull
    private final String make;
    @NonNull
    private final String model;
    private final int year;
    @NonNull
    private final String licensePlate;
    @NonNull
    private final String vin;

    public VehicleInfo(
            @NonNull String id,
            @NonNull String make,
            @NonNull String model,
            int year,
            @NonNull String licensePlate,
            @NonNull String vin
    ) {
        this.id = id;
        this.make = make;
        this.model = model;
        this.year = year;
        this.licensePlate = licensePlate;
        this.vin = vin;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getMake() {
        return make;
    }

    @NonNull
    public String getModel() {
        return model;
    }

    public int getYear() {
        return year;
    }

    @NonNull
    public String getLicensePlate() {
        return licensePlate;
    }

    @NonNull
    public String getVin() {
        return vin;
    }
}
