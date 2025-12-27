package com.driveguard.rule_engine.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "trip_alerts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripAlerts{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_number")
    private String tripNumber;

    @Column(name = "vehicle_id")
    private String vehicleId;

    @Column(name = "alert_type")
    private String alertType;

    private String details;
    private Double latitude;
    private Double longitude;

    @Column(name = "timestamp", insertable = false, updatable = false)
    private LocalDateTime timestamp;
}
