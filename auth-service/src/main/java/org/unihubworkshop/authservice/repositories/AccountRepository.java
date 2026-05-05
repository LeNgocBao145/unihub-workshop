package org.unihubworkshop.authservice.repositories;



import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.unihubworkshop.authservice.models.Account;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {
    // Hàm cốt lõi để tìm kiếm lúc đăng nhập
    Optional<Account> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<Account> findFirstByUserId(UUID userId);
}
