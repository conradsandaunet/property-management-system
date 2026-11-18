package org.conrad.residentservice.config;

import org.conrad.residentservice.model.Apartment;
import org.conrad.residentservice.model.Building;
import org.conrad.residentservice.model.Resident;
import org.conrad.residentservice.repository.ApartmentRepository;
import org.conrad.residentservice.repository.BuildingRepository;
import org.conrad.residentservice.repository.ResidentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class BoardAccountSeed implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BoardAccountSeed.class);
    private static final String SEED_EMAIL = "board@example.com";
    static final String BUILDING = "Solsiden Borettslag";

    private final ResidentRepository residentRepository;
    private final ApartmentRepository apartmentRepository;
    private final BuildingRepository buildingRepository;
    private final PasswordEncoder passwordEncoder;
    private final String seedPassword;

    public BoardAccountSeed(
            ResidentRepository residentRepository,
            ApartmentRepository apartmentRepository,
            BuildingRepository buildingRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.seed.board-password}") String seedPassword
    ) {
        this.residentRepository = residentRepository;
        this.apartmentRepository = apartmentRepository;
        this.buildingRepository = buildingRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedPassword = seedPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (residentRepository.count() > 0) {
            return;
        }

        Building building = buildingRepository.findByNameIgnoreCase(BUILDING)
                .orElseGet(() -> buildingRepository.save(Building.builder().name(BUILDING).build()));

        Apartment apartment = Apartment.builder()
                .apartmentNumber("101")
                .floor(1)
                .building(building)
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
