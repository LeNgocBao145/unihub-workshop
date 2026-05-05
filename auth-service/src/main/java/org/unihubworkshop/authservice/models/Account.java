package org.unihubworkshop.authservice.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.unihubworkshop.authservice.enums.Provider;
import org.unihubworkshop.authservice.enums.Role;

import java.util.UUID;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Liên kết Many-to-One: Nhiều Account có thể thuộc về 1 User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;


    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private Provider provider;


    // Sẽ null nếu là tài khoản LOCAL, sẽ chứa ID của Google/Facebook nếu là mạng xã hội
    @Column(name = "provider_id")
    private String providerId;

    // Đổi lại cho đúng chuẩn naming convention của Java (hashPassword thay vì hash_password)
    // Sẽ null nếu người dùng đăng nhập bằng Google/Facebook
    @Column(name = "hashed_password")
    private String hashPassword;

    @Column(nullable = false, unique = true)
    private String email;
}