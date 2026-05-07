package org.unihubworkshop.paymentservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record SepayWebhookPayload(
        @JsonProperty("id")
        Long id,

        @JsonProperty("gateway")
        String gateway,

        @JsonProperty("transactionDate")
        String transactionDate,

        @JsonProperty("accountNumber")
        String accountNumber,

        @JsonProperty("subAccount")
        String subAccount,

        @JsonProperty("transferAmount")
        BigDecimal transferAmount,

        @JsonProperty("content")
        String content,

        @JsonProperty("transferType")
        String transferType,

        @JsonProperty("referenceCode")
        String referenceCode,

        @JsonProperty("description")
        String description,

        @JsonProperty("accumulated")
        BigDecimal accumulated
) {
    public BigDecimal amount() {
        return transferAmount;
    }

    public String transactionId() {
        return id != null ? id.toString() : null;
    }
}