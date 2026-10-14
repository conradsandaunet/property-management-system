package org.conrad.maintenanceservice.repository;

import org.conrad.maintenanceservice.model.MaintenanceRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, Long> {
    List<MaintenanceRequest> findByResidentIdOrderByCreatedAtDesc(Long residentId);
    Optional<MaintenanceRequest> findByIdAndResidentId(Long id, Long residentId);
}
