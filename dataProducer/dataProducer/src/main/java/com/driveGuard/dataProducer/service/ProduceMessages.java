package com.driveGuard.dataProducer.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ProduceMessages {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ProduceMessages(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void produceMessageByTopic(String topic, Object message) {
        kafkaTemplate.send(topic, message);
    }

    public void produceMessageByTopicAndKey(String topic, String key, Object message) {
        kafkaTemplate.send(topic, key, message);
    }

}
