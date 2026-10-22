package org.conrad.maintenanceservice.dto;

import org.conrad.maintenanceservice.model.*;

import java.time.LocalDateTime;

public record MaintenanceResponse(
        Long id,
        Long residentId,
        MaintenanceCategory category,
        String location,
        String description,
        MaintenancePriority priority,
        MaintenanceStatus status,
        Long assignedWorkerId,
        String assignedWorkerName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static MaintenanceResponse from(MaintenanceRequest r) {
        MaintenanceWorker w = r.getAssignedWorker();
        return new MaintenanceResponse(
                r.getId(),
                r.getResidentId(),
                r.getCategory(),
                r.getLocation(),
                r.getDescription(),
                r.getPriority(),
                r.getStatus(),
                w != null ? w.getId() : null,
                w != null ? w.getName() : null,
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
