package com.driveguard.rule_engine.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TripStatusMessage {
    @JsonProperty("cnr")
    private Integer cnr;

    @JsonProperty("tripStatus")
    private boolean tripStatus;

    @JsonProperty("tripNumber")
    private String tripNumber;

    @JsonProperty("timestamp")
    private String timestamp;
}
