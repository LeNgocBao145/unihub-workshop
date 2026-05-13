package org.unihubworkshop.workshopservice.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.unihubworkshop.workshopservice.cache.CacheProvider;
import org.unihubworkshop.workshopservice.models.Workshop;
import org.unihubworkshop.workshopservice.repositories.WorkshopRepository;
import org.unihubworkshop.workshopservice.exceptions.NotFoundException;

import java.util.Optional;
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
    private static final String QR_CODE_CACHE_KEY_PREFIX = "qrcode:payment:";
    private static final long SLOT_CACHE_TTL_SECONDS = 300; // 5 minutes
    
    private final WorkshopRepository workshopRepository;
    private final CacheProvider cacheProvider;
    private final WorkshopService workshopService;

    public CacheAsideService(WorkshopRepository workshopRepository, CacheProvider cacheProvider, WorkshopService workshopService) {
        this.workshopRepository = workshopRepository;
        this.cacheProvider = cacheProvider;
        this.workshopService = workshopService;
    }

    /**
     * Get available slots with cache aside pattern
     * 1. Check cache first
     * 2. If miss, load from DB
     * 3. Cache the result with TTL to avoid stale data
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
        Workshop workshop = workshopService.findWorkshopById(workshopId);
        
        // Update cache with TTL to ensure fresh data
        cacheProvider.put(cacheKey, workshop.getAvailableSlots(), SLOT_CACHE_TTL_SECONDS, 
                java.util.concurrent.TimeUnit.SECONDS);
        return workshop.getAvailableSlots();
    }

    /**
     * Decrement slots with cache invalidation
     * Uses pessimistic locking to avoid race conditions
     */
    @Transactional
    public boolean decrementSlotWithLock(UUID workshopId) {
        log.info("Attempting to decrement slot for workshop: {}", workshopId);

        Workshop workshop = workshopService.findWorkshopById(workshopId);

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

        Workshop workshop = workshopService.findWorkshopById(workshopId);

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

    /**
     * Get QR code from cache by registration ID
     */
    public Optional<String> getQRCodeFromCache(UUID registrationId) {
        log.debug("Getting QR code from cache for registration: {}", registrationId);
        String cacheKey = QR_CODE_CACHE_KEY_PREFIX + registrationId;
        return cacheProvider.getString(cacheKey);
    }

    /**
     * Put QR code in cache with TTL using registration ID
     * Also caches payment ID mapping for later deletion
     */
    public void putQRCodeToCache(UUID registrationId, UUID paymentId, String qrCodeUrl, long ttlSeconds) {
        log.debug("Putting QR code in cache for registration: {}, payment: {}, TTL: {} seconds", 
                registrationId, paymentId, ttlSeconds);
        
        // Cache QR code with registration ID as key
        String registrationCacheKey = QR_CODE_CACHE_KEY_PREFIX + registrationId;
        cacheProvider.putString(registrationCacheKey, qrCodeUrl, ttlSeconds, java.util.concurrent.TimeUnit.SECONDS);
        
        // Cache payment ID mapping for deletion purposes
        String paymentMappingKey = "payment:registration:" + paymentId;
        cacheProvider.putString(paymentMappingKey, registrationId.toString(), ttlSeconds, java.util.concurrent.TimeUnit.SECONDS);
    }

    /**
     * Delete QR code from cache by registration ID
     */
    public void deleteQRCodeFromCache(UUID registrationId) {
        log.debug("Deleting QR code from cache for registration: {}", registrationId);
        String cacheKey = QR_CODE_CACHE_KEY_PREFIX + registrationId;
        cacheProvider.delete(cacheKey);
    }

    /**
     * Delete QR code from cache by payment ID
     * First resolves payment ID to registration ID, then deletes
     */
    public void deleteQRCodeByCacheKeyFromPaymentId(UUID paymentId) {
        log.debug("Deleting QR code from cache by payment ID: {}", paymentId);
        
        String paymentMappingKey = "payment:registration:" + paymentId;
        var registrationIdStr = cacheProvider.getString(paymentMappingKey);
        
        if (registrationIdStr.isPresent()) {
            try {
                UUID registrationId = UUID.fromString(registrationIdStr.get());
                deleteQRCodeFromCache(registrationId);
                log.info("Successfully deleted cached QR code for registration: {} (via payment: {})", 
                        registrationId, paymentId);
            } catch (IllegalArgumentException e) {
                log.warn("Failed to parse registration ID from cache for payment: {}", paymentId, e);
            }
        }
        
        // Also delete the mapping key itself
        cacheProvider.delete(paymentMappingKey);
    }
    public void syncCacheFromDB(UUID workshopId) {
        // 1. Lấy thông tin mới nhất từ Database
        Workshop workshop = workshopRepository.findById(workshopId)
                .orElseThrow(() -> new NotFoundException("Workshop not found"));

        // 2. Sử dụng Prefix chuẩn của class
        String cacheKey = SLOT_CACHE_KEY_PREFIX + workshopId;
        Integer realAvailableSlots = workshop.getAvailableSlots();

        // 3. Set lại giá trị vào Cache thông qua CacheProvider
        cacheProvider.put(cacheKey, realAvailableSlots, SLOT_CACHE_TTL_SECONDS,
                java.util.concurrent.TimeUnit.SECONDS);

        log.info("Synced cache from DB for workshop {}. Real slots: {}", workshopId, realAvailableSlots);
    }
}
