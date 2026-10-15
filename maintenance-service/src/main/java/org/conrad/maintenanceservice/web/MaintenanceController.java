package org.conrad.maintenanceservice.web;

import jakarta.validation.Valid;
import org.conrad.maintenanceservice.dto.AssignWorkerRequest;
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
        return ResponseEntity.status(HttpStatus.CREATED).body(MaintenanceResponse.from(created));
    }

    @GetMapping("/my")
    public ResponseEntity<List<MaintenanceResponse>> myRequests() {
        List<MaintenanceResponse> responses = maintenanceService.listForResident(currentResidentId())
                .stream().map(MaintenanceResponse::from).toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/open")
    public ResponseEntity<List<MaintenanceResponse>> getOpenRequests() {
        List<MaintenanceResponse> responses = maintenanceService.listOpen()
                .stream().map(MaintenanceResponse::from).toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaintenanceResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(maintenanceService.getById(id, currentResidentId()));
    }

    @PutMapping("{id}/assignment")
    public ResponseEntity<MaintenanceResponse> assign(@PathVariable Long id, @Valid @RequestBody
        AssignWorkerRequest request) {
        return ResponseEntity.ok(MaintenanceResponse.from(maintenanceService.assign(id, request.workerId())));
    }

    private Long currentResidentId() {
        return Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getName());
    }
}

