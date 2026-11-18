package org.conrad.residentservice.web;

import org.conrad.residentservice.dto.BuildingRequest;
import org.conrad.residentservice.dto.BuildingResponse;
import org.conrad.residentservice.model.BuildingFacade;
import org.conrad.residentservice.service.BuildingService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

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

    // Residents see the facade of their own building on their home page, so reading is open to
    // everyone who is logged in. Changing it is board only (class-level rule).
    @GetMapping("/{id}/facade")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> facade(@PathVariable Long id) {
        BuildingFacade facade = buildingService.getFacade(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(facade.getContentType()))
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore())
                .body(facade.getData());
    }

    @PutMapping(path = "/{id}/facade", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> setFacade(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        try {
            buildingService.setFacade(CurrentUser.name(), id, file.getOriginalFilename(), file.getBytes());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Couldn't read the file");
        }
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/facade")
    public ResponseEntity<Void> removeFacade(@PathVariable Long id) {
        buildingService.removeFacade(CurrentUser.name(), id);
        return ResponseEntity.noContent().build();
    }
}
