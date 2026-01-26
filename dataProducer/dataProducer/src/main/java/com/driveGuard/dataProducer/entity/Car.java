package com.driveGuard.dataProducer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Car {
    @Id
    private Integer cnr;
    private String manufacturer;
    private String model;
    private String fuel;
    private Integer powerKw;
    private String transmission;
    private Integer weightKg;
    @Column(name = "active_trip")
    private Boolean isActiveTrip;
    @Column(name = "active_trip_number", nullable = true)
    private String activeTripNumber;
}

