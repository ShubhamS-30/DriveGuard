package com.driveguard.rule_engine.config;

import com.driveguard.rule_engine.dto.Alert;
import com.driveguard.rule_engine.dto.TripRow;
import com.driveguard.rule_engine.service.RuleEngineService;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.tomcat.util.digester.Rule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.support.serializer.JsonSerde;

@Configuration
@EnableKafkaStreams
public class KStreamConfig {

    @Value("${data.cab.status.topic.name}")
    private String cabStatusTopicName;

    @Value("${data.cab.location.topic.name}")
    private String cabLocationTopicName;

    @Value("${data.cab.alert.topic.name}") // You'll need to add this to application.properties
    private String cabAlertTopicName;

    private final RuleEngineService ruleEngineService;

    public KStreamConfig(RuleEngineService ruleEngineService) {
        this.ruleEngineService = ruleEngineService;
    }
    /**
     * Defines the KStreams topology for the rule engine.
     */
    @Bean
    public KTable<String, Alert> kStreamTopology(StreamsBuilder streamsBuilder){

        KStream<String, TripRow> locationStream = streamsBuilder.stream(cabLocationTopicName,
                Consumed.with(Serdes.String(), new JsonSerde<>(TripRow.class)));

        KGroupedStream<String, TripRow> groupedByVehicle = locationStream.groupByKey();

        KTable<String, Alert> alertTable = groupedByVehicle.aggregate(
                () -> null,
                (vehicleId, newRow, previousAlert) -> {

                    return ruleEngineService.processRules(vehicleId, newRow).orElse(null);
//                    try {
//                        // --- SPEEDING RULE (STATELESS) ---
//                        if (newRow.getTarget_speed() != null && newRow.getSpeed_osrm() != null) {
//                            double currentSpeed = Double.parseDouble(newRow.getTarget_speed());
//                            double speedLimit = Double.parseDouble(newRow.getSpeed_osrm());
//
//                            if (currentSpeed > speedLimit) {
//                                log.info("SPEEDING ALERT for Vehicle:" + vehicleId );
//                                String details = String.format("Speeding: %.0f km/h in a %.0f km/h zone.", currentSpeed, speedLimit);
//                                return new Alert(vehicleId, "SPEEDING", details, System.currentTimeMillis(), Double.parseDouble(newRow.getLatitude()), Double.parseDouble(newRow.getLongitude()));
//                            }
//                        }
//                        // --- UNUSUAL STOP RULE (STATELESS) ---
//                        if (newRow.getTarget_speed() != null && "0.0".equals(newRow.getTarget_speed())) {
//                            if ("motorway".equals(newRow.getWay_type())) {
//                                String details = "Vehicle stopped on a motorway.";
//                                return new Alert(vehicleId, "UNUSUAL_STOP", details, System.currentTimeMillis(), Double.parseDouble(newRow.getLatitude()), Double.parseDouble(newRow.getLongitude()));
//                            }
//                        }
//                    } catch (NumberFormatException e) {
//                        log.error("Failed to parse speed data for vehicle: " + vehicleId, e);
//                    }
//                    return null;
                },
                // We must be more specific and disable caching.
                Materialized.<String, Alert, KeyValueStore<Bytes, byte[]>>as("alert-state-store") // Give the store a name
                        .withKeySerde(Serdes.String())
                        .withValueSerde(new JsonSerde<>(Alert.class))
                        .withCachingDisabled() // <-- This forces the KTable to emit all changes
        );

        // 4. Publish results to the 'anomaly_alerts' topic
        alertTable.toStream()
                .filter((key, alert) -> alert != null) // Filter out all the 'null' (no-alert) messages
                .to(cabAlertTopicName, Produced.with(Serdes.String(), new JsonSerde<>(Alert.class)));


        return alertTable;
    }
}
