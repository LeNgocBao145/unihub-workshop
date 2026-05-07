package org.unihubworkshop.notificationservice.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.UUID;

@Service
public class QRCodeService {
    private static final Logger log = LoggerFactory.getLogger(QRCodeService.class);
    private static final int QR_CODE_SIZE = 300;

    /**
     * Generates QR code as byte array for use in email attachments.
     *
     * @param registrationId the registration ID to encode
     * @return byte array of PNG image
     */
    public byte[] generateQRCode(UUID registrationId) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(
                    registrationId.toString(),
                    BarcodeFormat.QR_CODE,
                    QR_CODE_SIZE,
                    QR_CODE_SIZE
            );

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            
            log.debug("QR code generated successfully for registration: {}", registrationId);
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate QR code for registration: {}", registrationId, e);
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    /**
     * Generates QR code as base64 encoded string.
     * Useful for embedding in JSON responses or HTML data URIs.
     *
     * @param registrationId the registration ID to encode
     * @return base64 encoded string of PNG image
     */
    public String generateQRCodeBase64(UUID registrationId) {
        try {
            byte[] qrCodeBytes = generateQRCode(registrationId);
            return Base64.getEncoder().encodeToString(qrCodeBytes);
        } catch (Exception e) {
            log.error("Failed to generate base64 QR code for registration: {}", registrationId, e);
            throw new RuntimeException("Failed to generate base64 QR code", e);
        }
    }
}
