package org.unihubworkshop.notificationservice.strategy;

import org.unihubworkshop.notificationservice.dto.RegistrationConfirmedEvent;

public interface NotificationStrategy {
    void sendNotification(RegistrationConfirmedEvent event);
}
