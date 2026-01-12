package com.driveguard.rule_engine.config;

import com.driveguard.rule_engine.dto.Alert;
import com.driveguard.rule_engine.dto.TripRow;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, Alert> alertConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "alert-storage-group");

        // Use JsonDeserializer and trust your package to prevent security errors
        JsonDeserializer<Alert> deserializer = new JsonDeserializer<>(Alert.class);
        deserializer.setRemoveTypeHeaders(false);
        deserializer.addTrustedPackages("com.driveguard.rule_engine.dto");
        deserializer.setUseTypeHeaders(false); // Force it to use the Alert class provided above

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Alert> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Alert> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(alertConsumerFactory());
        return factory;
    }

    // ============ TripRow Consumer Configuration for Location Streaming ============

    @Bean
    public ConsumerFactory<String, TripRow> tripRowConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "location-stream-group");

        JsonDeserializer<TripRow> deserializer = new JsonDeserializer<>(TripRow.class);
        deserializer.setRemoveTypeHeaders(false);
        deserializer.addTrustedPackages("com.driveguard.rule_engine.dto");
        deserializer.setUseTypeHeaders(false);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TripRow> tripRowKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, TripRow> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(tripRowConsumerFactory());
        return factory;
    }
}