package org.unihubworkshop.paymentservice.cache;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis cache to store user email associated with payments
 * Email is cached when charge payment is created and retrieved when webhook is processed
 * Cache automatically expires after 30 minutes to avoid memory leaks
 */
@Component
@RequiredArgsConstructor
public class PaymentEmailCache {
    private static final Logger log = LoggerFactory.getLogger(PaymentEmailCache.class);
    private static final String PAYMENT_EMAIL_PREFIX = "payment_email:";
    private static final long CACHE_EXPIRY_MINUTES = 30;

    private final RedisTemplate<String, String> redisTemplate;

    public void putEmail(UUID paymentId, String userEmail) {
        String key = buildKey(paymentId);
        redisTemplate.opsForValue().set(key, userEmail, CACHE_EXPIRY_MINUTES, TimeUnit.MINUTES);
        log.debug("Cached user email for payment: {}", paymentId);
    }

    public String getEmail(UUID paymentId) {
        String key = buildKey(paymentId);
        return redisTemplate.opsForValue().get(key);
    }

    public String removeEmail(UUID paymentId) {
        String key = buildKey(paymentId);
        String email = getEmail(paymentId);
        if (email != null) {
            Boolean deleted = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(deleted)) {
                log.debug("Removed user email from cache for payment: {}", paymentId);
            }
        }
        return email;
    }

    public void clear() {
        // Clear all payment email cache keys
        var keys = redisTemplate.keys(PAYMENT_EMAIL_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Cleared all payment email cache entries");
        }
    }

    private String buildKey(UUID paymentId) {
        return PAYMENT_EMAIL_PREFIX + paymentId;
    }
}

