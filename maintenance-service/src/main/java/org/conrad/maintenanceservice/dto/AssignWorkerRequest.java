package org.conrad.maintenanceservice.dto;

import jakarta.validation.constraints.NotNull;

public record AssignWorkerRequest(
        @NotNull Long workerId
) {}
