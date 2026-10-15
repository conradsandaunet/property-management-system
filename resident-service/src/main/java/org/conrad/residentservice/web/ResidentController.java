package org.conrad.residentservice.web;

import org.conrad.residentservice.dto.HouseholdMemberResponse;
import org.conrad.residentservice.dto.ResidentBoardUpdateRequest;
import org.conrad.residentservice.dto.ResidentCreateRequest;
import org.conrad.residentservice.dto.ResidentSummaryResponse;
import org.conrad.residentservice.dto.ResidentUpdateRequest;
import org.conrad.residentservice.model.Apartment;
import org.conrad.residentservice.model.Resident;
import org.conrad.residentservice.repository.ApartmentRepository;
import org.conrad.residentservice.repository.ResidentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.http.HttpStatus;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/residents")
public class ResidentController {

    private static final Logger log = LoggerFactory.getLogger(ResidentController.class);
    private static final int MAX_PHONE_LENGTH = 20;

    private final ResidentRepository residentRepository;
    private final ApartmentRepository apartmentRepository;
    private final PasswordEncoder passwordEncoder;

    public ResidentController(ResidentRepository residentRepository, ApartmentRepository apartmentRepository, PasswordEncoder passwordEncoder) {
        this.residentRepository = residentRepository;
        this.apartmentRepository = apartmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping
    @PreAuthorize("hasRole('BOARD')")
    public ResponseEntity<Void> create(@RequestBody ResidentCreateRequest request) {
        Apartment apartment = apartmentRepository.findById(request.apartmentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Apartment not found"));

        Resident resident = Resident.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .phone(request.phone())
                .passwordHash(passwordEncoder.encode(request.password()))
                .manager(request.manager())
                .apartment(apartment)
                .build();

        residentRepository.save(resident);

        log.info("Board member (residentId={}) created new resident email={}, manager={}, apartmentId={}",
                currentUserName(), resident.getEmail(), resident.isManager(), request.apartmentId());

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/household")
    public List<HouseholdMemberResponse> household() {
        Resident me = currentResident();
        return residentRepository.findByApartmentIdAndActiveTrue(me.getApartment().getId()).stream()
                .sorted(Comparator.comparing(Resident::getLastName).thenComparing(Resident::getFirstName))
                .map(r -> new HouseholdMemberResponse(r.getId(), r.getFirstName(), r.getLastName()))
                .toList();
    }

    @PatchMapping("/me")
    public ResidentSummaryResponse updateMe(@RequestBody ResidentUpdateRequest request) {
        Resident me = currentResident();
        applyEmail(me, request.email());
        applyPhone(me, request.phone());
        residentRepository.save(me);
        return toSummary(me);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('BOARD')")
    public ResidentSummaryResponse update(@PathVariable Long id, @RequestBody ResidentBoardUpdateRequest request) {
        Resident target = findResident(id);

        if (isCurrentUser(id) && (Boolean.FALSE.equals(request.manager()) || Boolean.FALSE.equals(request.active()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A board member cannot demote or deactivate themselves");
        }

        if (request.firstName() != null) {
            target.setFirstName(requireNotBlank(request.firstName(), "firstName"));
        }
        if (request.lastName() != null) {
            target.setLastName(requireNotBlank(request.lastName(), "lastName"));
        }
        applyEmail(target, request.email());
        applyPhone(target, request.phone());
        if (request.manager() != null) {
            target.setManager(request.manager());
        }
        if (request.active() != null) {
            target.setActive(request.active());
        }
        if (request.apartmentId() != null) {
            target.setApartment(apartmentRepository.findById(request.apartmentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Apartment not found")));
        }

        residentRepository.save(target);
        log.info("Board member (residentId={}) updated residentId={}", currentUserName(), id);
        return toSummary(target);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('BOARD')")
    public ResponseEntity<Void> remove(@PathVariable Long id) {
        if (isCurrentUser(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A board member cannot remove themselves");
        }

        Resident target = findResident(id);
        target.setActive(false);
        residentRepository.save(target);

        log.info("Board member (residentId={}) removed (deactivated) residentId={}", currentUserName(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('BOARD')")
    public List<ResidentSummaryResponse> search(@RequestParam String query,
                                                @RequestParam(defaultValue = "false") boolean includeInactive) {
        return residentRepository.search(query.trim(), includeInactive).stream()
                .map(this::toSummary)
                .toList();
    }

    private Resident currentResident() {
        Long id;
        try {
            id = Long.valueOf(currentUserName());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only residents can use this endpoint");
        }
        Resident resident = findResident(id);
        if (!resident.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is inactive");
        }
        return resident;
    }

    private Resident findResident(Long id) {
        return residentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resident not found"));
    }

    private boolean isCurrentUser(Long id) {
        return String.valueOf(id).equals(currentUserName());
    }

    private String currentUserName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "unknown";
    }

    private void applyEmail(Resident resident, String newEmail) {
        if (newEmail == null) {
            return;
        }
        String email = requireNotBlank(newEmail, "email");
        if (!email.equals(resident.getEmail())) {
            if (residentRepository.existsByEmail(email)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already in use");
            }
            resident.setEmail(email);
        }
    }

    private void applyPhone(Resident resident, String newPhone) {
        if (newPhone == null) {
            return;
        }
        String phone = newPhone.trim();
        if (phone.length() > MAX_PHONE_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "phone must be at most " + MAX_PHONE_LENGTH + " characters");
        }
        resident.setPhone(phone.isEmpty() ? null : phone);
    }

    private String requireNotBlank(String value, String field) {
        if (value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " must not be blank");
        }
        return value.trim();
    }

    private ResidentSummaryResponse toSummary(Resident resident) {
        Apartment apartment = resident.getApartment();
        return new ResidentSummaryResponse(
                resident.getId(),
                resident.getFirstName(),
                resident.getLastName(),
                resident.getEmail(),
                resident.getPhone(),
                resident.isManager(),
                resident.isActive(),
                apartment.getId(),
                apartment.getApartmentNumber()
        );
    }
}
