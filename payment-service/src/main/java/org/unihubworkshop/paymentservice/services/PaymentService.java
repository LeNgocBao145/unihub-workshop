package org.unihubworkshop.paymentservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final SepayClient sepayClient;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public ChargePaymentResponse chargePayment(ChargePaymentRequest request) {
        log.info("Processing charge payment for registration: {}", request.registrationId());

        // Check if payment already exists for this registration
        paymentRepository.findByRegistrationId(request.registrationId())
                .ifPresent(existing -> {
                    throw new DuplicatePaymentException(
                            "Payment already exists for registration: " + request.registrationId());
                });

        // Create new payment record
        Payment payment = new Payment();
        payment.setRegistrationId(request.registrationId());
        payment.setAmount(request.amount());
        payment.setProvider(PaymentProvider.SEPAY);
        payment.setStatus(PaymentStatus.PENDING);

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment created with ID: {}", savedPayment.getId());

        // Generate QR code
        String qrCodeUrl = sepayClient.generateQRCode(
                savedPayment.getAmount(),
                "UNIHUB-" + savedPayment.getId()
        );

        log.info("QR code generated for payment: {}", savedPayment.getId());

        return new ChargePaymentResponse(savedPayment.getId(), qrCodeUrl);
    }

    @Transactional
    public void handleWebhookPayment(SepayWebhookPayload payload) {
        log.info("Processing webhook payment with reference code: {}", payload.referenceCode());

        // Check for duplicate payments using bank reference code
        if (paymentRepository.findByBankReferenceCode(payload.referenceCode()).isPresent()) {
            log.warn("Duplicate payment detected with reference code: {}", payload.referenceCode());
            throw new DuplicatePaymentException(
                    "Payment with reference code already processed: " + payload.referenceCode());
        }

        // Find payment by provider transaction ID
        Payment payment = paymentRepository.findByProviderTransactionId(payload.transactionId())
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found with transaction ID: " + payload.transactionId()));

        // Update payment details
        payment.setBankReferenceCode(payload.referenceCode());
        payment.setProviderTransactionId(payload.transactionId());
        payment.setActualContent(payload.content());
        payment.setGateway(payload.accountNumber());
        payment.setStatus(PaymentStatus.SUCCESS);

        Payment updatedPayment = paymentRepository.save(payment);
        log.info("Payment updated to SUCCESS with ID: {}", updatedPayment.getId());

        // Publish event to RabbitMQ
        publishPaymentStatusUpdatedEvent(updatedPayment);
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

    private void publishPaymentStatusUpdatedEvent(Payment payment) {
        PaymentStatusUpdatedEvent event = new PaymentStatusUpdatedEvent(
                payment.getId(),
                payment.getRegistrationId(),
                payment.getStatus().name(),
                payment.getAmount()
        );

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PAYMENT_EXCHANGE,
                RabbitMQConfig.PAYMENT_STATUS_UPDATED_ROUTING_KEY,
                event
        );

        log.info("Payment status updated event published for registration: {}", 
                 payment.getRegistrationId());
    }
}

