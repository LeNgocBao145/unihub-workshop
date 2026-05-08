package org.unihubworkshop.notificationservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.unihubworkshop.notificationservice.dto.RegistrationConfirmedEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ configuration for notification service.
 * Configures message broker connection and message listeners.
 */
@Configuration
public class RabbitMQConfig {
    public static final String REGISTRATION_EXCHANGE = "registration.exchange";
    public static final String REGISTRATION_CONFIRMED_QUEUE = "registration.confirmed.queue";
    public static final String REGISTRATION_CONFIRMED_ROUTING_KEY = "registration.confirmed";

    // Exchange
    @Bean
    public TopicExchange registrationExchange() {
        return new TopicExchange(REGISTRATION_EXCHANGE, true, false);
    }

    // Queue
    @Bean
    public Queue registrationConfirmedQueue() {
        return new Queue(REGISTRATION_CONFIRMED_QUEUE, true);
    }

    // Binding
    @Bean
    public Binding registrationConfirmedBinding(
            Queue registrationConfirmedQueue,
            TopicExchange registrationExchange) {
        return BindingBuilder
                .bind(registrationConfirmedQueue)
                .to(registrationExchange)
                .with(REGISTRATION_CONFIRMED_ROUTING_KEY);
    }

    /**
     * Override default MessageConverter to add type mapping.
     * Spring will auto-inject this into the auto-configured RabbitTemplate.
     */
    @Bean
    @SuppressWarnings("deprecation")
    public MessageConverter messageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultClassMapper classMapper = new DefaultClassMapper();
        
        // Map workshop-service event types to notification-service DTOs
        Map<String, Class<?>> idClassMapping = new HashMap<>();
        idClassMapping.put(
            "org.unihubworkshop.workshopservice.events.RegistrationConfirmedEvent",
            RegistrationConfirmedEvent.class
        );
        
        classMapper.setIdClassMapping(idClassMapping);
        classMapper.setDefaultType(RegistrationConfirmedEvent.class);
        classMapper.setTrustedPackages("*");
        
        converter.setClassMapper(classMapper);
        return converter;
    }
}



