package com.driveguard.rule_engine.service;

import com.driveguard.rule_engine.Mapper;
import com.driveguard.rule_engine.dto.Alert;
import com.driveguard.rule_engine.entity.TripAlerts;
import com.driveguard.rule_engine.exception.NoAlertsFoundException;
import com.driveguard.rule_engine.repository.TripAlertsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class AlertPersistenceService {

    private final TripAlertsRepository repository;

    private final AlertCacheService alertCacheService;

    private final Mapper mapper;

    // Constructor Injection
    public AlertPersistenceService(TripAlertsRepository repository, Mapper mapper, AlertCacheService alertCacheService) {
        this.alertCacheService = alertCacheService;
        this.repository = repository;
        this.mapper = mapper;
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

        // 1. Logic for Active Trips (Hot Path)
        if (alertCacheService.isTripActive(tripNumber)) {
            List<Alert> pagedAlerts = alertCacheService.getAlerts(tripNumber, pageable);
            long totalElements = alertCacheService.getAlertCount(tripNumber);

            if (pagedAlerts.isEmpty() && pageable.getOffset() == 0) {
                throw new NoAlertsFoundException("NO ALERTS FOUND FOR ACTIVE TRIP: " + tripNumber);
            }

            return new PageImpl<>(pagedAlerts, pageable, totalElements);
        }

        // 2. Fallback for Historical Trips (Cold Path)
        Page<TripAlerts> tripAlerts = repository.findByTripNumber(tripNumber, pageable);

        if (tripAlerts.isEmpty()) {
            throw new NoAlertsFoundException("NO ALERTS FOUND IN DB FOR TRIP: " + tripNumber);
        }

        return tripAlerts.map(mapper::mapToDTO);
    }
}
