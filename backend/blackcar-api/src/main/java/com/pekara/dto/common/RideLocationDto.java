package com.pekara.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideLocationDto {
    private Double latitude;
    private Double longitude;
    private Double heading;
    private Double speed;
    private LocalDateTime recordedAt;
}
