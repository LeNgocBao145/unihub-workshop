package org.unihubworkshop.workshopservice.events;

import java.util.UUID;

/**
 * Event published by payment service when payment status is updated.
 * This event is consumed by workshop service to update registration status to CONFIRMED.
 */
public record PaymentStatusUpdatedEvent(
        UUID registrationId,
        String userEmail
) {
    /**
     * Creates a PaymentStatusUpdatedEvent.
     *
     * @param registrationId Unique identifier of the registration
     * @param userEmail Email of the user
     */
    public PaymentStatusUpdatedEvent {
        if (registrationId == null) {
            throw new IllegalArgumentException("registrationId cannot be null");
        }
        if (userEmail == null || userEmail.isBlank()) {
            throw new IllegalArgumentException("userEmail cannot be null or blank");
        }
    }
}

