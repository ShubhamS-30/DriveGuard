package com.driveGuard.dataProducer.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "trips")
public class Trip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer tripId;
    @NotNull(message = "Trip Number cannot be null")
    private String tripNumber;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cnr")
    @NotNull(message = "Car cannot be null")
    private Car car;
    private String startTime;
    private String endTime;
}
