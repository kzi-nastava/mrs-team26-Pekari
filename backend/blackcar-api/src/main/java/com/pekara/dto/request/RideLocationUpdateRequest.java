package com.pekara.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideLocationUpdateRequest {
    private Double latitude;
    private Double longitude;
    private Double heading;
    private Double speed;
    private LocalDateTime recordedAt;
}
