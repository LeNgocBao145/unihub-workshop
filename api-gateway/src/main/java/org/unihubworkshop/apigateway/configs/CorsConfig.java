package org.unihubworkshop.apigateway.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Cho phép gửi cookie, token
        config.setAllowCredentials(true);

        config.addAllowedOriginPattern("*");

        // Cho phép mọi header (Authorization, Content-Type...)
        config.addAllowedHeader("*");

        // Cho phép mọi method (GET, POST, PUT, DELETE, OPTIONS)
        config.addAllowedMethod("*");

        // Lưu cache kết quả preflight trong 1 giờ để FE đỡ phải gửi OPTIONS liên tục
        config.setMaxAge(3600L);
        config.addExposedHeader("*");
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Áp dụng cấu hình này cho toàn bộ đường dẫn đi qua Gateway
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}
