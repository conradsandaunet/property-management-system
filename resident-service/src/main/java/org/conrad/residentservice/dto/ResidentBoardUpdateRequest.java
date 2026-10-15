package org.conrad.residentservice.dto;

public record ResidentBoardUpdateRequest(
        String firstName,
        String lastName,
        String email,
        String phone,
        Boolean manager,
        Boolean active,
        Long apartmentId
) {
}
