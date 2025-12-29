package com.pekara.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRideRequest {

    @NotBlank(message = "Pickup location is required")
    private String pickupLocation;

    /**
     * Optional intermediate stops (order matters).
     */
    private List<@NotBlank(message = "Stop location must not be blank") String> stops;

    @NotBlank(message = "Dropoff location is required")
    private String dropoffLocation;

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
