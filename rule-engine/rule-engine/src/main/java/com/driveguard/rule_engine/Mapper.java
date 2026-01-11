package com.driveguard.rule_engine;

import com.driveguard.rule_engine.dto.Alert;
import com.driveguard.rule_engine.entity.TripAlerts;
import org.springframework.stereotype.Service;

@Service
public class Mapper {

    public TripAlerts mapToEntity(Alert alert) {
        return TripAlerts.builder()
                .vehicleId(alert.getVehicleId())
                .tripNumber(alert.getTripNumber()) // Use the field from the DTO!
                .alertType(alert.getAlertType())
                .details(alert.getDetails())
                .latitude(alert.getLatitude())
                .longitude(alert.getLongitude())
                .build();
    }

    public Alert mapToDTO(TripAlerts tripAlerts) {
        if(tripAlerts == null) {
            return null;
        }
        Alert alert = new Alert();
        alert.setId(tripAlerts.getId());
        alert.setVehicleId(tripAlerts.getVehicleId());
        alert.setTripNumber(tripAlerts.getTripNumber());
        alert.setAlertType(tripAlerts.getAlertType());
        alert.setDetails(tripAlerts.getDetails());
        alert.setLatitude(tripAlerts.getLatitude());
        alert.setLongitude(tripAlerts.getLongitude());
        return alert;
    }
}
