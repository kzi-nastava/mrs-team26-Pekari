package com.pekara.dto.request;

import com.pekara.dto.common.LocationPointDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstimateRideRequest {
    private LocationPointDto pickup;
    private LocationPointDto dropoff;
    private String vehicleType;
    private Boolean babyTransport;
    private Boolean petTransport;
}
