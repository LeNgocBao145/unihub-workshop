package org.unihubworkshop.workshopservice.controllers;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.unihubworkshop.workshopservice.common.ApiResponse;
import org.unihubworkshop.workshopservice.common.UserContext;
import org.unihubworkshop.workshopservice.dto.RegistrationResponse;
import org.unihubworkshop.workshopservice.dto.RegistrationStatusResponse;
import org.unihubworkshop.workshopservice.dto.TicketDetailResponse;
import org.unihubworkshop.workshopservice.services.TicketService;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final UserContext userContext;

    public TicketController(TicketService ticketService, UserContext userContext) {
        this.ticketService = ticketService;
        this.userContext = userContext;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TicketDetailResponse>>> getAllTickets(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "1000") int size) {

        List<TicketDetailResponse> data = ticketService.getTickets(page - 1, size);
        ApiResponse<List<TicketDetailResponse>> response =
                new ApiResponse<>(true, "Get tickets successfully", data);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<RegistrationResponse>>> getCurrentUserTickets(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        List<RegistrationResponse> data = ticketService.getCurrentUserTickets(page - 1, size);
        ApiResponse<List<RegistrationResponse>> response =
                new ApiResponse<>(true, "Get current user tickets successfully", data);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/{registrationId}/status/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamRegistrationStatus(@PathVariable UUID registrationId) throws IOException {
        UUID userId = userContext.getUserId();
        SseEmitter emitter = new SseEmitter(60000L);

        try {
            RegistrationStatusResponse statusResponse = ticketService.getRegistrationStatus(registrationId, userId);
            emitter.send(SseEmitter.event()
                    .id(registrationId.toString())
                    .name("registration-status")
                    .data(statusResponse));
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }
}
