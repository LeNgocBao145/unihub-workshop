package org.unihubworkshop.paymentservice.event;

import java.io.Serializable;
import java.util.UUID;

public record PaymentStatusUpdatedEvent(
        UUID registrationId,
        String userEmail,
        UUID paymentId
) implements Serializable {}

