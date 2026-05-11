package org.unihubworkshop.workshopservice.mapper;

import org.springframework.stereotype.Component;
import org.unihubworkshop.workshopservice.dto.RegistrationResponse;
import org.unihubworkshop.workshopservice.dto.TicketDetailResponse;
import org.unihubworkshop.workshopservice.models.Registration;

@Component
public class RegistrationMapper {

    public RegistrationResponse toResponse(Registration registration) {
        RegistrationResponse dto = new RegistrationResponse();
        dto.setCreatedAt(registration.getCreatedAt());
        dto.setExpiresAt(registration.getExpiresAt());
        dto.setId(registration.getId());
        dto.setIsPresent(registration.getIsPresent());
        dto.setStatus(registration.getStatus());
        dto.setUserId(registration.getUserId());
        dto.setUpdatedAt(registration.getUpdatedAt());
        dto.setWorkshopId(registration.getWorkshopId());

        return dto;
    }
    
    public TicketDetailResponse toDetailResponse(Registration registration) {
        if (registration == null) {
            return null;
        }

        TicketDetailResponse.TicketDetailResponseBuilder builder = TicketDetailResponse.builder()
                .id(registration.getId())
                .workshopId(registration.getWorkshopId())
                .userId(registration.getUserId())
                .status(registration.getStatus())
                .isPresent(registration.getIsPresent())
                .expiresAt(registration.getExpiresAt())
                .createdAt(registration.getCreatedAt())
                .updatedAt(registration.getUpdatedAt());

        if (registration.getWorkshop() != null) {
            builder.workshopName(registration.getWorkshop().getName());
            if (registration.getWorkshop().getType() != null) {
                builder.typeWorkshop(registration.getWorkshop().getType().name());
            }
        }

        if (registration.getUser() != null) {
            builder.userName(registration.getUser().getName());
        }

        return builder.build();
    }
}
