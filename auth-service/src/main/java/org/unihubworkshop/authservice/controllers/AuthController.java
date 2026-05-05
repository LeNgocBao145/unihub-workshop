package org.unihubworkshop.authservice.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.unihubworkshop.authservice.dto.request.LoginRequest;
import org.unihubworkshop.authservice.dto.request.RegisterRequest;
import org.unihubworkshop.authservice.dto.response.LoginBeforeResponse;
import org.unihubworkshop.authservice.dto.response.LoginResponse;
import org.unihubworkshop.authservice.dto.response.RegisterResponse;
import org.unihubworkshop.authservice.services.AuthService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        LoginBeforeResponse rp = authService.login(request);
        long refreshTokenDurationMs  = 7 * 24 * 60 * 60 * 1000L;

        ResponseCookie springCookie = ResponseCookie.from("refreshToken", rp.getRefreshToken())
                .httpOnly(true)
                .secure(true) // Set true nếu chạy HTTPS (lên production BẮT BUỘC true)
                .path("/api/auth") // TRỌNG TÂM: Chỉ gửi cookie lên API này
                .maxAge(refreshTokenDurationMs / 1000) // Tính bằng giây
                .sameSite("Strict") // Chống CSRF (Hoặc "Lax" nếu frontend ở domain khác)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, springCookie.toString())
                .body(LoginResponse.builder()
                        .user(rp.getUser())
                        .message(rp.getMessage())
                        .accessToken(rp.getAccessToken())
                        .build());
    }
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@CookieValue(name = "refreshToken", required = false) String refreshToken) {
        return ResponseEntity.ok(authService.refreshToken(refreshToken));
    }
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@CookieValue(name = "refreshToken", required = false) String refreshToken) {

        authService.logout(refreshToken);
        return ResponseEntity.ok("Đăng xuất thành công");
    }
}
