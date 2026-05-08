package org.unihubworkshop.notificationservice.listeners;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.unihubworkshop.notificationservice.dto.RegistrationConfirmedEvent;
import org.unihubworkshop.notificationservice.services.EmailService;

/**
 * Listens to registration events from workshop-service via RabbitMQ.
 * Sends confirmation emails when a user registers for a workshop.
 */
@Service
@RequiredArgsConstructor
public class RegistrationEventListener {
    private static final Logger log = LoggerFactory.getLogger(RegistrationEventListener.class);

    private final EmailService emailService;

    /**
     * Listens to registration.confirmed queue and sends confirmation email.
     *
     * @param event the registration confirmed event
     */
    @RabbitListener(queues = "registration.confirmed.queue")
    public void onRegistrationConfirmed(RegistrationConfirmedEvent event) {
        log.info("Received registration confirmed event for workshop: {}, user: {}", 
                event.workshopId(), event.userId());
        
        try {
            // Send confirmation email
            emailService.sendRegistrationConfirmation(event);
            log.info("Confirmation email sent successfully for registration: {}", event.registrationId());
        } catch (Exception e) {
            log.error("Failed to send confirmation email for registration: {}", event.registrationId(), e);
            // In production, implement retry logic or dead-letter queue handling
        }
    }
}

