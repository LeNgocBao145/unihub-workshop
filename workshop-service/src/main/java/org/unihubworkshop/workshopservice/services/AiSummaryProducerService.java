package org.unihubworkshop.workshopservice.services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.unihubworkshop.workshopservice.config.RabbitMQConfig;
import org.unihubworkshop.workshopservice.events.AiSummaryTaskEvent;
import org.unihubworkshop.workshopservice.repositories.WorkshopRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiSummaryProducerService {

    private static final Logger log = LoggerFactory.getLogger(AiSummaryProducerService.class);
    
    private final RabbitTemplate rabbitTemplate;


    @Transactional
    public void submitPdfForSummary(UUID workshopId, String fileUrl) {

        AiSummaryTaskEvent taskEvent = new AiSummaryTaskEvent(workshopId, fileUrl);

        log.info("Đang đẩy task tóm tắt AI vào queue cho workshop: {}", workshopId);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.AI_SUMMARY_EXCHANGE,
                RabbitMQConfig.AI_SUMMARY_ROUTING_KEY,
                taskEvent
        );

    }
}