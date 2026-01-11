package com.driveguard.rule_engine.service;

import com.driveguard.rule_engine.Mapper;
import com.driveguard.rule_engine.dto.Alert;
import com.driveguard.rule_engine.entity.TripAlerts;
import com.driveguard.rule_engine.exception.NoAlertsFoundException;
import com.driveguard.rule_engine.repository.TripAlertsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AlertPersistenceService {

    private final TripAlertsRepository repository;

    private final Mapper mapper;

    // Constructor Injection
    public AlertPersistenceService(TripAlertsRepository repository, Mapper mapper) {
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
        Page<TripAlerts> tripAlerts = repository.findByTripNumber(tripNumber, pageable);
        if(tripAlerts.isEmpty()){
            throw new NoAlertsFoundException(String.format("NO ALERTS FOUND FOR TRIP NUMBER : %s", tripNumber));
        }
        return tripAlerts.map(mapper::mapToDTO);
    }
}
