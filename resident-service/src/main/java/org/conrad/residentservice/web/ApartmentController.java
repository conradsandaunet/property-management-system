package org.conrad.residentservice.web;

import org.conrad.residentservice.dto.ApartmentResponse;
import org.conrad.residentservice.dto.BuildingOverviewResponse;
import org.conrad.residentservice.model.Apartment;
import org.conrad.residentservice.model.Resident;
import org.conrad.residentservice.repository.ApartmentRepository;
import org.conrad.residentservice.repository.ResidentRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/apartments")
public class ApartmentController {

    private final ApartmentRepository apartmentRepository;
    private final ResidentRepository residentRepository;

    public ApartmentController(ApartmentRepository apartmentRepository, ResidentRepository residentRepository) {
        this.apartmentRepository = apartmentRepository;
        this.residentRepository = residentRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('BOARD')")
    public List<ApartmentResponse> getAll() {
        return apartmentRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/overview")
    @PreAuthorize("hasRole('BOARD')")
    public BuildingOverviewResponse overview() {
        long total = apartmentRepository.count();
        long occupied = residentRepository.countOccupiedApartments();
        return new BuildingOverviewResponse(total, occupied, total - occupied, residentRepository.countByActiveTrue());
    }

    private ApartmentResponse toResponse(Apartment apartment) {
        return new ApartmentResponse(
                apartment.getId(),
                apartment.getApartmentNumber(),
                apartment.getFloor(),
                apartment.getBuildingName(),
                (int) apartment.getResidents().stream().filter(Resident::isActive).count()
        );
    }

}
