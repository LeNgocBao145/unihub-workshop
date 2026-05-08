package org.unihubworkshop.workshopservice.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@Primary
@ConditionalOnClass(name = "org.springframework.data.redis.core.RedisTemplate")
@ConditionalOnProperty(
    name = "cache.provider",
    havingValue = "redis"
)
public class RedisCacheProvider implements CacheProvider {
    private static final Logger log = LoggerFactory.getLogger(RedisCacheProvider.class);
    
    private final RedisTemplate<String, String> redisTemplate;
    
    public RedisCacheProvider(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    @Override
    public Optional<Integer> get(String key) {
        log.debug("Getting value from Redis for key: {}", key);
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Optional.of(Integer.parseInt(value)) : Optional.empty();
    }
    
    @Override
    public void put(String key, Integer value) {
        log.debug("Putting value in Redis for key: {}", key);
        redisTemplate.opsForValue().set(key, value.toString());
    }
    
    @Override
    public void put(String key, Integer value, long timeout, TimeUnit unit) {
        log.debug("Putting value in Redis for key: {} with TTL", key);
        redisTemplate.opsForValue().set(key, value.toString(), timeout, unit);
    }
    
    @Override
    public void delete(String key) {
        log.debug("Deleting value from Redis for key: {}", key);
        redisTemplate.delete(key);
    }
    
    @Override
    public boolean exists(String key) {
        Boolean hasKey = redisTemplate.hasKey(key);
        return hasKey != null && hasKey;
    }
    
    @Override
    public void clear() {
        log.info("Clearing all Redis cache");
        redisTemplate.getConnectionFactory().getConnection().flushDb();
    }
}