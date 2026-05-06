package org.unihubworkshop.paymentservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.unihubworkshop.paymentservice.models.Payment;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByBankReferenceCode(String bankReferenceCode);
    Optional<Payment> findByProviderTransactionId(String providerTransactionId);
    Optional<Payment> findByRegistrationId(UUID registrationId);
}

