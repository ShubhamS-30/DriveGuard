package com.driveguard.rule_engine.config;

import com.driveguard.rule_engine.dto.Alert;
import com.driveguard.rule_engine.dto.TripRow;
import com.driveguard.rule_engine.dto.VehicleState;
import com.driveguard.rule_engine.service.RuleEngineService;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.util.Optional;

@Configuration
@EnableKafkaStreams
public class KStreamConfig {

    @Value("${data.cab.status.topic.name}")
    private String cabStatusTopicName;

    @Value("${data.cab.location.topic.name}")
    private String cabLocationTopicName;

    @Value("${data.cab.alert.topic.name}")
    private String cabAlertTopicName;

    private final RuleEngineService ruleEngineService;
    private static final String STATE_STORE_NAME = "rule-state-store";

    public KStreamConfig(RuleEngineService ruleEngineService) {
        this.ruleEngineService = ruleEngineService;
    }

    /**
     * Defines the KStreams topology for the rule engine.
     */
    @Bean
    public KTable<String, VehicleState> kStreamTopology(StreamsBuilder streamsBuilder) {
        KStream<String, TripRow> locationStream = streamsBuilder.stream(cabLocationTopicName,
                Consumed.with(Serdes.String(), new JsonSerde<>(TripRow.class)));

        return locationStream.groupByKey()
                .aggregate(
                        VehicleState::new, // Initializer
                        (vehicleId, newRow, currentState) -> {
                            // A. Run rules and get potential alert
                            Optional<Alert> alert = ruleEngineService.processRules(vehicleId, newRow, currentState);

                            // B. Update state for the NEXT message
                            currentState.setLastAlert(alert.orElse(null));
                            currentState.setPreviousTripRow(newRow);

                            return currentState;
                        },
                        Materialized.<String, VehicleState, KeyValueStore<Bytes, byte[]>>as(STATE_STORE_NAME)
                                .withKeySerde(Serdes.String())
                                .withValueSerde(new JsonSerde<>(VehicleState.class))
                                .withCachingDisabled()
                );
    }

    /**
     * Publishes alerts derived from the vehicle state table.
     */
    @Bean
    public KStream<String, Alert> publishAlerts(KTable<String, VehicleState> vehicleStateTable) {
        KStream<String, Alert> alertStream = vehicleStateTable.toStream()
                .filter((key, state) -> state.getLastAlert() != null) // Only pass messages with alerts:
                .mapValues(VehicleState::getLastAlert);

        // Route the alerts to the destination Kafka topic
        alertStream.to(cabAlertTopicName, Produced.with(Serdes.String(), new JsonSerde<>(Alert.class)));

        return alertStream;
    }
}
