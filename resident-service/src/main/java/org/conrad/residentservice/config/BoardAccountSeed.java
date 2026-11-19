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
import org.conrad.security.SeedLock;
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

    private final SeedLock seedLock;
    private final ResidentRepository residentRepository;
    private final ApartmentRepository apartmentRepository;
    private final BuildingRepository buildingRepository;
    private final PasswordEncoder passwordEncoder;
    private final String seedPassword;

    public BoardAccountSeed(
            SeedLock seedLock,
            ResidentRepository residentRepository,
            ApartmentRepository apartmentRepository,
            BuildingRepository buildingRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.seed.board-password}") String seedPassword
    ) {
        this.seedLock = seedLock;
        this.residentRepository = residentRepository;
        this.apartmentRepository = apartmentRepository;
        this.buildingRepository = buildingRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedPassword = seedPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Several instances may start together; one seeds, the others wait and then find the data.
        seedLock.runExclusively(this::seed);
    }

    private void seed() {
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

        // The password comes from BOARD_SEED_PASSWORD in .env, so it is deliberately not logged.
        log.info("Seeded initial board account: email={}", SEED_EMAIL);
    }
}
