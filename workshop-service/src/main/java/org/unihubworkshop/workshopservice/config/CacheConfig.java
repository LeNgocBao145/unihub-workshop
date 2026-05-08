package org.unihubworkshop.workshopservice.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.unihubworkshop.workshopservice.cache.CacheProvider;
import org.unihubworkshop.workshopservice.cache.InMemoryCacheProvider;

/**
 * Cache configuration
 * Configures cache provider based on application properties
 * 
 * To use Redis cache (default if Redis is available), add to application.properties:
 * cache.provider=redis
 * spring.data.redis.host=localhost
 * spring.data.redis.port=6379
 * 
 * To use Memcache, add to application.properties:
 * cache.provider=memcache
 * memcache.servers=localhost:11211
 * 
 * To use in-memory cache (default), add to application.properties:
 * cache.provider=memory
 */
@Configuration
public class CacheConfig {
    
    /**
     * Default cache provider (in-memory)
     * Will be used if Redis is not available or cache.provider is set to 'memory'
     */
    @Bean
    @Primary
    @ConditionalOnProperty(
        name = "cache.provider",
        havingValue = "memory",
        matchIfMissing = true
    )
    public CacheProvider inMemoryCacheProvider() {
        return new InMemoryCacheProvider();
    }
    
    /**
     * Redis template configuration for Redis cache provider
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.data.redis.core.RedisTemplate")
    @ConditionalOnProperty(
        name = "cache.provider",
        havingValue = "redis"
    )
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);
        
        template.afterPropertiesSet();
        return template;
    }
}


