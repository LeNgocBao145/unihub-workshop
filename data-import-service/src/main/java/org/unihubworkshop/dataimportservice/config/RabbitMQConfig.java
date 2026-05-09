package org.unihubworkshop.dataimportservice.config;

import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.import-queue:data-import-queue}")
    private String importQueue;

    @Bean
    public Queue importQueue() {
        return new Queue(importQueue, true);
    }
}
