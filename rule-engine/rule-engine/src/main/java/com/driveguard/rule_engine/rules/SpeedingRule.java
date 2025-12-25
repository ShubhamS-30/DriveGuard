package com.driveguard.rule_engine.rules;

import com.driveguard.rule_engine.dto.Alert;
import com.driveguard.rule_engine.dto.TripRow;
import com.driveguard.rule_engine.dto.VehicleState;
import com.driveguard.rule_engine.service.RuleStrategy;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SpeedingRule implements RuleStrategy {

    private static final double SPEEDING_THRESHOLD = 5.0; // km/h over the limit

    @Override
    public String getRuleId() {
        return "SpeedingRule";
    }

    @Override
    public Optional<Alert> evaluate(String vehicleId, TripRow row, VehicleState state) {
        if (row.getTarget_speed() == null || row.getSpeed_osrm() == null) return Optional.empty();

        double speed = Double.parseDouble(row.getTarget_speed());
        double limit = Double.parseDouble(row.getSpeed_osrm());

        if (speed > (limit + SPEEDING_THRESHOLD)) {
            return Optional.of(new Alert(vehicleId, getRuleId(), "OVER SPEEDING DETECTED",
                    System.currentTimeMillis(), Double.parseDouble(row.getLatitude()), Double.parseDouble(row.getLongitude())));
        }
        return Optional.empty();
    }
}
