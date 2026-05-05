package org.unihubworkshop.authservice.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginBeforeResponse {
    private String accessToken;
    private String message;
    private String refreshToken;
    private UserProfileResponse user;
}
