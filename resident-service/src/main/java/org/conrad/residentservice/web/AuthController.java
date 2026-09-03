package org.conrad.residentservice.web;

import org.conrad.residentservice.dto.LoginRequest;
import org.conrad.residentservice.dto.LoginResponse;
import org.conrad.residentservice.model.Resident;
import org.conrad.residentservice.repository.ResidentRepository;
import org.conrad.residentservice.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final ResidentRepository residentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(ResidentRepository residentRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.residentRepository = residentRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        Resident resident = residentRepository.findByEmail(request.email())
                .filter(r -> passwordEncoder.matches(request.password(), r.getPasswordHash()))
                .orElse(null);

        if (resident == null) {
            log.warn("Failed login attempt for email={}", request.email());
            return ResponseEntity.status(401).build();
        }

        log.info("Successful login for email={}", resident.getEmail());
        return ResponseEntity.ok(new LoginResponse(jwtService.issueToken(resident)));
    }
}
