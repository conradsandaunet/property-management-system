package org.conrad.maintenanceservice.dto;

import org.conrad.maintenanceservice.model.MaintenanceCategory;
import org.conrad.maintenanceservice.model.MaintenancePriority;
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
        LocalDateTime createdAt
) {}
