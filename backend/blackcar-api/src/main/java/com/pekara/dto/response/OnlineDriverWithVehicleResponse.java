package com.pekara.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnlineDriverWithVehicleResponse {
    private DriverStateResponse driverState;
    private String vehicleRegistration;
    private String vehicleType;
}
