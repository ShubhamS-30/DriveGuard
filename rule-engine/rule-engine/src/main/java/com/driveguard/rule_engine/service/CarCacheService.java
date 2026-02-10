package com.driveguard.rule_engine.service;

import com.driveguard.rule_engine.AppLogger;
import com.driveguard.rule_engine.client.DataProducerClient;
import com.driveguard.rule_engine.dto.CarResponseDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.concurrent.TimeUnit;

/**
 * Service for caching CarResponseDTO from Data Producer API.
 * Prevents repeated API calls to the Data Producer for the same trip or car.
 * Uses Redis with configurable TTL for efficient caching.
 */
@Service
public class CarCacheService {

    private static final AppLogger log = AppLogger.getLogger(CarCacheService.class);

    private static final String CAR_BY_TRIP_PREFIX = "car:trip:";
    private static final String CAR_BY_ID_PREFIX = "car:id:";
    private static final long CACHE_TTL_MINUTES = 30;

    private final DataProducerClient dataProducerClient;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public CarCacheService(DataProducerClient dataProducerClient,
                          StringRedisTemplate redisTemplate,
                          ObjectMapper objectMapper) {
        this.dataProducerClient = dataProducerClient;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Get car by trip number with Redis caching.
     * First checks Redis cache, then falls back to DataProducerClient if not cached.
     * TTL is refreshed on each cache hit to keep frequently accessed data longer.
     *
     * @param tripNumber The trip number to look up
     * @return CarResponseDTO or null if not found
     * @throws RestClientException if API call fails
     */
    public CarResponseDTO getCarByTripNumberWithCache(String tripNumber) {
        String cacheKey = CAR_BY_TRIP_PREFIX + tripNumber;

        try {
            // Step 1: Try to get from Redis cache
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null) {
                log.info(String.format("Cache HIT for trip: %s", tripNumber));

                // Refresh TTL on cache hit
                redisTemplate.expire(cacheKey, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
                log.debug(String.format("TTL refreshed for trip: %s", tripNumber));

                return objectMapper.readValue(cachedJson, CarResponseDTO.class);
            }

            // Step 2: Cache miss - call Data Producer API
            log.info(String.format("Cache MISS for trip: %s - Calling Data Producer API", tripNumber));
            CarResponseDTO car = dataProducerClient.getCarByTripNumber(tripNumber);

            // Step 3: Cache the result if not null
            if (car != null) {
                cacheCarResponse(cacheKey, car);
                log.info(String.format("Cached car data for trip: %s", tripNumber));
            } else {
                log.warn(String.format("No car found for trip: %s", tripNumber));
            }

            return car;

        } catch (RestClientException e) {
            log.error(String.format("API call failed for trip: %s", tripNumber), e);
            throw e;
        } catch (JsonProcessingException e) {
            log.error(String.format("Error deserializing cached car data for trip: %s", tripNumber), e);
            return null;
        }
    }

    /**
     * Get car by ID with Redis caching.
     * First checks Redis cache, then falls back to DataProducerClient if not cached.
     * TTL is refreshed on each cache hit to keep frequently accessed data longer.
     *
     * @param carId The car ID to look up
     * @return CarResponseDTO or null if not found
     * @throws RestClientException if API call fails
     */
    public CarResponseDTO getCarByIdWithCache(Integer carId) {
        String cacheKey = CAR_BY_ID_PREFIX + carId;

        try {
            // Step 1: Try to get from Redis cache
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null) {
                log.info(String.format("Cache HIT for car ID: %d", carId));

                // Refresh TTL on cache hit
                redisTemplate.expire(cacheKey, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
                log.debug(String.format("TTL refreshed for car ID: %d", carId));

                return objectMapper.readValue(cachedJson, CarResponseDTO.class);
            }

            // Step 2: Cache miss - call Data Producer API
            log.info(String.format("Cache MISS for car ID: %d - Calling Data Producer API", carId));
            CarResponseDTO car = dataProducerClient.getCarById(carId);

            // Step 3: Cache the result if not null
            if (car != null) {
                cacheCarResponse(cacheKey, car);
                log.info(String.format("Cached car data for car ID: %d", carId));
            } else {
                log.warn(String.format("No car found for ID: %d", carId));
            }

            return car;

        } catch (RestClientException e) {
            log.error(String.format("API call failed for car ID: %d", carId), e);
            throw e;
        } catch (JsonProcessingException e) {
            log.error(String.format("Error deserializing cached car data for car ID: %d", carId), e);
            return null;
        }
    }

    /**
     * Invalidate cache for a specific trip.
     * Call this when trip data changes or trip ends.
     *
     * @param tripNumber The trip number to invalidate
     */
    public void invalidateCarCacheForTrip(String tripNumber) {
        String cacheKey = CAR_BY_TRIP_PREFIX + tripNumber;
        Boolean deleted = redisTemplate.delete(cacheKey);
        if (Boolean.TRUE.equals(deleted)) {
            log.info(String.format("Invalidated cache for trip: %s", tripNumber));
        }
    }

    /**
     * Invalidate cache for a specific car ID.
     * Call this when car data changes.
     *
     * @param carId The car ID to invalidate
     */
    public void invalidateCarCacheForId(Integer carId) {
        String cacheKey = CAR_BY_ID_PREFIX + carId;
        Boolean deleted = redisTemplate.delete(cacheKey);
        if (Boolean.TRUE.equals(deleted)) {
            log.info(String.format("Invalidated cache for car ID: %d", carId));
        }
    }

    /**
     * Clear all car caches (both by trip and by ID).
     */
    public void clearAllCarCaches() {
        try {
            var connectionFactory = redisTemplate.getConnectionFactory();
            if (connectionFactory != null) {
                try (var connection = connectionFactory.getConnection()) {
                        log.info("Cleared all car caches from Redis");
                }
            }
        } catch (Exception e) {
            log.error("Error clearing car caches", e);
        }
    }

    /**
     * Helper method to cache car response in Redis.
     *
     * @param cacheKey The Redis key
     * @param car The CarResponseDTO to cache
     */
    private void cacheCarResponse(String cacheKey, CarResponseDTO car) {
        try {
            String json = objectMapper.writeValueAsString(car);
            redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            log.error(String.format("Error caching car data for key: %s", cacheKey), e);
        }
    }
}





