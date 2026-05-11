package org.unihubworkshop.workshopservice.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.unihubworkshop.workshopservice.clients.PaymentGrpcClient;
import org.unihubworkshop.workshopservice.common.UserContext;
import org.unihubworkshop.workshopservice.config.RabbitMQConfig;
import org.unihubworkshop.workshopservice.dto.RegistrationRequest;
import org.unihubworkshop.workshopservice.events.RegistrationConfirmedEvent;
import org.unihubworkshop.workshopservice.dto.BookTicketRequest;
import org.unihubworkshop.workshopservice.dto.RegistrationResponse;
import org.unihubworkshop.workshopservice.dto.TicketResponse;
import org.unihubworkshop.workshopservice.exceptions.InvalidWorkshopException;
import org.unihubworkshop.workshopservice.exceptions.NotFoundException;
import org.unihubworkshop.workshopservice.exceptions.PaymentServiceUnavailableException;
import org.unihubworkshop.workshopservice.mapper.RegistrationMapper;
import org.unihubworkshop.workshopservice.models.Registration;
import org.unihubworkshop.workshopservice.models.RegistrationStatus;
import org.unihubworkshop.workshopservice.models.StudentProfile;
import org.unihubworkshop.workshopservice.models.Workshop;
import org.unihubworkshop.workshopservice.models.WorkshopType;
import org.unihubworkshop.workshopservice.repositories.RegistrationRepository;
import org.unihubworkshop.workshopservice.repositories.WorkshopRepository;
import org.unihubworkshop.grpc.PaymentQRCodeResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TicketService {
    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    private final RegistrationRepository registrationRepository;
    private final WorkshopRepository workshopRepository;
    private final RegistrationMapper registrationMapper;
    private final CacheAsideService cacheAsideService;
    private final PaymentGrpcClient paymentGrpcClient;
    private final UserContext userContext;
    private final RabbitTemplate rabbitTemplate;
    private final StudentProfileService studentProfileService;

    public TicketService(
            RegistrationRepository registrationRepository,
            WorkshopRepository workshopRepository,
            RegistrationMapper registrationMapper,
            CacheAsideService cacheAsideService,
            PaymentGrpcClient paymentGrpcClient,
            UserContext userContext,
            RabbitTemplate rabbitTemplate,
            StudentProfileService studentProfileService) {
        this.registrationRepository = registrationRepository;
        this.workshopRepository = workshopRepository;
        this.registrationMapper = registrationMapper;
        this.cacheAsideService = cacheAsideService;
        this.paymentGrpcClient = paymentGrpcClient;
        this.userContext = userContext;
        this.rabbitTemplate = rabbitTemplate;
        this.studentProfileService = studentProfileService;
    }

    @Transactional(readOnly = true)
    public List<RegistrationResponse> getAllTickets(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return registrationRepository.findAll(pageable).stream()
                .map(registrationMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RegistrationResponse> getCurrentUserTickets(int page, int size) {
        log.info("Fetching tickets for current user: {}", userContext.getUserId());
        UUID userId = userContext.getUserId();
        Pageable pageable = PageRequest.of(page, size);
        return registrationRepository.findByUserId(userId, pageable).stream()
                .map(registrationMapper::toResponse)
                .toList();
    }

    /**
     * Book a ticket for a workshop with student profile verification
     * 1. Verify student profile information
     * 2. Check if workshop exists and has available slots (with cache)
     * 3. Decrement slot atomically (with pessimistic locking)
     * 4. Create registration record
     * 5. Call gRPC to payment service to get QR code
     * 6. Return ticket response with QR code
     */
    public TicketResponse bookTicket(UUID workshopId, BookTicketRequest request) {
        log.info("Booking ticket for workshop: {}, student code: {}", workshopId, request.getStudentCode());

        // Verify student profile
        studentProfileService.verifyStudentProfile(request);

        UUID userId = userContext.getUserId();
        return performTicketBooking(workshopId, userId);
    }

    public RegistrationResponse checkInWorkshop(RegistrationRequest request) {

        String id = request.getRegistrationId();
        UUID uuid = UUID.fromString(id);
        Registration registration = registrationRepository
                .findById(uuid)
                .orElseThrow(() ->
                        new NotFoundException("Registration not found"));

        registration.setIsPresent(true);

        Registration updatedRegistration =
                registrationRepository.save(registration);

        return registrationMapper.toResponse(updatedRegistration);
    }
    private TicketResponse performTicketBooking(UUID workshopId, UUID userId) {
        log.info("Booking ticket for workshop: {}, user: {}", workshopId, userId);

        Workshop workshop = workshopRepository.findById(workshopId)
                .orElseThrow(() -> new NotFoundException("Workshop not found"));

        Integer availableSlots = cacheAsideService.getAvailableSlotsWithCache(workshopId);
        if (availableSlots <= 0) {
            log.warn("No available slots for workshop: {}", workshopId);
            throw new InvalidWorkshopException("No available slots for this workshop");
        }

        boolean slotReserved = cacheAsideService.decrementSlotWithLock(workshopId);
        if (!slotReserved) {
            log.warn("Failed to reserve slot for workshop: {}", workshopId);
            throw new InvalidWorkshopException("Failed to reserve slot, no slots available");
        }

        String userEmail = userContext.getUserEmail();
        try {
            Registration registration = new Registration();
            registration.setWorkshopId(workshopId);
            registration.setUserId(userId);
            registration.setIsPresent(false);

            if (workshop.getType() == WorkshopType.FREE) {
                registration.setStatus(RegistrationStatus.CONFIRMED);
                registration.setExpiresAt(null);
                Registration savedRegistration = registrationRepository.saveAndFlush(registration);
                log.info("Free workshop registration confirmed: {}", savedRegistration.getId());

                List<String> speakerNames = workshopRepository.findSpeakerNamesByWorkshopId(workshopId);

                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.REGISTRATION_EXCHANGE,
                        RabbitMQConfig.REGISTRATION_CONFIRMED_ROUTING_KEY,
                        new RegistrationConfirmedEvent(
                                savedRegistration.getId(),
                                workshopId,
                                userId,
                                userEmail,
                                workshop.getName(),
                                speakerNames,
                                workshop.getRoom(),
                                workshop.getRoomMap()
                        )
                );

                return TicketResponse.builder()
                        .registrationId(savedRegistration.getId())
                        .workshopId(workshopId)
                        .userId(userId)
                        .status(savedRegistration.getStatus())
                        .createdAt(savedRegistration.getCreatedAt())
                        .build();
            }

            registration.setStatus(RegistrationStatus.RESERVED);
            registration.setExpiresAt(LocalDateTime.now().plusHours(1));
            Registration savedRegistration = registrationRepository.saveAndFlush(registration);
            log.info("Registration created with ID: {}", savedRegistration.getId());

            log.info("Calling payment service for QR code generation");
            PaymentQRCodeResponse qrCodeResponse = paymentGrpcClient.getQRCode(
                    savedRegistration.getId(),
                    workshop.getPrice(),
                    userEmail
            );

            log.info("Successfully generated QR code for registration: {}", savedRegistration.getId());

            return TicketResponse.builder()
                    .registrationId(savedRegistration.getId())
                    .workshopId(workshopId)
                    .userId(userId)
                    .status(savedRegistration.getStatus())
                    .paymentId(qrCodeResponse.getPaymentId())
                    .qrCodeUrl(qrCodeResponse.getQrCodeUrl())
                    .expiresAt(savedRegistration.getExpiresAt())
                    .createdAt(savedRegistration.getCreatedAt())
                    .build();

        } catch (PaymentServiceUnavailableException e) {
            log.warn("Payment service unavailable while booking workshop {}. Rolling back reserved slot.", workshopId);
            cacheAsideService.incrementSlot(workshopId);
            throw e;
        } catch (Exception e) {
            log.error("Error during ticket booking, rolling back slot reservation", e);
            cacheAsideService.incrementSlot(workshopId);
            throw new RuntimeException("Failed to complete ticket booking: " + e.getMessage(), e);
        }
    }

    /**
     * Cancel registration and return slot
     */
    public void cancelRegistration(UUID registrationId) {
        log.info("Cancelling registration: {}", registrationId);

        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new NotFoundException("Registration not found"));

        registration.setStatus(RegistrationStatus.CANCELLED);
        registrationRepository.save(registration);

        // Return slot to workshop
        cacheAsideService.incrementSlot(registration.getWorkshopId());
        log.info("Successfully cancelled registration: {}", registrationId);
    }
}
