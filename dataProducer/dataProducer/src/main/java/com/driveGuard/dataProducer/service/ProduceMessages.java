package com.driveGuard.dataProducer.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.driveGuard.dataProducer.AppLogger;

@Service
public class ProduceMessages {
    private static final AppLogger log = AppLogger.getLogger(ProduceMessages.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ProduceMessages(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void produceMessageByTopic(String topic, String message) {
        kafkaTemplate.send(topic, message);
    }

    public void produceMessageByTopicAndKey(String topic, String key, Object message) {
        kafkaTemplate.send(topic, key, message);
    }

    // @KafkaListener(topics = "#{T(java.lang.System).getProperty('topicId')}", groupId = "driveGuard-group")
    // public void listenToTopic(String message) {
    //     log.info("Received message: " + message);
    //     // Add logic to process the message as needed
    // }

}
