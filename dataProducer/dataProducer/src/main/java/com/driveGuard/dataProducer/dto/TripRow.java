package com.driveGuard.dataProducer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

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

    @JsonProperty("latitude")
    private String latitude;

    @JsonProperty("longitude")
    private String longitude;

    @JsonProperty("target_speed")
    private String targetSpeed;

    @JsonProperty("way_maxspeed")
    private String wayMaxspeed;

    @JsonProperty("speed_osrm")
    private String speedOsrm;

    @JsonProperty("elevation")
    private String elevation;

    @JsonProperty("fwd_azimuth")
    private String fwdAzimuth;

    @JsonProperty("way_type")
    private String wayType;

    @JsonProperty("way_surface")
    private String waySurface;

    @JsonProperty("node:intersection")
    private String nodeIntersection;

    @JsonProperty("node:railway")
    private String nodeRailway;

    @JsonProperty("node:crossing")
    private String nodeCrossing;

    @JsonProperty("node:highway")
    private String nodeHighway;

    @JsonProperty("node:stop")
    private String nodeStop;

    @JsonProperty("start_stop")
    private String startStop;

    @JsonProperty("azimuth_diff")
    private String azimuthDiff;

    @JsonProperty("elevation_diff")
    private String elevationDiff;

    @NotNull(message = "Car Id cannot be null")
    private String carId;

    @NotNull(message = "Timestamp cannot be null")
    private String timestamp;

    @NotNull(message = "Trip completion cannot be null")
    @Min(value = 0, message = "Trip completion must be at least 0")
    @Max(value = 100, message = "Trip completion must be at most 100")
    private double tripCompletion;
}
