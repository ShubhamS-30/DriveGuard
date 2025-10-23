package com.driveGuard.dataProducer.dto.message;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TripStatusMessage {
    @NotNull(message = "Car Number cannot be null")
    private Integer cnr;

    @NotNull(message = "Trip Status cannot be null")
    private boolean tripStatus;

    @NotNull(message = "Trip Number cannot be null")
    private String tripNumber;

    @NotNull(message = "Timestamp cannot be null")
    private String timestamp;
}
