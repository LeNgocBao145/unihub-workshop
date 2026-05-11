package org.unihubworkshop.paymentservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record ChargePaymentRequest(
        @NotNull(message = "Registration ID is required")
        UUID registrationId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        BigDecimal amount,

        @NotBlank(message = "User email is required")
        @Email(message = "User email must be valid")
        String userEmail,

        @NotBlank(message = "Registration status is required")
        String registrationStatus
) {}

