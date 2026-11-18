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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Demo apartments and residents, so the app isn't empty on first start. Runs after
 * {@link AdminSeedConfig}, which creates the board account in apartment 101.
 *
 * Other services' demo data refers to the ids created here (apartment 1-6, resident 1-8). They are
 * predictable because everything is inserted in this order into an empty database; that is why
 * seeding is skipped unless only the board account exists.
 */
@Component
@Order(2)
@ConditionalOnProperty(name = "app.seed.demo-data", havingValue = "true", matchIfMissing = true)
public class DemoResidentSeed implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoResidentSeed.class);
    static final String BUILDING = "Solsiden Borettslag";

    private final ResidentRepository residentRepository;
    private final ApartmentRepository apartmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final String residentPassword;

    public DemoResidentSeed(
            ResidentRepository residentRepository,
            ApartmentRepository apartmentRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.seed.resident-password}") String residentPassword
    ) {
        this.residentRepository = residentRepository;
        this.apartmentRepository = apartmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.residentPassword = residentPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (residentRepository.count() != 1 || apartmentRepository.count() != 1) {
            return;
        }

        // Apartment 1 (101) already exists for the board account: ids 2-6 are the new ones.
        List<Apartment> apartments = new ArrayList<>();
        apartments.add(apartment("102", 1));
        apartments.add(apartment("201", 2));
        apartments.add(apartment("202", 2));
        apartments.add(apartment("301", 3));
        apartments.add(apartment("302", 3));
        apartmentRepository.saveAll(apartments);

        residentRepository.save(resident("Kari", "Nordmann", "kari@example.com", "91000001", apartments.get(0)));
        residentRepository.save(resident("Ola", "Hansen", "ola@example.com", "91000002", apartments.get(1)));
        residentRepository.save(resident("Ingrid", "Hansen", "ingrid@example.com", "91000003", apartments.get(1)));
        residentRepository.save(resident("Lars", "Berg", "lars@example.com", "91000004", apartments.get(2)));
        residentRepository.save(resident("Sofie", "Larsen", "sofie@example.com", "91000005", apartments.get(3)));
        residentRepository.save(resident("Emil", "Dahl", "emil@example.com", "91000006", apartments.get(3)));
        residentRepository.save(resident("Nora", "Vik", "nora@example.com", "91000007", apartments.get(4)));

        log.info("Seeded 5 demo apartments and 7 demo residents (password: {})", residentPassword);
    }

    private Apartment apartment(String number, int floor) {
        return Apartment.builder().apartmentNumber(number).floor(floor).buildingName(BUILDING).build();
    }

    private Resident resident(String first, String last, String email, String phone, Apartment apartment) {
        return Resident.builder()
                .firstName(first)
                .lastName(last)
                .email(email)
                .phone(phone)
                .passwordHash(passwordEncoder.encode(residentPassword))
                .manager(false)
                .apartment(apartment)
                .build();
    }
}
