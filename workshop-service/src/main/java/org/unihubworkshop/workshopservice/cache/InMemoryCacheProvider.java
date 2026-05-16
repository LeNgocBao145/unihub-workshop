package org.unihubworkshop.workshopservice.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * In-memory cache implementation
 * Useful for development and testing
 * Not recommended for production with multiple instances
 */
@Component("inMemoryCacheProvider")
public class InMemoryCacheProvider implements CacheProvider {
    private static final Logger log = LoggerFactory.getLogger(InMemoryCacheProvider.class);
    
    private final Map<String, Integer> cache = new HashMap<>();
    private final Map<String, String> stringCache = new HashMap<>();

    @Override
    public void put(String key, Integer value, long timeout, TimeUnit unit) {
        // For in-memory implementation, we ignore TTL
        log.debug("Putting value in in-memory cache for key: {} (TTL ignored)", key);
        cache.put(key, value);
    }

    @Override
    public void put(String key, Integer value) {
        log.debug("Putting value in in-memory cache for key: {}", key);
        cache.put(key, value);
    }

    @Override
    public Optional<Integer> get(String key) {
        log.debug("Getting value from in-memory cache for key: {}", key);
        return Optional.ofNullable(cache.get(key));
    }

    @Override
    public Optional<String> getString(String key) {
        log.debug("Getting string value from in-memory cache for key: {}", key);
        return Optional.ofNullable(stringCache.get(key));
    }

    @Override
    public void putString(String key, String value, long timeout, TimeUnit unit) {
        // For in-memory implementation, we ignore TTL
        log.debug("Putting string value in in-memory cache for key: {} (TTL ignored)", key);
        stringCache.put(key, value);
    }

    @Override
    public synchronized <T> T execute(String script, Class<T> resultType, List<String> keys) {
        log.debug("Executing in-memory cache script for keys: {}", keys);

        if (keys == null || keys.isEmpty()) {
            throw new IllegalArgumentException("At least one cache key is required");
        }

        String key = keys.get(0);
        Integer current = cache.get(key);

        if (current == null) {
            return castResult(resultType, Long.valueOf(-2));
        }

        if (current <= 0) {
            return castResult(resultType, Long.valueOf(-1));
        }

        int updated = current - 1;
        cache.put(key, updated);
        return castResult(resultType, Long.valueOf(updated));
    }

    private <T> T castResult(Class<T> resultType, Long value) {
        if (resultType == Long.class) {
            return resultType.cast(value);
        }
        if (resultType == Integer.class) {
            return resultType.cast(value.intValue());
        }
        throw new UnsupportedOperationException("Unsupported execute result type: " + resultType.getName());
    }

    // ...existing code...
    
    @Override
    public void delete(String key) {
        log.debug("Deleting value from in-memory cache for key: {}", key);
        cache.remove(key);
    }
    
    @Override
    public boolean exists(String key) {
        return cache.containsKey(key);
    }
    
    @Override
    public void clear() {
        log.info("Clearing all in-memory cache");
        cache.clear();
        stringCache.clear();
    }
}

