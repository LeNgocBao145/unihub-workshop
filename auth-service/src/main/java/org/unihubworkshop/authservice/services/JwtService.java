package org.unihubworkshop.authservice.services;



import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.unihubworkshop.authservice.models.User;

import java.security.Key;
import java.util.Date;

@Service
public class JwtService {

    // Lấy secret key từ application.yml (Nên dài ít nhất 256-bit)
    @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String secret;
    @Value("${jwt.expiration:86400000}") // 1 ngày
    private long jwtExpiration;

    private Key getSignInKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getId().toString())
                .claim("userId", user.getId()) // Nhét userId vào payload để API Gateway đọc
                .claim("role", user.getRole())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }
}
