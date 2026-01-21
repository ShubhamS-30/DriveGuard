package com.driveguard.rule_engine.service;

import com.driveguard.rule_engine.AppLogger;
import com.driveguard.rule_engine.dto.Alert;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class AlertCacheService {
    private final StringRedisTemplate redisTemplate;

    // Prefix for the alert keys to keep Redis organized
    private static final String ALERT_KEY_PREFIX = "alerts:trip:";
    // Use ZSET instead of SET for individual member "expiration"
    private static final String ACTIVE_TRIPS_ZSET = "active_trips_zset";

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
        deleteAlerts(tripNumber);
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

    /**
     * CRON JOB: The "Consistency Guard".
     * Runs 30 minutes to clean up trips that missed their "Trip End" event.
     */
    @Scheduled(cron = "0 0/30 * * * ?")
    public void cleanupGhostTrips() {
        long cutoff = System.currentTimeMillis() - INACTIVITY_TIMEOUT_MS;

        // Find all trips that haven't sent an update in 30 minutes
        Set<String> expiredTrips = redisTemplate.opsForZSet().rangeByScore(ACTIVE_TRIPS_ZSET, 0, cutoff);

        if (expiredTrips != null && !expiredTrips.isEmpty()) {
            for (String tripNumber : expiredTrips) {
                log.warn(String.format("CRON: Detected ghost trip (no activity for %s ms). Cleaning: %s", INACTIVITY_TIMEOUT_MS, tripNumber));
                removeActiveTrip(tripNumber); // Deletes alerts and removes from ZSET
            }
        }
    }

    /**
     * Store a new alert in the trip's list.
     * Uses LPUSH (List Push) to add to the head of the list.
     */
    public void pushAlert(String tripNumber, Alert alert) {

        if (alert == null && isTripActive(tripNumber)) {
            return;
        }
        try {
            // Refresh activity in the ZSET for the Cron Job
            addActiveTrip(tripNumber);
            String alertJson = objectMapper.writeValueAsString(alert);
            String key = ALERT_KEY_PREFIX + tripNumber;

            // 1. Push the alert to the list
            redisTemplate.opsForList().leftPush(key, alertJson);

            // 2. Set an expiration to avoid memory leaks
            redisTemplate.expire(key, 5, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.error(String.format("Error serializing alert for trip %s: %s", tripNumber, e.getMessage()), e);
        }
    }

    /**
     * Retrieve all alerts for a trip from Redis.
     * Uses LRANGE (List Range) - O(N).
     */
    public List<Object> getAlerts(String tripNumber) {
        if (!isTripActive(tripNumber)) {
            return Collections.emptyList(); // No alerts for inactive trips
        }
        String key = ALERT_KEY_PREFIX + tripNumber;
        // 0 to -1 returns all elements in the list
        return Collections.singletonList(redisTemplate.opsForList().range(key, 0, -1));
    }

    /**
     * Delete all alerts for a specific trip.
     * Used when a trip ends.
     */
    public void deleteAlerts(String tripNumber) {
        String key = ALERT_KEY_PREFIX + tripNumber;
        redisTemplate.delete(key);
    }
}
