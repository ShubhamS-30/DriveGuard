package com.driveguard.rule_engine.controller;

import com.driveguard.rule_engine.service.LocationStreamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/location-stream")
public class LocationStreamController {

    private final LocationStreamService locationStreamService;

    @Autowired
    public LocationStreamController(LocationStreamService locationStreamService) {
        this.locationStreamService = locationStreamService;
    }

    /**
     * Get the status of the location streaming service
     * Includes information about active WebSocket connections by carId
     *
     * @return status information
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStreamStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "location-stream");
        status.put("status", "running");
        status.put("totalActiveConnections", locationStreamService.getActiveConnections());
        status.put("connectionsByCarId", locationStreamService.getConnectionsByCarId());
        status.put("websocketEndpointsAll", "ws://localhost:8082/ws/locations");
        status.put("websocketEndpointsByCarId", "ws://localhost:8082/ws/locations/{carId}");
        status.put("kafkaTopic", "trip-locations");
        return ResponseEntity.ok(status);
    }
}

