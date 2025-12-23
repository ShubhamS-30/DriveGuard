package com.driveguard.rule_engine.rules;

import com.driveguard.rule_engine.dto.Alert;
import com.driveguard.rule_engine.dto.TripRow;
import com.driveguard.rule_engine.service.RuleStrategy;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SpeedingRule implements RuleStrategy {

    private static final double SPEEDING_THRESHOLD = 5.0; // km/h over the limit
    private static final String SPEEDING_THRESHOLD_KEY = "speedingThreshold";

    @Override
    public String getRuleId() {
        return "SpeedingRule";
    }

    @Override
    public Optional<Alert> evaluate(String vehicleId, TripRow row) {
        if (row.getTarget_speed() == null || row.getSpeed_osrm() == null) return Optional.empty();

        double speed = Double.parseDouble(row.getTarget_speed());
        double limit = Double.parseDouble(row.getSpeed_osrm());

        // TODO: Fetch threshold from configuration, make table and add records to db and populate hashMap in RuleEngineService using post construct, and use here

        // In a real scenario, fetch '5.0' from rule_constants table
        if (speed > (limit + SPEEDING_THRESHOLD)) {
            System.out.println(String.format("SpeedingRule: Vehicle %s speed=%f limit=%f", vehicleId, speed, limit));
            return Optional.of(new Alert(vehicleId, getRuleId(), "Speeding detected",
                    System.currentTimeMillis(), Double.parseDouble(row.getLatitude()), Double.parseDouble(row.getLongitude())));
        }
        return Optional.empty();
    }
}
