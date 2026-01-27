package com.pekara.dto.request;

import com.pekara.dto.common.WebLocationPoint;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebCreateFavoriteRouteRequest {

    private String name;

    @NotNull(message = "Pickup location is required")
    @Valid
    private WebLocationPoint pickup;

    private List<@Valid WebLocationPoint> stops;

    @NotNull(message = "Dropoff location is required")
    @Valid
    private WebLocationPoint dropoff;

    private String vehicleType;

    private Boolean babyTransport;

    private Boolean petTransport;
}
