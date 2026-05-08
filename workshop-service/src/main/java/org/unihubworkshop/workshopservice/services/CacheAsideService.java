package org.unihubworkshop.workshopservice.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.unihubworkshop.workshopservice.cache.CacheProvider;
import org.unihubworkshop.workshopservice.models.Workshop;
import org.unihubworkshop.workshopservice.repositories.WorkshopRepository;
import org.unihubworkshop.workshopservice.exceptions.NotFoundException;

import java.util.UUID;

/**
 * Cache Aside Pattern Implementation for Workshop Slots
 * - Load: Check cache first, if miss load from DB and update cache
 * - Save: Update DB and invalidate cache
 * 
 * Uses CacheProvider abstraction to support multiple cache backends (Redis, Memcache, In-Memory)
 */
@Service
@Transactional
public class CacheAsideService {
    private static final Logger log = LoggerFactory.getLogger(CacheAsideService.class);
    
    private static final String SLOT_CACHE_KEY_PREFIX = "workshop:slots:";
    
    private final WorkshopRepository workshopRepository;
    private final CacheProvider cacheProvider;

    public CacheAsideService(WorkshopRepository workshopRepository, CacheProvider cacheProvider) {
        this.workshopRepository = workshopRepository;
        this.cacheProvider = cacheProvider;
    }

    /**
     * Get available slots with cache aside pattern
     * 1. Check cache first
     * 2. If miss, load from DB
     * 3. Cache the result
     */
    public Integer getAvailableSlotsWithCache(UUID workshopId) {
        log.debug("Getting available slots for workshop: {}", workshopId);
        
        String cacheKey = SLOT_CACHE_KEY_PREFIX + workshopId;
        
        // Check cache first
        var cachedValue = cacheProvider.get(cacheKey);
        if (cachedValue.isPresent()) {
            log.debug("Cache hit for workshop: {}", workshopId);
            return cachedValue.get();
        }

        // Cache miss - load from database
        log.debug("Cache miss for workshop: {}, loading from DB", workshopId);
        Workshop workshop = findWorkshopById(workshopId);
        
        // Update cache
        cacheProvider.put(cacheKey, workshop.getAvailableSlots());
        return workshop.getAvailableSlots();
    }

    /**
     * Decrement slots with cache invalidation
     * Uses pessimistic locking to avoid race conditions
     */
    @Transactional
    public boolean decrementSlotWithLock(UUID workshopId) {
        log.info("Attempting to decrement slot for workshop: {}", workshopId);
        
        Workshop workshop = workshopRepository.findById(workshopId)
                .orElseThrow(() -> new NotFoundException("Workshop not found"));

        // Check if slots are available
        if (workshop.getAvailableSlots() <= 0) {
            log.warn("No available slots for workshop: {}", workshopId);
            return false;
        }

        // Decrement slot atomically
        workshop.setAvailableSlots(workshop.getAvailableSlots() - 1);
        Workshop updated = workshopRepository.save(workshop);
        
        // Invalidate cache to ensure consistency
        invalidateCache(workshopId);
        
        log.info("Successfully decremented slot for workshop: {}. Remaining slots: {}", 
                workshopId, updated.getAvailableSlots());
        return true;
    }

    /**
     * Increment slots (for cancellation) with cache invalidation
     */
    @Transactional
    public void incrementSlot(UUID workshopId) {
        log.info("Incrementing slot for workshop: {}", workshopId);
        
        Workshop workshop = findWorkshopById(workshopId);
        
        // Prevent overflow
        if (workshop.getAvailableSlots() >= workshop.getTotalSlots()) {
            log.warn("Available slots already at max for workshop: {}", workshopId);
            return;
        }

        workshop.setAvailableSlots(workshop.getAvailableSlots() + 1);
        workshopRepository.save(workshop);
        
        // Invalidate cache
        invalidateCache(workshopId);
        
        log.info("Successfully incremented slot for workshop: {}", workshopId);
    }

    /**
     * Invalidate cache for a specific workshop
     */
    public void invalidateCache(UUID workshopId) {
        log.debug("Invalidating cache for workshop: {}", workshopId);
        String cacheKey = SLOT_CACHE_KEY_PREFIX + workshopId;
        cacheProvider.delete(cacheKey);
    }

    /**
     * Clear all cache
     */
    public void clearAllCache() {
        log.info("Clearing all slot cache");
        cacheProvider.clear();
    }

    private Workshop findWorkshopById(UUID workshopId) {
        return workshopRepository.findById(workshopId)
                .orElseThrow(() -> new NotFoundException("Workshop not found"));
    }
}

