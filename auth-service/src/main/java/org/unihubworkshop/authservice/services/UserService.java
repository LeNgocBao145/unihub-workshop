package org.unihubworkshop.authservice.services;


import org.unihubworkshop.authservice.dto.response.UserProfileResponse;

import java.util.UUID;

public interface UserService {
    UserProfileResponse getUserProfile(UUID userId);
}
