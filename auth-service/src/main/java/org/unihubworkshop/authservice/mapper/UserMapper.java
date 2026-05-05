package org.unihubworkshop.authservice.mapper;



import org.unihubworkshop.authservice.dto.request.RegisterRequest;
import org.unihubworkshop.authservice.enums.Role;
import org.unihubworkshop.authservice.models.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    private final PasswordEncoder passwordEncoder;

    public UserMapper(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public User toEntity(RegisterRequest request) {
        return User.builder()
                .name(request.getName())
                .role(Role.ATTENDEE) // Set role mặc định
                .build();
    }
}