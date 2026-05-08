package org.unihubworkshop.workshopservice.mapper;

import org.springframework.stereotype.Component;
import org.unihubworkshop.workshopservice.dto.CreateWorkshopRequest;
import org.unihubworkshop.workshopservice.dto.UpdateWorkshopRequest;
import org.unihubworkshop.workshopservice.dto.WorkshopResponse;
import org.unihubworkshop.workshopservice.dto.WorkshopSimpleResponse;
import org.unihubworkshop.workshopservice.dto.WorkshopPaymentResponse;
import org.unihubworkshop.workshopservice.models.RegistrationStatus;
import org.unihubworkshop.workshopservice.models.Workshop;

import java.util.UUID;

@Component
public class WorkshopMapper {

    public Workshop toEntity(CreateWorkshopRequest request) {
        Workshop workshop = new Workshop();
        workshop.setName(request.getName());
        workshop.setRoom(request.getRoom());
        workshop.setTotalSlots(request.getTotalSlots());
        workshop.setAvailableSlots(request.getTotalSlots());
        workshop.setDescription(request.getDescription());
        workshop.setPrice(request.getPrice());
        workshop.setType(request.getType());
        workshop.setStartAt(request.getStartAt());
        workshop.setEndAt(request.getEndAt());
        workshop.setSpeaker(request.getSpeaker());
        return workshop;
    }

    public void updateEntityFromRequest(UpdateWorkshopRequest request, Workshop workshop) {
        if (request.getName() != null) {
            workshop.setName(request.getName());
        }
        if (request.getRoom() != null) {
            workshop.setRoom(request.getRoom());
        }
        if (request.getRoomMap() != null) {
            workshop.setRoomMap(request.getRoomMap());
        }

        if (request.getDescription() != null) {
            workshop.setDescription(request.getDescription());
        }

        if (request.getSpeaker() != null) {
            workshop.setSpeaker(request.getSpeaker());
        }
    }

    public WorkshopResponse toDetailWorkshopResponse(
            Workshop workshop,
            RegistrationStatus registrationStatus
    ) {

        WorkshopResponse response = new WorkshopResponse();
        response.setId(workshop.getId());
        response.setName(workshop.getName());
        response.setHostId(workshop.getHostId());
        response.setRoom(workshop.getRoom());
        response.setRoomMap(workshop.getRoomMap());
        response.setTotalSlots(workshop.getTotalSlots());
        response.setAvailableSlots(workshop.getAvailableSlots());
        response.setDescription(workshop.getDescription());
        response.setPrice(workshop.getPrice());
        response.setType(workshop.getType());
        response.setStartAt(workshop.getStartAt());
        response.setEndAt(workshop.getEndAt());
        response.setCreatedAt(workshop.getCreatedAt());
        response.setUpdatedAt(workshop.getUpdatedAt());
        response.setSpeaker(workshop.getSpeaker());
        response.setRegistrationStatus(registrationStatus);
        return response;

    }

    public WorkshopResponse toResponse(Workshop workshop) {
        WorkshopResponse response = new WorkshopResponse();
        response.setId(workshop.getId());
        response.setName(workshop.getName());
        response.setHostId(workshop.getHostId());
        response.setRoom(workshop.getRoom());
        response.setRoomMap(workshop.getRoomMap());
        response.setTotalSlots(workshop.getTotalSlots());
        response.setAvailableSlots(workshop.getAvailableSlots());
        response.setDescription(workshop.getDescription());
        response.setPrice(workshop.getPrice());
        response.setType(workshop.getType());
        response.setStartAt(workshop.getStartAt());
        response.setEndAt(workshop.getEndAt());
        response.setCreatedAt(workshop.getCreatedAt());
        response.setUpdatedAt(workshop.getUpdatedAt());
        response.setSpeaker(workshop.getSpeaker());
        return response;
    }



    public WorkshopSimpleResponse toSimpleResponse(Workshop workshop) {
        WorkshopSimpleResponse response = new WorkshopSimpleResponse();
        response.setId(workshop.getId());
        response.setName(workshop.getName());
        response.setAvailableSlots(workshop.getAvailableSlots());
        response.setTotalSlots(workshop.getTotalSlots());
        return response;
    }
    
    public WorkshopPaymentResponse toPaymentResponse(Workshop workshop) {
        WorkshopPaymentResponse response = new WorkshopPaymentResponse();
        response.setId(workshop.getId());
        response.setName(workshop.getName());
        response.setPrice(workshop.getPrice());
        return response;
    }
}
