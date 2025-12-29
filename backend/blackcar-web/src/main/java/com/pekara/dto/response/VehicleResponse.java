package com.pekara.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleResponse {

    private String id;
    private String make;
    private String model;
    private Integer year;
    private String licensePlate;
    private String vin;
}
