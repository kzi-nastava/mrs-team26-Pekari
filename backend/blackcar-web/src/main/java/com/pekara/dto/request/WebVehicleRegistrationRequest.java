package com.pekara.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebVehicleRegistrationRequest {

    @NotBlank(message = "Vehicle model is required")
    private String model;

    @NotBlank(message = "Vehicle type is required")
    private String type;

    @NotBlank(message = "License plate is required")
    private String licensePlate;

    @NotNull(message = "Number of seats is required")
    @Min(value = 1, message = "Number of seats must be at least 1")
    @Max(value = 8, message = "Number of seats cannot exceed 8")
    private Integer numberOfSeats;

    private Boolean babyFriendly = false;

    private Boolean petFriendly = false;
}
