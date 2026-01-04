package com.pekara.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebRegisterDriverResponse {

    private String message;
    private String email;

    /**
     * Driver registrations are typically subject to admin review.
     * Suggested values: PENDING_APPROVAL, APPROVED, REJECTED
     */
    private String status;
}
