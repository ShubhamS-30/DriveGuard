package com.driveguard.rule_engine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Alert {

    // The ID of the vehicle this alert is for
    private String vehicleId;

    // The type of alert, e.g., "SPEEDING", "HARSH_BRAKING", "UNUSUAL_STOP"
    private String alertType;

    // A human-readable message explaining the alert
    private String details;

    // The timestamp (in epoch milliseconds) when the alert was generated
    private long timestamp;

    // The location where the alert occurred
    private double latitude;
    private double longitude;

}
