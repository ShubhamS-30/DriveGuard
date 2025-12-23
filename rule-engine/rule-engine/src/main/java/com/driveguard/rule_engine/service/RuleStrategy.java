package com.driveguard.rule_engine.service;

import com.driveguard.rule_engine.dto.Alert;
import com.driveguard.rule_engine.dto.TripRow;

import java.util.Optional;

public interface RuleStrategy {

    String getRuleId();

    /**
     * @param vehicleId The unique ID of the vehicle being processed.
     * @param row The current GPS/Trip data point.
     * @return An Optional containing an Alert if the rule is triggered, otherwise empty.
     */
    Optional<Alert> evaluate(String vehicleId, TripRow row);
}
