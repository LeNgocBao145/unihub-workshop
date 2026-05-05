package org.unihubworkshop.authservice.config;



import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. Tắt tính năng bảo vệ CSRF (vì chúng ta dùng JWT nên không cần thiết và thường gây lỗi 403/401 khi test Postman)
                .csrf(AbstractHttpConfigurer::disable)

                // 2. Cấu hình phân quyền các đường dẫn (URL)
                .authorizeHttpRequests(auth -> auth
                        // MỞ CỬA TỰ DO: Ai cũng có thể vào các API này
                        .anyRequest().permitAll()


                );

        return http.build();
    }
}
