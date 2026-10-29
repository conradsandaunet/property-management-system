package org.conrad.residentservice.web;

import org.conrad.residentservice.dto.ApartmentCreateRequest;
import org.conrad.residentservice.dto.ApartmentResponse;
import org.conrad.residentservice.dto.ApartmentUpdateRequest;
import org.conrad.residentservice.dto.BuildingOverviewResponse;
import org.conrad.residentservice.service.ApartmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/apartments")
public class ApartmentController {

    private final ApartmentService apartmentService;

    public ApartmentController(ApartmentService apartmentService) {
        this.apartmentService = apartmentService;
    }

    @GetMapping
    @PreAuthorize("hasRole('BOARD')")
    public List<ApartmentResponse> getAll() {
        return apartmentService.getAll();
    }

    @GetMapping("/overview")
    @PreAuthorize("hasRole('BOARD')")
    public BuildingOverviewResponse overview() {
        return apartmentService.getOverview();
    }

    @PostMapping
    @PreAuthorize("hasRole('BOARD')")
    public ResponseEntity<ApartmentResponse> create(@RequestBody ApartmentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(apartmentService.create(CurrentUser.name(), request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('BOARD')")
    public ApartmentResponse update(@PathVariable Long id, @RequestBody ApartmentUpdateRequest request) {
        return apartmentService.update(CurrentUser.name(), id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('BOARD')")
    public ResponseEntity<Void> remove(@PathVariable Long id) {
        apartmentService.remove(CurrentUser.name(), id);
        return ResponseEntity.noContent().build();
    }
}
