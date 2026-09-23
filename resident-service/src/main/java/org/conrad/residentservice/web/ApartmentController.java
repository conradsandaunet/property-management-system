package org.conrad.residentservice.web;

import org.conrad.residentservice.dto.ApartmentResponse;
import org.conrad.residentservice.model.Apartment;
import org.conrad.residentservice.repository.ApartmentRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/apartments")
public class ApartmentController {

    private final ApartmentRepository apartmentRepository;

    public ApartmentController(ApartmentRepository apartmentRepository) {
        this.apartmentRepository = apartmentRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('BOARD')")
    public List<ApartmentResponse> getAll() {
        return apartmentRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private ApartmentResponse toResponse(Apartment apartment) {
        return new ApartmentResponse(
                apartment.getId(),
                apartment.getApartmentNumber(),
                apartment.getFloor(),
                apartment.getBuildingName(),
                apartment.getResidents().size()
        );
    }



}
