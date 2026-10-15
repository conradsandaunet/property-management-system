package org.conrad.residentservice.dto;

public record MeResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        boolean manager,
        boolean active,
        Long apartmentId,
        String apartmentNumber,
        Integer floor,
        String buildingName
) {
}
