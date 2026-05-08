package org.unihubworkshop.workshopservice.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.unihubworkshop.workshopservice.common.PageResponse;
import org.unihubworkshop.workshopservice.dto.RegistrationResponse;
import org.unihubworkshop.workshopservice.dto.WorkshopResponse;
import org.unihubworkshop.workshopservice.mapper.RegistrationMapper;
import org.unihubworkshop.workshopservice.models.Registration;
import org.unihubworkshop.workshopservice.models.Workshop;
import org.unihubworkshop.workshopservice.repositories.RegistrationRepository;
import java.util.List;

@Service
@Transactional
public class TicketService {

    private final RegistrationRepository registrationRepository;
    private final RegistrationMapper registrationMapper;

    public TicketService(RegistrationRepository registrationRepository, RegistrationMapper registrationMapper) {
        this.registrationRepository = registrationRepository;
        this.registrationMapper = registrationMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<RegistrationResponse> getAllTickets(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Registration> registrationPage = registrationRepository.findAll(pageable);

        List<RegistrationResponse> content = registrationPage.getContent()
                .stream()
                .map(registrationMapper::toResponse)
                .toList();

        return PageResponse.<RegistrationResponse>builder()
                .content(content)
                .page(registrationPage.getNumber() + 1)
                .size(registrationPage.getSize())
                .totalElements(registrationPage.getTotalElements())
                .totalPages(registrationPage.getTotalPages())
                .hasNext(registrationPage.hasNext())
                .hasPrevious(registrationPage.hasPrevious())
                .build();
    }
}
