package com.driveguard.rule_engine.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import com.driveguard.rule_engine.websocket.LocationWebSocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final LocationWebSocketHandler locationWebSocketHandler;

    public WebSocketConfig(LocationWebSocketHandler locationWebSocketHandler) {
        this.locationWebSocketHandler = locationWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
       // Register with path variable for carId
        registry.addHandler(locationWebSocketHandler, "/ws/locations/{carId}")
                .setAllowedOrigins("*");
        // Also keep the general endpoint for all locations
        registry.addHandler(locationWebSocketHandler, "/ws/locations")
                .setAllowedOrigins("*");
    }
}

