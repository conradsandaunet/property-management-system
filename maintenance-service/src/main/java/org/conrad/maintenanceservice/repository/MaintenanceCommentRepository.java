package org.conrad.maintenanceservice.repository;

import org.conrad.maintenanceservice.model.MaintenanceComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaintenanceCommentRepository extends JpaRepository<MaintenanceComment, Long> {

    List<MaintenanceComment> findByRequestIdOrderByCreatedAtAsc(Long requestId);
}
