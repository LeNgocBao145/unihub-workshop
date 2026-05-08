package org.unihubworkshop.notificationservice.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.unihubworkshop.notificationservice.dto.RegistrationConfirmedEvent;

@Component
public class AppNotificationStrategy implements NotificationStrategy {
    private static final Logger log = LoggerFactory.getLogger(AppNotificationStrategy.class);

    @Override
    public void sendNotification(RegistrationConfirmedEvent event) {
        log.info("Sending app push notification to user: {} for workshop: {}", event.userId(), event.workshopName());
    }
}
