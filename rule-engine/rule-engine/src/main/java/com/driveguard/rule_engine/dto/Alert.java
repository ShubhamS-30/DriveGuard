package com.driveguard.rule_engine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Alert {

    // Unique identifier for the alert
    private Long id;

    // The ID of the vehicle this alert is for
    private String vehicleId;

    // Name of the rule that generated the alert
    private String alertType;

    // A human-readable message explaining the alert
    private String details;

    // The timestamp (in epoch milliseconds) when the alert was generated
    private LocalDateTime timestamp;

    // The trip number associated with the alert
    private String tripNumber;

    // The location where the alert occurred
    private double latitude;
    private double longitude;

}
