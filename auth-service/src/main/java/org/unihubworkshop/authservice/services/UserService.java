package org.unihubworkshop.authservice.services;


import org.unihubworkshop.authservice.dto.response.UserProfileResponse;

import java.util.UUID;

public interface UserService {
    // Định nghĩa hàm lấy thông tin profile dựa vào ID
    UserProfileResponse getUserProfile(UUID userId);
}
