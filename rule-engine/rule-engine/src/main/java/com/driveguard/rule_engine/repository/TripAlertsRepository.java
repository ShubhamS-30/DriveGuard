package com.driveguard.rule_engine.repository;

import com.driveguard.rule_engine.entity.TripAlerts;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripAlertsRepository extends JpaRepository<TripAlerts, Long> {
    // Used for fetching data by trip number ordered by timestamp
    List<TripAlerts> findByTripNumberOrderByTimestampAsc(String tripNumber);

    // Used for pagination in AlertPersistenceService
    Page<TripAlerts> findByTripNumber(String tripNumber, Pageable pageable);
}
