package org.unihubworkshop.paymentservice.clients;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.unihubworkshop.paymentservice.exceptions.SepayException;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class SepayClient {
    private static final Logger log = LoggerFactory.getLogger(
            SepayClient.class);

    private static final String SEPAY_QR_BASE_URL = "https://qr.sepay.vn/img";

    @Value("${sepay.account-number}")
    private String accountNumber;

    @Value("${sepay.bank}")
    private String bank;

    public String generateQRCode(BigDecimal amount, String description) {
//        if (true) { // Lát nữa bạn sẽ đổi thành false để test khôi phục
//            throw new RuntimeException("Giả lập cổng thanh toán Sepay bị sập!");
//        }
        try {
            String encodedDescription = URLEncoder.encode(description, StandardCharsets.UTF_8);
            
            return String.format("%s?acc=%s&bank=%s&amount=%s&des=%s",
                    SEPAY_QR_BASE_URL,
                    accountNumber,
                    bank,
                    amount.toPlainString(),
                    encodedDescription);
        } catch (Exception e) {
            log.error("Failed to generate SePay QR code", e);
            throw new SepayException("Failed to generate SePay QR code", e);
        }
    }
}

