package org.unihubworkshop.workshopservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.unihubworkshop.workshopservice.clients.PaymentGrpcClient;

/**
 * gRPC Configuration for Workshop Service
 */
@Configuration
public class GrpcClientConfig {

    /**
     * Create PaymentGrpcClient bean for dependency injection
     */
    @Bean
    public PaymentGrpcClient paymentGrpcClient() {
        return new PaymentGrpcClient();
    }
}

