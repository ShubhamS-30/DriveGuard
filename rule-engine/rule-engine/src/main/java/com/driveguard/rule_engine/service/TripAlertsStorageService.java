package com.driveguard.rule_engine.service;

import com.driveguard.rule_engine.AppLogger;
import com.driveguard.rule_engine.Mapper;
import com.driveguard.rule_engine.dto.Alert;
import com.driveguard.rule_engine.entity.TripAlerts;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class TripAlertsStorageService {
    private static final AppLogger log = AppLogger.getLogger(TripAlertsStorageService.class);

    // Thread-safe buffer to prevent data loss during high-volume streaming
    private final List<TripAlerts> buffer = Collections.synchronizedList(new ArrayList<>());

    @Value("${data.alerts.batch.size}")
    private int batchSize;

    private final AlertPersistenceService persistenceService;

    private final AlertCacheService alertCacheService;

    private final Mapper mapper;

    public TripAlertsStorageService(AlertPersistenceService persistenceService, Mapper mapper, AlertCacheService alertCacheService) {
        this.persistenceService = persistenceService;
        this.mapper = mapper;
        this.alertCacheService = alertCacheService;
    }

    @KafkaListener(topics = "${data.cab.alert.topic.name}", groupId = "alert-storage-group")
    public void consumeAndBufferAlert(Alert alert) {
        // 1. CACHE EVICTION: Delete stale cached pages for this trip
        // Do this FIRST so that any concurrent API calls don't get old data
        try {
            alertCacheService.evictTripCache(alert.getTripNumber());
        } catch (Exception e) {
            log.error("Failed to evict cache for trip: " + alert.getTripNumber(), e);
        }

        // 2. Refresh active status (Heartbeat)
        alertCacheService.addActiveTrip(alert.getTripNumber());

        // 3. MySQL Persistence (Batching)
        TripAlerts entity = mapper.mapToEntity(alert);
        buffer.add(entity);

        if (buffer.size() >= batchSize) {
            flushBuffer();
        }
    }


    /**
     * Periodically flushes the buffer every 5 seconds.
     * This handles cases where the batch size isn't reached quickly.
     */
    @Scheduled(fixedRate = 5000)
    public void flushBuffer() {
        if (buffer.isEmpty()) return;

        List<TripAlerts> toSave;
        synchronized (buffer) {
            toSave = new ArrayList<>(buffer);
            buffer.clear();
        }

        try {
            log.info(String.format("Performing MySQL batch insert for %s alerts.", toSave.size()));
            persistenceService.saveAlertBatch(toSave); // Hibernate uses rewriteBatchedStatements=true here
        } catch (Exception e) {
            log.error("Failed to persist alert batch to MySQL", e);
        }
    }

    /**
     * Shutdown Hook: Flushes any remaining alerts in the buffer to MySQL
     * before the application completely stops.
     */
    @PreDestroy
    public void onShutdown() {
        log.info("Shutdown initiated. Checking for remaining alerts in buffer...");
        if (!buffer.isEmpty()) {
            log.info(String.format("Flushing %s remaining alerts before exit.", buffer.size()));
            flushBuffer();
        }
        log.info("Shutdown flush complete.");
    }
}
