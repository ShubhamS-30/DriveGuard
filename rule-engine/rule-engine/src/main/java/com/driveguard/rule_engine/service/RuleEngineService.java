package com.driveguard.rule_engine.service;

import com.driveguard.rule_engine.AppLogger;
import com.driveguard.rule_engine.dto.Alert;
import com.driveguard.rule_engine.dto.TripRow;
import com.driveguard.rule_engine.dto.VehicleState;
import com.driveguard.rule_engine.entity.RuleConfig;
import com.driveguard.rule_engine.repository.RuleConfigRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RuleEngineService {
    private static final AppLogger log = AppLogger.getLogger(RuleEngineService.class);

    private final Map<String, RuleStrategy> ruleStrategyMap;
    private final RuleConfigRepository ruleConfigRepository;

    List<RuleConfig> dbRules;

    private static final double DEFAULT_SUPPRESSION_INTERVAL_MS = 20000.0;

    public RuleEngineService(List<RuleStrategy> strategies, RuleConfigRepository ruleConfigRepository) {
        this.ruleConfigRepository = ruleConfigRepository;
        // Load rules from DB once at startup
        this.dbRules = this.loadRulesFromDb();
        // Map simple class name
        this.ruleStrategyMap = strategies.stream()
                .collect(Collectors.toMap(s -> s.getClass().getSimpleName(), s -> s));
    }

    private List<RuleConfig> loadRulesFromDb() {
        return this.ruleConfigRepository.findAll();
    }

    public Optional<Alert> processRules(String vehicleId, TripRow row, VehicleState state) {


        for (var config : dbRules) {
            if (!config.isEnabled()) continue;

            String className = config.getRuleName();

            // Check if the class exists in our Strategy Map
            RuleStrategy strategy = ruleStrategyMap.get(className);

            if (strategy == null) {
                log.warn(String.format("WARNING: Rule logic for %s not found in Java code! Check class names.", className));
                continue;
            }

            // Check Per-Rule Suppression
            Long lastTriggered = state.getLastTriggeredMap().get(className);
            long currentTime = System.currentTimeMillis();
            if (lastTriggered != null && (currentTime - lastTriggered < DEFAULT_SUPPRESSION_INTERVAL_MS)) {
                continue;
            }

            // Evaluate Rule
            Optional<Alert> alert = strategy.evaluate(vehicleId, row, state);
            if (alert.isPresent()) {
                // 3. Update State on Trigger
                state.getLastTriggeredMap().put(className, currentTime);

                // return alert
                return alert;
            }
        }
        return Optional.empty();
    }
}
