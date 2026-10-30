package org.conrad.maintenanceservice.service;

import org.conrad.maintenanceservice.dto.MaintenanceCreateRequest;
import org.conrad.maintenanceservice.dto.MaintenanceResponse;
import org.conrad.maintenanceservice.model.MaintenanceRequest;
import org.conrad.maintenanceservice.model.MaintenanceStatus;
import org.conrad.maintenanceservice.model.MaintenanceWorker;
import org.conrad.maintenanceservice.repository.MaintenanceRequestRepository;
import org.conrad.maintenanceservice.repository.MaintenanceWorkerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@Service
public class MaintenanceService {

    private final MaintenanceRequestRepository repository;
    private final MaintenanceWorkerRepository workerRepository;

    public MaintenanceService(MaintenanceRequestRepository repository, MaintenanceWorkerRepository workerRepository) {
        this.repository = repository;
        this.workerRepository = workerRepository;
    }

    @Transactional
    public MaintenanceRequest create(Long residentId, MaintenanceCreateRequest request) {
        MaintenanceRequest entity = new MaintenanceRequest();
        entity.setResidentId(residentId);
        entity.setCategory(request.category());
        entity.setLocation(request.location());
        entity.setDescription(request.description());
        entity.setPriority(request.priority());
        return repository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<MaintenanceRequest> listForResident(Long residentId) {
        return repository.findByResidentIdOrderByCreatedAtDesc(residentId);
    }

    @Transactional(readOnly = true)
    public MaintenanceResponse getById(Long id, Long residentId) {
        MaintenanceRequest request = repository.findByIdAndResidentId(id, residentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Maintenance request not found for this resident"
                ));
        return MaintenanceResponse.from(request);
    }

    @Transactional(readOnly = false)
    public List<MaintenanceRequest> listOpen() {
        return repository.findByStatusNotOrderByCreatedAtAsc(MaintenanceStatus.RESOLVED)
                .stream()
                .sorted(Comparator.comparing(MaintenanceRequest::getPriority).reversed()
                        .thenComparing(MaintenanceRequest::getCreatedAt))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MaintenanceRequest> listAssignedTo(Long workerId) {
        return repository.findByAssignedWorkerIdAndStatusNot(workerId, MaintenanceStatus.RESOLVED)
                .stream()
                .sorted(Comparator.comparing(MaintenanceRequest::getPriority).reversed()
                        .thenComparing(MaintenanceRequest::getCreatedAt))
                .toList();
    }

    @Transactional
    public MaintenanceRequest assign(Long requestId, Long workerId) {
        MaintenanceRequest request = repository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
        MaintenanceWorker worker = workerRepository.findByIdAndActiveTrue(workerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Worker not found"));

        if (request.getStatus() == MaintenanceStatus.RESOLVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request already resolved");
        }
        request.setAssignedWorker(worker);

        if (request.getStatus() == MaintenanceStatus.SUBMITTED) {
            request.setStatus(MaintenanceStatus.ASSIGNED);
        }
        return request;
    }
}
