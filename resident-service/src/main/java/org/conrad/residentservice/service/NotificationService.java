package org.conrad.residentservice.service;

import org.conrad.residentservice.dto.NotificationResponse;
import org.conrad.residentservice.model.Notification;
import org.conrad.residentservice.repository.NotificationRepository;
import org.conrad.residentservice.repository.ResidentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;


@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final ResidentRepository residentRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               ResidentRepository residentRepository) {
        this.notificationRepository = notificationRepository;
        this.residentRepository = residentRepository;
    }

    @Transactional
    public void notifyMaintenanceCompleted(Long residentId, Long requestId) {
        if (!residentRepository.existsById(residentId)) {
            log.warn("MaintenanceCompleted for unknown resident {} (request {})", residentId, requestId);
            return;
        }

        if (notificationRepository.existsByResidentIdAndRequestId(residentId, requestId)) {
            log.info("Resident {} already notified about request {}", residentId, requestId);
            return;
        }
        notificationRepository.save(Notification.builder()
                .residentId(residentId)
                .requestId(requestId)
                .message("Your maintenance request #" + requestId + " has been completed.")
                .build());
        log.info("Notified resident {} about completed request {}", residentId, requestId);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listFor(Long residentId) {
        return notificationRepository.findByResidentIdOrderByCreatedAtDesc(residentId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional
    public NotificationResponse markRead(Long id, Long residentId) {
        Notification notification = notificationRepository.findByIdAndResidentId(id, residentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Notification not found"));
        notification.setRead(true);
        return NotificationResponse.from(notification);
    }
}
