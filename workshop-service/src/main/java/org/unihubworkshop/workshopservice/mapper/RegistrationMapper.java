package org.unihubworkshop.workshopservice.mapper;

import org.springframework.stereotype.Component;
import org.unihubworkshop.workshopservice.dto.RegistrationResponse;
import org.unihubworkshop.workshopservice.models.Registration;
import org.unihubworkshop.workshopservice.dto.RegistrationResponse;
import org.unihubworkshop.workshopservice.models.User;
import org.unihubworkshop.workshopservice.models.Workshop;


@Component
public class RegistrationMapper {

    public RegistrationResponse toResponse(Registration registration) {
        RegistrationResponse dto = new RegistrationResponse();
        Workshop w = registration.getWorkshop();
        User u = registration.getUser();
        dto.setCreatedAt(registration.getCreatedAt());
        dto.setExpiresAt(registration.getExpiresAt());
        dto.setId(registration.getId());
        dto.setIsPresent(registration.getIsPresent());
        dto.setStatus(registration.getStatus());
        dto.setUserId(u.getId());
        dto.setUpdatedAt(registration.getUpdatedAt());
        dto.setWorkshopId(w.getId());
        dto.setWorkshopName(w.getName());
        dto.setType(w.getType());
        dto.setStatus(dto.getStatus());
        dto.setUserName(u.getName());
        return dto;
    }
}
