package com.driveGuard.dataProducer.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.driveGuard.dataProducer.AppLogger;

@Service
public class ProduceMessages {
    private static final AppLogger log = AppLogger.getLogger(ProduceMessages.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    public ProduceMessages(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void produceMessageByTopic(String topic, String message) {
        kafkaTemplate.send(topic, message);
        log.info("Produced message to topic '" + topic + "': " + message);
    }

    // @KafkaListener(topics = "#{T(java.lang.System).getProperty('topicId')}", groupId = "driveGuard-group")
    // public void listenToTopic(String message) {
    //     log.info("Received message: " + message);
    //     // Add logic to process the message as needed
    // }

}
