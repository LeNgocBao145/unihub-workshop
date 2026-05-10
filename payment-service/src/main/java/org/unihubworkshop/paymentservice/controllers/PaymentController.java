package org.unihubworkshop.paymentservice.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.unihubworkshop.paymentservice.dto.ApiResponse;
import org.unihubworkshop.paymentservice.dto.ChargePaymentRequest;
import org.unihubworkshop.paymentservice.dto.ChargePaymentResponse;
import org.unihubworkshop.paymentservice.dto.PaymentResponse;
import org.unihubworkshop.paymentservice.dto.SepayWebhookPayload;
import org.unihubworkshop.paymentservice.services.PaymentService;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private static final Logger log = LoggerFactory.getLogger(
            PaymentController.class);

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/charge")
    public ResponseEntity<ApiResponse<ChargePaymentResponse>> chargePayment(
            @Valid @RequestBody ChargePaymentRequest request, String userEmail) {
        ChargePaymentResponse response = paymentService.chargePayment(request, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("QR code generated successfully", response));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(@PathVariable UUID paymentId) {
        PaymentResponse response = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/registration/{registrationId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByRegistration(
            @PathVariable UUID registrationId) {
        PaymentResponse response = paymentService.getPaymentByRegistrationId(registrationId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/webhook/sepay")
    public ResponseEntity<ApiResponse<Void>> handleSepayWebhook(
            @RequestBody SepayWebhookPayload payload) {
        log.info("Received SePay webhook - content: {}, referenceCode: {}, transactionId: {}, gateway: {}", 
                payload.content(), payload.referenceCode(), payload.transactionId(), payload.gateway());

        try {
            paymentService.handleWebhookPayment(payload);
            log.info("Payment processed successfully for reference code: {}", payload.referenceCode());
            return ResponseEntity.ok(ApiResponse.ok("Payment processed successfully", null));
        } catch (Exception e) {
            log.error("Error processing SePay webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to process payment", null));
        }
    }
}


