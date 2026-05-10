package org.unihubworkshop.paymentservice.grpc;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unihubworkshop.grpc.PaymentQRCodeRequest;
import org.unihubworkshop.grpc.PaymentQRCodeResponse;
import org.unihubworkshop.grpc.PaymentServiceGrpc;
import org.unihubworkshop.paymentservice.cache.PaymentEmailCache;
import org.unihubworkshop.paymentservice.services.PaymentService;
import org.unihubworkshop.paymentservice.dto.ChargePaymentRequest;
import org.unihubworkshop.paymentservice.dto.ChargePaymentResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * gRPC Service Implementation for Payment Service
 * Handles requests from other services to get QR codes
 */
@GrpcService
@RequiredArgsConstructor
public class PaymentGrpcServiceImpl extends PaymentServiceGrpc.PaymentServiceImplBase {
    private static final Logger log = LoggerFactory.getLogger(PaymentGrpcServiceImpl.class);

    private final PaymentService paymentService;
    private final PaymentEmailCache paymentEmailCache;

    /**
     * Get QR code for payment
     */
    @Override
    public void getQRCode(
            PaymentQRCodeRequest request,
            StreamObserver<PaymentQRCodeResponse> responseObserver) {
        
        log.info("Received gRPC request for QR code for registration: {}", request.getRegistrationId());

        try {
            UUID registrationId = UUID.fromString(request.getRegistrationId());
            BigDecimal amount = new BigDecimal(request.getAmount());
            String userEmail = request.getUserEmail();

            if(paymentEmailCache.getEmail(registrationId) == null){
                paymentEmailCache.putEmail(registrationId, userEmail);
            }

            ChargePaymentRequest chargeRequest = new ChargePaymentRequest(
                    registrationId,
                    amount
            );

            ChargePaymentResponse chargeResponse = paymentService.chargePayment(chargeRequest, userEmail);

            PaymentQRCodeResponse response = PaymentQRCodeResponse.newBuilder()
                    .setPaymentId(chargeResponse.paymentId().toString())
                    .setQrCodeUrl(chargeResponse.qrCodeUrl())
                    .setStatus("PENDING")
                    .setExpiresAt(LocalDateTime.now()
                            .plusMinutes(15)
                            .atZone(ZoneId.systemDefault())
                            .toEpochSecond())
                    .build();

            log.info("Successfully generated QR code for registration: {}", registrationId);
            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error processing gRPC request for QR code", e);
            responseObserver.onError(e);
        }
    }
}

