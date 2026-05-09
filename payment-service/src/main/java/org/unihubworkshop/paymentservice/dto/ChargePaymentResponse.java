package org.unihubworkshop.paymentservice.dto;

import java.util.UUID;

public record ChargePaymentResponse(
        UUID paymentId,
        String qrCodeUrl
) {}


