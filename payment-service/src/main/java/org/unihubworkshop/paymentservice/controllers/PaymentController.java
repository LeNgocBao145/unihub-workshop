package org.unihubworkshop.paymentservice.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/charge")
    public ResponseEntity<ApiResponse<ChargePaymentResponse>> chargePayment(
            @Valid @RequestBody ChargePaymentRequest request) {
        ChargePaymentResponse response = paymentService.chargePayment(request);
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
        log.info("Received SePay webhook with reference code: {}", payload.referenceCode());

        try {
            paymentService.handleWebhookPayment(payload);
            return ResponseEntity.ok(ApiResponse.ok("Payment processed successfully", null));
        } catch (Exception e) {
            log.error("Error processing SePay webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to process payment", null));
        }
    }
}


