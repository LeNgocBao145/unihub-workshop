package org.unihubworkshop.workshopservice.mapper;

import org.springframework.stereotype.Component;
import org.unihubworkshop.workshopservice.dto.RegistrationResponse;
import org.unihubworkshop.workshopservice.dto.StudentProfileResponse;
import org.unihubworkshop.workshopservice.models.Registration;
import org.unihubworkshop.workshopservice.dto.RegistrationResponse;



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
}
