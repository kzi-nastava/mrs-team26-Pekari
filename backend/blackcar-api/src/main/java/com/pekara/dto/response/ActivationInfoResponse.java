package com.pekara.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivationInfoResponse {
    private boolean requiresPassword;
    private String role;
    private String email;
}
