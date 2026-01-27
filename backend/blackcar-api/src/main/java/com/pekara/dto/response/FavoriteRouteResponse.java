package com.pekara.dto.response;

import com.pekara.dto.common.LocationPointDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteRouteResponse {
    private Long id;
    private String name;
    private LocationPointDto pickup;
    private List<LocationPointDto> stops;
    private LocationPointDto dropoff;
    private String vehicleType;
    private Boolean babyTransport;
    private Boolean petTransport;
}
