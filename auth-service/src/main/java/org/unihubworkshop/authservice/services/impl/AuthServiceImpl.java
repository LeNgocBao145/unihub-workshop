package org.unihubworkshop.authservice.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.unihubworkshop.authservice.dto.request.LoginRequest;
import org.unihubworkshop.authservice.dto.request.LogoutRequest;
import org.unihubworkshop.authservice.dto.request.RefreshTokenRequest;
import org.unihubworkshop.authservice.dto.request.RegisterRequest;
import org.unihubworkshop.authservice.dto.response.LoginResponse;
import org.unihubworkshop.authservice.dto.response.RefreshTokenResponse;
import org.unihubworkshop.authservice.dto.response.RegisterResponse;
import org.unihubworkshop.authservice.enums.Provider;
import org.unihubworkshop.authservice.enums.Role;
import org.unihubworkshop.authservice.mapper.UserMapper;
import org.unihubworkshop.authservice.models.Account;
import org.unihubworkshop.authservice.models.RefreshToken;
import org.unihubworkshop.authservice.models.User;
import org.unihubworkshop.authservice.repositories.AccountRepository;
import org.unihubworkshop.authservice.repositories.RefreshTokenRepository;
import org.unihubworkshop.authservice.repositories.UserRepository;
import org.unihubworkshop.authservice.services.AuthService;
import org.unihubworkshop.authservice.services.JwtService;
import org.unihubworkshop.authservice.services.RefreshTokenService;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService; // Bổ sung cái này
    private final RefreshTokenRepository refreshTokenRepository;
    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {

        Account account = accountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản!"));

        // 2. Kiểm tra xem tài khoản này có phải là tài khoản LOCAL không
        // (Phòng trường hợp user đăng nhập bằng Google nhưng lại cố tình dùng form login mật khẩu)
        if (account.getProvider() != Provider.CREDENTIALS) {
            throw new RuntimeException("Vui lòng đăng nhập bằng phương thức " + account.getProvider());
        }

        // 3. Kiểm tra mật khẩu
        if (!passwordEncoder.matches(request.getPassword(), account.getHashPassword())) {
            throw new RuntimeException("Sai mật khẩu!");
        }

        // 4. Lấy thông tin User từ Account
        User user = account.getUser();

        // 5. Tạo Access Token (JWT)
        String accessToken = jwtService.generateToken(user);

        // 6. Xử lý Refresh Token
        refreshTokenRepository.deleteByUser(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .message("Đăng nhập thành công")
                .build();
    }

    @Override
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenRepository.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration) // Kiểm tra hết hạn chưa
                .map(RefreshToken::getUser) // Nếu ok, lấy User ra
                .map(user -> {
                    // Tạo Access Token mới
                    String newAccessToken = jwtService.generateToken(user);
                    return RefreshTokenResponse.builder()
                            .accessToken(newAccessToken)
                            .message("Làm mới token thành công")
                            .build();
                })
                .orElseThrow(() -> new RuntimeException("Refresh token không tồn tại trong hệ thống!"));
    }

    @Override
    @Transactional // RẤT QUAN TRỌNG: Đảm bảo lưu thành công cả 2 bảng hoặc không lưu gì cả
    public RegisterResponse register(RegisterRequest request) {
        // 1. Kiểm tra xem email đã tồn tại trong bảng accounts chưa
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng!");
        }

        // 2. Tạo entity User trước
        User user = User.builder()
                .name(request.getName())
                .role(Role.ATTENDEE)
                .build();

        // Lưu User vào DB để lấy được UUID
        user = userRepository.save(user);

        // 3. Tạo entity Account và liên kết với User vừa tạo
        Account account = Account.builder()
                .user(user)
                .email(request.getEmail())
                .hashPassword(passwordEncoder.encode(request.getPassword()))
                .provider(Provider.CREDENTIALS) // Đánh dấu đây là tài khoản đăng nhập bằng mật khẩu
                .build();

        accountRepository.save(account);

        return RegisterResponse.builder()
                .message("Đăng ký tài khoản thành công!")
                .build();
    }
    @Override
    public void logout(LogoutRequest request) {
        // Tìm và xóa Refresh Token khỏi Database
        refreshTokenRepository.findByToken(request.getRefreshToken())
                .ifPresent(refreshTokenRepository::delete);
    }
}
