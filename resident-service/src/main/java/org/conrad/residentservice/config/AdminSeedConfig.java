package org.conrad.residentservice.config;

import org.conrad.residentservice.model.Apartment;
import org.conrad.residentservice.model.Resident;
import org.conrad.residentservice.repository.ApartmentRepository;
import org.conrad.residentservice.repository.ResidentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeedConfig implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeedConfig.class);
    private static final String SEED_EMAIL = "board@example.com";

    private final ResidentRepository residentRepository;
    private final ApartmentRepository apartmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final String seedPassword;

    public AdminSeedConfig(
            ResidentRepository residentRepository,
            ApartmentRepository apartmentRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.seed.board-password}") String seedPassword
    ) {
        this.residentRepository = residentRepository;
        this.apartmentRepository = apartmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedPassword = seedPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (residentRepository.count() > 0) {
            return;
        }

        Apartment apartment = Apartment.builder()
                .apartmentNumber("SEED-1")
                .floor(0)
                .buildingName("Seed Building")
                .build();
        apartmentRepository.save(apartment);

        Resident board = Resident.builder()
                .firstName("Board")
                .lastName("Admin")
                .email(SEED_EMAIL)
                .passwordHash(passwordEncoder.encode(seedPassword))
                .manager(true)
                .apartment(apartment)
                .build();
        residentRepository.save(board);

        log.info("Seeded initial board account: email={}, password={}", SEED_EMAIL, seedPassword);
    }
}
