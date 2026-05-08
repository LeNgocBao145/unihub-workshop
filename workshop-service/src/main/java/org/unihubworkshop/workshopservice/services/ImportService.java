package org.unihubworkshop.workshopservice.services;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class ImportService {

    private final S3Service s3Service;
    private final RabbitTemplate rabbitTemplate;
    private final String importQueue;

    public ImportService(S3Service s3Service,
                         RabbitTemplate rabbitTemplate,
                         @Value("${app.rabbitmq.import-queue:data-import-queue}") String importQueue) {
        this.s3Service = s3Service;
        this.rabbitTemplate = rabbitTemplate;
        this.importQueue = importQueue;
    }

    public String uploadAndPublish(MultipartFile file) {
        try {
            String url = s3Service.uploadFile(file, "imports");
            // send url as message to queue
            rabbitTemplate.convertAndSend(importQueue, url);
            return url;
        } catch (IOException ex) {
            throw new RuntimeException("Failed to upload file", ex);
        }
    }
}
