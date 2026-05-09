package org.unihubworkshop.workshopservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.unihubworkshop.workshopservice.interceptor.UserContextInterceptor;

/**
 * Web configuration for interceptors and other mvc settings
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    private final UserContextInterceptor userContextInterceptor;
    
    public WebConfig(UserContextInterceptor userContextInterceptor) {
        this.userContextInterceptor = userContextInterceptor;
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userContextInterceptor);
    }
}

