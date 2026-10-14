package org.conrad.maintenanceservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.conrad.maintenanceservice.model.MaintenanceCategory;
import org.conrad.maintenanceservice.model.MaintenancePriority;

public record MaintenanceCreateRequest(
        @NotNull MaintenanceCategory category,
        @NotBlank @Size(max = 100) String location,
        @NotBlank String description,
        @NotNull MaintenancePriority priority
) {}
