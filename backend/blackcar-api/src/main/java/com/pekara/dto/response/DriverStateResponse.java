package com.pekara.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverStateResponse {
    private Long driverId;
    private String driverEmail;
    private Boolean online;
    private Boolean busy;
    private Double latitude;
    private Double longitude;
    private LocalDateTime updatedAt;
}
