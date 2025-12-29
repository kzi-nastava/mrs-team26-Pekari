package com.pekara.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActiveVehicleResponse {

    private Long vehicleId;
    private String vehicleType;
    private String licensePlate;
    private Double latitude;
    private Double longitude;
    private Boolean isBusy;
    private String status; // FREE, BUSY, IN_RIDE

    private DriverBasicInfo driver;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DriverBasicInfo {
        private Long id;
        private String firstName;
        private String lastName;
    }
}
