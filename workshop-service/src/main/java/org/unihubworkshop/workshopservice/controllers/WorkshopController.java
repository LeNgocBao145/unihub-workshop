package org.unihubworkshop.workshopservice.controllers;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.unihubworkshop.workshopservice.common.ApiResponse;
import org.unihubworkshop.workshopservice.dto.*;
import org.unihubworkshop.workshopservice.services.TicketService;
import org.unihubworkshop.workshopservice.services.WorkshopService;

import java.io.IOException;
import java.time.LocalDateTime;


import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/workshops")
public class WorkshopController {

    private final WorkshopService workshopService;
    private final TicketService ticketService;

    public WorkshopController(WorkshopService workshopService, TicketService ticketService) {
        this.workshopService = workshopService;
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<WorkshopResponse> createWorkshop( @RequestHeader("X-User-Id") UUID userId,
            @Valid @ModelAttribute CreateWorkshopRequest request) throws IOException {

            WorkshopResponse response = workshopService.createWorkshop(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/tickets")
    public ResponseEntity<ApiResponse<TicketResponse>> bookTicket(
            @PathVariable UUID id,
            @Valid @RequestBody BookTicketRequest request) {
        TicketResponse data = ticketService.bookTicket(id, request);
        ApiResponse<TicketResponse> response =
                new ApiResponse<>(true, "Ticket booked successfully", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/check-in")
    public ResponseEntity<ApiResponse<RegistrationResponse>> checkInWorkshop(   @Valid @RequestBody RegistrationRequest request) {
        RegistrationResponse data = ticketService.checkInWorkshop(request);
        ApiResponse<RegistrationResponse> response =
                new ApiResponse<>(true, "Ticket booked successfully", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/tickets/{registrationId}")
    public ResponseEntity<ApiResponse<TicketResponse>> getTicketQRCode(
            @PathVariable UUID id,
            @PathVariable UUID registrationId) {
        TicketResponse data = ticketService.getRegistrationQRCode(id, registrationId);
        ApiResponse<TicketResponse> response =
                new ApiResponse<>(true, "Get ticket QR code successfully", data);
        return ResponseEntity.ok(response);
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
        @RequestParam(defaultValue = "1") String page,
        @RequestParam(defaultValue = "10") String size
    ) {
        int pageNum = Integer.parseInt(page);
        int sizeNum = Integer.parseInt(size);
        List<WorkshopResponse> data = workshopService.getAllWorkshops(name, startDate, endDate, pageNum - 1, sizeNum);

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
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id,
            @Valid @ModelAttribute UpdateWorkshopRequest request) throws IOException {
        WorkshopResponse response = workshopService.updateWorkshop(userId, id, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkshop(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id) {
        workshopService.deleteWorkshop(userId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reference-data")
    public ResponseEntity<ApiResponse<ReferenceDataResponse>> getReferenceData() {
        ReferenceDataResponse data = workshopService.getAllReferenceData();
        ApiResponse<ReferenceDataResponse> response =
                new ApiResponse<>(true, "Get reference data successfully", data);
        return ResponseEntity.ok(response);
    }
}
