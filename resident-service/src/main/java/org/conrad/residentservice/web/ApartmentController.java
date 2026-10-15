package org.conrad.residentservice.web;

import org.conrad.residentservice.dto.ApartmentResponse;
import org.conrad.residentservice.dto.BuildingOverviewResponse;
import org.conrad.residentservice.service.ApartmentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
