package com.pekara.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebFavouriteRouteResponse {

    private Long id;

    /**
     * Optional user-defined label for the route (e.g., "Home → Airport").
     */
    private String name;

    private String pickupLocation;

    /**
     * Optional intermediate stops (order matters).
     */
    private List<String> stops;

    private String dropoffLocation;

    private String vehicleType;

    private Boolean babyTransport;

    private Boolean petTransport;
}
