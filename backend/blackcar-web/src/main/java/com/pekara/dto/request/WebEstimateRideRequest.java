package com.pekara.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebEstimateRideRequest {

    @NotBlank(message = "Pickup location is required")
    private String pickupLocation;

    @NotBlank(message = "Dropoff location is required")
    private String dropoffLocation;

    @NotNull(message = "Vehicle type is required")
    private String vehicleType;

    @NotNull(message = "Baby transport preference is required")
    private Boolean babyTransport;

    @NotNull(message = "Pet transport preference is required")
    private Boolean petTransport;
}
