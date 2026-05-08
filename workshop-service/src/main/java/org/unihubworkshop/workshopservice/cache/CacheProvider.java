package org.unihubworkshop.workshopservice.cache;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Cache abstraction interface
 * Allows switching between different cache implementations (Redis, Memcache, etc.)
 * without changing business logic
 */
public interface CacheProvider {
    
    /**
     * Get value from cache
     * @param key cache key
     * @return Optional containing the cached value or empty if not found
     */
    Optional<Integer> get(String key);
    
    /**
     * Put value in cache
     * @param key cache key
     * @param value value to cache
     */
    void put(String key, Integer value);
    
    /**
     * Put value in cache with expiration time
     * @param key cache key
     * @param value value to cache
     * @param timeout expiration time
     * @param unit time unit
     */
    void put(String key, Integer value, long timeout, TimeUnit unit);
    
    /**
     * Remove value from cache
     * @param key cache key
     */
    void delete(String key);
    
    /**
     * Check if key exists in cache
     * @param key cache key
     * @return true if exists, false otherwise
     */
    boolean exists(String key);
    
    /**
     * Clear all cache
     */
    void clear();
}

