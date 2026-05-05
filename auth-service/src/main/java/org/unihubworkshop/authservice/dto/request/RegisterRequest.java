package org.unihubworkshop.authservice.dto.request;

import lombok.Data;

@Data
public class RegisterRequest {
    private String name;
    private String password;
    private String email;
}
