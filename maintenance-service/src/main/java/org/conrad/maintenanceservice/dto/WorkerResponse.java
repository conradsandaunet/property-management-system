package org.conrad.maintenanceservice.dto;

import org.conrad.maintenanceservice.model.MaintenanceCategory;
import org.conrad.maintenanceservice.model.MaintenanceWorker;

import java.util.Set;

public record WorkerResponse(
        Long id,
        String name,
        String phone,
        String email,
        Set<MaintenanceCategory> specialties
) {
    public static WorkerResponse from(MaintenanceWorker w) {
        return new WorkerResponse(
                w.getId(),
                w.getName(),
                w.getPhone(),
                w.getEmail(),
                w.getSpecialties()
        );
    }
}
