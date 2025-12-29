package com.pekara.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RideHistoryFilterRequest {

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String sortBy; // e.g., "startTime", "endTime", "price"
    private String sortDirection; // "ASC" or "DESC"
}
