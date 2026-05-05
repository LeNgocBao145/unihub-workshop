package org.unihubworkshop.authservice.dto.response;


import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UserProfileResponse {
    private String name;
    private String email;
    private String role;
}
