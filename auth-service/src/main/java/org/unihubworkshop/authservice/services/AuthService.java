package org.unihubworkshop.authservice.services;

import org.unihubworkshop.authservice.dto.request.LoginRequest;
import org.unihubworkshop.authservice.dto.request.LogoutRequest;
import org.unihubworkshop.authservice.dto.request.RefreshTokenRequest;
import org.unihubworkshop.authservice.dto.request.RegisterRequest;
import org.unihubworkshop.authservice.dto.response.LoginResponse;
import org.unihubworkshop.authservice.dto.response.RefreshTokenResponse;
import org.unihubworkshop.authservice.dto.response.RegisterResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    RefreshTokenResponse refreshToken(RefreshTokenRequest request);
    RegisterResponse register(RegisterRequest request);
    void logout(LogoutRequest request);
}
