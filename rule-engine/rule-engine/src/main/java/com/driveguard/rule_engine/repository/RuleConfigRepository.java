package com.driveguard.rule_engine.repository;

import com.driveguard.rule_engine.entity.RuleConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RuleConfigRepository extends JpaRepository<RuleConfig,Long> {
    Optional<RuleConfig> findByRuleName(String ruleName);
}
