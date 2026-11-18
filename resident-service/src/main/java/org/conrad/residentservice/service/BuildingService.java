package org.conrad.residentservice.service;

import org.conrad.residentservice.dto.BuildingRequest;
import org.conrad.residentservice.dto.BuildingResponse;
import org.conrad.residentservice.model.Building;
import org.conrad.residentservice.model.BuildingFacade;
import org.conrad.residentservice.repository.BuildingFacadeRepository;
import org.conrad.residentservice.repository.BuildingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class BuildingService {

    private static final Logger log = LoggerFactory.getLogger(BuildingService.class);
    private static final int MAX_NAME_LENGTH = 255;

    private final BuildingRepository buildingRepository;
    private final BuildingFacadeRepository facadeRepository;

    public BuildingService(BuildingRepository buildingRepository, BuildingFacadeRepository facadeRepository) {
        this.buildingRepository = buildingRepository;
        this.facadeRepository = facadeRepository;
    }

    @Transactional(readOnly = true)
    public List<BuildingResponse> getAll() {
        Set<Long> withFacade = facadeRepository.findBuildingIds();
        return buildingRepository.findAll().stream()
                .sorted(Comparator.comparing(Building::getName, String.CASE_INSENSITIVE_ORDER))
                .map(b -> toResponse(b, withFacade.contains(b.getId())))
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

    // ---- facade photo ----

    @Transactional(readOnly = true)
    public BuildingFacade getFacade(Long buildingId) {
        return facadeRepository.findById(buildingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Building has no facade photo"));
    }

    /** Sets or replaces the facade photo. Same checks as apartment photos (type from the bytes, size). */
    @Transactional
    public void setFacade(String actor, Long buildingId, String fileName, byte[] data) {
        require(buildingId);
        if (data == null || data.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The file is empty");
        }
        if (data.length > ApartmentPhotoService.MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "The photo must be at most 5 MB");
        }
        String contentType = ApartmentPhotoService.sniffImageType(data);
        if (contentType == null) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only JPEG, PNG and WebP images are allowed");
        }
        facadeRepository.save(BuildingFacade.builder()
                .buildingId(buildingId)
                .fileName(fileName == null || fileName.isBlank() ? "facade" : fileName)
                .contentType(contentType)
                .sizeBytes(data.length)
                .data(data)
                .build());
        log.info("Board member (residentId={}) set the facade photo of buildingId={}", actor, buildingId);
    }

    @Transactional
    public void removeFacade(String actor, Long buildingId) {
        facadeRepository.delete(getFacade(buildingId));
        log.info("Board member (residentId={}) removed the facade photo of buildingId={}", actor, buildingId);
    }

    private BuildingResponse toResponse(Building building) {
        return toResponse(building, building.getId() != null && facadeRepository.existsById(building.getId()));
    }

    private BuildingResponse toResponse(Building building, boolean hasFacade) {
        return new BuildingResponse(building.getId(), building.getName(), building.getApartments().size(), hasFacade);
    }
}
