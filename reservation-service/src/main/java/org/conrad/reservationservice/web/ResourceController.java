package org.conrad.reservationservice.web;

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

    @GetMapping
    public ResponseEntity<List<Resource>> listAll() {
        return ResponseEntity.ok(resourceService.listAll());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Resource> setStatus(
            @PathVariable Long id,
            @RequestParam ResourceStatus status) {
        Resource updated = resourceService.setStatus(id, status);
        return ResponseEntity.ok(updated);
    }

}
