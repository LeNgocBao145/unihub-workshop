package org.unihubworkshop.workshopservice.mapper;

import org.springframework.stereotype.Component;
import org.unihubworkshop.workshopservice.dto.CreateWorkshopRequest;
import org.unihubworkshop.workshopservice.dto.UpdateWorkshopRequest;
import org.unihubworkshop.workshopservice.dto.WorkshopResponse;
import org.unihubworkshop.workshopservice.models.Workshop;

@Component
public class WorkshopMapper {

    public Workshop toEntity(CreateWorkshopRequest request) {
        Workshop workshop = new Workshop();
        workshop.setName(request.getName());
        workshop.setHostId(request.getHostId());
        workshop.setRoom(request.getRoom());
        workshop.setRoomMap(request.getRoomMap());
        workshop.setTotalSlots(request.getTotalSlots());
        workshop.setAvailableSlots(request.getTotalSlots());
        workshop.setDescription(request.getDescription());
        workshop.setPrice(request.getPrice());
        workshop.setType(request.getType());
        workshop.setStartAt(request.getStartAt());
        workshop.setEndAt(request.getEndAt());
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
        if (request.getTotalSlots() != null) {
            workshop.setTotalSlots(request.getTotalSlots());
        }
        if (request.getAvailableSlots() != null) {
            workshop.setAvailableSlots(request.getAvailableSlots());
        }
        if (request.getDescription() != null) {
            workshop.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            workshop.setPrice(request.getPrice());
        }
        if (request.getType() != null) {
            workshop.setType(request.getType());
        }
        if (request.getStartAt() != null) {
            workshop.setStartAt(request.getStartAt());
        }
        if (request.getEndAt() != null) {
            workshop.setEndAt(request.getEndAt());
        }
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
        return response;
    }
}
