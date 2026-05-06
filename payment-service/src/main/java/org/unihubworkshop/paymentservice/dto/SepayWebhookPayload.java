package org.unihubworkshop.paymentservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record SepayWebhookPayload(
        @JsonProperty("transaction_id")
        String transactionId,

        @JsonProperty("reference_code")
        String referenceCode,

        @JsonProperty("account_number")
        String accountNumber,

        @JsonProperty("amount")
        BigDecimal amount,

        @JsonProperty("content")
        String content,

        @JsonProperty("transfer_type")
        String transferType,

        @JsonProperty("transfer_date")
        String transferDate,

        @JsonProperty("description")
        String description
) {}

