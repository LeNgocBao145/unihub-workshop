package org.unihubworkshop.workshopservice.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.unihubworkshop.workshopservice.common.UserContext;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Interceptor to extract user information from request headers
 * Headers are set by API Gateway after validating JWT token
 */
@Component
public class UserContextInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(UserContextInterceptor.class);
    
    private static final String X_USER_ID_HEADER = "X-User-Id";
    private static final String X_USER_EMAIL_HEADER = "X-User-Email";
    private static final String X_USER_ROLE_HEADER = "X-User-Role";
    
    private final UserContext userContext;
    
    public UserContextInterceptor(UserContext userContext) {
        this.userContext = userContext;
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String userId = request.getHeader(X_USER_ID_HEADER);
        String userEmail = request.getHeader(X_USER_EMAIL_HEADER);
        String userRole = request.getHeader(X_USER_ROLE_HEADER);
        
        if (userId != null) {
            log.debug("Setting user context from headers. UserId: {}, Email: {}", userId, userEmail);
            userContext.setUserId(userId);
        }
        
        if (userEmail != null) {
            userContext.setUserEmail(userEmail);
        }
        
        if (userRole != null) {
            userContext.setUserRole(userRole);
        }
        
        return true;
    }
}

