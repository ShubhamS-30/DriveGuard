package com.driveGuard.dataProducer.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TripDTO {
    @NotNull(message = "Trip ID cannot be null")
    private Integer tripId;
    @NotNull(message = "Trip Number cannot be null")
    private String tripNumber;
    @NotNull(message = "Car cannot be null")
    private CarDTO car;
    private String startTime;
    private String endTime;

    private String startLocationLatitude;
    private String startLocationLongitude;
    private String endLocationLatitude;
    private String endLocationLongitude;
    private Double totalDistanceKm;
}
