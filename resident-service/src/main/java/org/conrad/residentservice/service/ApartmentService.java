package org.conrad.residentservice.service;

import org.conrad.residentservice.dto.ApartmentCreateRequest;
import org.conrad.residentservice.dto.ApartmentResponse;
import org.conrad.residentservice.dto.ApartmentUpdateRequest;
import org.conrad.residentservice.dto.BuildingOverviewResponse;
import org.conrad.residentservice.model.Apartment;
import org.conrad.residentservice.model.Resident;
import org.conrad.residentservice.repository.ApartmentRepository;
import org.conrad.residentservice.repository.ResidentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ApartmentService {

    private static final Logger log = LoggerFactory.getLogger(ApartmentService.class);
    private static final int MAX_NUMBER_LENGTH = 10;
    private static final int MAX_BUILDING_LENGTH = 255;

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

    @Transactional
    public ApartmentResponse create(String actor, ApartmentCreateRequest request) {
        String number = requireText(request.apartmentNumber(), "apartmentNumber", MAX_NUMBER_LENGTH);
        String building = requireText(request.buildingName(), "buildingName", MAX_BUILDING_LENGTH);
        if (request.floor() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "floor is required");
        }
        requireUnique(building, number);

        Apartment apartment = Apartment.builder()
                .apartmentNumber(number)
                .floor(request.floor())
                .buildingName(building)
                .build();
        apartmentRepository.save(apartment);

        log.info("Board member (residentId={}) created apartmentId={} number={} building={}",
                actor, apartment.getId(), number, building);
        return toResponse(apartment);
    }

    @Transactional
    public ApartmentResponse update(String actor, Long id, ApartmentUpdateRequest request) {
        Apartment apartment = findApartment(id);

        String number = request.apartmentNumber() != null
                ? requireText(request.apartmentNumber(), "apartmentNumber", MAX_NUMBER_LENGTH)
                : apartment.getApartmentNumber();
        String building = request.buildingName() != null
                ? requireText(request.buildingName(), "buildingName", MAX_BUILDING_LENGTH)
                : apartment.getBuildingName();

        boolean identityChanged = !number.equalsIgnoreCase(apartment.getApartmentNumber())
                || !building.equalsIgnoreCase(apartment.getBuildingName());
        if (identityChanged) {
            requireUnique(building, number);
        }

        apartment.setApartmentNumber(number);
        apartment.setBuildingName(building);
        if (request.floor() != null) {
            apartment.setFloor(request.floor());
        }
        apartmentRepository.save(apartment);

        log.info("Board member (residentId={}) updated apartmentId={}", actor, id);
        return toResponse(apartment);
    }

    @Transactional
    public void remove(String actor, Long id) {
        Apartment apartment = findApartment(id);
        // Deactivated residents still point at the apartment, so any resident blocks deletion.
        if (residentRepository.existsByApartmentId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Apartment has residents. Move or remove them first");
        }
        apartmentRepository.delete(apartment);

        log.info("Board member (residentId={}) removed apartmentId={}", actor, id);
    }

    private Apartment findApartment(Long id) {
        return apartmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Apartment not found"));
    }

    private void requireUnique(String building, String number) {
        if (apartmentRepository.existsByBuildingNameIgnoreCaseAndApartmentNumberIgnoreCase(building, number)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Apartment " + number + " already exists in " + building);
        }
    }

    private String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " must not be blank");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    field + " must be at most " + maxLength + " characters");
        }
        return trimmed;
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
