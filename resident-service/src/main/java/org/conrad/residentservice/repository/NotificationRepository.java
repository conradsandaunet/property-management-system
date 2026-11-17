package org.conrad.residentservice.repository;

import org.conrad.residentservice.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByResidentIdOrderByCreatedAtDesc(Long residentId);

    Optional<Notification> findByIdAndResidentId(Long id, Long residentId);

    boolean existsByResidentIdAndRequestId(Long residentId, Long requestId);
}
