package org.conrad.residentservice.web;

import org.conrad.residentservice.dto.HouseholdMemberResponse;
import org.conrad.residentservice.dto.ResidentBoardUpdateRequest;
import org.conrad.residentservice.dto.ResidentCreateRequest;
import org.conrad.residentservice.dto.ResidentSummaryResponse;
import org.conrad.residentservice.dto.ResidentUpdateRequest;
import org.conrad.residentservice.service.ResidentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/residents")
public class ResidentController {

    private final ResidentService residentService;

    public ResidentController(ResidentService residentService) {
        this.residentService = residentService;
    }

    @PostMapping
    @PreAuthorize("hasRole('BOARD')")
    public ResponseEntity<Void> create(@RequestBody ResidentCreateRequest request) {
        residentService.create(CurrentUser.name(), request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/household")
    public List<HouseholdMemberResponse> household() {
        return residentService.household(CurrentUser.requireResidentId());
    }

    @PatchMapping("/me")
    public ResidentSummaryResponse updateMe(@RequestBody ResidentUpdateRequest request) {
        return residentService.updateOwnContact(CurrentUser.requireResidentId(), request);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('BOARD')")
    public ResidentSummaryResponse update(@PathVariable Long id, @RequestBody ResidentBoardUpdateRequest request) {
        return residentService.update(CurrentUser.name(), id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('BOARD')")
    public ResponseEntity<Void> remove(@PathVariable Long id) {
        residentService.remove(CurrentUser.name(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('BOARD')")
    public List<ResidentSummaryResponse> search(@RequestParam String query,
                                                @RequestParam(defaultValue = "false") boolean includeInactive) {
        return residentService.search(query, includeInactive);
    }
}
