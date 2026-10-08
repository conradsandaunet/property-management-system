package org.conrad.maintenanceservice.service;

import org.conrad.maintenanceservice.dto.MaintenanceCreateRequest;
import org.conrad.maintenanceservice.dto.MaintenanceResponse;
import org.conrad.maintenanceservice.model.MaintenanceRequest;
import org.conrad.maintenanceservice.repository.MaintenanceRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class MaintenanceService {

    private final MaintenanceRequestRepository repository;

    public MaintenanceService(MaintenanceRequestRepository repository) {
        this.repository = repository;
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
}
