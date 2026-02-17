package com.pekara.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebRideStatsResponse {

    private List<WebRideStatsDayDto> dailyData;
    private long totalRides;
    private double totalDistanceKm;
    private BigDecimal totalAmount;
    private double avgRidesPerDay;
    private double avgDistancePerDay;
    private BigDecimal avgAmountPerDay;
}
