package org.conrad.residentservice.service;

import org.conrad.residentservice.dto.ApartmentResponse;
import org.conrad.residentservice.dto.BuildingOverviewResponse;
import org.conrad.residentservice.model.Apartment;
import org.conrad.residentservice.model.Resident;
import org.conrad.residentservice.repository.ApartmentRepository;
import org.conrad.residentservice.repository.ResidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ApartmentService {

    private final ApartmentRepository apartmentRepository;
    private final ResidentRepository residentRepository;

    public ApartmentService(ApartmentRepository apartmentRepository, ResidentRepository residentRepository) {
        this.apartmentRepository = apartmentRepository;
        this.residentRepository = residentRepository;
    }

    @Transactional(readOnly = true)
    public List<ApartmentResponse> getAll() {
        return apartmentRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BuildingOverviewResponse getOverview() {
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
