package com.pekara.service;

import com.pekara.dto.PricingDto;
import com.pekara.model.Pricing;
import com.pekara.repository.PricingRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PricingServiceImpl implements PricingService {

    private final PricingRepository pricingRepository;

    @PostConstruct
    public void initDefaultPricing() {
        if (pricingRepository.count() == 0) {
            log.info("Initializing default pricing");
            pricingRepository.save(Pricing.builder()
                    .vehicleType("STANDARD")
                    .basePrice(new BigDecimal("200"))
                    .pricePerKm(new BigDecimal("120"))
                    .build());
            pricingRepository.save(Pricing.builder()
                    .vehicleType("VAN")
                    .basePrice(new BigDecimal("300"))
                    .pricePerKm(new BigDecimal("120"))
                    .build());
            pricingRepository.save(Pricing.builder()
                    .vehicleType("LUX")
                    .basePrice(new BigDecimal("500"))
                    .pricePerKm(new BigDecimal("120"))
                    .build());
        }
    }

    @Override
    public List<PricingDto> getAllPricing() {
        return pricingRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public PricingDto getPricingByVehicleType(String vehicleType) {
        return pricingRepository.findById(vehicleType.toUpperCase())
                .map(this::mapToDto)
                .orElseGet(() -> {
                    log.warn("Pricing not found for vehicle type: {}, returning default STANDARD pricing", vehicleType);
                    return pricingRepository.findById("STANDARD")
                            .map(this::mapToDto)
                            .orElse(PricingDto.builder()
                                    .vehicleType("STANDARD")
                                    .basePrice(new BigDecimal("200"))
                                    .pricePerKm(new BigDecimal("120"))
                                    .build());
                });
    }

    @Override
    @Transactional
    public PricingDto updatePricing(PricingDto pricingDto) {
        Pricing pricing = pricingRepository.findById(pricingDto.getVehicleType().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Pricing not found for vehicle type: " + pricingDto.getVehicleType()));
        
        pricing.setBasePrice(pricingDto.getBasePrice());
        pricing.setPricePerKm(pricingDto.getPricePerKm());
        
        Pricing saved = pricingRepository.save(pricing);
        return mapToDto(saved);
    }

    private PricingDto mapToDto(Pricing pricing) {
        return PricingDto.builder()
                .vehicleType(pricing.getVehicleType())
                .basePrice(pricing.getBasePrice())
                .pricePerKm(pricing.getPricePerKm())
                .build();
    }
}
