package org.conrad.residentservice.service;

import org.conrad.residentservice.dto.MeResponse;
import org.conrad.residentservice.model.Apartment;
import org.conrad.residentservice.model.Resident;
import org.conrad.residentservice.repository.ResidentRepository;
import org.conrad.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final ResidentRepository residentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(ResidentRepository residentRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.residentRepository = residentRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public Optional<String> login(String email, String password) {
        Optional<Resident> found = residentRepository.findByEmail(email)
                .filter(Resident::isActive)
                .filter(r -> passwordEncoder.matches(password, r.getPasswordHash()));

        if (found.isEmpty()) {
            log.warn("Failed login attempt for email={}", email);
            return Optional.empty();
        }

        Resident resident = found.get();
        log.info("Successful login for email={}", resident.getEmail());
        return Optional.of(jwtService.issueToken(resident.getId().toString(), resident.getEmail(),
                resident.isManager(), resident.getApartment().getId()));
    }

    @Transactional(readOnly = true)
    public MeResponse getMe(Long residentId) {
        Resident resident = residentRepository.findById(residentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resident not found"));

        Apartment apartment = resident.getApartment();
        return new MeResponse(
                resident.getId(),
                resident.getFirstName(),
                resident.getLastName(),
                resident.getEmail(),
                resident.getPhone(),
                resident.isManager(),
                resident.isActive(),
                apartment.getId(),
                apartment.getApartmentNumber(),
                apartment.getFloor(),
                apartment.getBuildingName()
        );
    }
}
