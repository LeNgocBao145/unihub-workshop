package org.unihubworkshop.paymentservice.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

public record PaymentStatusUpdatedEvent(
        UUID paymentId,
        UUID registrationId,
        String status,
        BigDecimal amount
) implements Serializable {}

