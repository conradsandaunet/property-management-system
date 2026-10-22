package org.conrad.maintenanceservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.conrad.maintenanceservice.model.MaintenanceCategory;

import java.util.Set;

public record WorkerCreateRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 20) String phone,
        @Email @Size(max = 255) String email,
        @NotEmpty Set<MaintenanceCategory> specialties
) {}
