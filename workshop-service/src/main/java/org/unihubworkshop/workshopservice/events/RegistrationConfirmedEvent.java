package org.unihubworkshop.workshopservice.events;

import java.util.List;
import java.util.UUID;

/**
 * Event published when a user successfully registers for a workshop.
 * This event is consumed by the notification service to send confirmation emails/notifications.
 */
public record RegistrationConfirmedEvent(
        UUID registrationId,
        UUID workshopId,
        UUID userId,
        String userEmail,
        String workshopName,
        List<String> speakerNames,
        String room,
        String roomMap
) {
    /**
     * Creates a RegistrationConfirmedEvent.
     *
     * @param registrationId Unique identifier of the registration
     * @param workshopId Unique identifier of the workshop
     * @param userId Unique identifier of the user
     * @param userEmail Email address of the user
     * @param workshopName Name of the workshop
     * @param speakerNames List of speaker names
     * @param room Room location of the workshop
     * @param roomMap URL to the room map
     */
    public RegistrationConfirmedEvent {
        // Validation can be added here if needed
        if (registrationId == null || workshopId == null || userId == null || userEmail == null) {
            throw new IllegalArgumentException("registrationId, workshopId, userId, and userEmail cannot be null");
        }
    }
}

