package com.pekara.dto.response;

import com.pekara.dto.common.LocationPointDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideEstimateResponse {
    private BigDecimal estimatedPrice;
    private Integer estimatedDurationMinutes;
    private Double distanceKm;
    private String vehicleType;
    private List<LocationPointDto> routePoints;
}
