package org.conrad.reservationservice.web;

import org.conrad.reservationservice.dto.BlockResourceRequest;
import org.conrad.reservationservice.dto.ResourceResponse;
import org.conrad.reservationservice.dto.SlotResponse;
import org.conrad.reservationservice.model.Resource;
import org.conrad.reservationservice.model.ResourceStatus;
import org.conrad.reservationservice.service.ReservationService;
import org.conrad.reservationservice.service.ResourceService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/resources")
public class ResourceController {

    private final ResourceService resourceService;
    private final ReservationService reservationService;

    public ResourceController(ResourceService resourceService, ReservationService reservationService) {
        this.resourceService = resourceService;
        this.reservationService = reservationService;
    }

    private ResourceResponse toResponse(Resource resource) {
        return new ResourceResponse(
                resource.getId(),
                resource.getName(),
                resource.getType(),
                resource.getStatus(),
                resource.getMaintenanceFrom(),
                resource.getMaintenanceTo()
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
    @GetMapping("/{id}/availability")
    public ResponseEntity<List<SlotResponse>> availability(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(reservationService.availableSlots(id, date));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('BOARD')")
    public ResponseEntity<ResourceResponse> setStatus(
            @PathVariable Long id,
            @RequestParam ResourceStatus status) {
        Resource updated = resourceService.setStatus(id, status);
        return ResponseEntity.ok(toResponse(updated));
    }

    /** Block the resource for a period, e.g. {"from": "2026-11-20T08:00", "to": "2026-11-25T22:00"}. */
    @PostMapping("/{id}/block")
    @PreAuthorize("hasRole('BOARD')")
    public ResponseEntity<ResourceResponse> block(@PathVariable Long id, @RequestBody BlockResourceRequest request) {
        Resource updated = resourceService.block(id, request.from(), request.to());
        return ResponseEntity.ok(toResponse(updated));
    }

    @DeleteMapping("/{id}/block")
    @PreAuthorize("hasRole('BOARD')")
    public ResponseEntity<ResourceResponse> unblock(@PathVariable Long id) {
        Resource updated = resourceService.unblock(id);
        return ResponseEntity.ok(toResponse(updated));
    }

}
