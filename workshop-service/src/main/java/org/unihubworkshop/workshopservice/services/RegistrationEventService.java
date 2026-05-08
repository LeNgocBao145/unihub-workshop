package org.unihubworkshop.workshopservice.services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.unihubworkshop.workshopservice.config.RabbitMQConfig;
import org.unihubworkshop.workshopservice.events.PaymentStatusUpdatedEvent;
import org.unihubworkshop.workshopservice.events.RegistrationConfirmedEvent;
import org.unihubworkshop.workshopservice.models.Registration;
import org.unihubworkshop.workshopservice.models.RegistrationStatus;
import org.unihubworkshop.workshopservice.models.Workshop;
import org.unihubworkshop.workshopservice.repositories.RegistrationRepository;
import org.unihubworkshop.workshopservice.repositories.WorkshopRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistrationEventService {
    private static final Logger log = LoggerFactory.getLogger(RegistrationEventService.class);
    
    private final RegistrationRepository registrationRepository;
    private final WorkshopRepository workshopRepository;
    private final RabbitTemplate rabbitTemplate;

    /**
     * Handles payment status updated event from payment service.
     * Updates registration status to CONFIRMED and sends confirmation email event.
     *
     * @param event the payment status updated event containing registration ID
     */
    @RabbitListener(queues = RabbitMQConfig.PAYMENT_STATUS_UPDATED_QUEUE)
    @Transactional
    public void handlePaymentStatusUpdated(PaymentStatusUpdatedEvent event) {
        log.info("Received payment status updated event for registration: {}", event.registrationId());
        
        Registration registration = registrationRepository.findById(event.registrationId())
                .orElseThrow(() -> new RuntimeException("Registration not found: " + event.registrationId()));
        
        registration.setStatus(RegistrationStatus.CONFIRMED);
        Registration confirmedRegistration = registrationRepository.save(registration);
        log.info("Registration {} status updated to CONFIRMED after successful payment", event.registrationId());

        // Fetch workshop details to send confirmation email
        Workshop workshop = workshopRepository.findById(registration.getWorkshopId())
                .orElseThrow(() -> new RuntimeException("Workshop not found: " + registration.getWorkshopId()));
        
        List<String> speakerNames = workshopRepository.findSpeakerNamesByWorkshopId(registration.getWorkshopId());

        // Send registration confirmed event for notification service to send email
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.REGISTRATION_EXCHANGE,
                RabbitMQConfig.REGISTRATION_CONFIRMED_ROUTING_KEY,
                new RegistrationConfirmedEvent(
                        confirmedRegistration.getId(),
                        registration.getWorkshopId(),
                        registration.getUserId(),
                        event.userEmail(),
                        workshop.getName(),
                        speakerNames,
                        workshop.getRoom(),
                        workshop.getRoomMap()
                )
        );
        
        log.info("Sent registration confirmation email event for registration: {}", event.registrationId());
    }
}
