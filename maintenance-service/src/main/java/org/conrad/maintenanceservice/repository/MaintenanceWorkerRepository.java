package org.conrad.maintenanceservice.repository;

import org.conrad.maintenanceservice.model.MaintenanceCategory;
import org.conrad.maintenanceservice.model.MaintenanceWorker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaintenanceWorkerRepository extends JpaRepository<MaintenanceWorker, Long> {

    List<MaintenanceWorker> findByActiveTrueOrderByNameAsc();

    List<MaintenanceWorker> findByActiveTrueAndSpecialtiesContainingOrderByNameAsc(MaintenanceCategory category);

    Optional<MaintenanceWorker> findByIdAndActiveTrue(Long id);

}
