package org.unihubworkshop.paymentservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.unihubworkshop.paymentservice.interceptor.WebhookAuthInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    private final WebhookAuthInterceptor webhookAuthInterceptor;
    
    public WebConfig(WebhookAuthInterceptor webhookAuthInterceptor) {
        this.webhookAuthInterceptor = webhookAuthInterceptor;
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(webhookAuthInterceptor)
                .addPathPatterns("/api/payments/webhook/**");
    }
}
