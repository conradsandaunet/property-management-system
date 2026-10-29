package org.conrad.reservationservice.service;


import org.conrad.reservationservice.model.Resource;
import org.conrad.reservationservice.model.ResourceStatus;
import org.conrad.reservationservice.repository.ResourceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;

    public ResourceService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    public List<Resource> listAll() {
        return resourceRepository.findAll();
    }

    public Resource setStatus(Long resourceId, ResourceStatus status) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));
        resource.setStatus(status);
        return resourceRepository.save(resource);
    }

    public Resource block(Long resourceId, LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from and to are required");
        }
        if (!from.isBefore(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from must be before to");
        }
        Resource resource = findResource(resourceId);
        resource.setMaintenanceFrom(from);
        resource.setMaintenanceTo(to);
        return resourceRepository.save(resource);
    }

    public Resource unblock(Long resourceId) {
        Resource resource = findResource(resourceId);
        resource.setMaintenanceFrom(null);
        resource.setMaintenanceTo(null);
        return resourceRepository.save(resource);
    }

    private Resource findResource(Long resourceId) {
        return resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));
    }
}
