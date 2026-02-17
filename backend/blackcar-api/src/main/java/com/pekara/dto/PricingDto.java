package com.pekara.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingDto {
    private String vehicleType;
    private BigDecimal basePrice;
    private BigDecimal pricePerKm;
}
