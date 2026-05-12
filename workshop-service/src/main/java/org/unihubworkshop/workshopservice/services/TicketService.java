package org.unihubworkshop.workshopservice.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.unihubworkshop.workshopservice.clients.PaymentGrpcClient;
import org.unihubworkshop.workshopservice.common.UserContext;
import org.unihubworkshop.workshopservice.config.RabbitMQConfig;
import org.unihubworkshop.workshopservice.dto.*;
import org.unihubworkshop.workshopservice.events.RegistrationConfirmedEvent;
import org.unihubworkshop.workshopservice.exceptions.InvalidWorkshopException;
import org.unihubworkshop.workshopservice.exceptions.NotFoundException;
import org.unihubworkshop.workshopservice.exceptions.PaymentServiceUnavailableException;
import org.unihubworkshop.workshopservice.mapper.RegistrationMapper;
import org.unihubworkshop.workshopservice.models.Registration;
import org.unihubworkshop.workshopservice.models.RegistrationStatus;
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
    private static final long PAYMENT_EXPIRATION_MINUTES = 15;

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
    public List<TicketDetailResponse> getTickets(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Registration> registrations = registrationRepository.findAllWithDetails(pageable);

        return registrations.stream()
                .map(registrationMapper::toDetailResponse)
                .toList();
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

        UUID userId = userContext.getUserId();

        // Verify student profile
        studentProfileService.verifyStudentProfile(request);

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


        // Reserve slot with optimistic check
        reserveWorkshopSlot(workshopId);

        try {
            if (workshop.getType() == WorkshopType.FREE) {
                return createFreeWorkshopRegistration(workshopId, userId, workshop);
            }
            return createPaidWorkshopRegistration(workshopId, userId, workshop);
        } catch (Exception e) {
            log.error("Error during ticket booking, rolling back slot reservation", e);
            cacheAsideService.incrementSlot(workshopId);
            throw new RuntimeException("Failed to complete ticket booking: " + e.getMessage(), e);
        }
    }

    /**
     * Reserve a slot for the workshop
     * Throws exception if no slots available
     */
    private void reserveWorkshopSlot(UUID workshopId) {
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
    }

    /**
     * Create registration for free workshop
     * Status is set to CONFIRMED immediately without payment
     */
    private TicketResponse createFreeWorkshopRegistration(UUID workshopId, UUID userId, Workshop workshop) {
        log.info("Creating free workshop registration for user: {}", userId);

        Registration registration = new Registration();
        registration.setWorkshopId(workshopId);
        registration.setUserId(userId);
        registration.setIsPresent(false);
        registration.setStatus(RegistrationStatus.CONFIRMED);
        registration.setExpiresAt(null);

        Registration savedRegistration = registrationRepository.saveAndFlush(registration);
        log.info("Free workshop registration confirmed: {}", savedRegistration.getId());

        publishRegistrationConfirmedEvent(savedRegistration, workshop);

        return TicketResponse.builder()
                .registrationId(savedRegistration.getId())
                .workshopId(workshopId)
                .userId(userId)
                .status(savedRegistration.getStatus())
                .createdAt(savedRegistration.getCreatedAt())
                .build();
    }

    /**
     * Create registration for paid workshop
     * Status is set to RESERVED and waits for payment
     * Calls payment service to generate QR code
     */
    private TicketResponse createPaidWorkshopRegistration(UUID workshopId, UUID userId, Workshop workshop) {
        log.info("Creating paid workshop registration for user: {}", userId);

        Registration registration = new Registration();
        registration.setWorkshopId(workshopId);
        registration.setUserId(userId);
        registration.setIsPresent(false);
        registration.setStatus(RegistrationStatus.RESERVED);
        registration.setExpiresAt(LocalDateTime.now().plusMinutes(PAYMENT_EXPIRATION_MINUTES));

        Registration savedRegistration = registrationRepository.saveAndFlush(registration);
        log.info("Registration created with ID: {}", savedRegistration.getId());

        try {
            PaymentQRCodeResponse qrCodeResponse = generatePaymentQRCode(savedRegistration, workshop);
            UUID paymentId = UUID.fromString(qrCodeResponse.getPaymentId());

            // Cache QR code with TTL
            cacheQRCode(savedRegistration.getId(), paymentId, qrCodeResponse.getQrCodeUrl(),
                    savedRegistration.getExpiresAt());

            return buildTicketResponse(savedRegistration, paymentId, qrCodeResponse.getQrCodeUrl());
        } catch (PaymentServiceUnavailableException e) {
            log.warn("Payment service unavailable while booking workshop {}. Rolling back reserved slot.", workshopId);
            cacheAsideService.incrementSlot(workshopId);
            throw e;
        }
    }

    /**
     * Generate QR code from payment service
     */
    private PaymentQRCodeResponse generatePaymentQRCode(Registration registration, Workshop workshop) {
        log.info("Calling payment service for QR code generation");
        String userEmail = userContext.getUserEmail();
        PaymentQRCodeResponse qrCodeResponse = paymentGrpcClient.getQRCode(
                registration.getId(),
                workshop.getPrice(),
                userEmail,
                RegistrationStatus.RESERVED
        );
        log.info("Successfully generated QR code for registration: {}", registration.getId());
        return qrCodeResponse;
    }

    /**
     * Publish registration confirmed event via RabbitMQ
     */
    private void publishRegistrationConfirmedEvent(Registration registration, Workshop workshop) {
        List<String> speakerNames = workshopRepository.findSpeakerNamesByWorkshopId(workshop.getId());
        String userEmail = userContext.getUserEmail();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.REGISTRATION_EXCHANGE,
                RabbitMQConfig.REGISTRATION_CONFIRMED_ROUTING_KEY,
                new RegistrationConfirmedEvent(
                        registration.getId(),
                        workshop.getId(),
                        registration.getUserId(),
                        userEmail,
                        workshop.getName(),
                        speakerNames,
                        workshop.getRoom(),
                        workshop.getRoomMap()
                )
        );
    }


    /**
     * Build ticket response with common fields
     */
    private TicketResponse buildTicketResponse(Registration registration, UUID paymentId, String qrCodeUrl) {
        return TicketResponse.builder()
                .registrationId(registration.getId())
                .workshopId(registration.getWorkshopId())
                .userId(registration.getUserId())
                .status(registration.getStatus())
                .paymentId(paymentId)
                .qrCodeUrl(qrCodeUrl)
                .expiresAt(registration.getExpiresAt())
                .createdAt(registration.getCreatedAt())
                .build();
    }

    private void cacheQRCode(UUID registrationId, UUID paymentId, String qrCodeUrl, LocalDateTime expiresAt) {
        if (expiresAt == null || registrationId == null || paymentId == null) {
            return;
        }
        long ttlSeconds = java.time.temporal.ChronoUnit.SECONDS
                .between(LocalDateTime.now(), expiresAt);
        if (ttlSeconds > 0) {
            cacheAsideService.putQRCodeToCache(registrationId, paymentId, qrCodeUrl, ttlSeconds);
        }
    }

    /**
     * Get QR code for existing registration
     * Used when user already has a registration and needs to retrieve QR code again
     * No request body needed - just registration ID
     */
    @Transactional
    public TicketResponse getRegistrationQRCode(UUID workshopId, UUID registrationId) {
        log.info("Retrieving QR code for registration: {}, workshop: {}", registrationId, workshopId);

        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new NotFoundException("Registration not found"));

        Workshop workshop = workshopRepository.findById(workshopId)
                .orElseThrow(() -> new NotFoundException("Workshop not found"));

        // Verify registration belongs to this workshop
        if (!registration.getWorkshopId().equals(workshopId)) {
            log.warn("Registration {} does not belong to workshop {}", registrationId, workshopId);
            throw new InvalidWorkshopException("Registration does not belong to this workshop");
        }

        // For free workshops, no QR code needed
        if (workshop.getType() == WorkshopType.FREE) {
            log.info("Free workshop - no QR code needed for registration: {}", registrationId);
            return buildTicketResponse(registration, null, null);
        }

        String userEmail = userContext.getUserEmail();

        // For paid workshops, check status and handle accordingly
        if (registration.getStatus() == RegistrationStatus.CANCELLED) {
            log.info("Registration is cancelled, calling payment service for new QR code for registration: {}",
                    registrationId);
            PaymentQRCodeResponse qrCodeResponse = paymentGrpcClient.getQRCode(
                    registrationId,
                    workshop.getPrice(),
                    userEmail,
                    RegistrationStatus.CANCELLED
            );

            registration.setStatus(RegistrationStatus.RESERVED);
            registration.setExpiresAt(LocalDateTime.now().plusMinutes(PAYMENT_EXPIRATION_MINUTES));
            Registration updatedRegistration = registrationRepository.saveAndFlush(registration);

            UUID paymentId = UUID.fromString(qrCodeResponse.getPaymentId());
            cacheQRCode(updatedRegistration.getId(), paymentId, qrCodeResponse.getQrCodeUrl(),
                    updatedRegistration.getExpiresAt());

            return buildTicketResponse(updatedRegistration, paymentId, qrCodeResponse.getQrCodeUrl());
        }

        // For RESERVED or other statuses, try to get from cache first
        var cachedQRCode = cacheAsideService.getQRCodeFromCache(registrationId);
        if (cachedQRCode.isPresent()) {
            log.info("Found QR code in cache for registration: {}", registrationId);
            return buildTicketResponse(registration, null, cachedQRCode.get());
        }

        // QR code not in cache, call payment service
        log.info("QR code not found in cache, calling payment service for registration: {}", registrationId);
        PaymentQRCodeResponse qrCodeResponse = paymentGrpcClient.getQRCode(
                registrationId,
                workshop.getPrice(),
                userEmail,
                registration.getStatus()
        );

        UUID paymentId = UUID.fromString(qrCodeResponse.getPaymentId());
        cacheQRCode(registrationId, paymentId, qrCodeResponse.getQrCodeUrl(), registration.getExpiresAt());

        log.info("Successfully retrieved QR code for registration: {}", registrationId);
        return buildTicketResponse(registration, paymentId, qrCodeResponse.getQrCodeUrl());
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

    /**
     * Get registration status for streaming
     * Only the user who created the registration can access it
     * Returns only status, not full registration details
     */
    @Transactional(readOnly = true)
    public RegistrationStatusResponse getRegistrationStatus(UUID registrationId, UUID currentUserId) {
        log.info("Fetching registration status for registration: {}, user: {}", registrationId, currentUserId);

        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new NotFoundException("Registration not found"));

        // Verify that only the user who created the registration can access it
        if (!registration.getUserId().equals(currentUserId)) {
            log.warn("Unauthorized access attempt to registration {} by user {}", registrationId, currentUserId);
            throw new InvalidWorkshopException("You do not have permission to access this registration");
        }

        return RegistrationStatusResponse.builder()
                .registrationId(registration.getId())
                .status(registration.getStatus())
                .build();
    }
}
