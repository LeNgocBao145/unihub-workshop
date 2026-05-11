package org.unihubworkshop.workshopservice.events;

import java.util.UUID;

/**
 * Event published by payment service when payment status is updated to SUCCESS.
 * This event is consumed by workshop service to:
 * - Update registration status to CONFIRMED
 * - Delete cached QR code using paymentId
 */
public record PaymentStatusUpdatedEvent(
        UUID registrationId,
        String userEmail,
        UUID paymentId
) {
    /**
     * Creates a PaymentStatusUpdatedEvent.
     *
     * @param registrationId Unique identifier of the registration
     * @param userEmail Email of the user
     * @param paymentId Payment ID for cache cleanup
     */
    public PaymentStatusUpdatedEvent {
        if (registrationId == null) {
            throw new IllegalArgumentException("registrationId cannot be null");
        }
        if (userEmail == null || userEmail.isBlank()) {
            throw new IllegalArgumentException("userEmail cannot be null or blank");
        }
        if (paymentId == null) {
            throw new IllegalArgumentException("paymentId cannot be null");
        }
    }
}

