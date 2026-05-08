package org.unihubworkshop.workshopservice.services;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class PdfExtractionService {

    private static final Logger log = LoggerFactory.getLogger(PdfExtractionService.class);

    private final S3Client s3Client;

    @Value("${r2.bucket-name}")
    private String bucketName;


    public String extractAndCleanText(String objectKey) {
        log.info("Bắt đầu tải và bóc tách chữ từ R2, objectKey: {}", objectKey);

        // 1. Tạo request lấy file từ Cloudflare R2
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();


        try (InputStream s3InputStream = s3Client.getObject(getObjectRequest);
             PDDocument document = PDDocument.load(s3InputStream)) {

            PDFTextStripper stripper = new PDFTextStripper();
            String rawText = stripper.getText(document);

            return cleanText(rawText);

        } catch (Exception e) {
            log.error("Lỗi khi đọc file PDF từ R2 với key: {}", objectKey, e);
            throw new RuntimeException("Không thể đọc nội dung file PDF từ R2", e);
        }
    }

    private String cleanText(String rawText) {
        if (rawText == null || rawText.isEmpty()) return "";
        String cleaned = rawText.replaceAll("[\\r\\n]+", " ");
        cleaned = cleaned.replaceAll("\\s{2,}", " ");
        return cleaned.trim();
    }
}