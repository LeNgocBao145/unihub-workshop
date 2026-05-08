package org.unihubworkshop.dataimportservice.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.unihubworkshop.dataimportservice.services.ImportService;

@Component
public class ImportListener {

    private static final Logger log = LoggerFactory.getLogger(ImportListener.class);

    private final ImportService importService;

    public ImportListener(ImportService importService) {
        this.importService = importService;
    }

    @RabbitListener(queues = "${app.rabbitmq.import-queue:data-import-queue}")
    public void handleImportMessage(String fileUrl) {
        log.info("Received import message: {}", fileUrl);
        try {
            String report = importService.processUrl(fileUrl);
            log.info("Import result: {}", report);
        } catch (Exception ex) {
            log.error("Failed to process import file {}", fileUrl, ex);
        }
    }
}
