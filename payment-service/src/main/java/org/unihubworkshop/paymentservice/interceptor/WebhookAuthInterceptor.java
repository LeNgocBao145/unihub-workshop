package org.unihubworkshop.paymentservice.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class WebhookAuthInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(WebhookAuthInterceptor.class);
    
    @Value("${sepay.webhook.api-key}")
    private String webhookApiKey;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Apikey ")) {
            log.warn("Webhook request without valid Authorization header");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Unauthorized\"}");
            return false;
        }
        
        String apiKey = authHeader.substring(7);
        
        if (!webhookApiKey.equals(apiKey)) {
            log.warn("Webhook request with invalid API key");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\":\"Invalid API key\"}");
            return false;
        }
        
        return true;
    }
}
