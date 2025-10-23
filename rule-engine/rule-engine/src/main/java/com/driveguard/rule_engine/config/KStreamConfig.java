package com.driveguard.rule_engine.config;

import com.driveguard.rule_engine.AppLogger;
import com.driveguard.rule_engine.dto.Alert;
import com.driveguard.rule_engine.dto.TripRow;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
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

    private static final AppLogger log = AppLogger.getLogger(KStreamConfig.class);

    @Bean
    public KStream<String,String> kStreamTopology(StreamsBuilder streamsBuilder){

        // 1. Consume the raw GPS data stream
        //    (Assumes key is vehicleId as a String, value is TripRow as JSON)
        KStream<String, TripRow> locationStream = streamsBuilder.stream(cabLocationTopicName,
                Consumed.with(Serdes.String(), new JsonSerde<>(TripRow.class)));

        locationStream.peek((vehicleId, newRow) -> {
            log.info("INCOMING ROW Vehicle:" + vehicleId + " details = " + newRow);
        });

        // 2. Group the stream by key (vehicleId)
        //    This is essential for stateful operations.
//        KGroupedStream<String, TripRow> groupedByVehicle = locationStream.groupByKey();

        // 3. Apply the stateful rule logic using aggregate()
        //    This compares the new row (value) with the last known state (aggregate)
//        KTable<String, Alert> alertTable = groupedByVehicle.aggregate(
//                () -> null, // Initializer: The initial state for a new car is null
//                (vehicleId, newRow, previousState) -> {
//                    // This is your Rule Engine Logic!
//                    log.info("TRIP DATA = " + newRow.toString());
//
//                    // --- SPEEDING RULE ---
//
//
//                    // --- HARSH BRAKING RULE ---
//
//
//                    // --- UNUSUAL STOP RULE ---
//
//
//                    // No alert, just pass on the new state
//                    return null; // We will filter this out later
//                },
//                Materialized.with(Serdes.String(), new JsonSerde<>(Alert.class))
//        );

        // 4. Publish results to the anomaly_alerts topic


        return null; // The bean just needs to build the topology
    }
}
