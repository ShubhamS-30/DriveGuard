package com.driveGuard.dataProducer.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CarDTO {
    @NotNull(message = "Car Number cannot be null")
    private Integer cnr;
    private String manufacturer;
    private String model;
    private String fuel;
    private Integer powerKw;
    private String transmission;
    private Integer weightKg;
    private Boolean isActiveTrip;
    private String activeTripNumber;
}
