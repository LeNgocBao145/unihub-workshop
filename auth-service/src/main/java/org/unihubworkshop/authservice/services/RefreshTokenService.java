package org.unihubworkshop.authservice.services;


import org.unihubworkshop.authservice.models.RefreshToken;

import java.util.UUID;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(UUID userId);
    RefreshToken verifyExpiration(RefreshToken token);
}