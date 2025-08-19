package com.driveGuard.dataProducer.entity;

import jakarta.persistence.*;
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
}

