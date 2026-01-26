package com.pekara.dto.response;

import com.pekara.dto.common.WebLocationPoint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebRideEstimateResponse {

    private BigDecimal estimatedPrice;
    private Integer estimatedDurationMinutes;
    private Double distanceKm;
    private String vehicleType;
    private List<WebLocationPoint> routePoints;
}
