package org.conrad.residentservice.dto;

public record ResidentSummaryResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        boolean manager,
        boolean active,
        Long apartmentId,
        String apartmentNumber
) {
}
