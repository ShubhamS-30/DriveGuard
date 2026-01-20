package com.driveguard.rule_engine.service;

import com.driveguard.rule_engine.AppLogger;
import com.driveguard.rule_engine.dto.TripRow;
import com.driveguard.rule_engine.dto.TripStatusMessage;
import com.driveguard.rule_engine.websocket.LocationWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;

import java.util.Map;

@Service
public class LocationStreamService {

    private static final AppLogger log = AppLogger.getLogger(LocationStreamService.class);

    private final LocationWebSocketHandler webSocketHandler;

    @Autowired
    public LocationStreamService(LocationWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    /**
     * Consume location data from Kafka topic and broadcast via WebSocket
     *
     * @param tripRow the location data received from Kafka
     */
    @KafkaListener(
            topics = "${data.cab.location.topic.name}",
            groupId = "location-stream-group",
            containerFactory = "tripRowKafkaListenerContainerFactory"
    )
    public void consumeLocationData(TripRow tripRow) {
        try {
            // Broadcast the location data to all connected WebSocket clients
            webSocketHandler.broadcastLocation(tripRow);
        } catch (Exception e) {
            log.error(String.format("Error processing location data: %s", e.getMessage()), e);
        }
    }

    @KafkaListener(
            topics = "${data.cab.status.topic.name}",
            groupId = "trip-status-group",
            containerFactory = "tripStatusKafkaListenerContainerFactory"
    )
    public void consumeTripStatus(TripStatusMessage tripStatusMessage) {
        try {
            String carId = String.format("%03d", tripStatusMessage.getCnr());
            boolean tripStatus = tripStatusMessage.isTripStatus();

            if (!tripStatus) {
                log.info(String.format("Trip ended for carId: %s, tripNumber: %s. Closing WebSocket connections.",
                        carId, tripStatusMessage.getTripNumber()));
                webSocketHandler.closeConnectionsForCar(carId, CloseStatus.NORMAL);
            } else {
                log.info(String.format("Trip started for carId: %s, tripNumber: %s",
                        carId, tripStatusMessage.getTripNumber()));
            }
        } catch (Exception e) {
            log.error(String.format("Error processing trip status message: %s",
                    tripStatusMessage.getTripNumber()), e);
        }
    }

    /**
     * Get the current number of active WebSocket connections
     */
    public int getActiveConnections() {
        return webSocketHandler.getActiveConnectionCount();
    }

    /**
     * Get active connections breakdown by carId
     */
    public Map<String, Integer> getConnectionsByCarId() {
        return webSocketHandler.getConnectionsByCarId();
    }

    /**
     * Get active connections for a specific carId
     */
    public int getConnectionsForCar(String carId) {
        return webSocketHandler.getActiveConnectionCountForCar(carId);
    }
}
