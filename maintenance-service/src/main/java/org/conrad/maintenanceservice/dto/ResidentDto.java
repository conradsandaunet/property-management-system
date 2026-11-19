package org.conrad.maintenanceservice.dto;

public record ResidentDto(
        Long id,
        Long apartmentId,
        String apartmentNumber,
        String buildingName
) {
}
