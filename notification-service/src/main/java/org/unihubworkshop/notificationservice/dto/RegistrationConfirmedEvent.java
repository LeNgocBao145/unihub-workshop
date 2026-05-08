package org.unihubworkshop.notificationservice.dto;

import java.util.List;
import java.util.UUID;

public record RegistrationConfirmedEvent(
        UUID registrationId,
        UUID workshopId,
        UUID userId,
        String userEmail,
        String workshopName,
        List<String> speakerNames,
        String room,
        String roomMap
){}
