package com.driveguard.rule_engine.service;

import com.driveguard.rule_engine.AppLogger;
import com.driveguard.rule_engine.Mapper;
import com.driveguard.rule_engine.dto.Alert;
import com.driveguard.rule_engine.entity.TripAlerts;
import com.driveguard.rule_engine.exception.NoAlertsFoundException;
import com.driveguard.rule_engine.repository.TripAlertsRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class AlertPersistenceService {

    private final TripAlertsRepository repository;

    private final AlertCacheService alertCacheService;

    private final Mapper mapper;

    private AlertPersistenceService self;

    private static final AppLogger log = AppLogger.getLogger(AlertPersistenceService.class);

    // Constructor Injection
    public AlertPersistenceService(TripAlertsRepository repository, Mapper mapper, AlertCacheService alertCacheService) {
        this.alertCacheService = alertCacheService;
        this.repository = repository;
        this.mapper = mapper;
    }

    @PostConstruct
    public void init() {
        this.self = this;
    }

    @Transactional
    public void saveAlertBatch(List<TripAlerts> alerts) {
        if (!alerts.isEmpty()) {
            // This triggers the MySQL batching (rewriteBatchedStatements=true)
            repository.saveAll(alerts);
        }
    }

    @Transactional(readOnly = true)
    public Page<Alert> getAlertsByTripNumber(String tripNumber, Pageable pageable) {
        // 1. Check Redis (Pass 2 args)
        Page<Alert> cachedPage = alertCacheService.getCachedPage(tripNumber, pageable);
        if (cachedPage != null) {
            return cachedPage;
        }

        // 2. Database Fallback
        Page<TripAlerts> tripAlerts = repository.findByTripNumber(tripNumber, pageable);
        if (tripAlerts.isEmpty()) {
            throw new NoAlertsFoundException("NO ALERTS FOUND FOR TRIP: " + tripNumber);
        }

        Page<Alert> resultPage = tripAlerts.map(mapper::mapToDTO);

        // 3. Save to Redis (Pass 3 args)
        alertCacheService.cacheAlertPage(tripNumber, pageable, resultPage);

        return resultPage;
    }

    @Scheduled(fixedRate = 60000)
    public void refreshActiveTripCaches() {
        // Get all trips that are currently active
        Set<String> activeTrips = alertCacheService.getAllActiveTrips();

        if (activeTrips == null || activeTrips.isEmpty()) return;

        log.info(String.format("Cron: Warming cache for %s active trips", activeTrips.size()));

        // We typically refresh Page 0 (the most viewed page)
        Pageable firstPage = PageRequest.of(0, 20);

        for (String tripNumber : activeTrips) {
            try {
                // Call through self-reference to properly handle @Transactional
                if (self != null) {
                    self.getAlertsByTripNumber(tripNumber, firstPage);
                } else {
                    log.info(String.format("Self-reference is null while refreshing cache for trip: %s, Unable to refresh keys for active trips.", tripNumber));
                }
            } catch (NoAlertsFoundException ignored) {
                // It's okay if an active trip doesn't have alerts yet
            }
        }
    }
}
