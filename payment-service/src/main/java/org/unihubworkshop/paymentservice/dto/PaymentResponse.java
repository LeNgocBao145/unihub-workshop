package org.unihubworkshop.paymentservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID registrationId,
        BigDecimal amount,
        String provider,
        String gateway,
        String providerTransactionId,
        String bankReferenceCode,
        String actualContent,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}

