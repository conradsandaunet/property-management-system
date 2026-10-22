package org.conrad.maintenanceservice.service;

import org.conrad.maintenanceservice.dto.WorkerCreateRequest;
import org.conrad.maintenanceservice.model.MaintenanceCategory;
import org.conrad.maintenanceservice.model.MaintenanceWorker;
import org.conrad.maintenanceservice.repository.MaintenanceWorkerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Service
public class WorkerService {

    private final MaintenanceWorkerRepository repository;

    public WorkerService(MaintenanceWorkerRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public MaintenanceWorker create(WorkerCreateRequest request) {
        MaintenanceWorker worker = new MaintenanceWorker();
        worker.setName(request.name());
        worker.setPhone(request.phone());
        worker.setEmail(request.email());
        worker.setSpecialties(new HashSet<>(request.specialties()));
        return repository.save(worker);
    }

    @Transactional(readOnly = true)
    public List<MaintenanceWorker> listActive(MaintenanceCategory category) {
        return category == null
                ? repository.findByActiveTrueOrderByNameAsc()
                : repository.findByActiveTrueAndSpecialtiesContainingOrderByNameAsc(category);
    }
}
