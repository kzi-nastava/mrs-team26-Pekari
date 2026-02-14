package com.pekara.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebBlockUserRequest {

    @NotNull(message = "blocked is required")
    private Boolean blocked;

    private String blockedNote;
}
