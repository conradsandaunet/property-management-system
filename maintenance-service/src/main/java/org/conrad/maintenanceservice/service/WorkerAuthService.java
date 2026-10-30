package org.conrad.maintenanceservice.service;

import org.conrad.maintenanceservice.model.MaintenanceWorker;
import org.conrad.maintenanceservice.repository.MaintenanceWorkerRepository;
import org.conrad.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class WorkerAuthService {

    private static final Logger log = LoggerFactory.getLogger(WorkerAuthService.class);

    private final MaintenanceWorkerRepository workerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public WorkerAuthService(MaintenanceWorkerRepository workerRepository, PasswordEncoder passwordEncoder,
                             JwtService jwtService) {
        this.workerRepository = workerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public Optional<String> login(String email, String password) {
        Optional<MaintenanceWorker> found = workerRepository.findByEmail(email)
                .filter(MaintenanceWorker::isActive)
                .filter(w -> w.getPasswordHash() != null)
                .filter(w -> password != null && passwordEncoder.matches(password, w.getPasswordHash()));

        if (found.isEmpty()) {
            log.warn("Failed worker login attempt for email={}", email);
            return Optional.empty();
        }

        MaintenanceWorker worker = found.get();
        log.info("Successful worker login for email={}", worker.getEmail());
        return Optional.of(jwtService.issueWorkerToken(worker.getId(), worker.getEmail()));
    }
}
