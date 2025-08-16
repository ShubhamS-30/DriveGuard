package com.driveGuard.dataProducer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

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
}
