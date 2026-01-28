package com.pekara.dto.response;

import com.pekara.dto.common.WebLocationPoint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebFavouriteRouteResponse {

    private Long id;

    /**
     * Optional user-defined label for the route (e.g., "Home → Airport").
     */
    private String name;

    private WebLocationPoint pickup;

    /**
     * Optional intermediate stops (order matters).
     */
    private List<WebLocationPoint> stops;

    private WebLocationPoint dropoff;

    private String vehicleType;

    private Boolean babyTransport;

    private Boolean petTransport;
}
