package org.conrad.maintenanceservice.messaging;

import org.conrad.maintenanceservice.event.MaintenanceCompleted;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

@Component
public class MaintenanceEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public MaintenanceEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper,
                                     @Value("${app.kafka.topics.maintenance-completed}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMaintenanceCompleted(MaintenanceCompleted event) {
        String payload = objectMapper.writeValueAsString(event);
        kafkaTemplate.send(topic, String.valueOf(event.requestId()), payload)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Failed to publish MaintenanceCompleted for request {}", event.requestId(), error);
                    } else {
                        log.info("Published MaintenanceCompleted for request {}", event.requestId());
                    }
                });
    }
}
