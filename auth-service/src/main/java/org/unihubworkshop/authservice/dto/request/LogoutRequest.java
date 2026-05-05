package org.unihubworkshop.authservice.dto.request;

import lombok.Data;

@Data
public class LogoutRequest {
    private String refreshToken;
}
