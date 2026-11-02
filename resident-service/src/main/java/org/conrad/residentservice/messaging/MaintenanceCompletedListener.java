package org.conrad.residentservice.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import tools.jackson.databind.ObjectMapper;
import org.conrad.residentservice.repository.ResidentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MaintenanceCompletedListener {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceCompletedListener.class);

    private final ResidentRepository residentRepository;
    private final ObjectMapper objectMapper;

    public MaintenanceCompletedListener(ResidentRepository residentRepository, ObjectMapper objectMapper) {
        this.residentRepository = residentRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${app.kafka.topics.maintenance-completed}")
    public void onMessage(String payload) {
        MaintenanceCompleted event = objectMapper.readValue(payload, MaintenanceCompleted.class);

        residentRepository.findById(event.residentId()).ifPresentOrElse(
                resident -> log.info("Would notify {} {} <{}>: maintenance request {} has been completed",
                        resident.getFirstName(), resident.getLastName(), resident.getEmail(), event.requestId()),
                () -> log.warn("MaintenanceCompleted for unknown resident {} (request {})",
                        event.residentId(), event.requestId()));
    }
}
