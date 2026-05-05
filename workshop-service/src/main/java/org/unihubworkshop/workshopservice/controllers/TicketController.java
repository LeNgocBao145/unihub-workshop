package org.unihubworkshop.workshopservice.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.unihubworkshop.workshopservice.common.ApiResponse;
import org.unihubworkshop.workshopservice.dto.RegistrationResponse;
import org.unihubworkshop.workshopservice.services.TicketService;
import java.util.List;

@RestController
@RequestMapping("/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RegistrationResponse>>> getAllTickets(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        List<RegistrationResponse> data = ticketService.getAllTickets(page - 1, size);
        ApiResponse<List<RegistrationResponse>> response =
                new ApiResponse<>(true, "Get tickets successfully", data);
        return ResponseEntity.ok(response);
    }
}
