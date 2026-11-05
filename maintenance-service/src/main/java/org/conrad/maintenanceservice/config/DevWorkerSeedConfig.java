package org.conrad.maintenanceservice.config;

import org.conrad.maintenanceservice.model.MaintenanceCategory;
import org.conrad.maintenanceservice.model.MaintenanceWorker;
import org.conrad.maintenanceservice.repository.MaintenanceWorkerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
public class DevWorkerSeedConfig implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevWorkerSeedConfig.class);

    private final MaintenanceWorkerRepository workerRepository;
    private final PasswordEncoder passwordEncoder;
    private final String seedPassword;

    public DevWorkerSeedConfig(
            MaintenanceWorkerRepository workerRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.seed.worker-password}") String seedPassword
    ) {
        this.workerRepository = workerRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedPassword = seedPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (workerRepository.count() == 0) {
            workerRepository.save(worker("Per Rørlegger", "90000001", "per@example.com",
                    EnumSet.of(MaintenanceCategory.PLUMBING, MaintenanceCategory.HEATING)));
            workerRepository.save(worker("Jon Elektriker", "90000002", "jon@example.com",
                    EnumSet.of(MaintenanceCategory.ELECTRICAL, MaintenanceCategory.APPLIANCE)));
            workerRepository.save(worker("Ola Altmuligman", "90000003", "ola@example.com",
                    EnumSet.allOf(MaintenanceCategory.class)));

            log.info("Seeded 3 demo maintenance workers");
            return;
        }

        // Workers seeded before login existed have no password yet.
        workerRepository.findAll().stream()
                .filter(w -> w.getPasswordHash() == null)
                .forEach(w -> {
                    w.setPasswordHash(passwordEncoder.encode(seedPassword));
                    workerRepository.save(w);
                    log.info("Set seed password for existing worker email={}", w.getEmail());
                });
    }

    private MaintenanceWorker worker(String name, String phone, String email,
                                     Set<MaintenanceCategory> specialties) {
        MaintenanceWorker w = new MaintenanceWorker();
        w.setName(name);
        w.setPhone(phone);
        w.setEmail(email);
        w.setPasswordHash(passwordEncoder.encode(seedPassword));
        w.setSpecialties(specialties);
        return w;
    }
}
