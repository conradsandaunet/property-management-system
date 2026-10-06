package org.conrad.maintenanceservice.web;

import jakarta.validation.Valid;
import org.conrad.maintenanceservice.dto.MaintenanceCreateRequest;
import org.conrad.maintenanceservice.dto.MaintenanceResponse;
import org.conrad.maintenanceservice.model.MaintenanceRequest;
import org.conrad.maintenanceservice.service.MaintenanceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/maintenance")
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    public MaintenanceController(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @PostMapping
    public ResponseEntity<MaintenanceResponse> create(@Valid @RequestBody MaintenanceCreateRequest request) {
        MaintenanceRequest created = maintenanceService.create(currentResidentId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @GetMapping("/my")
    public ResponseEntity<List<MaintenanceResponse>> myRequests() {
        List<MaintenanceResponse> responses = maintenanceService.listForResident(currentResidentId())
                .stream().map(this::toResponse).toList();
        return ResponseEntity.ok(responses);
    }

    private Long currentResidentId() {
        return Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getName());
    }

    private MaintenanceResponse toResponse(MaintenanceRequest r) {
        return new MaintenanceResponse(
                r.getId(),
                r.getResidentId(),
                r.getCategory(),
                r.getLocation(),
                r.getDescription(),
                r.getPriority(),
                r.getStatus(),
                r.getCreatedAt()
        );
    }
}
