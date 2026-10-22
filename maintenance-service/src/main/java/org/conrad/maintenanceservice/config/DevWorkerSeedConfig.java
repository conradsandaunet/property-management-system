package org.conrad.maintenanceservice.config;

import org.conrad.maintenanceservice.model.MaintenanceCategory;
import org.conrad.maintenanceservice.model.MaintenanceWorker;
import org.conrad.maintenanceservice.repository.MaintenanceWorkerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
public class DevWorkerSeedConfig implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevWorkerSeedConfig.class);

    private final MaintenanceWorkerRepository workerRepository;

    public DevWorkerSeedConfig(MaintenanceWorkerRepository workerRepository) {
        this.workerRepository = workerRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (workerRepository.count() > 0) {
            return;
        }
        workerRepository.save(worker("Per Rørlegger", "90000001", "per@example.com",
                EnumSet.of(MaintenanceCategory.PLUMBING, MaintenanceCategory.HEATING)));
        workerRepository.save(worker("Jon Elektriker", "90000002", "jon@example.com",
                EnumSet.of(MaintenanceCategory.ELECTRICAL, MaintenanceCategory.APPLIANCE)));
        workerRepository.save(worker("Ola Altmuligman", "90000003", "ola@example.com",
                EnumSet.allOf(MaintenanceCategory.class)));

        log.info("Seeded 3 demo maintenance workers");
    }

    private MaintenanceWorker worker(String name, String phone, String email,
                                     Set<MaintenanceCategory> specialties) {
        MaintenanceWorker w = new MaintenanceWorker();
        w.setName(name);
        w.setPhone(phone);
        w.setEmail(email);
        w.setSpecialties(specialties);
        return w;
    }
}
