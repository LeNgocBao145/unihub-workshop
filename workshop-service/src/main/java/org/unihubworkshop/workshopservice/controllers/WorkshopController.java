package org.unihubworkshop.workshopservice.controllers;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.unihubworkshop.workshopservice.dto.CreateWorkshopRequest;
import org.unihubworkshop.workshopservice.dto.UpdateWorkshopRequest;
import org.unihubworkshop.workshopservice.dto.WorkshopResponse;
import org.unihubworkshop.workshopservice.services.WorkshopService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/workshops")
public class WorkshopController {

    private final WorkshopService workshopService;

    public WorkshopController(WorkshopService workshopService) {
        this.workshopService = workshopService;
    }

    @PostMapping
    public ResponseEntity<WorkshopResponse> createWorkshop(
            @Valid @RequestBody CreateWorkshopRequest request) {
        WorkshopResponse response = workshopService.createWorkshop(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkshopResponse> getWorkshop(@PathVariable UUID id) {
        WorkshopResponse response = workshopService.getWorkshopById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<WorkshopResponse>> getAllWorkshops() {
        List<WorkshopResponse> responses = workshopService.getAllWorkshops();
        return ResponseEntity.ok(responses);
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