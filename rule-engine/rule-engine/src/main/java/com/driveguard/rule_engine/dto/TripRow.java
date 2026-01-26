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

    private String target_speed;

    private String way_Maxspeed;

    private String speed_osrm;

    private String elevation;

    private String fwd_azimuth;

    private String way_type;

    private String way_surface;

    private String node_intersection;

    private String node_railway;

    private String node_crossing;

    private String node_highway;

    private String node_stop;

    private String start_stop;

    private String azimuth_diff;

    private String elevation_diff;

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
