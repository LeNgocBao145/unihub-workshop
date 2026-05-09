package org.unihubworkshop.paymentservice.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.unihubworkshop.paymentservice.exceptions.PaymentGatewayUnavailableException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@Service
public class PaymentResilienceService {

    private static final Logger log = LoggerFactory.getLogger(PaymentResilienceService.class);

    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicReference<CircuitState> circuitState = new AtomicReference<>(CircuitState.CLOSED);

    private volatile long openUntilEpochMillis = 0;

    @Value("${payment.resilience.enabled:true}")
    private boolean enabled;

    @Value("${payment.resilience.timeout-ms:10000}")
    private long timeoutMs;

    @Value("${payment.resilience.max-retries:3}")
    private int maxRetries;

    @Value("${payment.resilience.initial-backoff-ms:200}")
    private long initialBackoffMs;

    @Value("${payment.resilience.backoff-multiplier:2.0}")
    private double backoffMultiplier;

    @Value("${payment.resilience.max-backoff-ms:3000}")
    private long maxBackoffMs;

    @Value("${payment.resilience.jitter-factor:0.3}")
    private double jitterFactor;

    @Value("${payment.resilience.failure-threshold:5}")
    private int failureThreshold;

    @Value("${payment.resilience.open-state-duration-ms:10000}")
    private long openStateDurationMs;

    public String executeWithResilience(Supplier<String> supplier, String operationName) {
        if (!enabled) {
            return supplier.get();
        }

        if (!canAttemptCall(operationName)) {
            throw new PaymentGatewayUnavailableException("Circuit is open for operation: " + operationName);
        }

        long backoff = initialBackoffMs;
        Throwable lastError = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String result = executeWithTimeout(supplier);
                recordSuccess();
                return result;
            } catch (Exception ex) {
                lastError = ex;
                recordFailure(operationName, ex);

                if (attempt == maxRetries) {
                    break;
                }

                sleepWithBackoffAndJitter(backoff);
                backoff = Math.min(maxBackoffMs, (long) Math.ceil(backoff * backoffMultiplier));

                if (!canAttemptCall(operationName)) {
                    break;
                }
            }
        }

        throw new PaymentGatewayUnavailableException(
                "Payment operation failed after retries: " + operationName,
                lastError
        );
    }

    private String executeWithTimeout(Supplier<String> supplier) throws Exception {
        try {
            return CompletableFuture.supplyAsync(supplier)
                    .orTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                    .get(timeoutMs + 200, TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            Throwable rootCause = ex.getCause() != null ? ex.getCause() : ex;
            if (rootCause instanceof TimeoutException) {
                throw new PaymentGatewayUnavailableException("Payment gateway timeout after " + timeoutMs + "ms", rootCause);
            }
            throw ex;
        }
    }

    private boolean canAttemptCall(String operationName) {
        CircuitState state = circuitState.get();
        long now = System.currentTimeMillis();

        if (state == CircuitState.CLOSED) {
            return true;
        }

        if (state == CircuitState.OPEN) {
            if (now < openUntilEpochMillis) {
                log.warn("Circuit is OPEN for operation {} until {}", operationName, openUntilEpochMillis);
                return false;
            }
            circuitState.set(CircuitState.HALF_OPEN);
            log.info("Circuit switched to HALF_OPEN for operation {}", operationName);
            return true;
        }

        return true;
    }

    private void recordSuccess() {
        consecutiveFailures.set(0);
        if (circuitState.get() != CircuitState.CLOSED) {
            circuitState.set(CircuitState.CLOSED);
            log.info("Circuit closed after successful payment operation");
        }
    }

    private void recordFailure(String operationName, Throwable throwable) {
        int failures = consecutiveFailures.incrementAndGet();
        log.warn("Payment operation {} failed ({} consecutive failures)", operationName, failures, throwable);

        if (failures >= failureThreshold) {
            circuitState.set(CircuitState.OPEN);
            openUntilEpochMillis = System.currentTimeMillis() + openStateDurationMs;
            log.error("Circuit opened for {} due to repeated failures. Open for {}ms", operationName, openStateDurationMs);
        } else if (circuitState.get() == CircuitState.HALF_OPEN) {
            circuitState.set(CircuitState.OPEN);
            openUntilEpochMillis = System.currentTimeMillis() + openStateDurationMs;
            log.error("Circuit reopened from HALF_OPEN for {}. Open for {}ms", operationName, openStateDurationMs);
        }
    }

    private void sleepWithBackoffAndJitter(long baseBackoffMs) {
        double jitterRange = Math.max(0, jitterFactor);
        double randomFactor = 1 + ThreadLocalRandom.current().nextDouble(-jitterRange, jitterRange);
        long sleepMillis = Math.max(50, (long) (baseBackoffMs * randomFactor));

        try {
            Thread.sleep(sleepMillis);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new PaymentGatewayUnavailableException("Retry sleep interrupted", interruptedException);
        }
    }

    enum CircuitState {
        CLOSED,
        OPEN,
        HALF_OPEN
    }
}
