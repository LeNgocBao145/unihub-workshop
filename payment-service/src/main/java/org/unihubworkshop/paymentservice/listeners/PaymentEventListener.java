package org.unihubworkshop.paymentservice.listeners;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.unihubworkshop.paymentservice.config.RabbitMQConfig;
import org.unihubworkshop.paymentservice.event.PaymentStatusUpdatedEvent;
import org.unihubworkshop.paymentservice.services.SseService;

@Component
@RequiredArgsConstructor
public class PaymentEventListener {
    
    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);
    private final SseService sseService;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_STATUS_UPDATED_QUEUE)
    public void handlePaymentStatusUpdated(PaymentStatusUpdatedEvent event) {
        log.info("Received PaymentStatusUpdatedEvent from RabbitMQ for registration: {}", event.registrationId());
        

        sseService.sendPaymentSuccessEvent(event);
        
        // Lưu ý: Nếu bạn có Notification Service (gửi email)
        // thì service đó cũng sẽ có một @RabbitListener tương tự file này để nghe độc lập.
    }
}