package org.unihubworkshop.authservice.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.unihubworkshop.authservice.dto.response.UserProfileResponse;
import org.unihubworkshop.authservice.models.Account;
import org.unihubworkshop.authservice.models.User;
import org.unihubworkshop.authservice.repositories.AccountRepository;
import org.unihubworkshop.authservice.repositories.UserRepository;
import org.unihubworkshop.authservice.services.UserService;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    @Override
    public UserProfileResponse getUserProfile(UUID userId) {
        // 1. Tìm user trong Database
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + userId));
        Account account = accountRepository.findFirstByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản liên kết!"));
        // 2. Map từ Entity sang DTO để giấu Password đi
        return UserProfileResponse.builder()
                .name(user.getName())
                .email(account.getEmail())
                .role(user.getRole().toString())
                .build();
    }
}
