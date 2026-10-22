package org.conrad.maintenanceservice.web;

import jakarta.validation.Valid;
import org.conrad.maintenanceservice.dto.WorkerCreateRequest;
import org.conrad.maintenanceservice.dto.WorkerResponse;
import org.conrad.maintenanceservice.model.MaintenanceCategory;
import org.conrad.maintenanceservice.service.WorkerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("workers")
public class WorkerController {

    private final WorkerService workerService;

    public WorkerController(WorkerService workerService) {
        this.workerService = workerService;
    }

    @PostMapping
    public ResponseEntity<WorkerResponse> create(@Valid @RequestBody WorkerCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(WorkerResponse.from(workerService.create(request)));
    }

    @GetMapping
    public ResponseEntity<List<WorkerResponse>> list(@RequestParam(required = false)MaintenanceCategory category) {
        return ResponseEntity.ok(workerService.listActive(category)
                .stream()
                .map(WorkerResponse::from)
                .toList());
    }
}
