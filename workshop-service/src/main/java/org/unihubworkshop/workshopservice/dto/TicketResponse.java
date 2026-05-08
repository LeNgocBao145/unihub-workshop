package org.unihubworkshop.workshopservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.unihubworkshop.workshopservice.models.RegistrationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TicketResponse {
    private UUID registrationId;
    private UUID workshopId;
    private UUID userId;
    private RegistrationStatus status;
    private String paymentId;
    private String qrCodeUrl;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}

