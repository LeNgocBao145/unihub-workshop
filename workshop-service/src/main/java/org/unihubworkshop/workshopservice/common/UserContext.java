package org.unihubworkshop.workshopservice.common;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.UUID;

/**
 * Request-scoped component to hold current user information
 * Extracted from JWT token by API Gateway and passed via headers
 */
@Component
@RequestScope
public class UserContext {
    
    private UUID userId;
    private String userEmail;
    private String userRole;
    
    public UUID getUserId() {
        return userId;
    }
    
    public void setUserId(UUID userId) {
        this.userId = userId;
    }
    
    public void setUserId(String userId) {
        this.userId = UUID.fromString(userId);
    }
    
    public String getUserEmail() {
        return userEmail;
    }
    
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
    
    public String getUserRole() {
        return userRole;
    }
    
    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }
}

