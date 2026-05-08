package org.unihubworkshop.workshopservice.listeners;


import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.unihubworkshop.workshopservice.config.RabbitMQConfig;
import org.unihubworkshop.workshopservice.events.AiSummaryTaskEvent;
import org.unihubworkshop.workshopservice.models.Workshop;
import org.unihubworkshop.workshopservice.repositories.WorkshopRepository;
import org.unihubworkshop.workshopservice.services.PdfExtractionService;


import org.unihubworkshop.workshopservice.services.AiService;

@Service
@RequiredArgsConstructor
public class AiSummaryListener {

    private static final Logger log = LoggerFactory.getLogger(AiSummaryListener.class);

    private final WorkshopRepository workshopRepository;

    private final PdfExtractionService pdfExtractionService;
    private final AiService aiService;

    @RabbitListener(queues = RabbitMQConfig.AI_SUMMARY_QUEUE)
    @Transactional
    public void processAiSummaryTask(AiSummaryTaskEvent event) {
        log.info("Worker nhận nhiệm vụ tóm tắt AI cho Workshop ID: {}", event.workshopId());

        try {

            String cleanedText = pdfExtractionService.extractAndCleanText(event.fileUrl());

            String prompt = "Hãy đóng vai một chuyên gia giáo dục, tóm tắt nội dung workshop sau đây một cách ngắn gọn, súc tích để sinh viên dễ nắm bắt. Nội dung:\n" + cleanedText;
            String summaryResult = aiService.generateSummary(prompt);

            Workshop workshop = workshopRepository.findById(event.workshopId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Workshop"));
            workshop.setDescription(summaryResult);
            workshopRepository.save(workshop);

            log.info("Đã tóm tắt và cập nhật thành công cho Workshop ID: {}", event.workshopId());

        } catch (Exception e) {
            log.error("Lỗi khi xử lý tóm tắt cho Workshop ID: {}", event.workshopId(), e);
            throw new RuntimeException("Xử lý AI thất bại", e);
        }
    }
}