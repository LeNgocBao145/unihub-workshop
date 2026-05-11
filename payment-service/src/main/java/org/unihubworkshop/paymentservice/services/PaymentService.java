package org.unihubworkshop.paymentservice.services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.unihubworkshop.paymentservice.cache.PaymentEmailCache;
import org.unihubworkshop.paymentservice.clients.SepayClient;
import org.unihubworkshop.paymentservice.config.RabbitMQConfig;
import org.unihubworkshop.paymentservice.dto.ChargePaymentRequest;
import org.unihubworkshop.paymentservice.dto.ChargePaymentResponse;
import org.unihubworkshop.paymentservice.dto.PaymentResponse;
import org.unihubworkshop.paymentservice.dto.SepayWebhookPayload;
import org.unihubworkshop.paymentservice.event.PaymentStatusUpdatedEvent;
import org.unihubworkshop.paymentservice.exceptions.DuplicatePaymentException;
import org.unihubworkshop.paymentservice.exceptions.PaymentNotFoundException;
import org.unihubworkshop.paymentservice.mapper.PaymentMapper;
import org.unihubworkshop.paymentservice.models.Payment;
import org.unihubworkshop.paymentservice.models.PaymentProvider;
import org.unihubworkshop.paymentservice.models.PaymentStatus;
import org.unihubworkshop.paymentservice.repositories.PaymentRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(
            PaymentService.class);
    private static final int QR_CODE_EXPIRY_MINUTES = 15;

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final SepayClient sepayClient;
    private final PaymentResilienceService paymentResilienceService;
    private final RabbitTemplate rabbitTemplate;
    private final PaymentEmailCache paymentEmailCache;

    @Transactional
    public ChargePaymentResponse chargePayment(ChargePaymentRequest request) {
        log.info("Processing charge payment for registration: {}, status: {}", 
                request.registrationId(), request.registrationStatus());

        // Check if there's a valid pending payment (not expired) for this registration
        var validPayment = paymentRepository.findValidPendingPayment(
                request.registrationId(),
                PaymentStatus.PENDING,
                LocalDateTime.now()
        );
        
        if (validPayment.isPresent()) {
            Payment payment = validPayment.get();
            log.info("Reusing existing valid QR code for registration: {}", request.registrationId());
            return new ChargePaymentResponse(payment.getId(), 
                    generateExistingQRCode(payment));
        }

        // Check if payment already exists but expired (FAILED or expired PENDING)
        var existingPayment = paymentRepository.findByRegistrationId(request.registrationId());
        if (existingPayment.isPresent()) {
            Payment payment = existingPayment.get();
            
            // If status is FAILED, refresh the expired_at and update status to PENDING
            if (payment.getStatus() == PaymentStatus.FAILED) {
                log.info("Found FAILED payment, refreshing for registration: {}", request.registrationId());
                payment.setExpiredAt(LocalDateTime.now().plusMinutes(QR_CODE_EXPIRY_MINUTES));
                payment.setStatus(PaymentStatus.PENDING);
                Payment updatedPayment = paymentRepository.save(payment);
                return new ChargePaymentResponse(updatedPayment.getId(), 
                        generateExistingQRCode(updatedPayment));
            }
            
            // If expired PENDING payment, refresh it
            if (payment.getStatus() == PaymentStatus.PENDING && 
                    payment.getExpiredAt() != null && 
                    payment.getExpiredAt().isBefore(LocalDateTime.now())) {
                log.info("Found expired PENDING payment, refreshing for registration: {}", request.registrationId());
                payment.setExpiredAt(LocalDateTime.now().plusMinutes(QR_CODE_EXPIRY_MINUTES));
                Payment updatedPayment = paymentRepository.save(payment);
                return new ChargePaymentResponse(updatedPayment.getId(), 
                        generateExistingQRCode(updatedPayment));
            }
        }

        // Check if payment already succeeded for this registration
        var successfulPayment = paymentRepository.findByRegistrationIdAndStatus(
                request.registrationId(), 
                PaymentStatus.SUCCESS
        );
        if (successfulPayment.isPresent()) {
            throw new DuplicatePaymentException(
                    "Payment already successfully completed for registration: " + request.registrationId());
        }

        // Create new payment record
        Payment payment = new Payment();
        payment.setRegistrationId(request.registrationId());
        payment.setAmount(request.amount());
        payment.setProvider(PaymentProvider.SEPAY);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setExpiredAt(LocalDateTime.now().plusMinutes(QR_CODE_EXPIRY_MINUTES));

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment created with ID: {}", savedPayment.getId());

        if(paymentEmailCache.getEmail(savedPayment.getId()) == null){
            paymentEmailCache.putEmail(savedPayment.getId(), request.userEmail());
        }

        // Generate QR code
        String qrCodeUrl = paymentResilienceService.executeWithResilience(
            () -> sepayClient.generateQRCode(
                savedPayment.getAmount(),
                "UNIHUB" + savedPayment.getId()
            ),
            "generate-qr-code"
        );

        log.info("QR code generated for payment: {}", savedPayment.getId());

        return new ChargePaymentResponse(savedPayment.getId(), qrCodeUrl);
    }

    @Transactional
    public void handleWebhookPayment(SepayWebhookPayload payload) {
        log.info("Processing webhook payment with reference code: {}", payload.referenceCode());

        // Extract payment ID from content (format: "UNIHUB<paymentId>")
        UUID paymentId = extractPaymentIdFromContent(payload.content());

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found with ID: " + paymentId));
        
        // Check if payment is already processed (bankReferenceCode already set)
        if (payment.getBankReferenceCode() != null) {
            log.warn("Duplicate payment detected for payment ID: {}", paymentId);
            throw new DuplicatePaymentException(
                    "Payment already processed for payment ID: " + paymentId);
        }

        // Update payment details
        payment.setBankReferenceCode(payload.referenceCode());
        payment.setProviderTransactionId(payload.transactionId());
        payment.setActualContent(payload.content());
        payment.setGateway(payload.gateway());
        payment.setStatus(PaymentStatus.SUCCESS);

        Payment updatedPayment = paymentRepository.save(payment);
        log.info("Payment updated to SUCCESS with ID: {}", updatedPayment.getId());

        // Retrieve cached user email
        String userEmail = paymentEmailCache.getEmail(paymentId);
        if (userEmail == null) {
            log.warn("User email not found in cache for payment ID: {}", paymentId);
            return;
        }

        // Publish events to RabbitMQ
        publishPaymentStatusUpdatedEvent(updatedPayment, userEmail);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found with ID: " + paymentId));
        return paymentMapper.toResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByRegistrationId(UUID registrationId) {
        Payment payment = paymentRepository.findByRegistrationId(registrationId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found for registration: " + registrationId));
        return paymentMapper.toResponse(payment);
    }

    private void publishPaymentStatusUpdatedEvent(Payment payment, String userEmail) {
        PaymentStatusUpdatedEvent event = new PaymentStatusUpdatedEvent(
                payment.getRegistrationId(),
                userEmail,
                payment.getId()
        );

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PAYMENT_EXCHANGE,
                RabbitMQConfig.PAYMENT_STATUS_UPDATED_ROUTING_KEY,
                event
        );

        log.info("Payment status updated event published for registration: {}",
                 payment.getRegistrationId());
    }

    private String generateExistingQRCode(Payment payment) {
        return paymentResilienceService.executeWithResilience(
            () -> sepayClient.generateQRCode(
                payment.getAmount(),
                "UNIHUB" + payment.getId()
            ),
            "generate-existing-qr-code"
        );
    }

    private UUID extractPaymentIdFromContent(String content) {
        log.info("Extracting payment ID from content: {}", content);
        
        if (content == null || content.trim().isEmpty()) {
            throw new PaymentNotFoundException("Content is null or empty");
        }
        
        String trimmedContent = content.trim();
        
        if (!trimmedContent.contains("UNIHUB")) {
            throw new PaymentNotFoundException("Content does not contain 'UNIHUB' prefix: " + content);
        }
        
        try {
            int startIndex = trimmedContent.indexOf("UNIHUB") + 6;
            String paymentIdStr = trimmedContent.substring(startIndex).trim();
            
            int spaceIndex = paymentIdStr.indexOf(' ');
            if (spaceIndex > 0) {
                paymentIdStr = paymentIdStr.substring(0, spaceIndex);
            }
            
            if (paymentIdStr.length() != 32) {
                throw new PaymentNotFoundException("Invalid UUID length: " + paymentIdStr.length());
            }
            
            String uuidWithDashes = paymentIdStr.substring(0, 8) + "-" +
                    paymentIdStr.substring(8, 12) + "-" +
                    paymentIdStr.substring(12, 16) + "-" +
                    paymentIdStr.substring(16, 20) + "-" +
                    paymentIdStr.substring(20, 32);
            
            log.info("Reconstructed UUID: {}", uuidWithDashes);
            return UUID.fromString(uuidWithDashes);
        } catch (Exception e) {
            log.error("Failed to parse payment ID from content: {}", content, e);
            throw new PaymentNotFoundException("Invalid payment ID format in content: " + content);
        }
    }
}

