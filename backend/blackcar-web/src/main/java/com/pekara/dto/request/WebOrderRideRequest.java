package com.pekara.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.pekara.dto.common.WebLocationPoint;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebOrderRideRequest {

    @NotNull(message = "Pickup location is required")
    @Valid
    private WebLocationPoint pickup;

    /** Optional intermediate stops (order matters). */
    private List<@Valid WebLocationPoint> stops;

    @NotNull(message = "Dropoff location is required")
    @Valid
    private WebLocationPoint dropoff;

    /**
     * Optional additional passengers (by email). Ride is paid by the creator.
     */
    private List<@Email(message = "Passenger email must be valid") String> passengerEmails;

    @NotNull(message = "Vehicle type is required")
    private String vehicleType;

    @NotNull(message = "Baby transport preference is required")
    private Boolean babyTransport;

    @NotNull(message = "Pet transport preference is required")
    private Boolean petTransport;

    /**
     * Optional scheduled start time. If provided, must be within the next 5 hours.
     */
    private LocalDateTime scheduledAt;
}
