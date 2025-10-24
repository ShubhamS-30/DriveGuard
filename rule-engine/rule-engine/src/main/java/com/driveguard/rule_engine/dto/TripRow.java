package com.driveguard.rule_engine.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripRow {

    private String latitude;

    private String longitude;

    private String targetSpeed;

    private String wayMaxspeed;

    private String speedOsrm;

    private String elevation;

    private String fwdAzimuth;

    private String wayType;

    private String waySurface;

    private String nodeIntersection;

    private String nodeRailway;

    private String nodeCrossing;

    private String nodeHighway;

    private String nodeStop;

    private String startStop;

    private String azimuthDiff;

    private String elevationDiff;

    @NotNull(message = "Car Id cannot be null")
    private String carId;

    @NotNull(message = "Trip Number cannot be null")
    private String tripNumber;

    @NotNull(message = "Timestamp cannot be null")
    private String timestamp;

    @NotNull(message = "Trip completion cannot be null")
    @Min(value = 0, message = "Trip completion must be at least 0")
    @Max(value = 100, message = "Trip completion must be at most 100")
    private double tripCompletion;
}
