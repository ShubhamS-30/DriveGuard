package com.driveguard.rule_engine.websocket;

import com.driveguard.rule_engine.AppLogger;
import com.driveguard.rule_engine.dto.TripRow;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LocationWebSocketHandler extends TextWebSocketHandler {

    private static final AppLogger log = AppLogger.getLogger(LocationWebSocketHandler.class);

    // Sessions subscribed to all locations (no carId filter)
    private final Set<WebSocketSession> allLocationSessions = Collections.synchronizedSet(new HashSet<>());

    // Sessions segregated by carId: carId -> Set of sessions
    private final Map<String, Set<WebSocketSession>> carIdSessions = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // Extract carId from URI path if present
        String uri = Objects.requireNonNull(session.getUri()).getPath();
        String carId = extractCarIdFromPath(uri);

        if (carId != null && !carId.isEmpty()) {
            // Add to carId-specific sessions
            carIdSessions.computeIfAbsent(carId, k -> Collections.synchronizedSet(new HashSet<>()))
                    .add(session);
            log.info(String.format("WebSocket connection established for carId: %s. Sessions for this car: %d", carId, carIdSessions.get(carId).size()));
            session.sendMessage(new TextMessage("Connected to location stream for car: " + carId));
        } else {
            // Add to all locations sessions
            allLocationSessions.add(session);
            log.info(String.format("WebSocket connection established for all locations. Total connections: %d", allLocationSessions.size()));
            session.sendMessage(new TextMessage("Connected to location stream for all vehicles"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session,@NonNull CloseStatus status) throws Exception {
        String uri = Objects.requireNonNull(session.getUri()).getPath();
        String carId = extractCarIdFromPath(uri);

        if (carId != null && !carId.isEmpty()) {
            Set<WebSocketSession> sessions = carIdSessions.get(carId);
            if (sessions != null) {
                sessions.remove(session);
                log.info(String.format("WebSocket connection closed for carId: %s. Remaining sessions: %d", carId, sessions.size()));
                // Remove the carId entry if no sessions left
                if (sessions.isEmpty()) {
                    carIdSessions.remove(carId);
                }
            }
        } else {
            allLocationSessions.remove(session);
            log.info(String.format("WebSocket connection closed for all locations. Total connections: %d", allLocationSessions.size()));
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, @NonNull Throwable exception) throws Exception {
        log.error("WebSocket transport error: ", exception);
        try {
            session.close(CloseStatus.SERVER_ERROR);
        } catch (IOException e) {
            log.debug(String.format("Error closing session: %s", e.getMessage()));
        }
    }

    /**
     * Broadcast location data to relevant WebSocket clients based on carId
     *
     * @param tripRow the location data from Kafka topic
     */
    public void broadcastLocation(TripRow tripRow) {
        String carId = tripRow.getCarId();

        try {
            String jsonMessage = objectMapper.writeValueAsString(tripRow);
            TextMessage message = new TextMessage(jsonMessage);

            // 1. Send to carId-specific subscribers
            if (carId != null && !carId.isEmpty()) {
                Set<WebSocketSession> carSessions = carIdSessions.get(carId);
                if (carSessions != null && !carSessions.isEmpty()) {
                    broadcastToSessions(carSessions, message);
                }
            }

            // 2. Send to all-locations subscribers
            if (!allLocationSessions.isEmpty()) {
                broadcastToSessions(allLocationSessions, message);
            }

            if ((carId != null && carIdSessions.containsKey(carId)) || !allLocationSessions.isEmpty()) {
                log.debug(String.format("Broadcasted location for car: %s", carId));
            } else {
                log.debug(String.format("No active subscriptions for car: %s", carId));
            }
        } catch (Exception e) {
            log.error(String.format("Error broadcasting location data for car %s: ", carId), e);
        }
    }

    /**
     * Broadcast message to a specific set of sessions
     *
     * @param sessions the set of sessions to broadcast to
     * @param message the message to send
     */
    private void broadcastToSessions(Set<WebSocketSession> sessions, TextMessage message) {
        for (WebSocketSession session : new HashSet<>(sessions)) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(message);
                } else {
                    sessions.remove(session);
                }
            } catch (IOException e) {
                log.debug(String.format("Error sending message to session %s: %s", session.getId(), e.getMessage()));
                try {
                    session.close(CloseStatus.SERVER_ERROR);
                } catch (IOException ex) {
                    log.debug(String.format("Error closing session: %s", ex.getMessage()));
                }
                sessions.remove(session);
            }
        }
    }

    /**
     * Extract carId from WebSocket URI path
     * Format: /ws/locations/{carId}
     *
     * @param path the URI path
     * @return the carId if present, null otherwise
     */
    private String extractCarIdFromPath(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        String[] parts = path.split("/");
        if (parts.length >= 4) {
            String carId = parts[parts.length - 1];
            return carId.isEmpty() ? null : carId;
        }
        return null;
    }

    /**
     * Get the number of active WebSocket connections for a specific carId
     *
     * @param carId the car ID
     * @return the number of active connections for this car
     */
    public int getActiveConnectionCountForCar(String carId) {
        Set<WebSocketSession> sessions = carIdSessions.get(carId);
        return sessions != null ? sessions.size() : 0;
    }

    /**
     * Get the total number of active WebSocket connections
     */
    public int getActiveConnectionCount() {
        int carIdCount = carIdSessions.values().stream()
                .mapToInt(Set::size)
                .sum();
        return carIdCount + allLocationSessions.size();
    }

    /**
     * Get carId-specific connection count
     */
    public Map<String, Integer> getConnectionsByCarId() {
        Map<String, Integer> result = new HashMap<>();
        carIdSessions.forEach((carId, sessions) -> result.put(carId, sessions.size()));
        if (!allLocationSessions.isEmpty()) {
            result.put("ALL_LOCATIONS", allLocationSessions.size());
        }
        return result;
    }
}
