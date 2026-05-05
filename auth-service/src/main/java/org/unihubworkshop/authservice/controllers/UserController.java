package org.unihubworkshop.authservice.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.unihubworkshop.authservice.dto.response.UserProfileResponse;
import org.unihubworkshop.authservice.services.UserService;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService; // Có thể gọi qua UserService nếu hệ thống phức tạp

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(
            @RequestHeader("X-User-Id") UUID userId
    ) {


        return ResponseEntity.ok(userService.getUserProfile(userId));
    }
}
