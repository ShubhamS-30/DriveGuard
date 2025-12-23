package com.driveguard.rule_engine.service;

import com.driveguard.rule_engine.AppLogger;
import com.driveguard.rule_engine.dto.Alert;
import com.driveguard.rule_engine.dto.TripRow;
import com.driveguard.rule_engine.entity.RuleConfig;
import com.driveguard.rule_engine.repository.RuleConfigRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RuleEngineService {
    private static final AppLogger log  = AppLogger.getLogger(RuleEngineService.class);

    private final Map<String,RuleStrategy> ruleStrategyMap;
    private final RuleConfigRepository ruleConfigRepository;

    List<RuleConfig> dbRules;

    public RuleEngineService(List<RuleStrategy> strategies, RuleConfigRepository ruleConfigRepository) {
        this.ruleConfigRepository = ruleConfigRepository;
        // Load rules from DB once at startup
        this.dbRules = this.loadRulesFromDb();
        // Map simple class name
        this.ruleStrategyMap = strategies.stream()
                .collect(Collectors.toMap(s -> s.getClass().getSimpleName(), s -> s));
    }

    private List<RuleConfig> loadRulesFromDb(){
        return this.ruleConfigRepository.findAll();
    }

    public Optional<Alert> processRules(String vehicleId, TripRow row){

        for (var config : dbRules) {
            String className = config.getRuleName();

            // 2. Check if the class exists in our Strategy Map
            RuleStrategy strategy = ruleStrategyMap.get(className);

            if (strategy == null) {
                log.warn(String.format("WARNING: Rule logic for %s not found in Java code! Check class names.",className));
                continue;
            }

            // 3. Only execute if enabled in DB
            if (config.isEnabled()) {
                Optional<Alert> alert = strategy.evaluate(vehicleId, row);
                if (alert.isPresent()) return alert;
            }
        }
        return Optional.empty();
    }
}
