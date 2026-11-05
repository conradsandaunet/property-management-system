package org.conrad.maintenanceservice.web;

import jakarta.validation.Valid;
import org.conrad.maintenanceservice.dto.LoginRequest;
import org.conrad.maintenanceservice.dto.LoginResponse;
import org.conrad.maintenanceservice.dto.WorkerCreateRequest;
import org.conrad.maintenanceservice.dto.WorkerCreatedResponse;
import org.conrad.maintenanceservice.dto.WorkerResponse;
import org.conrad.maintenanceservice.model.MaintenanceCategory;
import org.conrad.maintenanceservice.service.WorkerAuthService;
import org.conrad.maintenanceservice.service.WorkerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("workers")
public class WorkerController {

    private final WorkerService workerService;
    private final WorkerAuthService workerAuthService;

    public WorkerController(WorkerService workerService, WorkerAuthService workerAuthService) {
        this.workerService = workerService;
        this.workerAuthService = workerAuthService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return workerAuthService.login(request.email(), request.password())
                .map(token -> ResponseEntity.ok(new LoginResponse(token)))
                .orElseGet(() -> ResponseEntity.status(401).build());
    }

    @PostMapping
    public ResponseEntity<WorkerCreatedResponse> create(@Valid @RequestBody WorkerCreateRequest request) {
        WorkerService.CreatedWorker created = workerService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new WorkerCreatedResponse(WorkerResponse.from(created.worker()), created.temporaryPassword()));
    }

    @GetMapping
    public ResponseEntity<List<WorkerResponse>> list(@RequestParam(required = false)MaintenanceCategory category) {
        return ResponseEntity.ok(workerService.listActive(category)
                .stream()
                .map(WorkerResponse::from)
                .toList());
    }
}
