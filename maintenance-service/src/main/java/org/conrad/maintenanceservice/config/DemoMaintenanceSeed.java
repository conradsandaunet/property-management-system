package org.conrad.maintenanceservice.config;

import org.conrad.maintenanceservice.model.MaintenanceCategory;
import org.conrad.maintenanceservice.model.MaintenancePriority;
import org.conrad.maintenanceservice.model.MaintenanceRequest;
import org.conrad.maintenanceservice.model.MaintenanceStatus;
import org.conrad.maintenanceservice.model.MaintenanceWorker;
import org.conrad.maintenanceservice.repository.MaintenanceRequestRepository;
import org.conrad.maintenanceservice.repository.MaintenanceWorkerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.conrad.security.SeedLock;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Demo maintenance requests in every status. residentId refers to the residents seeded by
 * resident-service (ids 2-8), and the workers come from {@link WorkerSeed}.
 */
@Component
@Order(2)
@ConditionalOnProperty(name = "app.seed.demo-data", havingValue = "true", matchIfMissing = true)
public class DemoMaintenanceSeed implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoMaintenanceSeed.class);

    private final SeedLock seedLock;
    private final MaintenanceRequestRepository requestRepository;
    private final MaintenanceWorkerRepository workerRepository;

    public DemoMaintenanceSeed(SeedLock seedLock, MaintenanceRequestRepository requestRepository,
                               MaintenanceWorkerRepository workerRepository) {
        this.seedLock = seedLock;
        this.requestRepository = requestRepository;
        this.workerRepository = workerRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Several instances may start together; one seeds, the others wait and then find the data.
        seedLock.runExclusively(this::seed);
    }

    private void seed() {
        if (requestRepository.count() > 0) {
            return;
        }

        MaintenanceWorker per = worker("per@example.com");
        MaintenanceWorker jon = worker("jon@example.com");
        MaintenanceWorker ola = worker("ola@example.com");

        requestRepository.save(request(2L, MaintenanceCategory.PLUMBING, "Bad", MaintenancePriority.HIGH,
                "Det lekker vann fra røret under servanten.", MaintenanceStatus.SUBMITTED, null));
        requestRepository.save(request(5L, MaintenanceCategory.ELECTRICAL, "Stue", MaintenancePriority.MEDIUM,
                "Stikkontakten ved TV-en gir ikke strøm.", MaintenanceStatus.SUBMITTED, null));
        requestRepository.save(request(3L, MaintenanceCategory.HEATING, "Soverom", MaintenancePriority.MEDIUM,
                "Radiatoren blir ikke varm.", MaintenanceStatus.ASSIGNED, per));
        requestRepository.save(request(6L, MaintenanceCategory.APPLIANCE, "Kjøkken", MaintenancePriority.LOW,
                "Oppvaskmaskinen tømmer ikke vannet.", MaintenanceStatus.ASSIGNED, jon));
        requestRepository.save(request(7L, MaintenanceCategory.ELECTRICAL, "Gang", MaintenancePriority.HIGH,
                "Sikringen går hver gang vi bruker kjøkkenviften.", MaintenanceStatus.IN_PROGRESS, jon));
        requestRepository.save(request(8L, MaintenanceCategory.PLUMBING, "Toalett", MaintenancePriority.MEDIUM,
                "Toalettet renner kontinuerlig.", MaintenanceStatus.RESOLVED, per));
        requestRepository.save(request(4L, MaintenanceCategory.OTHER, "Trappeoppgang", MaintenancePriority.LOW,
                "Lyspæren i 2. etasje er ødelagt.", MaintenanceStatus.RESOLVED, ola));

        log.info("Seeded 7 demo maintenance requests");
    }

    private MaintenanceWorker worker(String email) {
        return workerRepository.findAll().stream()
                .filter(w -> email.equals(w.getEmail()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Demo worker missing: " + email));
    }

    private MaintenanceRequest request(Long residentId, MaintenanceCategory category, String location,
                                       MaintenancePriority priority, String description,
                                       MaintenanceStatus status, MaintenanceWorker worker) {
        MaintenanceRequest r = new MaintenanceRequest();
        r.setResidentId(residentId);
        r.setCategory(category);
        r.setLocation(location);
        r.setPriority(priority);
        r.setDescription(description);
        r.setStatus(status);
        r.setAssignedWorker(worker);
        return r;
    }
}
