package org.conrad.maintenanceservice.dto;

import jakarta.validation.constraints.NotNull;
import org.conrad.maintenanceservice.model.MaintenanceStatus;

public record StatusChangeRequest(
        @NotNull MaintenanceStatus status
) {}
