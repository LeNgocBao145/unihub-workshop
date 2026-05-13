package org.unihubworkshop.authservice.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.unihubworkshop.authservice.enums.Role;
import org.unihubworkshop.authservice.models.User;
import org.unihubworkshop.authservice.services.JwtService;


import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class TestTokenController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // TODO: Inject class tạo Token của bạn vào đây (JwtService, JwtUtils, v.v...)
    @Autowired
    private JwtService jwtService;

    @GetMapping("/export-tokens")
    public ResponseEntity<String> exportTokens() {
        try {
            // Lấy 60 user từ database (dựa vào email dạng số@gmail.com)
            String sql = "SELECT u.id as user_id, u.name, a.email, u.role " +
                         "FROM users u JOIN accounts a ON u.id = a.user_id " +
                         "WHERE a.email SIMILAR TO '[0-9]+@gmail.com' " +
                         "ORDER BY CAST(SPLIT_PART(a.email, '@', 1) AS INTEGER)";

            List<Map<String, Object>> users = jdbcTemplate.queryForList(sql);
            StringBuilder csv = new StringBuilder();

            for (Map<String, Object> user : users) {
                String userId = user.get("user_id").toString();
                String email = user.get("email").toString();
                String name = user.get("name").toString();
                

                int index = Integer.parseInt(email.split("@")[0]);
                
                // Giả lập mã số sinh viên cho 60 user (23127001 -> 23127060)
                String studentCode = String.format("%d", index);


               String token = jwtService.generateToken(User.builder().id(UUID.fromString(userId)).role(Role.ATTENDEE).build(), email);

                // Nối chuỗi theo format CSV: token,studentCode,name
                csv.append(token).append(",")
                   .append(studentCode).append(",")
                   .append(name).append("\n");
            }

            // Ghi ra file users.csv ở thư mục gốc của project backend
            Files.writeString(Path.of("users.csv"), csv.toString());

            return ResponseEntity.ok("✅ Đã xuất thành công " + users.size() + " tokens ra file users.csv!");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("❌ Lỗi: " + e.getMessage());
        }
    }
}