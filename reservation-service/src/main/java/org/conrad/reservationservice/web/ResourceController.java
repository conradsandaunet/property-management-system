package org.conrad.reservationservice.web;

import org.conrad.reservationservice.dto.ResourceResponse;
import org.conrad.reservationservice.model.Resource;
import org.conrad.reservationservice.model.ResourceStatus;
import org.conrad.reservationservice.service.ResourceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/resources")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    private ResourceResponse toResponse(Resource resource) {
        return new ResourceResponse(
                resource.getId(),
                resource.getName(),
                resource.getType(),
                resource.getStatus()
        );
    }

    @GetMapping
    public ResponseEntity<List<ResourceResponse>> listAll() {
        List<ResourceResponse> responses = resourceService.listAll()
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ResourceResponse> setStatus(
            @PathVariable Long id,
            @RequestParam ResourceStatus status) {
        Resource updated = resourceService.setStatus(id, status);
        return ResponseEntity.ok(toResponse(updated));
    }

}
