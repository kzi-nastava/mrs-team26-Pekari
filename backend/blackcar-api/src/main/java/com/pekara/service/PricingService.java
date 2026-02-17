package com.pekara.service;

import com.pekara.dto.PricingDto;
import java.util.List;

public interface PricingService {
    List<PricingDto> getAllPricing();
    PricingDto getPricingByVehicleType(String vehicleType);
    PricingDto updatePricing(PricingDto pricingDto);
}
