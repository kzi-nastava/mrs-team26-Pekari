package com.pekara.dto.request;

import com.pekara.dto.common.LocationPointDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRideRequest {
    private LocationPointDto pickup;
    /** Optional intermediate stops (order matters). */
    private List<LocationPointDto> stops;
    private LocationPointDto dropoff;

    /** Optional additional passengers (by email). Ride is paid by the creator. */
    private List<String> passengerEmails;

    private String vehicleType;
    private Boolean babyTransport;
    private Boolean petTransport;

    /** Optional scheduled start time. If provided, must be within the next 5 hours. */
    private LocalDateTime scheduledAt;
}
