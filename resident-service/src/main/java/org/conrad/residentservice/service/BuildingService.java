package org.conrad.residentservice.service;

import org.conrad.residentservice.dto.BuildingRequest;
import org.conrad.residentservice.dto.BuildingResponse;
import org.conrad.residentservice.model.Building;
import org.conrad.residentservice.repository.BuildingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@Service
public class BuildingService {

    private static final Logger log = LoggerFactory.getLogger(BuildingService.class);
    private static final int MAX_NAME_LENGTH = 255;

    private final BuildingRepository buildingRepository;

    public BuildingService(BuildingRepository buildingRepository) {
        this.buildingRepository = buildingRepository;
    }

    @Transactional(readOnly = true)
    public List<BuildingResponse> getAll() {
        return buildingRepository.findAll().stream()
                .sorted(Comparator.comparing(Building::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toResponse)
                .toList();
    }

    /** For other services in this module: the building, or 404. */
    @Transactional(readOnly = true)
    public Building require(Long id) {
        return buildingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Building not found"));
    }

    @Transactional
    public BuildingResponse create(String actor, BuildingRequest request) {
        String name = requireName(request.name());
        requireUnique(name);

        Building building = buildingRepository.save(Building.builder().name(name).build());

        log.info("Board member (residentId={}) created buildingId={} name={}", actor, building.getId(), name);
        return toResponse(building);
    }

    @Transactional
    public BuildingResponse update(String actor, Long id, BuildingRequest request) {
        Building building = require(id);
        String name = requireName(request.name());
        if (!name.equalsIgnoreCase(building.getName())) {
            requireUnique(name);
        }
        building.setName(name);
        buildingRepository.save(building);

        log.info("Board member (residentId={}) renamed buildingId={} to {}", actor, id, name);
        return toResponse(building);
    }

    @Transactional
    public void remove(String actor, Long id) {
        Building building = require(id);
        if (!building.getApartments().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Building has apartments. Move or remove them first");
        }
        buildingRepository.delete(building);

        log.info("Board member (residentId={}) removed buildingId={}", actor, id);
    }

    private void requireUnique(String name) {
        if (buildingRepository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A building named " + name + " already exists");
        }
    }

    private String requireName(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name must not be blank");
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "name must be at most " + MAX_NAME_LENGTH + " characters");
        }
        return trimmed;
    }

    private BuildingResponse toResponse(Building building) {
        return new BuildingResponse(building.getId(), building.getName(), building.getApartments().size());
    }
}
