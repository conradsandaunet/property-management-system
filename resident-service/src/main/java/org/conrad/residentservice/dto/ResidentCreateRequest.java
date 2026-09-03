package org.conrad.residentservice.dto;

public record ResidentCreateRequest(
        String firstName,
        String lastName,
        String email,
        String phone,
        String password,
        boolean manager,
        Long apartmentId
) {
}
