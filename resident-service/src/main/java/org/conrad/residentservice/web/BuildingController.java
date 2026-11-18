package org.conrad.residentservice.web;

import org.conrad.residentservice.dto.BuildingRequest;
import org.conrad.residentservice.dto.BuildingResponse;
import org.conrad.residentservice.service.BuildingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/buildings")
@PreAuthorize("hasRole('BOARD')")
public class BuildingController {

    private final BuildingService buildingService;

    public BuildingController(BuildingService buildingService) {
        this.buildingService = buildingService;
    }

    @GetMapping
    public List<BuildingResponse> getAll() {
        return buildingService.getAll();
    }

    @PostMapping
    public ResponseEntity<BuildingResponse> create(@RequestBody BuildingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(buildingService.create(CurrentUser.name(), request));
    }

    @PatchMapping("/{id}")
    public BuildingResponse update(@PathVariable Long id, @RequestBody BuildingRequest request) {
        return buildingService.update(CurrentUser.name(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable Long id) {
        buildingService.remove(CurrentUser.name(), id);
        return ResponseEntity.noContent().build();
    }
}
