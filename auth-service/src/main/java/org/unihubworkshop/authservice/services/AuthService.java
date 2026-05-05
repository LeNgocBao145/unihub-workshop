package org.unihubworkshop.authservice.services;

import org.unihubworkshop.authservice.dto.request.LoginRequest;
import org.unihubworkshop.authservice.dto.request.RegisterRequest;
import org.unihubworkshop.authservice.dto.response.LoginBeforeResponse;

import org.unihubworkshop.authservice.dto.response.RefreshTokenResponse;
import org.unihubworkshop.authservice.dto.response.RegisterResponse;

public interface AuthService {
    LoginBeforeResponse login(LoginRequest request);
    RefreshTokenResponse refreshToken(String request);
    RegisterResponse register(RegisterRequest request);
    void logout(String request);
}
