package org.unihubworkshop.workshopservice.services;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.unihubworkshop.workshopservice.dto.CreateWorkshopRequest;
import org.unihubworkshop.workshopservice.dto.StatisticsResponse;
import org.unihubworkshop.workshopservice.dto.UpdateWorkshopRequest;
import org.unihubworkshop.workshopservice.dto.WorkshopResponse;
import org.unihubworkshop.workshopservice.dto.WorkshopSimpleResponse;
import org.unihubworkshop.workshopservice.dto.WorkshopPaymentResponse;
import org.unihubworkshop.workshopservice.exceptions.InvalidWorkshopException;
import org.unihubworkshop.workshopservice.exceptions.ResourceNotFoundException;
import org.unihubworkshop.workshopservice.mapper.WorkshopMapper;
import org.unihubworkshop.workshopservice.models.Workshop;
import org.unihubworkshop.workshopservice.repositories.WorkshopRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.unihubworkshop.workshopservice.exceptions.NotFoundException;
import org.unihubworkshop.workshopservice.exceptions.AccessDeniedException;

import java.io.IOException;
import java.time.LocalDateTime;
import org.unihubworkshop.workshopservice.services.AiSummaryProducerService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
@Service
@Transactional
public class WorkshopService {
    private final WorkshopRepository workshopRepository;
    private final WorkshopMapper workshopMapper;
    private static final String WORKSHOP_NOT_FOUND = "Workshop with ID %s not found";
    private final ImageService imageService;
    private final AiSummaryProducerService aiSummaryProducerService;
    public WorkshopService(WorkshopRepository workshopRepository,
                           WorkshopMapper workshopMapper, ImageService imageService, AiSummaryProducerService aiSummaryProducerService ){
        this.workshopMapper = workshopMapper;
        this.workshopRepository = workshopRepository;
        this.imageService = imageService;
        this.aiSummaryProducerService = aiSummaryProducerService;
    }

    public WorkshopResponse createWorkshop(UUID userId, CreateWorkshopRequest request) throws IOException {
        validateSlots(request.getTotalSlots(), null);
        Workshop workshop = workshopMapper.toEntity(request);

        workshop.setHostId(userId);
        String pdfUrl = imageService.uploadWorkshopPdf(request.getSummaryFile());
        workshop.setPdfUrl(pdfUrl);
        String mapUrl = imageService.uploadMap(request.getRoomMap());
        workshop.setRoomMap(mapUrl);
        Workshop savedWorkshop = workshopRepository.save(workshop);

        if (pdfUrl != null && !pdfUrl.isEmpty()) {
            aiSummaryProducerService.submitPdfForSummary(savedWorkshop.getId(), pdfUrl);
        }
        return workshopMapper.toResponse(savedWorkshop);

    }
    @Transactional(readOnly = true)
    public WorkshopResponse getWorkshopById(UUID id) {
        Workshop workshop = findWorkshopById(id);
        return mapToResponseWithImageUrl(workshop);
    }
    @Transactional(readOnly = true)
       public List<WorkshopResponse> getAllWorkshops(
        String name,
        LocalDateTime startDate,
        LocalDateTime endDate,
        int page,
        int size
    ) {
            Pageable pageable = PageRequest.of(page, size);

    Specification<Workshop> spec = (root, query, cb) -> cb.conjunction();

    if (name != null && !name.isEmpty()) {
        spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
    }

    if (startDate != null) {
        spec = spec.and((root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("startAt"), startDate));
    }

    if (endDate != null) {
        spec = spec.and((root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("endAt"), endDate));
    }
        return workshopRepository.findAll(spec, pageable)
                .stream()
                .map(this::mapToResponseWithImageUrl)
                .toList();
    }
    @Transactional(readOnly = true)
    public List<WorkshopResponse> getWorkshopsByHostId(UUID hostId) {
        return workshopRepository.findByHostId(hostId)
                .stream()
                .map(this::mapToResponseWithImageUrl)
                .toList();
    }
    @Transactional(readOnly = true)
    public List<WorkshopResponse> searchWorkshopsByName(String name) {
        return workshopRepository.findByNameContainingIgnoreCase(name)
                .stream()
                .map(this::mapToResponseWithImageUrl)
                .toList();
    }
    @Transactional(readOnly = true)
    public List<WorkshopResponse> getWorkshopsByRoom(String room) {
        return workshopRepository.findByRoom(room)
                .stream()
                .map(this::mapToResponseWithImageUrl)
                .toList();
    }
    public WorkshopResponse updateWorkshop(UUID userId, UUID id, UpdateWorkshopRequest request) throws IOException {

        Workshop workshop = findWorkshopById(id);
        workshopMapper.updateEntityFromRequest(request, workshop);
        MultipartFile file = request.getSummaryFile();
        if (file != null && !file.isEmpty()) {
            String fileUrl = imageService.uploadWorkshopPdf(file);
            workshop.setPdfUrl(fileUrl);
            aiSummaryProducerService.submitPdfForSummary(id, fileUrl);
        }

        Workshop updatedWorkshop = workshopRepository.save(workshop);

        return mapToResponseWithImageUrl(updatedWorkshop);
    }

    public void deleteWorkshop(UUID userId, UUID id) {
        Workshop workshop = findWorkshopById(id);
        if(workshop.getPdfUrl() != null){
            imageService.deleteFile(workshop.getPdfUrl());
        }
        if(workshop.getRoomMap() != null){
            imageService.deleteFile(workshop.getRoomMap());
        }
        workshopRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public StatisticsResponse getStatistics() {
        List<Workshop> allWorkshops = workshopRepository.findAll();

        long totalWorkshops = allWorkshops.size();
        long totalRegistrations = allWorkshops.stream()
                .mapToLong(w -> (long) w.getTotalSlots() - w.getAvailableSlots())
                .sum();

        List<WorkshopSimpleResponse> simpleWorkshops = allWorkshops.stream()
                .map(workshopMapper::toSimpleResponse)
                .toList();

        return new StatisticsResponse(totalWorkshops, totalRegistrations, simpleWorkshops);
    }

    @Transactional(readOnly = true)
    public WorkshopPaymentResponse getWorkshopPaymentInfo(UUID id) {
        Workshop workshop = findWorkshopById(id);
        return workshopMapper.toPaymentResponse(workshop);
    }

    private Workshop findWorkshopById(UUID id) {
        return workshopRepository.findById(id)
                 .orElseThrow(() -> new NotFoundException("Workshop not found"));

    }
    private void validateSlots(Integer totalSlots, Integer availableSlots) {
        if (totalSlots <= 0) {
            throw new InvalidWorkshopException("Total slots must be greater than zero");
        }
        if (availableSlots != null && availableSlots > totalSlots) {
            throw new InvalidWorkshopException("Available slots cannot exceed total slots");
        }
    }
    private WorkshopResponse mapToResponseWithImageUrl(Workshop workshop) {
        WorkshopResponse response = workshopMapper.toResponse(workshop);

        if (workshop.getRoomMap() != null && !workshop.getRoomMap().isEmpty()) {
            if(!workshop.getRoomMap().startsWith("https")){
               response.setRoomMap(imageService.getImageUrl(workshop.getRoomMap()));
            }
        }
        return response;
    }
}
