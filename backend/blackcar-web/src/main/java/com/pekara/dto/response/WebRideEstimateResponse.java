package com.pekara.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebRideEstimateResponse {

    private BigDecimal estimatedPrice;
    private Integer estimatedDurationMinutes;
    private Double distanceKm;
    private String vehicleType;
}
