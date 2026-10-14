package org.conrad.maintenanceservice.dto;

import org.conrad.maintenanceservice.model.MaintenanceCategory;
import org.conrad.maintenanceservice.model.MaintenancePriority;
import org.conrad.maintenanceservice.model.MaintenanceRequest;
import org.conrad.maintenanceservice.model.MaintenanceStatus;

import java.time.LocalDateTime;

public record MaintenanceResponse(
        Long id,
        Long residentId,
        MaintenanceCategory category,
        String location,
        String description,
        MaintenancePriority priority,
        MaintenanceStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static MaintenanceResponse from(MaintenanceRequest r) {
        return new MaintenanceResponse(
                r.getId(),
                r.getResidentId(),
                r.getCategory(),
                r.getLocation(),
                r.getDescription(),
                r.getPriority(),
                r.getStatus(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
