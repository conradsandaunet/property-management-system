package org.conrad.maintenanceservice.service;

import org.conrad.maintenanceservice.dto.WorkerCreateRequest;
import org.conrad.maintenanceservice.model.MaintenanceCategory;
import org.conrad.maintenanceservice.model.MaintenanceWorker;
import org.conrad.maintenanceservice.repository.MaintenanceWorkerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;

@Service
public class WorkerService {

    private static final String PASSWORD_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int PASSWORD_LENGTH = 12;

    public record CreatedWorker(MaintenanceWorker worker, String temporaryPassword) {
    }

    private final MaintenanceWorkerRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public WorkerService(MaintenanceWorkerRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CreatedWorker create(WorkerCreateRequest request) {
        if (repository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A worker with this email already exists");
        }

        String temporaryPassword = generatePassword();

        MaintenanceWorker worker = new MaintenanceWorker();
        worker.setName(request.name());
        worker.setPhone(request.phone());
        worker.setEmail(request.email());
        worker.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        worker.setSpecialties(new HashSet<>(request.specialties()));
        return new CreatedWorker(repository.save(worker), temporaryPassword);
    }

    @Transactional(readOnly = true)
    public List<MaintenanceWorker> listActive(MaintenanceCategory category) {
        return category == null
                ? repository.findByActiveTrueOrderByNameAsc()
                : repository.findByActiveTrueAndSpecialtiesContainingOrderByNameAsc(category);
    }

    private String generatePassword() {
        StringBuilder sb = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            sb.append(PASSWORD_ALPHABET.charAt(random.nextInt(PASSWORD_ALPHABET.length())));
        }
        return sb.toString();
    }
}
