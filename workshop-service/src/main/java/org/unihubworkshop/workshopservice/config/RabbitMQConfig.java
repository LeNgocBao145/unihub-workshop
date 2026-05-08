package org.unihubworkshop.workshopservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String REGISTRATION_EXCHANGE = "registration.exchange";
    public static final String REGISTRATION_CONFIRMED_ROUTING_KEY = "registration.confirmed";

    public static final String PAYMENT_EXCHANGE = "payment.exchange";
    public static final String PAYMENT_STATUS_UPDATED_QUEUE = "payment.status.updated.queue";
    public static final String PAYMENT_STATUS_UPDATED_ROUTING_KEY = "payment.status.updated";

    // --- Config cho AI Summary ---
    public static final String AI_SUMMARY_EXCHANGE = "ai.summary.exchange";
    public static final String AI_SUMMARY_QUEUE = "ai.summary.queue";
    public static final String AI_SUMMARY_ROUTING_KEY = "ai.summary.routing.key";

    @Bean
    public TopicExchange registrationExchange() {
        return new TopicExchange(REGISTRATION_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange paymentExchange() {
        return new DirectExchange(PAYMENT_EXCHANGE, true, false);
    }

    @Bean
    public Queue paymentStatusUpdatedQueue() {
        return new Queue(PAYMENT_STATUS_UPDATED_QUEUE, true);
    }
    @Value("${app.rabbitmq.import-queue:data-import-queue}")
    private String importQueue;

    @Bean
    public Binding paymentStatusUpdatedBinding(
            Queue paymentStatusUpdatedQueue,
            DirectExchange paymentExchange) {
        return BindingBuilder
                .bind(paymentStatusUpdatedQueue)
                .to(paymentExchange)
                .with(PAYMENT_STATUS_UPDATED_ROUTING_KEY);
    }


    // --- BEAN CHO AI SUMMARY (MỚI THÊM VÀO) ---
    @Bean
    public DirectExchange aiSummaryExchange() {
        return new DirectExchange(AI_SUMMARY_EXCHANGE, true, false);
    }

    @Bean
    public Queue aiSummaryQueue() {
        return new Queue(AI_SUMMARY_QUEUE, true);
    }

    @Bean
    public Binding aiSummaryBinding(
            Queue aiSummaryQueue,
            DirectExchange aiSummaryExchange) {
        return BindingBuilder
                .bind(aiSummaryQueue)
                .to(aiSummaryExchange)
                .with(AI_SUMMARY_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    public Queue importQueue() {
        return new Queue(importQueue, true);
    }


    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}


