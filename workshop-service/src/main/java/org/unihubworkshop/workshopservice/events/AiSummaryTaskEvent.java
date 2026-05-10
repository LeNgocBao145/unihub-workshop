package org.unihubworkshop.workshopservice.events;


import java.util.UUID;

public record AiSummaryTaskEvent(
        UUID workshopId,
        String fileUrl
) {}