package com.example.blackcar.data.api.model;

import java.time.LocalDateTime;

public class DriverStateResponse {
    public Long driverId;
    public String driverEmail;
    public Boolean online;
    public Boolean busy;
    public Double latitude;
    public Double longitude;
    public String updatedAt; // use String to avoid Java 8 time parsing on Android if not needed
}
