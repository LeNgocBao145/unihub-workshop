package org.unihubworkshop.authservice.dto.response;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RefreshTokenResponse {
    private String accessToken;
    private String message;
}

