package com.driveguard.rule_engine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing Car information received from Data Producer API.
 * This DTO is independent of the data producer module to avoid direct dependencies.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CarResponseDTO {
    private Integer cnr;                    // Car Number (ID)
    private String manufacturer;            // Car manufacturer
    private String model;                   // Car model
    private String fuel;                    // Fuel type
    private Integer powerKw;                // Engine power in KW
    private String transmission;            // Transmission type
    private Integer weightKg;               // Vehicle weight in KG
    private Boolean isActiveTrip;           // Whether car is on an active trip
    private String activeTripNumber;        // Current trip number
}

