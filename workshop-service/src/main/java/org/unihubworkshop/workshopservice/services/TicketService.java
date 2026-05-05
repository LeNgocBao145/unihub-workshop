package org.unihubworkshop.workshopservice.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.unihubworkshop.workshopservice.dto.RegistrationResponse;
import org.unihubworkshop.workshopservice.mapper.RegistrationMapper;
import org.unihubworkshop.workshopservice.models.Registration;
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
    public List<RegistrationResponse> getAllTickets(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return registrationRepository.findAll(pageable).stream()
                .map(registrationMapper::toResponse)
                .toList();
    }
}
