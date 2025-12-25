package com.driveguard.rule_engine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleState {
    // Required for suppression logic: tracks when each rule last fired
    private Map<String, Long> lastTriggeredMap = new HashMap<>();

    // The previous TripRow processed for this vehicle
    private TripRow previousTripRow;

    // The most recent alert generated (used to bridge back to the stream)
    private Alert lastAlert;
}
