package com.example.blackcar.data.api.model;

import com.google.gson.annotations.SerializedName;

public class PricingResponse {
    @SerializedName("vehicleType")
    private String vehicleType;

    @SerializedName("basePrice")
    private double basePrice;

    @SerializedName("pricePerKm")
    private double pricePerKm;

    public PricingResponse() {}

    public PricingResponse(String vehicleType, double basePrice, double pricePerKm) {
        this.vehicleType = vehicleType;
        this.basePrice = basePrice;
        this.pricePerKm = pricePerKm;
    }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public double getBasePrice() { return basePrice; }
    public void setBasePrice(double basePrice) { this.basePrice = basePrice; }

    public double getPricePerKm() { return pricePerKm; }
    public void setPricePerKm(double pricePerKm) { this.pricePerKm = pricePerKm; }
}
