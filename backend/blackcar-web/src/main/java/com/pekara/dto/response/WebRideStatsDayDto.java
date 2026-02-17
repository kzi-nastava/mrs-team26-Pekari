package com.pekara.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebRideStatsDayDto {

    private LocalDate date;
    private long rideCount;
    private double distanceKm;
    private BigDecimal amount;
}
