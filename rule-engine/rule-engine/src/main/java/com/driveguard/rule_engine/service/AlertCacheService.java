package com.driveguard.rule_engine.service;

import com.driveguard.rule_engine.AppLogger;
import com.driveguard.rule_engine.dto.Alert;
import com.driveguard.rule_engine.dto.RestPage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class AlertCacheService {
    private final StringRedisTemplate redisTemplate;

    // Use ZSET instead of SET for individual member "expiration"
    private static final String ACTIVE_TRIPS_ZSET = "active_trips_zset";

    private static final String PAGE_CACHE_PREFIX = "cache:alerts:";

    // Threshold after which a trip is considered "Dead" if no events arrive
    private static final long INACTIVITY_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(60);

    private final ObjectMapper objectMapper;

    private static final AppLogger log = AppLogger.getLogger(AlertCacheService.class);

    private final Map<String, Long> localHeartbeatCache = new ConcurrentHashMap<>();

    public AlertCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Adds/Updates trip with a timestamp score.
     * If the engine crashes, the timestamp remains static until it's cleared by Cron.
     * location stream is very high frequency (e.g., 10 messages per second per vehicle), calling Redis for every single message might be overkill.
     * You can optimize this by only updating Redis if the "last pulse" was more than, say, 1 minute ago.
     */
    public void addActiveTrip(String tripNumber) {
        long now = System.currentTimeMillis();
        Long lastPulse = localHeartbeatCache.getOrDefault(tripNumber, null);

        // Only hit Redis if we haven't pulsed in the last 60 seconds
        if (lastPulse == null || (now - lastPulse > 60000)) {
            redisTemplate.opsForZSet().add(ACTIVE_TRIPS_ZSET, tripNumber, now);
            localHeartbeatCache.put(tripNumber, now);
        }
    }

    /**
     * Explicitly removes trip and its alerts.
     */
    public void removeActiveTrip(String tripNumber) {
        redisTemplate.opsForZSet().remove(ACTIVE_TRIPS_ZSET, tripNumber);
        log.info("Manually removed trip: " + tripNumber);
    }

    /**
     * Checks membership in the ZSET.
     */
    public boolean isTripActive(String tripNumber) {
        Double score = redisTemplate.opsForZSet().score(ACTIVE_TRIPS_ZSET, tripNumber);
        return score != null;
    }

    /**
     * Returns all members from the ZSET.
     */
    public Set<String> getAllActiveTrips() {
        return redisTemplate.opsForZSet().range(ACTIVE_TRIPS_ZSET, 0, -1);
    }

    // Runs once when the application is fully started and ready
    @EventListener(ApplicationReadyEvent.class)
    public void onStartUp() {
        log.info("System Started: Running initial ghost trip cleanup...");
        cleanupGhostTrips();
    }

    // Runs every 30 minutes
    @Scheduled(cron = "0 0/30 * * * ?")
    public void scheduledCleanup() {
        log.info("Scheduled Cron: Running ghost trip cleanup...");
        cleanupGhostTrips();
    }

    public void cleanupGhostTrips() {
        log.info("Starting ghost trip eviction process...");
        long cutoff = System.currentTimeMillis() - INACTIVITY_TIMEOUT_MS;

        // Efficiently find trips to remove
        Set<String> expiredTrips = redisTemplate.opsForZSet().rangeByScore(ACTIVE_TRIPS_ZSET, 0, cutoff);

        if (expiredTrips != null && !expiredTrips.isEmpty()) {
            log.warn(String.format("Detected %d ghost trips. Starting eviction...", expiredTrips.size()));

            // Optimization: Remove from ZSET in one batch command
            redisTemplate.opsForZSet().removeRangeByScore(ACTIVE_TRIPS_ZSET, 0, cutoff);

            for (String tripNumber : expiredTrips) {
                // Clear the paginated alert cache we built earlier
                evictTripCache(tripNumber);
                log.info("Cleaned up cache for trip: " + tripNumber);
            }
        }
    }

    /**
     * Caches a page of alerts.
     * Generates the key internally using tripNumber and pageable.
     */
    public void cacheAlertPage(String tripNumber, Pageable pageable, Page<Alert> page) {
        String key = generatePageKey(tripNumber, pageable);
        try {
            String json = objectMapper.writeValueAsString(page);
            // Rare access TTL: 30 minutes
            redisTemplate.opsForValue().set(key, json, 30, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            log.error("Failed to cache alert page for trip: " + tripNumber, e);
        }
    }

    /**
     * Retrieves a cached page based on tripNumber and pageable.
     */
    public Page<Alert> getCachedPage(String tripNumber, Pageable pageable) {
        String key = generatePageKey(tripNumber, pageable);
        String json = redisTemplate.opsForValue().get(key);

        if (json == null) return null;

        try {
            return objectMapper.readValue(json, new TypeReference<RestPage<Alert>>() {
            });
        } catch (Exception e) {
            log.error("Error deserializing cached page for trip: " + tripNumber, e);
            return null;
        }
    }

    private String generatePageKey(String tripNumber, Pageable pageable) {
        return PAGE_CACHE_PREFIX + tripNumber + ":p" + pageable.getPageNumber() + ":s" + pageable.getPageSize();
    }

    /**
     * Evicts all cached pages for a specific trip.
     */
    public void evictTripCache(String tripNumber) {
        String pattern = PAGE_CACHE_PREFIX + tripNumber + ":*";

        try {
            // Scan pattern and delete keys in batches
            Set<String> keysToDelete = new HashSet<>();
            ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();

            try (var cursor = redisTemplate.scan(options)) {
                cursor.forEachRemaining(key -> {
                    keysToDelete.add(key);

                    // Delete in small batches of 50 to avoid long-running delete commands
                    if (keysToDelete.size() >= 50) {
                        redisTemplate.delete(keysToDelete);
                        keysToDelete.clear();
                    }
                });
            }

            // Final flush for remaining keys
            if (!keysToDelete.isEmpty()) {
                redisTemplate.delete(keysToDelete);
            }
        } catch (Exception e) {
            log.error(String.format("Error during Redis SCAN for trip: %s", tripNumber), e);
        }
    }
}
