package org.unihubworkshop.workshopservice.listeners;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.unihubworkshop.workshopservice.config.RabbitMQConfig;
import org.unihubworkshop.workshopservice.events.RegistrationConfirmedEvent;
import org.unihubworkshop.workshopservice.events.PaymentStatusUpdatedEvent;
import org.unihubworkshop.workshopservice.models.Registration;
import org.unihubworkshop.workshopservice.models.RegistrationStatus;
import org.unihubworkshop.workshopservice.models.Workshop;
import org.unihubworkshop.workshopservice.repositories.RegistrationRepository;
import org.unihubworkshop.workshopservice.repositories.WorkshopRepository;
import org.unihubworkshop.workshopservice.services.CacheAsideService;
import org.unihubworkshop.workshopservice.services.WorkshopService;

import java.util.List;

/**
 * Listener for handling payment status updated events from payment service.
 * Handles PaymentStatusUpdatedEvent by:
 * - Updating registration status to CONFIRMED when payment is successful
 * - Sending RegistrationConfirmedEvent for notification service to send confirmation email
 * - Deleting cached QR code after successful payment
 */
@Service
@RequiredArgsConstructor
public class PaymentStatusEventListener {
    private static final Logger log = LoggerFactory.getLogger(PaymentStatusEventListener.class);

    private final CacheAsideService cacheAsideService;
    private final RegistrationRepository registrationRepository;
    private final WorkshopService workshopService;
    private final WorkshopRepository workshopRepository;
    private final RabbitTemplate rabbitTemplate;
    @RabbitListener(queues = RabbitMQConfig.PAYMENT_STATUS_UPDATED_QUEUE)
    @Transactional
    public void handlePaymentStatusUpdated(PaymentStatusUpdatedEvent event) {
        log.info("Received payment status updated event for registration: {}", event.registrationId());

        try {
            // Update registration status to CONFIRMED when payment is successful
            Registration registration = registrationRepository.findById(event.registrationId())
                    .orElseThrow(() -> new RuntimeException("Registration not found: " + event.registrationId()));

            registration.setStatus(RegistrationStatus.CONFIRMED);
            Registration confirmedRegistration = registrationRepository.save(registration);
            log.info("Registration {} status updated to CONFIRMED after successful payment", event.registrationId());

            // Fetch workshop details to send confirmation email
            Workshop workshop = workshopService.findWorkshopById(registration.getWorkshopId());

            List<String> speakerNames = workshopRepository.findSpeakerNamesByWorkshopId(registration.getWorkshopId());

            // Send registration confirmed event for notification service to send email
            IO.println("Toi day");
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

            // Delete cached QR code after successful payment
            if (event.paymentId() != null) {
                cacheAsideService.deleteQRCodeByCacheKeyFromPaymentId(event.paymentId());
                log.info("Successfully deleted cached QR code for payment: {}", event.paymentId());
            }

        } catch (Exception e) {
            log.error("Error handling PaymentStatusUpdatedEvent for registration: {}", 
                    event.registrationId(), e);
            // Log but don't rethrow - we need to ensure the event is processed
            throw new RuntimeException("Failed to process payment status update", e);
        }
    }
}
