package com.driveguard.rule_engine.rules;

import com.driveguard.rule_engine.dto.Alert;
import com.driveguard.rule_engine.dto.TripRow;
import com.driveguard.rule_engine.dto.VehicleState;
import com.driveguard.rule_engine.service.RuleStrategy;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DangerousTurningRule implements RuleStrategy {

    private static final double TURNING_ANGLE_THRESHOLD = 30.0; // degrees

    @Override
    public String getRuleId() {
        return "DangerousTurningRule";
    }

    @Override
    public Optional<Alert> evaluate(String vehicleId, TripRow row, VehicleState state) {

        if (row.getTarget_speed() == null || row.getSpeed_osrm() == null || row.getAzimuth_diff() == null)
            return Optional.empty();
        try {
            double speed = Double.parseDouble(row.getTarget_speed());
            double angle = Math.abs(Double.parseDouble(row.getAzimuth_diff()));
            double limit = Double.parseDouble(row.getSpeed_osrm());
            if (speed > limit && angle > TURNING_ANGLE_THRESHOLD) {
                return Optional.of(new Alert(
                        vehicleId,
                        getRuleId(),
                        String.format("Sharp turn of %.1f degrees detected while speeding", angle),
                        System.currentTimeMillis(),
                        row.getTripNumber(),
                        Double.parseDouble(row.getLatitude()),
                        Double.parseDouble(row.getLongitude())
                ));
            }
        } catch (NumberFormatException ex) {
            return Optional.of(new Alert(vehicleId, getRuleId(), String.format("Invalid number format %s", ex.getMessage()),
                    System.currentTimeMillis(), row.getTripNumber(), Double.parseDouble(row.getLatitude()), Double.parseDouble(row.getLongitude())));
        }
        return Optional.empty();
    }
}
