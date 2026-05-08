package org.unihubworkshop.workshopservice.controllers;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.unihubworkshop.workshopservice.common.ApiResponse;
import org.unihubworkshop.workshopservice.dto.CreateWorkshopRequest;
import org.unihubworkshop.workshopservice.dto.StatisticsResponse;
import org.unihubworkshop.workshopservice.dto.UpdateWorkshopRequest;
import org.unihubworkshop.workshopservice.dto.WorkshopResponse;
import org.unihubworkshop.workshopservice.dto.WorkshopPaymentResponse;
import org.unihubworkshop.workshopservice.services.WorkshopService;
import org.springframework.web.multipart.MultipartFile;
import org.unihubworkshop.workshopservice.services.ImportService;
import java.time.LocalDateTime;


import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/workshops")
public class WorkshopController {

    private final WorkshopService workshopService;
    private final ImportService importService;

    public WorkshopController(WorkshopService workshopService, ImportService importService) {
        this.workshopService = workshopService;
        this.importService = importService;
    }

    @PostMapping("/import-csv")
    public ResponseEntity<ApiResponse<String>> importCsv(@RequestParam("file") MultipartFile file) {
        String message = importService.uploadAndPublish(file);
        ApiResponse<String> response = new ApiResponse<>(true, "File queued for import", message);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<WorkshopResponse> createWorkshop(
            @Valid @RequestBody CreateWorkshopRequest request) {
        WorkshopResponse response = workshopService.createWorkshop(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkshopResponse>> getWorkshopById(@PathVariable UUID id) {
        WorkshopResponse data = workshopService.getWorkshopById(id);
        ApiResponse<WorkshopResponse> response =
                new ApiResponse<>(true, "Get workshop successfully", data);
        /*
        ResponseEntity.ok(data): Trả về status 200 + body
         */
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/payment")
    public ResponseEntity<ApiResponse<WorkshopPaymentResponse>> getWorkshopPaymentInfo(@PathVariable UUID id) {
        WorkshopPaymentResponse data = workshopService.getWorkshopPaymentInfo(id);
        ApiResponse<WorkshopPaymentResponse> response =
                new ApiResponse<>(true, "Get workshop payment info successfully", data);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkshopResponse>>> getAllWorkshops(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) LocalDateTime startDate,
        @RequestParam(required = false) LocalDateTime endDate,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        List<WorkshopResponse> data = workshopService.getAllWorkshops(name, startDate, endDate, page - 1, size);

        ApiResponse<List<WorkshopResponse>> response =
                new ApiResponse<>(true, "Get all workshops successfully", data);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<List<WorkshopResponse>> searchWorkshopsByName(
            @RequestParam String name) {
        List<WorkshopResponse> responses = workshopService.searchWorkshopsByName(name);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/host/{hostId}")
    public ResponseEntity<List<WorkshopResponse>> getWorkshopsByHost(
            @PathVariable UUID hostId) {
        List<WorkshopResponse> responses = workshopService.getWorkshopsByHostId(hostId);
        return ResponseEntity.ok(responses);
    }
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<StatisticsResponse>> getStatistics() {
        StatisticsResponse data = workshopService.getStatistics();
        ApiResponse<StatisticsResponse> response =
                new ApiResponse<>(true, "Get statistics successfully", data);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/room/{room}")
    public ResponseEntity<List<WorkshopResponse>> getWorkshopsByRoom(
            @PathVariable String room) {
        List<WorkshopResponse> responses = workshopService.getWorkshopsByRoom(room);
        return ResponseEntity.ok(responses);
    }



    @PutMapping("/{id}")
    public ResponseEntity<WorkshopResponse> updateWorkshop(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWorkshopRequest request) {
        WorkshopResponse response = workshopService.updateWorkshop(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkshop(@PathVariable UUID id) {
        workshopService.deleteWorkshop(id);
        return ResponseEntity.noContent().build();
    }
}
