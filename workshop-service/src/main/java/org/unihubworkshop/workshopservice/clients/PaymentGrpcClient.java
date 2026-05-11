package org.unihubworkshop.workshopservice.clients;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.unihubworkshop.grpc.PaymentQRCodeRequest;
import org.unihubworkshop.grpc.PaymentQRCodeResponse;
import org.unihubworkshop.grpc.PaymentServiceGrpc;
import org.unihubworkshop.workshopservice.exceptions.PaymentServiceUnavailableException;
import org.unihubworkshop.workshopservice.models.RegistrationStatus;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

/**
 * gRPC Client for Payment Service
 * Handles communication with payment-service to get QR codes
 */
@Service
public class PaymentGrpcClient {
    private static final Logger log = LoggerFactory.getLogger(PaymentGrpcClient.class);

    @Value("${payment.service.grpc.address:localhost}")
    private String paymentHost;

    @Value("${payment.service.grpc.port:9090}")
    private int paymentPort;

    @Value("${payment.service.grpc.timeout-ms:1500}")
    private long timeoutMs;

    private ManagedChannel channel;
    private PaymentServiceGrpc.PaymentServiceBlockingStub blockingStub;

    /**
     * Initialize gRPC channel and stub
     */
    private void initializeChannel() {
        if (channel == null || channel.isShutdown()) {
            log.info("Initializing gRPC channel to payment service: {}:{}", paymentHost, paymentPort);
            channel = ManagedChannelBuilder
                    .forAddress(paymentHost, paymentPort)
                    .usePlaintext()
                    .build();
            blockingStub = PaymentServiceGrpc.newBlockingStub(channel);
        }
    }

    /**
     * Get QR code from payment service via gRPC
     */
    public PaymentQRCodeResponse getQRCode(
            UUID registrationId,
            BigDecimal amount,
            String userEmail,
            RegistrationStatus registrationStatus) {

        log.info("Requesting QR code from payment service for registration: {}, status: {}",
                registrationId, registrationStatus);

        initializeChannel();

        try {
            PaymentQRCodeRequest request = PaymentQRCodeRequest.newBuilder()
                    .setRegistrationId(registrationId.toString())
                    .setAmount(amount.toPlainString())
                    .setUserEmail(userEmail)
                    .setRegistrationStatus(registrationStatus.toString())
                    .build();

            PaymentQRCodeResponse response = blockingStub
                .withDeadlineAfter(timeoutMs, TimeUnit.MILLISECONDS)
                .getQRCode(request);

            log.info("Successfully retrieved QR code for registration: {}. PaymentId: {}",
                    registrationId, response.getPaymentId());

            return response;
        } catch (Exception e) {
            log.error("Error calling payment service for registration: {}", registrationId, e);
            throw new PaymentServiceUnavailableException(
                    "Payment service is temporarily unavailable. Please retry shortly.",
                    e
            );
        }
    }

    /**
     * Shutdown the channel
     */
    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            log.info("Shutting down gRPC channel");
            channel.shutdown();
        }
    }
}

