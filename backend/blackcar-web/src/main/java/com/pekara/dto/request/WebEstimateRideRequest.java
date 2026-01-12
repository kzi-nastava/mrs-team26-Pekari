package com.pekara.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.pekara.dto.common.WebLocationPoint;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebEstimateRideRequest {

    @NotNull(message = "Pickup location is required")
    @Valid
    private WebLocationPoint pickup;

    @NotNull(message = "Dropoff location is required")
    @Valid
    private WebLocationPoint dropoff;

    @NotNull(message = "Vehicle type is required")
    private String vehicleType;

    @NotNull(message = "Baby transport preference is required")
    private Boolean babyTransport;

    @NotNull(message = "Pet transport preference is required")
    private Boolean petTransport;
}
