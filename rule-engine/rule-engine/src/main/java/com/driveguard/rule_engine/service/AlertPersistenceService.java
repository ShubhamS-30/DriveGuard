package com.driveguard.rule_engine.service;

import com.driveguard.rule_engine.AppLogger;
import com.driveguard.rule_engine.entity.TripAlerts;
import com.driveguard.rule_engine.repository.TripAlertsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AlertPersistenceService {

    private final TripAlertsRepository repository;

    // Constructor Injection
    public AlertPersistenceService(TripAlertsRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void saveAlertBatch(List<TripAlerts> alerts) {
        if (!alerts.isEmpty()) {
            // This triggers the MySQL batching (rewriteBatchedStatements=true)
            repository.saveAll(alerts);
        }
    }
}
