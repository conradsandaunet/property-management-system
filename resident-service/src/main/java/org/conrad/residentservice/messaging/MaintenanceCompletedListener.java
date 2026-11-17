package org.conrad.residentservice.messaging;

import org.conrad.residentservice.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class MaintenanceCompletedListener {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public MaintenanceCompletedListener(NotificationService notificationService, ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${app.kafka.topics.maintenance-completed}")
    public void onMessage(String payload) {
        MaintenanceCompleted event = objectMapper.readValue(payload, MaintenanceCompleted.class);
        notificationService.notifyMaintenanceCompleted(event.residentId(), event.requestId());
    }
}