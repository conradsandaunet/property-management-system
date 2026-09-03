package org.conrad.residentservice.web;

import org.conrad.residentservice.dto.ResidentCreateRequest;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/residents")
public class ResidentController {

    private static final Logger log = LoggerFactory.getLogger(ResidentController.class);

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

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String createdBy = authentication != null ? authentication.getName() : "unknown";
        log.info("Board member (residentId={}) created new resident email={}, manager={}, apartmentId={}",
                createdBy, resident.getEmail(), resident.isManager(), request.apartmentId());

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
