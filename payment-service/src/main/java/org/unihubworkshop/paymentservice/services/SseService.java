package org.unihubworkshop.paymentservice.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.unihubworkshop.paymentservice.event.PaymentStatusUpdatedEvent;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseService {
    private static final Logger log = LoggerFactory.getLogger(SseService.class);
    
    // Lưu trữ các kết nối, Key là registrationId
    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(UUID registrationId) {
        // Đặt timeout khoảng 15 phút (bằng thời gian sống của QR code)
        SseEmitter emitter = new SseEmitter(15 * 60 * 1000L);
        
        emitters.put(registrationId, emitter);
        log.info("SSE connection created for registration: {}", registrationId);

        // Xử lý khi kết nối bị đóng hoặc timeout
        emitter.onCompletion(() -> emitters.remove(registrationId));
        emitter.onTimeout(() -> {
            emitter.complete();
            emitters.remove(registrationId);
        });
        emitter.onError(e -> {
            emitter.completeWithError(e);
            emitters.remove(registrationId);
        });

        // Gửi một tín hiệu khởi tạo để báo kết nối thành công
        try {
            emitter.send(SseEmitter.event().name("INIT").data("Connected"));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    public void sendPaymentSuccessEvent(PaymentStatusUpdatedEvent event) {
        UUID registrationId = event.registrationId();
        SseEmitter emitter = emitters.get(registrationId);
        
        if (emitter != null) {
            try {
                log.info("Pushing success event to client for registration: {}", registrationId);
                // Đẩy JSON data xuống Client
                Map<String, Object> payload = new HashMap<>();
                payload.put("registrationId", event.registrationId());
                payload.put("paymentId", event.paymentId());
                payload.put("status", "PAYMENT_SUCCESS");
                emitter.send(SseEmitter.event()
                        .name("PAYMENT_SUCCESS")
                        .data(payload));
                
                // Đóng đường ống sau khi đã xong việc
                emitter.complete();
            } catch (IOException e) {
                log.error("Error sending SSE event", e);
                emitter.completeWithError(e);
            } finally {
                emitters.remove(registrationId);
            }
        } else {
            log.debug("No active SSE connection found for registration: {}", registrationId);
        }
    }
}