package org.unihubworkshop.workshopservice.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.unihubworkshop.workshopservice.dto.CreateWorkshopRequest;
import org.unihubworkshop.workshopservice.dto.UpdateWorkshopRequest;
import org.unihubworkshop.workshopservice.dto.WorkshopResponse;
import org.unihubworkshop.workshopservice.exceptions.InvalidWorkshopException;
import org.unihubworkshop.workshopservice.exceptions.ResourceNotFoundException;
import org.unihubworkshop.workshopservice.mapper.WorkshopMapper;
import org.unihubworkshop.workshopservice.models.Workshop;
import org.unihubworkshop.workshopservice.repositories.WorkshopRepository;
import java.util.List;
import java.util.UUID;
@Service
@Transactional
public class WorkshopService {
    private final WorkshopRepository workshopRepository;
    private final WorkshopMapper workshopMapper;
    private static final String WORKSHOP_NOT_FOUND = "Workshop with ID %s not found";

    public WorkshopService(WorkshopRepository workshopRepository,
                           WorkshopMapper workshopMapper){
        this.workshopMapper = workshopMapper;
        this.workshopRepository = workshopRepository;
    }

    public WorkshopResponse createWorkshop(CreateWorkshopRequest request) {
        validateSlots(request.getTotalSlots(), null);
        Workshop workshop = workshopMapper.toEntity(request);
        Workshop savedWorkshop = workshopRepository.save(workshop);
        return workshopMapper.toResponse(savedWorkshop);
    }
    @Transactional(readOnly = true)
    public WorkshopResponse getWorkshopById(UUID id) {
        Workshop workshop = findWorkshopById(id);
        return workshopMapper.toResponse(workshop);
    }
    @Transactional(readOnly = true)
    public List<WorkshopResponse> getAllWorkshops() {
        return workshopRepository.findAll()
                .stream()
                .map(workshopMapper::toResponse)
                .toList();
    }
    @Transactional(readOnly = true)
    public List<WorkshopResponse> getWorkshopsByHostId(UUID hostId) {
        return workshopRepository.findByHostId(hostId)
                .stream()
                .map(workshopMapper::toResponse)
                .toList();
    }
    @Transactional(readOnly = true)
    public List<WorkshopResponse> searchWorkshopsByName(String name) {
        return workshopRepository.findByNameContainingIgnoreCase(name)
                .stream()
                .map(workshopMapper::toResponse)
                .toList();
    }
    @Transactional(readOnly = true)
    public List<WorkshopResponse> getWorkshopsByRoom(String room) {
        return workshopRepository.findByRoom(room)
                .stream()
                .map(workshopMapper::toResponse)
                .toList();
    }
    public WorkshopResponse updateWorkshop(UUID id, UpdateWorkshopRequest request) {
        Workshop workshop = findWorkshopById(id);
        if (request.getTotalSlots() != null) {
            validateSlots(request.getTotalSlots(), request.getAvailableSlots());
        }
        workshopMapper.updateEntityFromRequest(request, workshop);
        Workshop updatedWorkshop = workshopRepository.save(workshop);
        return workshopMapper.toResponse(updatedWorkshop);
    }
    public void deleteWorkshop(UUID id) {
        findWorkshopById(id);
        workshopRepository.deleteById(id);
    }
    private Workshop findWorkshopById(UUID id) {
        return workshopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(WORKSHOP_NOT_FOUND, id)));
    }
    private void validateSlots(Integer totalSlots, Integer availableSlots) {
        if (totalSlots <= 0) {
            throw new InvalidWorkshopException("Total slots must be greater than zero");
        }
        if (availableSlots != null && availableSlots > totalSlots) {
            throw new InvalidWorkshopException("Available slots cannot exceed total slots");
        }
    }
}