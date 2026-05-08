package org.unihubworkshop.workshopservice.services;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);


    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${gemini.api-key}")
    private String apiKey;

    public String generateSummary(String rawText) {
        log.info("Bắt đầu gửi dữ liệu sang AI cá nhân (Gemini 2.5 Flash) để tóm tắt...");

        // 1. Chuẩn bị ngữ cảnh và nội dung
        String systemRules = """
            Nhiệm vụ của bạn là đọc nội dung file giới thiệu workshop và tóm tắt lại cho sinh viên dễ hiểu.
        
            Quy tắc trình bày:
            - Độ dài tối đa: 40 - 50 từ.
            - Trình bày rõ ràng, súc tích.
            - Liệt kê những điểm nhấn chính (key takeaways) mà sinh viên sẽ học được.
            - Tuyệt đối không bịa đặt thông tin không có trong văn bản gốc.
            """;

        String userInstruction = "Dưới đây là nội dung văn bản cần tóm tắt:\n" + rawText;


        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);


        Map<String, Object> requestBody = new HashMap<>();

        // Gắn System Message
        requestBody.put("systemInstruction", Map.of(
                "parts", List.of(Map.of("text", systemRules))
        ));

        // Gắn User Message
        requestBody.put("contents", List.of(
                Map.of("parts", List.of(Map.of("text", userInstruction)))
        ));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            // 4. Nhận kết quả về dưới dạng String thô (chắc chắn 100% không bị lỗi ép kiểu)
            String rawJsonResponse = restTemplate.postForObject(url, request, String.class);

            // Tự dùng ObjectMapper để đọc chuỗi JSON
            ObjectMapper mapper = new ObjectMapper();
            JsonNode responseNode = mapper.readTree(rawJsonResponse);

            // 5. Bóc tách JSON để lấy nội dung text sâu bên trong
            if (responseNode != null && responseNode.has("candidates")) {
                String aiResponse = responseNode
                        .path("candidates").get(0)
                        .path("content")
                        .path("parts").get(0)
                        .path("text").asText();

                log.info("AI đã tóm tắt xong!");
                return aiResponse;
            } else {
                log.error("Phản hồi bất thường từ AI: {}", rawJsonResponse);
                throw new RuntimeException("Phản hồi từ AI bị rỗng hoặc sai cấu trúc.");
            }

        } catch (Exception e) {
            log.error("Lỗi khi kết nối với AI API: {}", e.getMessage(), e);
            throw new RuntimeException("Dịch vụ AI hiện đang gián đoạn, vui lòng thử lại sau.", e);
        }
    }
}