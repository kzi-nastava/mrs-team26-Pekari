package com.pekara.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebUserListItemResponse {

    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private Boolean blocked;
    private String blockedNote;
}
