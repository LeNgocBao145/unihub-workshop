package org.unihubworkshop.paymentservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.unihubworkshop.paymentservice.models.Payment;
import org.unihubworkshop.paymentservice.models.PaymentStatus;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByBankReferenceCode(String bankReferenceCode);
    Optional<Payment> findByProviderTransactionId(String providerTransactionId);
    Optional<Payment> findByRegistrationId(UUID registrationId);
    
    @Query("SELECT p FROM Payment p WHERE p.registrationId = :registrationId " +
           "AND p.status = :status " +
           "AND (p.expiredAt IS NULL OR p.expiredAt > :now) " +
           "ORDER BY p.createdAt DESC LIMIT 1")
    Optional<Payment> findValidPendingPayment(
            @Param("registrationId") UUID registrationId,
            @Param("status") PaymentStatus status,
            @Param("now") LocalDateTime now
    );

    Optional<Payment> findByRegistrationIdAndStatus(UUID registrationId, PaymentStatus status);
}

