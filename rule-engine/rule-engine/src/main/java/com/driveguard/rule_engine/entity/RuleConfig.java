package com.driveguard.rule_engine.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "rule_config")
@Data
public class RuleConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_name", unique = true, nullable = false)
    private String ruleName;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "description")
    private String description;

    @Column(name = "is_enabled")
    private boolean isEnabled;
}
