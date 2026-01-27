package com.pekara.dto.request;

import com.pekara.dto.common.WebLocationPoint;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebStopRideEarlyRequest {

    @NotNull(message = "Stop location is required")
    @Valid
    private WebLocationPoint stopLocation;
}
